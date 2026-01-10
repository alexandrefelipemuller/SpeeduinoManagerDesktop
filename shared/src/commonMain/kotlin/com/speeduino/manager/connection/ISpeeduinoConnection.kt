package com.speeduino.manager.connection

/**
 * Interface abstrata para comunicação com Speeduino
 *
 * Permite implementações diversas de transporte (TCP, Bluetooth, USB Serial, etc)
 * sem alterar a lógica de protocolo ou UI.
 *
 * Implementações devem garantir:
 * - Thread-safety para operações de I/O
 * - Timeout adequado para evitar bloqueios
 * - Limpeza de recursos em close()
 */
interface ISpeeduinoConnection {

    /**
     * Conecta ao dispositivo Speeduino
     * @throws Exception se falhar
     */
    suspend fun connect()

    /**
     * Desconecta do dispositivo
     */
    fun disconnect()

    /**
     * Envia dados brutos
     * @param data Bytes a enviar
     * @throws Exception se não conectado ou erro de I/O
     */
    fun send(data: ByteArray)

    /**
     * Recebe dados brutos
     * @param size Número de bytes a ler (0 = ler até timeout)
     * @return Bytes recebidos
     * @throws Exception se não conectado ou erro de I/O
     */
    fun receive(size: Int = 0): ByteArray

    /**
     * Verifica se está conectado
     */
    fun isConnected(): Boolean

    /**
     * Obtém informações da conexão para debug
     */
    fun getConnectionInfo(): String

    /**
     * Indica se a conexão suporta o protocolo modern (CRC32-based)
     */
    fun supportsModernProtocol(): Boolean = true

    /**
     * Callback para mudança de estado de conexão
     */
    fun setOnConnectionStateChanged(callback: (Boolean) -> Unit)

    /**
     * Callback para erros de conexão
     */
    fun setOnError(callback: (String) -> Unit)

    /**
     * Limpa recursos da conexao.
     */
    fun close() {
        disconnect()
    }
}
