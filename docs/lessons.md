# 常见问题与教训

## MCP 服务绑定唯一键冲突

`agent_mcp_server` 表唯一键为 `(agent_id, mcp_server_id, deleted)`。逻辑删除后重新绑定相同服务时直接插入会冲突。

**解决：** 使用 `INSERT ... ON DUPLICATE KEY UPDATE`：
```java
@Insert("INSERT INTO agent_mcp_server (...) VALUES (...) "
      + "ON DUPLICATE KEY UPDATE deleted = 0, sort_order = VALUES(sort_order), updated_at = NOW(3)")
int insertOrReactivate(...);
```

## Element Plus Checkbox 数值类型转换

`el-checkbox` 的 `:label` 绑定会将数值类型隐式转为字符串，导致后端 `List<Long>` 反序列化失败。
```typescript
const cleanIds: number[] = selected.map((v: any) => Number(v)).filter((n: number) => Number.isFinite(n) && n > 0)
```

## 物理删除 vs 软删除

`agent_mcp_server` 使用软删除。物理删除 `deleted=1` 的记录会丢失审计信息。除非数据合规要求清理，否则保持软删除。

## MyBatis-Plus 注解语义

`@Insert` 注解用于 UPDATE 语句语义不当，应使用 `@Update`。

## MCP 服务必须独立启动

`hify-mcp-services/order-service` 是独立 Spring Boot 应用（端口 8090），必须单独启动。
```bash
cd hify-mcp-services/order-service
java -jar target/order-mcp-service-1.0.0.jar
```

## 列表接口必须填充关联字段

响应 DTO 中有不在实体中的关联字段（如 `mcpServerIds`），list 方法必须在批量填充阶段查询并赋值。

## MyBatis-Plus 逻辑删除 + 唯一键

`@TableLogic` 的 `delete()` 实际执行 `UPDATE SET deleted=1`。唯一键必须包含 `deleted` 字段。

## spring-boot:run 本地启动

从 `hify-app/` 目录执行：`cd hify-app && ../mvnw spring-boot:run`

## Git 推送到 GitHub

需先配置代理（VPN 端口 7890）：
```bash
git config http.proxy http://127.0.0.1:7890
git config https.proxy http://127.0.0.1:7890
```

## Maven 多模块编译顺序

向 `hify-common` 新增类后，依赖方编译可能找不到新类。先 install common：
```bash
./mvnw clean install -pl hify-common -DskipTests
./mvnw compile -pl hify-chat -am
```

## Java static final 字段在 try-catch 中的赋值

`static final` 字段在 try-catch 中分别赋值会导致编译错误。使用临时局部变量：
```java
static {
    SecretKey key = null;
    boolean enabled = false;
    try { key = ...; enabled = true; }
    catch (...) { log.error(...); }
    MASTER_KEY = key;
    ENABLED = enabled;
}
```

## 前端 fetch() 不经过 axios 拦截器

`ChatView.vue` 的 SSE 流式请求使用原生 `fetch()`，不会触发 axios 请求拦截器自动附加 JWT token。需手动从 `localStorage` 取 `hify_token`：
```typescript
const token = localStorage.getItem('hify_token')
await fetch(url, {
  headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
})
```
同时检查 `response.ok`，否则 401 响应不会抛异常，导致静默空消息。

## axios 响应拦截器已解包 data

`request.ts` 拦截器 `code=200` 时返回 `data`（即 `response.data.data`），调用方直接访问业务字段：
```typescript
// 错误：res.data.token（二次解包，data 为 undefined）
// 正确：res.token
const res = await post('/v1/auth/login', { username, password })
saveAuth(res.token, { userId: res.userId, username: res.username })
```

## 注册/登录报"系统内部错误"排查顺序

1. 检查 `.env` 是否存在且密码正确（`application.yml` 通过环境变量读取 DB 配置）
2. 检查 MySQL 连接 `mysql -h <host> -u appuser -p<PASS> -e "SELECT 1"`
3. 检查数据库表是否已初始化 `SHOW TABLES LIKE 'hify_user'`
4. 检查 JAR 是否过期需 `mvn clean package -DskipTests` 重新编译

## MySQL 8.0 不支持 ALTER TABLE ADD COLUMN IF NOT EXISTS

`schema.sql` 中 `ALTER TABLE agent ADD COLUMN IF NOT EXISTS ...` 在 MySQL 8.0 会报语法错误。用动态 SQL：
```sql
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA='hify' AND TABLE_NAME='agent' AND COLUMN_NAME='workflow_id') = 0,
  'ALTER TABLE agent ADD COLUMN workflow_id BIGINT',
  'SELECT ''exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
```
