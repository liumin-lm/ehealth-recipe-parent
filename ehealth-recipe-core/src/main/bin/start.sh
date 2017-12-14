#!/usr/bin/env bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
CONF_DIR=${DEPLOY_DIR}/conf
SERVER_NAME="ehealth-recipe"

PIDS=`ps -f | grep java | grep "$CONF_DIR" |awk '{print $2}'`
PIDS=`ps -ef|grep "$CONF_DIR"|grep -v grep|awk '{print $2}'`
if [ -n "$PIDS" ]; then
    echo "ERROR: The $SERVER_NAME already started!"
    echo "PID: $PIDS"
    exit 1
fi

LOGS_DIR=${DEPLOY_DIR}/logs
if [ ! -d ${LOGS_DIR} ]; then
    mkdir ${LOGS_DIR}
fi
STDOUT_FILE=${LOGS_DIR}/stdout.log

LIB_DIR=${DEPLOY_DIR}/lib
LIB_JARS=`ls ${LIB_DIR}|grep .jar|awk '{print "'${LIB_DIR}'/"$0}'|tr "\n" ":"`

JAVA_OPTS=" -Djava.net.preferIPv4Stack=true "
JAVA_MEM_OPTS=""
BITS=`java -version 2>&1 | grep -i 64-bit`
if [ -n "$BITS" ]; then
    JAVA_MEM_OPTS=" -server -Xmx512m -Xms512m -Xmn256m -XX:PermSize=128m -Xss256k -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 "
else
    JAVA_MEM_OPTS=" -server -Xms512m -Xmx512m -XX:PermSize=128m -XX:SurvivorRatio=2 -XX:+UseParallelGC "
fi

echo -e "Starting server...\c"
nohup java ${JAVA_OPTS} ${JAVA_MEM_OPTS} -classpath ${CONF_DIR}:${LIB_JARS} recipe.standalone.App > ${STDOUT_FILE} 2>&1 &

PIDS=`ps -f | grep java | grep "$DEPLOY_DIR" | awk '{print $2}'`
echo "PID: $PIDS"
echo "STDOUT: $STDOUT_FILE"
