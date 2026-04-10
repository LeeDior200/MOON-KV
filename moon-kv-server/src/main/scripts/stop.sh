#!/bin/bash

MOON_KV_HOME=$(cd "$(dirname "$0")/.." && pwd)
PID_FILE="$MOON_KV_HOME/moon-kv.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "MOON-KV Server is not running (no PID file found)"
    exit 1
fi

PID=$(cat "$PID_FILE")

if ! kill -0 "$PID" 2>/dev/null; then
    echo "MOON-KV Server is not running (process $PID not found)"
    rm -f "$PID_FILE"
    exit 1
fi

echo "Stopping MOON-KV Server (PID: $PID)..."
kill "$PID"

for i in {1..10}; do
    if ! kill -0 "$PID" 2>/dev/null; then
        echo "MOON-KV Server stopped successfully"
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
done

echo "MOON-KV Server did not stop gracefully, forcing shutdown..."
kill -9 "$PID"
rm -f "$PID_FILE"
echo "MOON-KV Server stopped (forced)"
