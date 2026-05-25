# Hify 安全规范

## SSRF 防护

`UrlSecurityValidator` 位于 `hify-common`，所有对外 HTTP 请求必须经过验证。

### 覆盖清单

| 组件 | 调用点 | 验证方式 |
|------|--------|---------|
| AbstractProviderAdapter | chat/streamChat/testConnection/listModelIds | validateUrl() |
| HttpJsonRpcTransport | 构造函数 | validateUrl() |
| HttpNodeExecutor | execute() | isValidUrl() |
| ProviderServiceImpl | create() syncModels 前 | validateUrl() |
| EmbeddingService | callApi/embedViaOpenAiApi | 待添加 |

### OkHttp 重定向禁用

**所有** OkHttpClient 实例必须禁用重定向，否则攻击者可通过 302 绕过验证：
```java
new OkHttpClient.Builder()
    .followRedirects(false)
    .followSslRedirects(false)
    .build();
```

### 验证规则

- HTTP 仅允许 localhost/127.0.0.1/::1（Ollama 等本地服务）
- HTTPS 禁止内网 IP（含 IPv4 和 IPv6）
- 禁止 URL 中包含 @ 和 #
- 禁止 localhost HTTPS（用 HTTP 即可）

## SQL 注入防护

### `.last()` 禁止使用

MyBatis-Plus 的 `.last()` 绕过参数化查询，即使值为字面量也必须替换：
```java
// 错误
.last("LIMIT " + n)

// 正确
Page.of(1, n) + selectPage
```

### 动态排序字段

`orderBy(true, condition, column)` 中 column 必须用 Lambda 表达式，禁止字符串拼接。

## @Transactional 注意事项

- 只放在 Controller 调用的 public 入口方法上
- 内部 self-called private 方法自动参与外层事务
- 从无事务方法通过 `this.xxx()` 调用带注解的方法，注解不生效
- 不要在 private 方法上加 `@Transactional`

## XSS 防护

前端渲染 LLM 输出必须双层防护：
```typescript
const md = new MarkdownIt({ html: false })
const renderMarkdown = (text: string) => DOMPurify.sanitize(md.render(text))
```

## 文件上传安全

- 文件类型白名单（pdf, docx, txt, md）
- 魔数检查（不依赖扩展名）
- 路径遍历防护（禁止 `..`、`/`、`\`）
- 大小限制（50MB）

## 异常信息泄露

- `GlobalExceptionHandler` 对非业务异常返回通用消息
- `BizException` 消息可返回客户端（开发者有意编写的安全消息）
- `IllegalArgumentException`/`IllegalStateException` → 通用 `PARAM_ERROR`
- 兜底 `Exception` → 通用 `INTERNAL_ERROR`

## Docker Compose 安全

- 所有密码通过 `${ENV_VAR}` 引用，不硬编码
- healthcheck 密码用 `CMD-SHELL` + 双引号：`-p"$${MYSQL_ROOT_PASSWORD}"`
- 移除 `${VAR:-default}` 中的弱默认值，强制用户配置 `.env`

## AES 加密

- 生产环境必须设置 `HIFY_ENCRYPTION_KEY`（64 位 hex）
- 启用后加密/解密失败抛出 `RuntimeException`（不回退明文）
- 静态初始化对非法 hex 字符做了安全降级（日志警告 + 禁用加密）
