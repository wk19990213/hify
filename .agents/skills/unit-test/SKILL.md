---
name: 单测
description: 对 Service 方法进行单元测试。先读代码分析执行路径，输出测试计划，用户确认后执行 TDD 循环。
---

# 单元测试 Skill

## 触发方式

```
/单测 <Service类名>.<方法名>
```

或直接描述：
```
/单测 AgentServiceImpl.create
```

## 流程总览

```
读代码 → 输出测试计划 → 用户确认 → 技术清单检查 → 写测试 → 跑 → 分析失败
```

**关键原则**：在用户确认测试计划前，不写任何测试代码。

---

## 一、读代码

按顺序读取以下文件，完整理解被测方法：

### 1.1 被测 Service 实现类
- 通读整个方法体
- 画出所有 if/else 分支、循环、异常抛出点
- 识别外部依赖调用（Mapper、其他 Service、RPC 等）
- 记录 Bean Validation 注解（@Valid、@NotBlank 等是否在 Service 层生效）

### 1.2 方法的入参 DTO 和出参 DTO
- 读取每个字段的类型、注解（@NotNull/@NotBlank/@Size/@Min/@Max 等）
- 读取 ErrorCode / BizException 的子类，确认校验失败时抛的异常码
- 注意嵌套对象的校验规则（@Valid 级联）

### 1.3 依赖的 Mapper / 其他 Service 接口
- 只读到接口方法签名（不读实现）
- 确认方法签名：入参类型、返回值类型（Optional? List? null?）

---

## 二、输出测试计划（不写代码）

### 2.1 执行路径树

用缩进文本树展示所有代码路径：

```
方法入口: create(req)
├── 路径1: req 校验通过 + 数据正常 → 返回成功
│   ├── 1a: 所有字段合法 → 正常插入，返回 DTO
│   └── 1b: 可选字段为空 → 使用默认值
├── 路径2: 参数校验失败 → 抛出 BizException
│   ├── 2a: name 为空 → PARAM_INVALID
│   ├── 2b: name 重复 → RESOURCE_CONFLICT
│   └── 2c: modelConfigId 对应的模型不存在 → NOT_FOUND
└── 路径3: 外部依赖异常 → 向上传播
    └── 3a: Mapper 抛 SQL 异常 → DataAccessException
```

### 2.2 边界条件清单

```
- null 值：每个可空字段传 null 的行为
- 空字符串：每个 String 字段传 "" 的行为
- 超长字符串：name 超过 DB 限制
- 边界数值：分页 page=0、page=-1、pageSize=0、pageSize=超大
- 并发/重复：唯一约束冲突（如 name 已存在但 deleted=1）
```

### 2.3 分优先级的测试场景表

| 优先级 | 场景 | 输入 | 预期结果 |
|--------|------|------|----------|
| P0 | 正常创建 | 合法 req | 返回 DTO，DB 有记录 |
| P0 | 必填字段缺失 | name=null | BizException(301) |
| P0 | 唯一约束冲突 | name 已存在 | BizException(304) |
| P1 | 可选字段默认值 | 不填 description | description 为空串 |
| P1 | 依赖异常 | Mapper.insert 抛异常 | 异常向上传播 |
| P2 | 超长字符串 | name 超过 64 字符 | 根据校验策略决定 |

P0 = 必须测（核心路径 + 核心异常），P1 = 应该测（边界 + 默认值），P2 = 可选（极端边界）。

---

## 三、技术确认清单（写代码前）

在用户确认测试计划后，写代码前，必须逐项确认：

### 3.1 测试框架与 Mock
- [ ] 测试框架：JUnit 5（Jupiter）
- [ ] Mock 方式：`@ExtendWith(MockitoExtension.class)`，依赖用 `@Mock`，被测类用 `@InjectMocks`
- [ ] 断言库：AssertJ（`assertThat(...).isEqualTo(...)`）
- [ ] 异常断言：`assertThatThrownBy(() -> ...).isInstanceOf(BizException.class)`

### 3.2 Bean Validation 识别
- [ ] 检查 DTO 是否有 `@NotNull/@NotBlank/@Size` 等注解
- [ ] 如果有 Bean Validation 注解 → 必须在测试计划中标注"需要 @SpringBootTest + @AutoConfigureMockMvc 做集成测试"或"使用 Validator 手动注入做单元测试"
- [ ] 如果校验在 Controller 层（Spring 自动校验），Service 层不触发 → 测试计划中标注"Bean Validation 由 Controller 层负责，本测试不覆盖"

### 3.3 数据库 / Mapper
- [ ] Mapper 用 `@Mock`，不启真实数据库
- [ ] MyBatis-Plus `LambdaQueryWrapper` / `LambdaUpdateWrapper` 的 eq/set 调用无法 mock 返回值 —— 用 `when(mapper.selectList(any())).thenReturn(...)` 匹配任意参数
- [ ] `mapper.insert(entity)` 的副作用（实体 id 回填）需要在 mock 中手动模拟

---

## 四、测试文件拆分原则

### 4.1 两个文件

| 文件 | 命名 | 覆盖内容 |
|------|------|----------|
| Service 逻辑测试 | `XxxServiceImplTest.java` | 业务逻辑、异常处理、外部依赖交互 |
| DTO 约束测试 | `XxxRequestTest.java` | Bean Validation 注解、字段约束 |

### 4.2 拆分规则
- **Service 逻辑测试**：Mock 所有外部依赖，聚焦方法内的 if/else/循环/异常处理/返回值组装
- **DTO 约束测试**：用 `Validator`（`jakarta.validation.Validator`）手动校验 DTO，验证 `@NotNull/@NotBlank/@Size` 等注解是否生效，不涉及任何 Service/Mapper

### 4.3 DTO 约束测试示例结构

```java
class XxxRequestTest {
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldFailWhenNameIsNull() {
        XxxRequest req = new XxxRequest();
        req.setName(null);
        var violations = validator.validate(req);
        assertThat(violations).isNotEmpty();
    }
}
```

---

## 五、跑测试与处理失败

### 5.1 执行命令

```bash
cd hify-{module} && mvn test -Dtest=com.hify.{module}.service.impl.XxxServiceImplTest -Dmaven.test.failure.ignore=false
```

或同时跑两个：
```bash
cd hify-{module} && mvn test -Dtest="com.hify.{module}.service.impl.XxxServiceImplTest,com.hify.{module}.dto.XxxRequestTest"
```

### 5.2 失败分析流程

```
测试失败
  ├── 步骤1: 读错误信息，判断是断言失败还是运行时异常
  │   ├── 断言失败 → 步骤2
  │   └── 运行时异常（NPE / 类找不到） → 步骤3
  ├── 步骤2: 断言失败 → 检查预期值是否正确
  │   ├── 预期值写错 → 修正测试
  │   └── 预期值正确 → 实现代码有 bug，修复实现
  ├── 步骤3: 运行时异常 → 检查 mock 配置
  │   ├── mock 漏了 → 补充 mock
  │   └── mock 参数不匹配 → 用 any() 放宽匹配
  └── 步骤4: 修复后重新跑，直到全部通过
```

**关键原则**：不要假设是测试写错了。先对比「测试期望的行为」和「代码实际的行为」，找出真正的根因后再动手改。

### 5.3 完成后输出

```
测试结果:
  XxxServiceImplTest: 8/8 通过
  XxxRequestTest: 5/5 通过
执行路径覆盖率: 100% (路径1a,1b,2a,2b,2c,3a 全部覆盖)
边界条件覆盖: null/空串/超长/重复 全部覆盖
```

---

## 六、注意事项

- 禁止测试 MyBatis-Plus 或 Spring 框架本身（如测试 `LambdaQueryWrapper.eq()` 是否工作）
- 禁止 mock Thread 或 System.currentTimeMillis()，除非涉及时间窗口逻辑
- 禁止为 getter/setter/toString 写测试
- 禁止使用 `Thread.sleep()` 或 `await().atMost()` 等待异步结果——Service 层不应有异步
- 如果 Service 方法内调用了 `BeanUtils.copyProperties`，不能 mock，需要构造真实源对象
