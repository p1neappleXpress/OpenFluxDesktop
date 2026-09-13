#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

APP_NAME="OpenFlux"
VERSION="${VERSION:-1.0.0}"
MAIN_JAR_NAME="openflux-desktop.jar"
MAIN_CLASS="tech.p1neapplexpress.openfluxdesktop.MainKt"
VENDOR="p1neappleXpress"

OUT_DIR="$ROOT/dist"
BUILD_DIR="$ROOT/build/jpackage"
INPUT_DIR="$BUILD_DIR/input"

UNAME_S="$(uname -s)"
UNAME_M="$(uname -m)"
case "$UNAME_S" in
  Linux*)  OS=linux ;;
  Darwin*) OS=darwin ;;
  MINGW*|MSYS*|CYGWIN*) OS=windows ;;
  *) echo "!! unknown OS: $UNAME_S"; exit 1 ;;
esac
case "$UNAME_M" in
  x86_64|amd64) ARCH=amd64 ;;
  aarch64|arm64) ARCH=arm64 ;;
  *) ARCH=amd64 ;;
esac

ICON_SRC="$ROOT/icon.jpg"
ICON_PNG="$ROOT/packaging/icon.png"
ICON_ICO="$ROOT/packaging/icon.ico"
ICON_ICNS="$ROOT/packaging/icon.icns"
mkdir -p "$ROOT/packaging"

if [[ ! -f "$ICON_SRC" ]]; then
    echo "!! not found: $ICON_SRC"
    exit 1
fi

if [[ "$OS" == "darwin" ]]; then
    if [[ ! -f "$ICON_ICNS" ]]; then
        TMP_ICONSET="$(mktemp -d)/icon.iconset"
        mkdir -p "$TMP_ICONSET"
        sips -s format png "$ICON_SRC" --out "$TMP_ICONSET/icon_512x512.png" >/dev/null
        sips -z 512 512 "$TMP_ICONSET/icon_512x512.png" >/dev/null
        sips -z 256 256 "$TMP_ICONSET/icon_512x512.png" --out "$TMP_ICONSET/icon_256x256.png" >/dev/null
        sips -z 128 128 "$TMP_ICONSET/icon_512x512.png" --out "$TMP_ICONSET/icon_128x128.png" >/dev/null
        sips -z 64  64  "$TMP_ICONSET/icon_512x512.png" --out "$TMP_ICONSET/icon_32x32@2x.png" >/dev/null
        sips -z 32  32  "$TMP_ICONSET/icon_512x512.png" --out "$TMP_ICONSET/icon_32x32.png" >/dev/null
        sips -z 16  16  "$TMP_ICONSET/icon_512x512.png" --out "$TMP_ICONSET/icon_16x16.png" >/dev/null
        iconutil -c icns "$TMP_ICONSET" -o "$ICON_ICNS" || ICON_ICNS=""
    fi
    ICON_FILE="$ICON_ICNS"
elif [[ "$OS" == "linux" ]]; then
    if [[ ! -f "$ICON_PNG" ]]; then
        sips -s format png "$ICON_SRC" --out "$ICON_PNG" >/dev/null 2>&1 \
          || convert "$ICON_SRC" -resize 512x512 "$ICON_PNG"
        sips -z 512 512 "$ICON_PNG" >/dev/null 2>&1 || true
    fi
    ICON_FILE="$ICON_PNG"
else
    if [[ ! -f "$ICON_ICO" ]]; then
        if command -v magick >/dev/null 2>&1; then
            magick "$ICON_SRC" -resize 256x256 -define icon:auto-resize=256,128,64,48,32,16 "$ICON_ICO"
        elif command -v convert >/dev/null 2>&1; then
            convert "$ICON_SRC" -resize 256x256 -define icon:auto-resize=256,128,64,48,32,16 "$ICON_ICO"
        else
            ICON_ICO=""
        fi
    fi
    ICON_FILE="$ICON_ICO"
fi

rm -rf "$BUILD_DIR"
mkdir -p "$INPUT_DIR" "$OUT_DIR"

echo "==> [1/4] Gradle: fat-jar"
./gradlew --no-daemon :desktopApp:packageUberJarForCurrentOS

JAR="$(find desktopApp/build/compose/jars -maxdepth 1 -name '*.jar' | head -n1)"
if [[ -z "${JAR:-}" ]]; then
    echo "!! jar not found: desktopApp/build/compose/jars/*.jar"
    exit 1
fi
cp "$JAR" "$INPUT_DIR/$MAIN_JAR_NAME"
echo "    jar: $(basename "$JAR") -> $INPUT_DIR/$MAIN_JAR_NAME"

echo "==> [2/4] bundling universal-bypass-tool for $OS/$ARCH"
BIN_NAME="universal-bypass-tool-${OS}-${ARCH}"
[[ "$OS" == "windows" ]] && BIN_NAME="${BIN_NAME}.exe"

mkdir -p "$INPUT_DIR/bin"

echo "==> [3/4] jpackage for $OS/$ARCH"
case "$OS" in
  linux)   TYPES=(deb rpm app-image) ;;
  windows) TYPES=(exe msi app-image) ;;
  darwin)  TYPES=(dmg pkg app-image) ;;
esac

ADD_MODULES="java.base,java.desktop,java.net.http,jdk.crypto.ec,jdk.unsupported"

for TYPE in "${TYPES[@]}"; do
    echo "    -> $TYPE"

    JPACKAGE_OPTS=(
      --type "$TYPE"
      --name "$APP_NAME"
      --app-version "$VERSION"
      --vendor "$VENDOR"
      --input "$INPUT_DIR"
      --main-jar "$MAIN_JAR_NAME"
      --main-class "$MAIN_CLASS"
      --dest "$OUT_DIR"
      --add-modules "$ADD_MODULES"
      --java-options "-Xmx512m"
      --java-options "-Dfile.encoding=UTF-8"
      --java-options "-Dopenflux.binDir=\$APPDIR/bin"
    )
    [[ -n "${ICON_FILE:-}" && -f "$ICON_FILE" ]] && JPACKAGE_OPTS+=(--icon "$ICON_FILE")

    jpackage "${JPACKAGE_OPTS[@]}" || echo "    !! type $TYPE failed"
done

echo "==> [4/4] artifacts:"
ls -lh "$OUT_DIR" || true
