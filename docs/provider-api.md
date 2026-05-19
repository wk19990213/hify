# 模型提供商 API

## 基础信息

| 项目         | 值                                                                   |
| ------------ | -------------------------------------------------------------------- |
| Base URL     | `http://localhost:8080/api`                                          |
| Content-Type | `application/json`                                                   |
| 响应格式     | `{ "code": 200, "message": "success", "data": {}, "success": true }` |

### 统一响应字段

| 字段      | 类型      | 说明                     |
| --------- | --------- | ------------------------ |
| `code`    | `int`     | 状态码，`200` 成功       |
| `message` | `string`  | 提示信息                 |
| `data`    | `object`  | 业务数据，各接口返回不同 |
| `success` | `boolean` | `true` / `false`         |

### 错误码

| code  | 说明                        |
| ----- | --------------------------- |
| `200` | 成功                        |
| `400` | 参数校验失败 / 请求格式错误 |
| `404` | 资源不存在                  |
| `500` | 系统内部错误                |

---

## 接口总览

| 方法     | 路径                                 | 说明                          |
| -------- | ------------------------------------ | ----------------------------- |
| `POST`   | `/v1/providers`                      | 创建提供商                    |
| `GET`    | `/v1/providers`                      | 分页列表                      |
| `GET`    | `/v1/providers/{id}`                 | 查看详情（含模型 + 健康状态） |
| `PUT`    | `/v1/providers/{id}`                 | 更新提供商                    |
| `DELETE` | `/v1/providers/{id}`                 | 删除提供商（逻辑删除）        |
| `POST`   | `/v1/providers/{id}/test-connection` | 连通性测试                    |

---

## 1. 创建提供商

```http
POST /v1/providers
```

### 请求参数

| 字段              | 类型     | 必填 | 默认值             | 说明                                                    |
| ----------------- | -------- | ---- | ------------------ | ------------------------------------------------------- |
| `name`            | `string` | 是   | —                  | 显示名称                                                |
| `type`            | `string` | 是   | —                  | `OPENAI` / `ANTHROPIC` / `OLLAMA` / `OPENAI_COMPATIBLE` |
| `baseUrl`         | `string` | 是   | —                  | API 基础地址                                            |
| `code`            | `string` | 否   | `{type}-{8位随机}` | 唯一编码                                                |
| `authConfig`      | `object` | 否   | —                  | 鉴权配置，如 `{"apiKey":"sk-xxx"}`                      |
| `timeoutMs`       | `int`    | 否   | `30000`            | 请求超时（毫秒）                                        |
| `maxRetries`      | `int`    | 否   | `3`                | 最大重试次数                                            |
| `retryIntervalMs` | `int`    | 否   | `1000`             | 重试间隔（毫秒）                                        |
| `status`          | `int`    | 否   | `1`                | `0`=禁用 `1`=启用                                       |
| `sortOrder`       | `int`    | 否   | `0`                | 排序权重                                                |
| `extraConfig`     | `object` | 否   | —                  | 额外配置（headers、代理等）                             |

### 请求示例

```bash
curl -X POST http://localhost:8080/api/v1/providers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "OpenAI",
    "type": "OPENAI",
    "baseUrl": "https://api.openai.com",
    "authConfig": {"apiKey": "sk-xxx"}
  }'
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": 1,
  "success": true
}
```

| 字段   | 说明              |
| ------ | ----------------- |
| `data` | 新创建的提供商 ID |

---

## 2. 分页列表

```http
GET /v1/providers?page=1&pageSize=20
```

### 请求参数

| 参数       | 类型  | 必填 | 默认值 | 说明                   |
| ---------- | ----- | ---- | ------ | ---------------------- |
| `page`     | `int` | 否   | `1`    | 页码                   |
| `pageSize` | `int` | 否   | `20`   | 每页条数（最大 `100`） |

### 请求示例

```bash
curl "http://localhost:8080/api/v1/providers?page=1&pageSize=10"
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "data": [
      {
        "id": 1,
        "name": "OpenAI",
        "code": "openai-a3c2e604",
        "type": "OPENAI",
        "baseUrl": "https://api.openai.com",
        "timeoutMs": 30000,
        "maxRetries": 3,
        "retryIntervalMs": 1000,
        "status": 1,
        "sortOrder": 0,
        "createdAt": "2026-05-19T16:58:39",
        "updatedAt": "2026-05-19T16:58:39"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 10
  },
  "success": true
}
```

### 响应字段

| 字段                          | 类型     | 说明              |
| ----------------------------- | -------- | ----------------- |
| `data.data`                   | `array`  | ProviderResp 列表 |
| `data.data[].id`              | `long`   | 提供商 ID         |
| `data.data[].name`            | `string` | 显示名称          |
| `data.data[].code`            | `string` | 唯一编码          |
| `data.data[].type`            | `string` | 提供商类型        |
| `data.data[].baseUrl`         | `string` | API 地址          |
| `data.data[].timeoutMs`       | `int`    | 超时（毫秒）      |
| `data.data[].maxRetries`      | `int`    | 最大重试          |
| `data.data[].retryIntervalMs` | `int`    | 重试间隔（毫秒）  |
| `data.data[].status`          | `int`    | `0`=禁用 `1`=启用 |
| `data.data[].sortOrder`       | `int`    | 排序权重          |
| `data.data[].createdAt`       | `string` | 创建时间          |
| `data.data[].updatedAt`       | `string` | 更新时间          |
| `data.total`                  | `long`   | 总记录数          |
| `data.page`                   | `long`   | 当前页            |
| `data.size`                   | `long`   | 每页条数          |

---

## 3. 查看详情

```http
GET /v1/providers/{id}
```

### 路径参数

| 参数 | 类型   | 说明      |
| ---- | ------ | --------- |
| `id` | `long` | 提供商 ID |

### 请求示例

```bash
curl http://localhost:8080/api/v1/providers/1
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "provider": {
      "id": 1,
      "name": "OpenAI",
      "code": "openai-a3c2e604",
      "type": "OPENAI",
      "baseUrl": "https://api.openai.com",
      "authConfig": { "apiKey": "sk-xxx" },
      "timeoutMs": 30000,
      "maxRetries": 3,
      "retryIntervalMs": 1000,
      "status": 1,
      "sortOrder": 0,
      "extraConfig": null,
      "createdAt": "2026-05-19T16:58:39",
      "updatedAt": "2026-05-19T16:58:39"
    },
    "modelConfigs": [
      {
        "id": 1,
        "providerId": 1,
        "modelId": "gpt-4-turbo",
        "name": "GPT-4 Turbo",
        "code": "gpt4t",
        "capabilities": { "vision": true, "tools": true },
        "priceConfig": { "input": 0.03, "output": 0.06 },
        "status": 1,
        "isDefault": 1,
        "sortOrder": 0,
        "createdAt": "2026-05-19T17:00:00",
        "updatedAt": "2026-05-19T17:00:00"
      }
    ],
    "health": {
      "providerId": 1,
      "status": "HEALTHY",
      "consecutiveFailures": 0,
      "avgLatencyMs": 120,
      "successRate": 99.5,
      "lastCheckTime": "2026-05-19T17:01:00",
      "lastSuccessTime": "2026-05-19T17:01:00",
      "lastErrorMsg": null
    }
  },
  "success": true
}
```

### 响应字段

| 字段                               | 类型      | 说明                                             |
| ---------------------------------- | --------- | ------------------------------------------------ |
| `data.provider`                    | `object`  | 提供商基本信息                                   |
| `data.provider.id`                 | `long`    | ID                                               |
| `data.provider.name`               | `string`  | 显示名称                                         |
| `data.provider.code`               | `string`  | 唯一编码                                         |
| `data.provider.type`               | `string`  | 提供商类型                                       |
| `data.provider.baseUrl`            | `string`  | API 地址                                         |
| `data.provider.authConfig`         | `object`  | 鉴权配置                                         |
| `data.provider.timeoutMs`          | `int`     | 超时                                             |
| `data.provider.maxRetries`         | `int`     | 最大重试                                         |
| `data.provider.status`             | `int`     | `0`=禁用 `1`=启用                                |
| `data.provider.sortOrder`          | `int`     | 排序                                             |
| `data.provider.extraConfig`        | `object`  | 额外配置                                         |
| `data.modelConfigs`                | `array`   | 模型配置列表，空数组 `[]` 无模型                 |
| `data.modelConfigs[].modelId`      | `string`  | 原始模型标识                                     |
| `data.modelConfigs[].name`         | `string`  | 显示名称                                         |
| `data.modelConfigs[].code`         | `string`  | 唯一编码                                         |
| `data.modelConfigs[].capabilities` | `object`  | 能力配置 JSON                                    |
| `data.modelConfigs[].priceConfig`  | `object`  | 计费配置 JSON                                    |
| `data.modelConfigs[].status`       | `int`     | `0`=禁用 `1`=启用 `2`=deprecated                 |
| `data.modelConfigs[].isDefault`    | `int`     | `0`=否 `1`=默认                                  |
| `data.health`                      | `object`  | 健康状态，`null` 无记录                          |
| `data.health.status`               | `string`  | `HEALTHY` / `DEGRADED` / `UNHEALTHY` / `UNKNOWN` |
| `data.health.consecutiveFailures`  | `int`     | 连续失败次数                                     |
| `data.health.avgLatencyMs`         | `int`     | 平均延迟（毫秒）                                 |
| `data.health.successRate`          | `decimal` | 成功率（百分比）                                 |
| `data.health.lastCheckTime`        | `string`  | 最后检查时间                                     |
| `data.health.lastSuccessTime`      | `string`  | 最后成功时间                                     |
| `data.health.lastErrorMsg`         | `string`  | 最后错误信息                                     |

---

## 4. 更新提供商

```http
PUT /v1/providers/{id}
```

部分更新，只传需要修改的字段。

### 路径参数

| 参数 | 类型   | 说明      |
| ---- | ------ | --------- |
| `id` | `long` | 提供商 ID |

### 请求参数

| 字段              | 类型     | 说明                                                    |
| ----------------- | -------- | ------------------------------------------------------- |
| `name`            | `string` | 显示名称                                                |
| `code`            | `string` | 唯一编码                                                |
| `type`            | `string` | `OPENAI` / `ANTHROPIC` / `OLLAMA` / `OPENAI_COMPATIBLE` |
| `baseUrl`         | `string` | API 地址                                                |
| `authConfig`      | `object` | 鉴权配置                                                |
| `timeoutMs`       | `int`    | 超时（毫秒）                                            |
| `maxRetries`      | `int`    | 最大重试次数                                            |
| `retryIntervalMs` | `int`    | 重试间隔（毫秒）                                        |
| `status`          | `int`    | `0`=禁用 `1`=启用                                       |
| `sortOrder`       | `int`    | 排序                                                    |
| `extraConfig`     | `object` | 额外配置                                                |

### 请求示例

```bash
curl -X PUT http://localhost:8080/api/v1/providers/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "OpenAI Updated", "status": 1}'
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "success": true
}
```

---

## 5. 删除提供商

```http
DELETE /v1/providers/{id}
```

逻辑删除，数据保留但不再出现在查询中。

### 路径参数

| 参数 | 类型   | 说明      |
| ---- | ------ | --------- |
| `id` | `long` | 提供商 ID |

### 请求示例

```bash
curl -X DELETE http://localhost:8080/api/v1/providers/1
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "success": true
}
```

---

## 6. 连通性测试

```http
POST /v1/providers/{id}/test-connection
```

测试提供商 API 是否可达，超时 10 秒。

### 路径参数

| 参数 | 类型   | 说明      |
| ---- | ------ | --------- |
| `id` | `long` | 提供商 ID |

### 分发规则

| Provider Type       | 调用接口                  | 认证方式                                                 |
| ------------------- | ------------------------- | -------------------------------------------------------- |
| `OPENAI`            | `GET {baseUrl}/v1/models` | `Authorization: Bearer {apiKey}`                         |
| `OPENAI_COMPATIBLE` | `GET {baseUrl}/v1/models` | `Authorization: Bearer {apiKey}`                         |
| `ANTHROPIC`         | `GET {baseUrl}/v1/models` | `x-api-key: {apiKey}`<br>`anthropic-version: 2023-06-01` |
| `OLLAMA`            | `GET {baseUrl}/api/tags`  | 无需认证                                                 |

### 请求示例

```bash
curl -X POST http://localhost:8080/api/v1/providers/1/test-connection
```

### 响应示例（成功）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true,
    "latencyMs": 450,
    "modelCount": 15,
    "errorMessage": null
  },
  "success": true
}
```

### 响应示例（失败）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": false,
    "latencyMs": 5067,
    "modelCount": 0,
    "errorMessage": "请求超时: https://api.openai.com/v1/models"
  },
  "success": true
}
```

### 响应字段

| 字段                | 类型      | 说明                       |
| ------------------- | --------- | -------------------------- |
| `data.success`      | `boolean` | `true` 连通，`false` 失败  |
| `data.latencyMs`    | `long`    | 请求延迟（毫秒）           |
| `data.modelCount`   | `int`     | 可用模型数量，失败时为 `0` |
| `data.errorMessage` | `string`  | 失败原因，成功时为 `null`  |
