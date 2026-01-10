#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

OS_NAME="$(uname -s)"

case "$OS_NAME" in
  Darwin)
    ./gradlew :desktopApp:packageReleaseDmg
    ;;
  Linux)
    ./gradlew :desktopApp:packageReleaseDeb :desktopApp:packageReleaseAppImage
    ;;
  CYGWIN*|MINGW*|MSYS*)
    ./gradlew :desktopApp:packageReleaseExe
    ;;
  *)
    echo "Unsupported OS: $OS_NAME" >&2
    exit 1
    ;;
esac
