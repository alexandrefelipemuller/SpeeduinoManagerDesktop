#!/usr/bin/env bash
# Cross-builds the desktop .deb for arm64 (Raspberry Pi) from an x86_64 dev
# machine, using Docker + QEMU user-mode emulation (binfmt). jpackage bundles
# the JRE of whatever machine runs it, so a native build here would produce
# an amd64 .deb that can't install on the Pi — this runs the whole Gradle
# build inside an emulated arm64 Debian container instead.
#
# Requires: `docker` with a working `linux/arm64` platform (i.e. qemu-user
# binfmt registered — most Docker Desktop / Docker Engine installs with
# qemu-user-static have this out of the box; test with:
#   docker run --rm --platform linux/arm64 arm64v8/debian:trixie uname -m
#
# Usage: ./scripts/build-arm64-deb.sh
# Output: desktopApp/build/compose/binaries/main-release/deb/*_arm64.deb
set -euo pipefail

cd "$(dirname "$0")/.."
REPO_ROOT="$(pwd)"
GRADLE_CACHE_VOLUME=speeduino-arm64-gradle

docker volume create "$GRADLE_CACHE_VOLUME" >/dev/null

docker run --rm \
  --platform linux/arm64 \
  -v "$REPO_ROOT:/work" \
  -v "$GRADLE_CACHE_VOLUME:/gradle-cache" \
  arm64v8/debian:trixie \
  bash -c '
    set -euo pipefail
    # Force IPv4: some networks (VPNs, restricted DNS) advertise AAAA
    # records with no real IPv6 route, which makes apt/Gradle downloads
    # hang for minutes before timing out.
    echo "Acquire::ForceIPv4 \"true\";" > /etc/apt/apt.conf.d/99force-ipv4
    export JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true"
    export GRADLE_OPTS="-Djava.net.preferIPv4Stack=true"

    apt-get update -qq
    # Debian trixie ships OpenJDK 21/25, not 17 — the desktopApp build
    # itself is not pinned to a toolchain, so 21 works fine.
    DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
      openjdk-21-jdk-headless fakeroot binutils fontconfig libfreetype6 xdg-utils file >/dev/null

    cd /work
    export GRADLE_USER_HOME=/gradle-cache
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64
    chmod +x ./gradlew
    ./gradlew :desktopApp:packageReleaseDeb --no-daemon --stacktrace
  '

echo "=== DONE ==="
find "$REPO_ROOT/desktopApp/build/compose/binaries" -name "*_arm64.deb" -exec ls -la {} \;
