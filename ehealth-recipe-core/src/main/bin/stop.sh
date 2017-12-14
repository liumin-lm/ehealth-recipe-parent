#!/usr/bin/env bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
CONF_DIR=${DEPLOY_DIR}/conf
SERVER_NAME="ehealth-recipe"

PIDS=`ps -f | grep java | grep "$CONF_DIR" |awk '{print $2}'`
PIDS=`ps -ef|grep "$CONF_DIR"|grep -v grep|awk '{print $2}'`
if [ -z "$PIDS" ]; then
    echo "ERROR: The .$SERVER_NAME does not started!"
    exit 1
fi

echo -e "Stopping $SERVER_NAME ..."
for PID in ${PIDS} ; do
    kill ${PID} > /dev/null 2>&1
done