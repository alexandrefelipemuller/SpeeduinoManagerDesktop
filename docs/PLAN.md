# Plano de criacao - SpeeduinoManager Desktop

## Visao geral

Criar um novo projeto Kotlin Multiplatform com alvo desktop Linux, reaproveitando o core do app Android existente. O objetivo e manter paridade de protocolo e modelos, com UI desktop separada.

## Fases

### Fase 1 - Bootstrap do projeto KMP

- Criar novo repositorio/projeto Gradle dentro de `SpeeduinoManagerDesktop/`.
- Configurar Gradle com Kotlin Multiplatform + Compose Multiplatform (desktop).
- Definir modulo compartilhado `shared` (codigo comum) e modulo `desktopApp` (UI).

### Fase 2 - Extracao do core compartilhado

- Identificar pacotes reutilizaveis do Android:
  - `protocol`
  - `connection` (TCP)
  - `model`
- Migrar para `shared` mantendo APIs estaveis.
- Ajustar dependencias Android-especificas para multiplataforma.

### Fase 3 - UI Desktop

- Implementar telas equivalentes ao Android:
  - Tela de conexao (host/porta)
  - Dashboard de telemetria
  - Logs
- Adicionar suporte a janela responsiva (desktop).

### Fase 4 - Integracao com simulador

- Garantir compatibilidade com `simulator/speeduino_tcp_simulator.py`.
- Documentar fluxo de teste local e exemplos de dados.

### Fase 5 - Qualidade e distribuicao

- Configurar testes unitarios para o modulo `shared`.
- Definir pipeline de build (local) e empacotamento (deb ou tar).

## Decisoes e premissas

- Alvo inicial: Linux x64.
- Windows sera planejado apos estabilizar o desktop Linux.
- A base do protocolo e definicoes (arquivo `reference/`) permanece a mesma do Android.

## Riscos

- Dependencias Android-only no core atual.
- Diferencas de threading/coroutines em desktop.
- Necessidade de adaptar log e storage.
