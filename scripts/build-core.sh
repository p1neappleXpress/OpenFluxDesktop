#!/usr/bin/env bash
# Builds the core the app bundles from an OpenFlux checkout, with the local
# Go, or in Docker without one:
#
#   scripts/build-core.sh              # uses the OpenFlux/ submodule
#   scripts/build-core.sh ../OpenFlux  # or any other checkout
#   GOOS=linux GOARCH=arm64 scripts/build-core.sh
#
# The target is GOOS/GOARCH (default: this machine). Writes into
# desktopApp/resources/<windows|macos|linux>: openflux-<os>-<arch>[.exe],
# openflux-core.version (branch@commit, shown under Settings → About) and,
# for Windows, wintun.dll (the full tunnel).
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
core=$(cd "${1:-$root/OpenFlux}" && pwd)
image=${2:-golang:$(sed -n 's/^go \([0-9.]*\).*/\1/p' "$core/go.mod" | head -n1)}

# The submodule must be on the commit this repository pins: a `checkout main` inside it once rolled the bundled
# core back to node-v1.0.0. CORE_UNPINNED=1 builds whatever is checked out.
if [ -z "${1:-}" ] && [ -z "${CORE_UNPINNED:-}" ]; then
  pinned=$(git -C "$root" ls-tree HEAD OpenFlux | awk '$2 == "commit" {print $3}')
  head=$(git -C "$core" rev-parse HEAD)
  if [ -n "$pinned" ] && [ "$pinned" != "$head" ]; then
    echo "OpenFlux/ is on ${head:0:7}, but this repository pins ${pinned:0:7}." >&2
    echo "Run: git submodule update --init OpenFlux   (or CORE_UNPINNED=1 to build it anyway)" >&2
    exit 1
  fi
fi

host_os() {
  case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) echo windows ;;
    Darwin) echo darwin ;;
    *) echo linux ;;
  esac
}
host_arch() {
  case "$(uname -m)" in
    arm64|aarch64) echo arm64 ;;
    *) echo amd64 ;;
  esac
}
goos=${GOOS:-$(host_os)}
goarch=${GOARCH:-$(host_arch)}
case "$goos" in
  windows) dir=windows; ext=.exe ;;
  darwin) dir=macos; ext= ;;
  linux) dir=linux; ext= ;;
  *) echo "unsupported GOOS=$goos" >&2; exit 1 ;;
esac
name="openflux-$goos-$goarch$ext"
out="$root/desktopApp/resources/$dir"
mkdir -p "$out"

if command -v go >/dev/null 2>&1 && [ -z "${2:-}" ]; then
  (cd "$core" && GOOS=$goos GOARCH=$goarch CGO_ENABLED=0 GOFLAGS=-buildvcs=false \
    GOTOOLCHAIN=auto go build -trimpath -ldflags "-s -w" -o "$out/$name" .)
else
  # Docker Desktop on Windows wants C:/... paths; elsewhere pwd is fine.
  host_path() { (cd "$1" && (pwd -W 2>/dev/null || pwd)); }
  MSYS_NO_PATHCONV=1 docker run --rm \
    -v "$(host_path "$core"):/src:ro" \
    -v "$(host_path "$out"):/out" \
    -v ofx-gomod:/go/pkg/mod \
    -v ofx-gocache:/root/.cache/go-build \
    -w /src \
    -e GOOS="$goos" -e GOARCH="$goarch" -e CGO_ENABLED=0 -e GOFLAGS=-buildvcs=false \
    "$image" \
    go build -trimpath -ldflags "-s -w" -o "/out/$name" .
fi
chmod +x "$out/$name"

if [ "$goos" = windows ]; then
  # Wintun for the full tunnel (--inbound=tun): the core loads wintun.dll
  # from its own folder. The official build, checked against its SHA-256.
  wintun_zip=$(mktemp)
  curl -fsSL -o "$wintun_zip" https://www.wintun.net/builds/wintun-0.14.1.zip
  echo "07c256185d6ee3652e09fa55c0b673e2624b565e02c4b9091c79ca7d2f24ef51  $wintun_zip" | sha256sum -c - >/dev/null
  unzip -p "$wintun_zip" wintun/bin/amd64/wintun.dll > "$out/wintun.dll"
  rm -f "$wintun_zip"
fi

branch=$(git -C "$core" rev-parse --abbrev-ref HEAD)
rev=$(git -C "$core" describe --always --dirty)
printf '%s@%s\n' "$branch" "$rev" > "$out/openflux-core.version"
echo "core $(cat "$out/openflux-core.version") -> $out/$name"
