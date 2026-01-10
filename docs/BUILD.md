# Build e execucao (planejado)

Este arquivo descreve o fluxo esperado para o projeto KMP desktop. Ajuste os comandos quando o projeto for criado.

## Pre-requisitos

- JDK 17+
- Kotlin Multiplatform + Compose Multiplatform (via Gradle)
- Python 3 (para simulador local)

## Build (desktop Linux)

Na pasta `SpeeduinoManagerDesktop`:

```bash
./gradlew :desktopApp:build
```

## Executar o app

```bash
./gradlew :desktopApp:run
```

## Empacotar distribuicao (Linux)

```bash
./gradlew :desktopApp:packageDistribution
```

## Empacotar distribuicao (Windows .exe)

Execute em uma maquina Windows:

```bash
./gradlew :desktopApp:packageExe
```

## Executar simulador

Na raiz do repo principal:

```bash
python3 simulator/speeduino_tcp_simulator.py --host 0.0.0.0 --port 5555
```

No app desktop, use:

- Host: `127.0.0.1`
- Porta: `5555`

## Troubleshooting rapido

- Verifique se a porta 5555 esta livre.
- Confirme que o JDK 17 esta ativo (`java -version`).
