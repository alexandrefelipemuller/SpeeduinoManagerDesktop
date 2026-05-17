# Gerar instalador `.exe` no Windows

Este projeto consegue gerar um instalador Windows funcional usando **Inno Setup** sobre a distribuicao release do Compose Desktop.

## Status

- O fluxo `jpackage`/WiX do Compose Desktop **nao esta confiavel neste ambiente**.
- O instalador que funcionou foi gerado com **Inno Setup**.
- Arquivo final esperado:
  - `build/inno/SpeeduinoManagerDesktop-1.0.4-setup.exe`

## Pre-requisitos

- Windows
- JDK 17
- Inno Setup instalado

Opcao usada aqui:

1. Baixar o instalador oficial do Inno Setup
2. Instalar em pasta local sem depender de `Program Files`

Exemplo usado:

```powershell
$out='C:\tmp\innosetup-6.7.1.exe'
Invoke-WebRequest -Uri 'https://github.com/jrsoftware/issrc/releases/download/is-6_7_1/innosetup-6.7.1.exe' -OutFile $out
Start-Process -FilePath $out -ArgumentList '/VERYSILENT','/SUPPRESSMSGBOXES','/NORESTART','/CURRENTUSER','/DIR=C:\tmp\InnoSetup6' -Wait
```

Compilador esperado:

```text
C:\tmp\InnoSetup6\ISCC.exe
```

## Arquivos envolvidos

- Script do instalador:
  - `build/inno/SpeeduinoManagerDesktop.iss`
- Distribuicao release do app:
  - `desktopApp/build/compose/binaries/main-release/app/SpeeduinoManagerDesktop`
- Saida final:
  - `build/inno/SpeeduinoManagerDesktop-1.0.4-setup.exe`

## Passo a passo

1. Gerar a distribuicao release:

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-user-home'
.\gradlew.bat :desktopApp:createReleaseDistributable --no-daemon
```

2. Compilar o instalador Inno Setup:

```powershell
& 'C:\tmp\InnoSetup6\ISCC.exe' 'C:\Users\Alexandre\Documents\SpeeduinoManagerDesktop\build\inno\SpeeduinoManagerDesktop.iss'
```

3. Pegar o artefato final em:

```text
build/inno/SpeeduinoManagerDesktop-1.0.4-setup.exe
```

## Observacoes

- O script `.iss` atual instala em:

```text
%LOCALAPPDATA%\Programs\SpeeduinoManagerDesktop
```

- O instalador cria:
  - atalho no menu iniciar
  - opcao de atalho na area de trabalho
  - desinstalador

- Se a versao mudar, atualize no arquivo:
  - `build/inno/SpeeduinoManagerDesktop.iss`

Campos principais:

- `MyAppVersion`
- `MyAppSourceDir`
- `MyAppOutputDir`

## Nao usar como fluxo principal

Evitar, neste ambiente:

- `:desktopApp:packageExe`
- `jpackage --type exe`
- `jpackage --type msi`

Motivo:

- o `light.exe` do WiX esta falhando durante o empacotamento final, embora o app e a distribuicao sejam gerados normalmente.
