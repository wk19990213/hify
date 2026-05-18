.PHONY: start stop restart build build-backend build-frontend clean package dev

# Hify Makefile
# On Windows (Git Bash): ./make.sh <target>
# On Linux/macOS:         make <target>

start:
	@chmod +x start.sh && ./start.sh

stop:
	@chmod +x stop.sh && ./stop.sh

restart: stop start

build: build-backend build-frontend

build-backend:
	@./mvnw clean package -DskipTests -q

build-frontend:
	@cd hify-web && npm run build

clean:
	@./mvnw clean -q
	@rm -rf hify-web/dist
	@rm -f .backend.pid .frontend.pid .backend.log .frontend.log

package: build
	@rm -rf dist hify.tar.gz
	@mkdir -p dist/hify
	@cp hify-app/target/hify-app-*.jar dist/hify/ 2>/dev/null || true
	@cp -r hify-web/dist dist/hify/frontend 2>/dev/null || true
	@cp docker-compose.yml dist/hify/ 2>/dev/null || true
	@cp -r docker dist/hify/ 2>/dev/null || true
	@cp start.sh stop.sh dist/hify/
	@cd dist && tar -czf ../hify.tar.gz hify
	@rm -rf dist
	@echo "done: hify.tar.gz"

dev:
	@cd hify-web && npm run dev
