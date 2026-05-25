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
