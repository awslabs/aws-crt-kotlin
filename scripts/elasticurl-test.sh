#!/bin/bash

TEST_SCRIPT=/tmp/http_client_test.py
SCRIPTPATH="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
JVM_EXE=$SCRIPTPATH/elasticurlJvm.sh

ROOT_PATH=$(dirname $SCRIPTPATH)

cd $ROOT_PATH

if [ ! -f $TEST_SCRIPT ]; then
    curl -L -o $TEST_SCRIPT https://raw.githubusercontent.com/awslabs/aws-c-http/master/integration-testing/http_client_test.py
fi

python3 $TEST_SCRIPT $JVM_EXE

