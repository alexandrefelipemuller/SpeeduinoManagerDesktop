# SpeeduinoManager Desktop (Linux)

Projeto desktop (Linux) em Kotlin Multiplatform baseado no SpeeduinoManager Android. O objetivo e reutilizar o maximo possivel de protocolo, modelos e conexoes, mantendo uma camada de UI desktop independente.

## Objetivo

- Reaproveitar o core de comunicacao e protocolo do projeto Android.
- Fornecer UI desktop leve para monitoramento e operacao basica.
- Manter evolucao sincronizada entre Android e Desktop.

## Escopo inicial

- Conexao TCP com simulador/ECU.
- Tela de monitoramento em tempo real (RPM, MAP, coolant, TPS, bateria, ignicao).
- Logs basicos de comunicacao e status de conexao.

## Documentacao

- Plano de criacao: `docs/PLAN.md`
- Instrucoes de build e execucao: `docs/BUILD.md`
- Diretrizes de arquitetura e modulos: `docs/ARCHITECTURE.md`

## Status

Bootstrap inicial criado com modulos `shared` e `desktopApp`.

Para executar:

```bash
cd SpeeduinoManagerDesktop
./gradlew :desktopApp:run
```
