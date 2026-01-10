import serial
import serial.tools.list_ports
import socket
import threading
import sys
import time

# --- Configurações Padrão ---
BAUDRATE = 115200 # Ajuste conforme o seu dispositivo
TCP_HOST = '0.0.0.0'  # Escuta em todas as interfaces
TCP_PORT = 5558

# Variáveis globais para a conexão
ser = None
tcp_socket = None
client_address = None
is_running = True

def serial_to_tcp():
    """Lê da porta serial e envia para o socket TCP."""
    global is_running
    print(f"[{threading.current_thread().name}] Thread para leitura Serial iniciada.")
    try:
        while is_running and tcp_socket:
            if ser.in_waiting > 0:
                # Lê todos os bytes disponíveis
                data = ser.read(ser.in_waiting)
                
                # Envia os dados brutos para o socket TCP
                # Certifique-se de que os dados são do tipo bytes
                if data:
                    try:
                        tcp_socket.sendall(data)
                        sys.stdout.write(f"\r[Serial -> TCP] Enviado {len(data)} bytes para {client_address[0]}:{client_address[1]}")
                        sys.stdout.flush()
                    except socket.error as e:
                        print(f"\n[ERRO TCP] Falha ao enviar dados para o cliente: {e}")
                        break  # Sai do loop para fechar a conexão
            
            # Pequena pausa para evitar alto uso da CPU
            time.sleep(0.001)

    except serial.SerialException as e:
        print(f"\n[ERRO SERIAL] Falha de comunicação serial: {e}")
    except Exception as e:
        print(f"\n[ERRO] Ocorreu um erro na thread serial_to_tcp: {e}")
    finally:
        print(f"\n[{threading.current_thread().name}] Thread de leitura Serial finalizada.")
        # O encerramento da conexão será tratado no loop principal.


def handle_tcp_client(conn, addr):
    """Gerencia a conexão do cliente TCP."""
    global tcp_socket, client_address, is_running
    tcp_socket = conn
    client_address = addr

    print(f"\n[TCP] Conexão estabelecida com {addr[0]}:{addr[1]}")
    
    # Inicia a thread para enviar dados da Serial para o TCP
    serial_thread = threading.Thread(target=serial_to_tcp, name="SerialToTCP")
    serial_thread.daemon = True # Permite que o programa principal saia mesmo com a thread rodando
    serial_thread.start()

    try:
        while is_running:
            # Recebe dados do cliente TCP (buffer de 4096 bytes)
            data = conn.recv(4096)
            
            if not data:
                # Cliente desconectou
                break
            filtered_data = bytearray()
            i = 0
            while i < len(data):
                byte = data[i]
                if byte == 0xFF:  # 0xFF é o IAC (Interpret as Command)
                    # Se for IAC, pula os próximos 2 bytes que são o comando e a opção
                    i += 3 
                else:
                    filtered_data.append(byte)
                    i += 1
            
            data_to_write = bytes(filtered_data)
            # Envia os dados brutos recebidos do TCP para a porta Serial
            ser.write(data)
            sys.stdout.write(f"\r[TCP -> Serial] Recebido {len(data)} bytes de {addr[0]}:{addr[1]}")
            sys.stdout.flush()

    except socket.error as e:
        print(f"\n[ERRO TCP] Conexão com o cliente perdida: {e}")
    except serial.SerialException as e:
        print(f"\n[ERRO SERIAL] Falha ao escrever na porta serial: {e}")
    except Exception as e:
        print(f"\n[ERRO] Ocorreu um erro na thread handle_tcp_client: {e}")
    finally:
        # Limpeza da conexão TCP e encerramento da thread serial
        print(f"\n[TCP] Conexão com {addr[0]}:{addr[1]} encerrada. Fechando socket...")
        tcp_socket.close()
        tcp_socket = None
        client_address = None
        # A thread serial_to_tcp irá naturalmente terminar no próximo ciclo.

def listar_portas_com():
    """Lista todas as portas COM disponíveis no sistema."""
    portas = serial.tools.list_ports.comports()
    return sorted(portas, key=lambda p: p.device)

def exibir_menu_portas():
    """Exibe menu interativo para seleção da porta COM."""
    print("\n" + "="*60)
    print("  SPEEDUINO TCP PROXY - Configuração")
    print("="*60)

    portas = listar_portas_com()

    if not portas:
        print("\n[ERRO] Nenhuma porta COM detectada no sistema!")
        print("Verifique se o dispositivo está conectado.")
        return None

    print("\nPortas COM disponíveis:\n")
    for idx, porta in enumerate(portas, 1):
        descricao = porta.description or "Sem descrição"
        print(f"  [{idx}] {porta.device} - {descricao}")

    print(f"\n  [0] Sair")
    print("-"*60)

    while True:
        try:
            escolha = input("\nEscolha a porta COM [1-{}]: ".format(len(portas)))

            if escolha == '0':
                print("\n[SAINDO] Programa encerrado pelo usuário.")
                return None

            idx = int(escolha) - 1

            if 0 <= idx < len(portas):
                porta_selecionada = portas[idx].device
                print(f"\n✓ Porta selecionada: {porta_selecionada}")
                return porta_selecionada
            else:
                print(f"[ERRO] Opção inválida! Escolha entre 1 e {len(portas)}")

        except ValueError:
            print("[ERRO] Digite apenas números!")
        except KeyboardInterrupt:
            print("\n\n[SAINDO] Programa encerrado pelo usuário.")
            return None

def configurar_tcp():
    """Permite configurar a porta TCP (opcional)."""
    print("\n" + "-"*60)
    print("  Configuração TCP")
    print("-"*60)

    usar_padrao = input(f"\nUsar porta TCP padrão {TCP_PORT}? [S/n]: ").strip().lower()

    if usar_padrao in ['n', 'nao', 'não']:
        while True:
            try:
                porta_tcp = input("Digite a porta TCP desejada [1024-65535]: ").strip()
                porta_tcp = int(porta_tcp)

                if 1024 <= porta_tcp <= 65535:
                    print(f"✓ Porta TCP configurada: {porta_tcp}")
                    return porta_tcp
                else:
                    print("[ERRO] Porta deve estar entre 1024 e 65535!")

            except ValueError:
                print("[ERRO] Digite apenas números!")
            except KeyboardInterrupt:
                print("\n[CANCELADO] Usando porta padrão.")
                return TCP_PORT

    return TCP_PORT

def start_server(serial_port, tcp_port):
    """Inicia o servidor TCP e a comunicação Serial."""
    global ser, is_running

    try:
        # 1. Tenta abrir a porta serial
        ser = serial.Serial(serial_port, BAUDRATE, timeout=0) # timeout=0 (non-blocking read)
        print(f"\n[SERIAL] Porta {serial_port} aberta com sucesso (Baudrate: {BAUDRATE}).")
    except serial.SerialException as e:
        print(f"\n[ERRO FATAL] Não foi possível abrir a porta serial {serial_port}: {e}")
        return

    # 2. Configura o servidor TCP
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        server_socket.bind((TCP_HOST, tcp_port))
        server_socket.listen(1) # Aceita apenas 1 conexão por vez
        print(f"[TCP] Servidor escutando em {TCP_HOST}:{tcp_port}...")
        print("\n" + "="*60)
        print("  PROXY ATIVO - Aguardando conexões...")
        print("  Pressione CTRL+C para encerrar")
        print("="*60 + "\n")
    except socket.error as e:
        print(f"[ERRO FATAL] Falha ao iniciar o servidor TCP: {e}")
        ser.close()
        return

    # 3. Loop principal de aceitação de conexões
    try:
        while is_running:
            # Espera por uma conexão (bloqueante)
            # Define um timeout para que o loop possa checar 'is_running'
            server_socket.settimeout(1) 
            try:
                conn, addr = server_socket.accept()
                
                if tcp_socket:
                    print(f"\n[TCP] Recusando nova conexão de {addr[0]}:{addr[1]}. Já há um cliente conectado.")
                    conn.close()
                    continue
                    
                # Inicia o tratamento do cliente em uma thread
                client_handler = threading.Thread(target=handle_tcp_client, args=(conn, addr), name="TCPHandler")
                client_handler.daemon = True
                client_handler.start()

            except socket.timeout:
                # O timeout de 1 segundo expirou, verifica 'is_running'
                continue
            except Exception as e:
                if is_running:
                    print(f"\n[ERRO TCP] Falha ao aceitar conexão: {e}")
                break

    except KeyboardInterrupt:
        print("\n[INTERRUPÇÃO] Detectado CTRL+C. Encerrando...")
    finally:
        is_running = False # Sinaliza o encerramento das threads

        # 4. Limpeza final
        if ser and ser.is_open:
            print("[SERIAL] Fechando porta serial.")
            ser.close()
            
        print("[TCP] Fechando socket do servidor.")
        server_socket.close()
        print("[FIM] Túnel encerrado.")


if __name__ == '__main__':
    # Menu interativo
    porta_com = exibir_menu_portas()

    if not porta_com:
        sys.exit(0)

    porta_tcp = configurar_tcp()

    # Exibe resumo da configuração
    print("\n" + "="*60)
    print("  RESUMO DA CONFIGURAÇÃO")
    print("="*60)
    print(f"  Porta Serial: {porta_com}")
    print(f"  Baudrate:     {BAUDRATE}")
    print(f"  Servidor TCP: {TCP_HOST}:{porta_tcp}")
    print("="*60)

    input("\nPressione ENTER para iniciar o proxy...")

    # Inicia o servidor
    start_server(porta_com, porta_tcp)

