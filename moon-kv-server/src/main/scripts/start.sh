#!/bin/bash

MOON_KV_HOME=$(cd "$(dirname "$0")/.." && pwd)
LIB_DIR="$MOON_KV_HOME/lib"
CONFIG_DIR="$MOON_KV_HOME/config"
LOG_DIR="$MOON_KV_HOME/logs"
DATA_DIR="$MOON_KV_HOME/data"

if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
fi

if [ ! -d "$DATA_DIR" ]; then
    mkdir -p "$DATA_DIR"
fi

if [ -z "$JAVA_HOME" ]; then
    JAVA_CMD="java"
else
    JAVA_CMD="$JAVA_HOME/bin/java"
fi

JVM_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

CLASSPATH="$CONFIG_DIR"
for jar in "$LIB_DIR"/*.jar; do
    CLASSPATH="$CLASSPATH:$jar"
done

PORT=${PORT:-4070}
CONFIG_FILE=${CONFIG_FILE:-"$CONFIG_DIR/server.properties"}

if [ -f "$CONFIG_FILE" ]; then
    echo "Loading configuration from $CONFIG_FILE"
fi

echo "Starting MOON-KV Server..."
echo "Home: $MOON_KV_HOME"
echo "Port: $PORT"
echo "Log Dir: $LOG_DIR"
echo "Data Dir: $DATA_DIR"

$JAVA_CMD $JVM_OPTS \
    -Dkv.wal.path="$DATA_DIR/kv_store.wal" \
    -Dlogback.configurationFile="$CONFIG_DIR/logback.xml" \
    -classpath "$CLASSPATH" \
    com.saki.server.ServerMain \
    --port $PORT \
    > "$LOG_DIR/console.log" 2>&1 &

PID=$!
echo $PID > "$MOON_KV_HOME/moon-kv.pid"

echo "MOON-KV Server started with PID: $PID"
echo "Dashboard: http://localhost:$PORT/"
echo "API: http://localhost:$PORT/api/v1/"
