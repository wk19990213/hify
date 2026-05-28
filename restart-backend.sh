#!/bin/bash
# Restart backend with .env vars loaded

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

# Kill old backend
PID=$(netstat -ano 2>/dev/null | grep ':8080.*LISTENING' | awk '{print $5}' | head -1)
if [ -n "$PID" ]; then
    taskkill //F //PID "$PID" 2>/dev/null
    echo "KILLED PID=$PID"
fi

# Load .env (parse key=value, skip comments and empty lines)
while IFS='=' read -r key value; do
    [[ -z "$key" || "$key" =~ ^# ]] && continue
    export "$key=$value"
done < .env

nohup java -jar hify-app/target/hify-app-1.0.0-SNAPSHOT.jar > .backend.log 2>&1 &
echo "STARTED PID=$!"

# Wait for ready
for i in $(seq 1 30); do
    if grep -q "Started HifyApplication" .backend.log 2>/dev/null; then
        echo "READY"
        exit 0
    fi
    sleep 2
done
echo "TIMEOUT"
