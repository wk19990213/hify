#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_PID_FILE="$PROJECT_DIR/.backend.pid"
FRONTEND_PID_FILE="$PROJECT_DIR/.frontend.pid"
GRACE_PERIOD=10

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

process_exists() {
    tasklist //FI "PID eq $1" 2>/dev/null | grep -q "$1"
}

stop_by_pid_file() {
    local pid_file="$1"
    local name="$2"

    if [ ! -f "$pid_file" ]; then
        log_warn "$name PID 文件不存在 ($pid_file)，跳过"
        return
    fi

    local pid
    pid=$(cat "$pid_file" 2>/dev/null || true)
    if [ -z "$pid" ]; then
        log_warn "$name PID 为空，跳过"
        rm -f "$pid_file"
        return
    fi

    if ! process_exists "$pid"; then
        log_warn "$name 进程 $pid 不存在，清理 PID 文件"
        rm -f "$pid_file"
        return
    fi

    log_info "停止 $name (PID: $pid)..."

    # 先发送 SIGTERM（graceful shutdown）
    taskkill //PID "$pid" 2>/dev/null || true

    # 等待退出
    local waited=0
    while process_exists "$pid"; do
        if [ "$waited" -ge "$GRACE_PERIOD" ]; then
            log_warn "$name 未在 ${GRACE_PERIOD}s 内退出，强制终止"
            taskkill //F //PID "$pid" 2>/dev/null || true
            sleep 1
            break
        fi
        sleep 1
        waited=$((waited + 1))
    done

    if process_exists "$pid"; then
        log_error "无法停止 $name 进程 $pid"
    else
        log_info "$name 已停止"
    fi

    rm -f "$pid_file"
}

log_info "停止 Hify..."

stop_by_pid_file "$FRONTEND_PID_FILE" "前端"
stop_by_pid_file "$BACKEND_PID_FILE" "后端"

# 兜底：释放 8080 端口
PORT_PID=$(netstat -ano 2>/dev/null | grep ':8080.*LISTENING' | awk '{print $5}' | head -1)
if [ -n "$PORT_PID" ]; then
    log_info "释放端口 8080 (PID: $PORT_PID)..."
    taskkill //F //PID "$PORT_PID" 2>/dev/null || true
fi

log_info "Hify 已停止"
