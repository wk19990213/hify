#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_PID_FILE="$PROJECT_DIR/.backend.pid"
FRONTEND_PID_FILE="$PROJECT_DIR/.frontend.pid"
HEALTH_URL="http://localhost:8080/api/v1/health"
MAX_RETRY=30
RETRY_INTERVAL=2

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ---- 1. check MySQL ----
log_info "checking MySQL..."
if ! timeout 3 bash -c "echo >/dev/tcp/127.0.0.1/3306" 2>/dev/null; then
    log_error "MySQL is not reachable at 127.0.0.1:3306"
fi
log_info "MySQL OK"

# ---- 2. check Redis ----
log_info "checking Redis..."
REDIS_HOST=$(grep 'host:' "$PROJECT_DIR/hify-app/src/main/resources/application.yml" | head -1 | awk '{print $2}')
if ! timeout 3 bash -c "echo >/dev/tcp/$REDIS_HOST/6379" 2>/dev/null; then
    log_error "Redis is not reachable at $REDIS_HOST:6379"
fi
log_info "Redis OK"

# ---- 3. stop old backend ----
# 3a. 通过 PID 文件
if [ -f "$BACKEND_PID_FILE" ]; then
    OLD_PID=$(cat "$BACKEND_PID_FILE")
    if tasklist //FI "PID eq $OLD_PID" 2>/dev/null | grep -q "$OLD_PID"; then
        log_info "stopping old backend (PID: $OLD_PID)..."
        taskkill //PID "$OLD_PID" 2>/dev/null || true
        sleep 2
        taskkill //F //PID "$OLD_PID" 2>/dev/null || true
    fi
    rm -f "$BACKEND_PID_FILE"
fi

# 3b. 释放 8080 端口（处理 PID 文件丢失但端口仍被占用的场景）
PORT_PID=$(netstat -ano 2>/dev/null | grep ':8080.*LISTENING' | awk '{print $5}' | head -1)
if [ -n "$PORT_PID" ]; then
    log_info "killing process on port 8080 (PID: $PORT_PID)..."
    taskkill //F //PID "$PORT_PID" 2>/dev/null || true
    sleep 1
fi

# ---- 4. build backend ----
log_info "building backend..."
cd "$PROJECT_DIR"
./mvnw clean package -DskipTests -q || log_error "backend build failed"

# ---- 5. start backend ----
log_info "starting backend..."
JAR_FILE=$(find hify-app/target -maxdepth 1 -name "hify-app-*.jar" ! -name "*-sources.jar" | head -1)
[ -z "$JAR_FILE" ] && log_error "JAR file not found"

nohup java -jar "$JAR_FILE" > "$PROJECT_DIR/.backend.log" 2>&1 &
BACKEND_PID=$!

# 从日志中提取 JVM 实际 PID（Windows 下 $! 可能捕获到 nohup 包装进程）
for i in $(seq 1 10); do
    REAL_PID=$(grep "with PID" "$PROJECT_DIR/.backend.log" 2>/dev/null | head -1 | sed 's/.*with PID \([0-9]*\).*/\1/')
    [ -n "$REAL_PID" ] && break
    sleep 1
done
if [ -n "$REAL_PID" ]; then
    BACKEND_PID="$REAL_PID"
    log_info "backend PID corrected to: $BACKEND_PID"
fi
echo "$BACKEND_PID" > "$BACKEND_PID_FILE"
log_info "backend PID: $BACKEND_PID"

# ---- 6. wait for health check ----
log_info "waiting for backend..."
for i in $(seq 1 $MAX_RETRY); do
    # check if process is still alive
    if ! tasklist //FI "PID eq $BACKEND_PID" 2>/dev/null | grep -q "$BACKEND_PID"; then
        echo ""
        log_warn "last 20 lines of backend log:"
        tail -20 "$PROJECT_DIR/.backend.log" 2>/dev/null || true
        echo ""
        log_error "backend process exited unexpectedly"
    fi
    # check log for Spring Boot started message
    if grep -q "Started HifyApplication" "$PROJECT_DIR/.backend.log" 2>/dev/null; then
        log_info "backend started ($i/$MAX_RETRY)"
        break
    fi
    if [ "$i" -eq "$MAX_RETRY" ]; then
        echo ""
        log_warn "last 20 lines of backend log:"
        tail -20 "$PROJECT_DIR/.backend.log" 2>/dev/null || true
        echo ""
        log_error "backend start timeout"
    fi
    printf "  %s/%s waiting...\r" "$i" "$MAX_RETRY"
    sleep "$RETRY_INTERVAL"
done

# ---- 7. start frontend ----
log_info "starting frontend..."
cd "$PROJECT_DIR/hify-web"
npm run dev > "$PROJECT_DIR/.frontend.log" 2>&1 &
FRONTEND_PID=$!
echo "$FRONTEND_PID" > "$FRONTEND_PID_FILE"
log_info "frontend PID: $FRONTEND_PID"

log_info "==================================="
log_info "Hify started"
log_info "  frontend : http://localhost:5173"
log_info "  backend  : http://localhost:8080/api"
log_info "  health   : http://localhost:8080/api/v1/health"
log_info "==================================="
