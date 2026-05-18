#!/bin/bash
# make.sh - Makefile dispatcher for Windows (no make required)
# Usage: ./make.sh [target]
# If no target given, shows available targets.

set -e
cd "$(dirname "$0")"

# Parse Makefile for .PHONY targets
show_targets() {
    echo "Available targets:"
    grep '^\.PHONY:' Makefile 2>/dev/null | sed 's/\.PHONY://' | tr ' ' '\n' | grep -v '^$' | sort -u | while read t; do
        echo "  $t"
    done
    echo ""
    echo "Usage: ./make.sh <target>"
}

TARGET="${1:-}"
if [ -z "$TARGET" ]; then
    show_targets
    exit 0
fi

case "$TARGET" in
    start)
        chmod +x start.sh && ./start.sh
        ;;
    stop)
        chmod +x stop.sh && ./stop.sh
        ;;
    restart)
        chmod +x stop.sh start.sh && ./stop.sh && ./start.sh
        ;;
    build)
        echo "=== building backend ==="
        ./mvnw clean package -DskipTests -q
        echo "=== building frontend ==="
        cd hify-web && npm run build
        ;;
    build-backend)
        ./mvnw clean package -DskipTests -q
        ;;
    build-frontend)
        cd hify-web && npm run build
        ;;
    clean)
        echo "=== cleaning ==="
        ./mvnw clean -q
        rm -rf hify-web/dist
        rm -f .backend.pid .frontend.pid .backend.log .frontend.log
        ;;
    package)
        "$0" build
        echo "=== packaging ==="
        rm -rf dist hify.tar.gz
        mkdir -p dist/hify
        cp hify-app/target/hify-app-*.jar dist/hify/ 2>/dev/null || true
        cp -r hify-web/dist dist/hify/frontend 2>/dev/null || true
        cp docker-compose.yml dist/hify/ 2>/dev/null || true
        cp -r docker dist/hify/ 2>/dev/null || true
        cp start.sh stop.sh dist/hify/
        cd dist && tar -czf ../hify.tar.gz hify
        cd .. && rm -rf dist
        echo "done: hify.tar.gz"
        ;;
    dev)
        cd hify-web && npm run dev
        ;;
    *)
        echo "Unknown target: $TARGET"
        show_targets
        exit 1
        ;;
esac
