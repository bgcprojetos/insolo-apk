#!/usr/bin/env sh
# Gradle bootstrapper para o projeto INSOLO.
# Baixa o Gradle 9.4.1 uma única vez no diretório ~/.gradle.
set -eu

GRADLE_VERSION="9.4.1"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
DIST_ROOT="$GRADLE_USER_HOME/insolo-gradle/gradle-$GRADLE_VERSION"
GRADLE_HOME="$DIST_ROOT/gradle-$GRADLE_VERSION"
ZIP_FILE="$DIST_ROOT/gradle-$GRADLE_VERSION-bin.zip"
URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$DIST_ROOT"
  if [ ! -f "$ZIP_FILE" ]; then
    echo "Baixando Gradle $GRADLE_VERSION..."
    if command -v curl >/dev/null 2>&1; then
      curl --fail --location --retry 3 "$URL" --output "$ZIP_FILE"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$ZIP_FILE" "$URL"
    else
      echo "Erro: instale curl ou wget para baixar o Gradle." >&2
      exit 1
    fi
  fi
  echo "Preparando Gradle $GRADLE_VERSION..."
  unzip -q -o "$ZIP_FILE" -d "$DIST_ROOT"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
