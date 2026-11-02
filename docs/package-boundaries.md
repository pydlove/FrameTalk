# 代码分包规则

## 1. 目标

分包的目标不是把文件拆得多，而是保证：

- 业务规则不依赖框架
- Controller 不直接访问 Mapper
- Python 不写业务数据库
- Vue 页面不直接发请求
- 跨语言协议只有一个事实来源
- 后续可以替换模型、渲染器和存储实现

## 2. Maven 模块依赖

根工程使用以下模块：

```text
frametalk
├── frametalk-commons
├── frametalk-core
├── frametalk-infrastructure
├── frametalk-interfaces
└── frametalk-boot
```

允许的依赖：

```text
frametalk-commons
    ^
    |
frametalk-core
    ^             ^
    |             |
frametalk-interfaces frametalk-infrastructure
    ^             ^
    |             |
    +-- frametalk-boot --+
```

正式规则：

| 模块 | 可以依赖 | 禁止依赖 |
| --- | --- | --- |
| `frametalk-commons` | JDK | Spring、MyBatis、Servlet、其他业务模块 |
| `frametalk-core` | `frametalk-commons` | Spring、MyBatis、Web、MySQL Driver、Jackson |
| `frametalk-infrastructure` | `frametalk-core`、Spring、MyBatis、Flyway | `frametalk-interfaces`、`frametalk-boot` |
| `frametalk-interfaces` | `frametalk-core`、Spring Web、Validation | `frametalk-infrastructure`、MyBatis Mapper |
| `frametalk-boot` | `frametalk-interfaces`、`frametalk-infrastructure` | 业务实现代码 |

模块之间不允许循环依赖。

## 3. Java 包命名

根包：

```text
com.pydlove.frametalk
```

业务模块按 feature 分包，不按全局技术层分包。

### 3.1 Core

```text
com.pydlove.frametalk.<module>.domain.model
com.pydlove.frametalk.<module>.domain.port
com.pydlove.frametalk.<module>.domain.repository
com.pydlove.frametalk.<module>.application.command
com.pydlove.frametalk.<module>.application.query
```

示例：

```text
com.pydlove.frametalk.script.domain.model.Script
com.pydlove.frametalk.script.domain.repository.ScriptRepository
com.pydlove.frametalk.script.application.command.GenerateScriptCommand
com.pydlove.frametalk.script.application.command.ScriptApplicationService
com.pydlove.frametalk.script.application.query.ScriptQueryService
```

### 3.2 Infrastructure

```text
com.pydlove.frametalk.<module>.infrastructure.persistence
com.pydlove.frametalk.<module>.infrastructure.adapter
```

示例：

```text
com.pydlove.frametalk.script.infrastructure.persistence.ScriptMapper
com.pydlove.frametalk.script.infrastructure.persistence.MyBatisScriptRepository
com.pydlove.frametalk.storage.infrastructure.local.LocalMediaStore
```

### 3.3 Interfaces

```text
com.pydlove.frametalk.<module>.interfaces.rest
com.pydlove.frametalk.<module>.interfaces.dto
```

示例：

```text
com.pydlove.frametalk.script.interfaces.rest.ScriptController
com.pydlove.frametalk.script.interfaces.dto.GenerateScriptRequest
com.pydlove.frametalk.script.interfaces.dto.ScriptVersionResponse
```

### 3.4 共享接口

```text
com.pydlove.frametalk.shared.interfaces.rest
com.pydlove.frametalk.shared.interfaces.dto
```

只放：

- 统一错误响应
- 异常处理
- 分页参数和响应
- 公共请求上下文

不把业务 DTO 放到 shared。

## 4. Core 业务模块

```text
com.pydlove.frametalk
├── identity
├── project
├── script
├── scene
├── asset
├── edit
├── render
├── job
├── capability
└── storage
```

### 4.1 `identity`

负责：

- 用户
- 登录会话
- 权限上下文

不负责：

- 项目业务权限
- 文件读取

### 4.2 `project`

负责：

- 项目创建和状态
- 项目版本 `revision`
- 项目配置
- 平台和目标时长

不负责：

- 脚本内容
- 文件读写

### 4.3 `script`

负责：

- 脚本主记录
- 脚本版本
- 改稿指令
- 脚本状态

不负责：

- LLM 调用
- 分镜结构

### 4.4 `scene`

负责：

- 结构化镜头
- 旁白段落
- 景别、字幕、素材意图
- 分镜版本

不负责：

- 素材文件内容
- 视频渲染

### 4.5 `asset`

负责：

- 素材元数据
- 素材候选
- 上传状态
- 素材引用关系

不负责：

- 分片文件存储
- 模型推理

### 4.6 `edit`

负责：

- `EditManifest` 领域模型
- 时间轴和轨道
- 项目版本绑定
- 剪辑操作

不负责：

- FFmpeg 执行
- Remotion 执行

### 4.7 `render`

负责：

- 渲染任务业务状态
- 预览与成片记录
- 产物校验结果

不负责：

- 直接启动 FFmpeg
- 直接访问媒体路径

### 4.8 `job`

负责：

- 任务创建
- 任务状态机
- 优先级
- 租约和重试策略
- 幂等键

不负责：

- 执行具体能力
- 写模型响应

### 4.9 `capability`

负责：

- 能力注册表
- 能力任务协议
- Worker 心跳和进度
- 任务结果验收

不负责：

- Python 实现
- 模型 Prompt

### 4.10 `storage`

负责：

- `MediaStore` 端口
- 文件路径规则接口
- 媒体流读取抽象
- 磁盘空间检查端口

不负责：

- 具体文件系统实现
- HTTP 下载

## 5. Infrastructure 边界

Infrastructure 实现 Core 定义的端口。

```text
infrastructure/
├── persistence/mysql
├── storage/local
├── storage/media
├── capability/http
├── job/scheduler
├── security
└── config
```

### 5.1 MySQL

允许：

- MyBatis Mapper
- SQL
- Row Entity
- Row 与领域对象的转换
- Flyway

禁止：

- 在 Controller 中调用 Mapper
- 在 Core 中引用 MyBatis
- 把数据库 Entity 直接返回给 Vue

### 5.2 本机文件

允许：

- 路径解析
- 分片合并
- 原子移动
- SHA-256
- `ffprobe`
- Range 读取

禁止：

- 接受前端拼好的绝对路径
- 把任意路径写入数据库
- 让 Python 访问根目录之外的路径

### 5.3 能力调度

Infrastructure 提供：

- 任务领取 SQL
- 租约超时扫描
- Python 心跳校验
- 能力结果校验

任务领取算法可以变化，但不能改变 Core 的任务状态语义。

## 6. Interfaces 边界

### 6.1 公开 API

```text
/api/v1/**
```

只允许：

- 参数校验
- 身份获取
- Core 应用服务调用
- DTO 转换
- HTTP 状态映射

禁止：

- 直接执行 SQL
- 直接写文件
- 直接调用 Python
- 直接拼接 FFmpeg 命令

### 6.2 内部 API

```text
/internal/v1/capability/**
```

只允许：

- Python Worker 调用
- 内部 Token 校验
- 任务领取、心跳、进度和结果回写
- 产物路径和哈希校验

内部 API 不得由 Vue 访问，也不得经过公开静态资源代理。

## 7. Vue 分包

```text
frametalk-web/src/
├── app/
│   ├── App.vue
│   ├── AppShell.vue
│   └── navigation.ts
├── api/
│   ├── client.ts
│   ├── projects.ts
│   ├── scripts.ts
│   ├── assets.ts
│   ├── edits.ts
│   ├── jobs.ts
│   └── renders.ts
├── components/
│   ├── ui/
│   ├── shell/
│   └── business/
├── composables/
├── router/
├── stores/
├── styles/
├── types/
├── utils/
└── views/
```

### 7.1 依赖规则

```text
views -> business -> ui
views -> stores -> api
business -> stores + types + ui
ui -> 无业务依赖
```

具体规则：

| 目录 | 可以依赖 | 禁止依赖 |
| --- | --- | --- |
| `components/ui` | Vue、设计令牌、Lucide | API、Store、业务类型 |
| `components/shell` | UI、路由定义 | 业务 Store、业务请求 |
| `components/business` | UI、Store、API、业务类型 | 直接操作本地文件 |
| `views` | 业务组件、Store | 直接 `fetch` |
| `api` | `client.ts`、生成类型 | Vue 组件、Pinia Store |
| `stores` | `api`、业务类型 | DOM、组件实现 |
| `utils` | 标准库、纯类型 | API、Store、Vue 生命周期 |

### 7.2 UI 组件

基础组件在 `components/ui/`：

```text
UiButton
UiInput
UiTextarea
UiFormField
UiSelect
UiCombobox
UiDialog
UiConfirmDialog
UiSegmentedControl
UiSearchInput
UiPagination
UiSpinner
UiStatusDot
```

业务页面不能复制按钮、输入框、菜单和弹窗的视觉实现。

### 7.3 业务组件

业务组件按领域拆分：

```text
business/
├── project/
├── script/
├── scene/
├── asset/
├── edit/
├── render/
└── settings/
```

示例：

```text
business/script/ScriptEditor.vue
business/script/ScriptVersionList.vue
business/scene/SceneList.vue
business/asset/AssetPicker.vue
business/edit/TranscriptTimeline.vue
business/render/RenderQueue.vue
```

一个业务组件不能同时实现两个领域。跨领域组合放在 `views/` 或 `app/`。

### 7.4 类型

公开 API 类型从 Java OpenAPI 生成：

```text
src/types/generated/api.d.ts
```

业务展示类型：

```text
src/types/<domain>.ts
```

不得在组件中手写 API 返回结构。

## 8. Python 分包

```text
frametalk-capability/src/frametalk_capability/
├── __init__.py
├── __main__.py
├── config.py
├── logging.py
├── client/
│   └── control_plane.py
├── worker/
│   ├── runner.py
│   ├── heartbeat.py
│   └── registry.py
├── capabilities/
│   ├── script_generate.py
│   ├── script_rewrite.py
│   ├── scene_plan.py
│   ├── transcript_create.py
│   ├── media_probe.py
│   └── render_preview.py
├── providers/
│   ├── llm.py
│   └── asr.py
├── media/
│   ├── ffmpeg.py
│   ├── probe.py
│   └── paths.py
├── rendering/
│   ├── remotion.py
│   └── result.py
└── contracts/
    ├── jobs.py
    ├── results.py
    └── edit_manifest.py
```

### 8.1 Python 禁止项

- 不引入数据库驱动
- 不引入 SQLAlchemy
- 不直接访问 MySQL
- 不写 `project`、`script_version`、`render_job` 等业务表
- 不暴露公开 HTTP API 给 Vue
- 不读取任务目录外的任意文件

### 8.2 Capability 契约

每个 capability 必须提供：

```text
name
input_model
output_model
execute(context, input) -> output
```

注册表负责名称到实现的映射。Worker 不写 `if capability == ...` 的业务分支。

## 9. Contracts

```text
contracts/
├── capability-api.openapi.yaml
├── capability-result.schema.json
├── edit-manifest.schema.json
└── README.md
```

### 9.1 规则

- 公开 API Schema 由 Java 生成
- 剪辑清单以 JSON Schema 为事实来源
- Python 的 Pydantic 模型必须通过契约测试
- Java DTO 必须通过 OpenAPI 契约测试
- 新增字段要先兼容读取，再切换写入

### 9.2 版本

所有持久化 Manifest 必须包含：

```text
schemaVersion
```

第一版使用：

```text
1.0
```

## 10. 包边界检查

### 10.1 ArchUnit

`frametalk-boot` 测试必须检查：

- Core 不依赖 Spring、MyBatis、Servlet
- Interfaces 不依赖 Infrastructure
- Infrastructure 不依赖 Interfaces
- Boot 之外不出现启动类

### 10.2 前端检查

后续增加：

- UI 组件不能导入 API/Store
- 页面不能直接 `fetch`
- 业务样式不能绕过 tokens
- 小于 11px 字号检查
- 业务组件不重复定义基础控件

### 10.3 Python 检查

后续增加：

- 禁止 MySQL 和 SQLAlchemy 依赖
- capability 必须注册
- 输入输出必须使用 Pydantic 模型
- 任务路径必须通过安全路径解析器

## 11. 新增模块流程

新增业务模块前必须回答：

1. 它拥有哪些业务数据？
2. 它是否是独立业务能力，而不只是一个工具类？
3. 它的领域端口是什么？
4. 它的外部适配器在哪里？
5. 它的公开 API 和内部能力协议是什么？
6. 前端由哪个 view 组合它？
7. 它是否需要新增共享契约？

不能仅因为“文件很多”或“想拆得干净”就新增模块。

## 12. 命名

- Java 类：PascalCase
- Java 包：小写，按 feature
- Vue 组件：PascalCase + `.vue`
- Python 模块：snake_case
- REST 路径：复数名词 + kebab-case
- capability 名称：`domain.action`
- 数据库表：snake_case
- 枚举值：小写 snake_case
- API 字段：camelCase
- 文件路径：UUID + 安全扩展名

## 13. 第一阶段完成标准

- 根 Maven 工程包含五个 Java 模块
- 模块依赖方向与本文一致
- Vue 包名、目录和设计令牌基础存在
- Python 包名和能力注册结构存在
- `contracts/` 目录和契约说明存在
- 已包含应用壳、登录注册、账号设置、外观设置和项目空态，不包含项目编辑器或时间轴实现
