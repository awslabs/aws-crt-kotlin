#!/bin/bash

err() {
    echo -e >&2 "ERROR: $*\n"
}

die() {
    err "$*"
    exit 1
}

SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"
PROJ_ROOT=$(realpath "$SCRIPT_DIR/..")

if [ -z "$OCI_EXE" ]; then
    if which finch > /dev/null 2>/dev/null; then
      OCI_EXE=finch
    elif which podman >/dev/null 2>/dev/null; then
        OCI_EXE=podman
    elif which docker >/dev/null 2>/dev/null; then
        OCI_EXE=docker
    else
        die "Cannot find a container executor. Search for docker and podman."
    fi
fi

echo "using container executor OCI_EXE=$OCI_EXE"

# linxuX64
$OCI_EXE build -f "$SCRIPT_DIR/linux-x64/Dockerfile" -t aws-crt-kotlin/linux-x64:latest "$PROJ_ROOT"
$OCI_EXE run --rm aws-crt-kotlin/linux-x64:latest > "$PROJ_ROOT/dockcross-linux-x64"
chmod ug+x "$PROJ_ROOT/dockcross-linux-x64"

# linuxArm64
$OCI_EXE build -f "$SCRIPT_DIR/linux-arm64/Dockerfile" -t aws-crt-kotlin/linux-arm64:latest "$PROJ_ROOT"
$OCI_EXE run --rm aws-crt-kotlin/linux-arm64:latest > "$PROJ_ROOT/dockcross-linux-arm64"
chmod ug+x "$PROJ_ROOT/dockcross-linux-arm64"
