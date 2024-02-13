#!/bin/bash -x
# Utility script for testing cross platform binaries (pre)compiled using linux containers
# $1 - image name
# $2 - architecture (x64 or arm64)
# $3 - test binary root directory (e.g. aws-crt-kotlin/build/bin)
err() {
    echo -e >&2 "ERROR: $*\n"
}

die() {
    err "$*"
    exit 1
}

usage() {
  cat >&2 <<ENDHELP
Usage: run-container-test-binary image-name arch test-bin-dir
ENDHELP
  exit 1
}

if [[ $# -ne 3 ]]; then
    echo 'Too many/few arguments, expecting three' >&2
    usage
fi

OS=$1
shift
ARCH=$1
shift
TEST_BIN_DIR=$1
shift

case $ARCH in
  x64)
    PLATFORM=linux/amd64
    ;;
  arm64)
    PLATFORM=linux/arm64
    ;;
  *)
    die "unexpected architecture $ARCH; expected one of x64, arm64"
esac

case $OS in
  ubuntu-22.04)
    IMAGE_NAME=public.ecr.aws/lts/ubuntu:22.04_stable
    ;;
  al2023)
    IMAGE_NAME=public.ecr.aws/amazonlinux/amazonlinux:2023
    ;;
  al2)
    IMAGE_NAME=public.ecr.aws/amazonlinux/amazonlinux:2
    ;;
  *)
    die "unexpected operating system $OS"
esac

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

CAPITAL_ARCH=$(echo $ARCH | python3 -c 'import sys; print(sys.stdin.readline().rstrip().capitalize())')
KN_TARGET="linux${CAPITAL_ARCH}"
TEST_EXE="./${KN_TARGET}/debugTest/test.kexe"

$OCI_EXE run --rm --platform $PLATFORM -v $TEST_BIN_DIR:/test -w/test $IMAGE_NAME $TEST_EXE
