#!/bin/bash
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"
PROJ_ROOT=$(realpath "$SCRIPT_DIR/..")

# linxuX64
docker build -f "$SCRIPT_DIR/linux-x64/Dockerfile" -t aws-crt-kotlin/linux-x64:latest "$PROJ_ROOT"
docker run --rm aws-crt-kotlin/linux-x64:latest > "$PROJ_ROOT/dockcross-linux-x64"
chmod ug+x "$PROJ_ROOT/dockcross-linux-x64"

# linuxArm64
docker build -f "$SCRIPT_DIR/linux-arm64/Dockerfile" -t aws-crt-kotlin/linux-arm64:latest "$PROJ_ROOT"
docker run --rm aws-crt-kotlin/linux-arm64:latest > "$PROJ_ROOT/dockcross-linux-arm64"
chmod ug+x "$PROJ_ROOT/dockcross-linux-arm64"
