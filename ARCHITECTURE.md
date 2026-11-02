# 帧言技术架构与代码分包

## 1. 文档定位

本文定义帧言（FrameTalk）第一版的技术架构、模块边界、依赖方向和代码分包方式。

详细的后端实现约束见 [后端技术选型](docs/backend-tech-stack.md)，具体包名和目录规则见 [代码分包规则](docs/package-boundaries.md)，前端视觉与组件约束见 [前端设计系统](docs/frontend-design-system.md)。

当前阶段已完成身份认证、登录注册、基础应用壳和设置页。脚本生成、语音转写和视频渲染流水线尚未实现。

## 2. 产品范围

帧言服务真人出镜的自媒体内容生产流程：

```text
项目配置
  -> AI 生成脚本
  -> 人工改稿
  -> 生成分镜和拍摄单
  -> 真人拍摄
  -> 上传原始视频
  -> 语音转写
  -> 文字化粗剪
  -> 素材与字幕包装
  -> 预览和导出
```

第一版只支持：

- 单个真人口播内容
- 1 到 3 分钟视频
- 1080x1920 竖屏输出
- 中文脚本和中文语音
- 本机 MySQL
- 本机磁盘存储

第一版不支持：

- TTS
- 数字人
- 唇形同步
- 多人实时协作
- 分布式渲染
- 对象存储
- Redis

## 3. 系统分层

```text
┌──────────────────────────────────────────────────────┐
│ Vue 3 交互面                                          │
│ 项目配置、脚本编辑、分镜、素材、时间轴、任务状态       │
└──────────────────────────┬───────────────────────────┘
                           │ HTTPS / JSON / SSE
┌──────────────────────────▼───────────────────────────┐
│ Java 控制面                                           │
│ 用户、项目、脚本版本、分镜、素材元数据、剪辑清单、任务 │
│ Spring Boot + MySQL + 本机媒体流                       │
└──────────────────────────┬───────────────────────────┘
                           │ 内部 HTTP / JSON
┌──────────────────────────▼───────────────────────────┐
│ Python 能力面                                         │
│ LLM、ASR、媒体探测、FFmpeg、Remotion、画面分析         │
└──────────────────────────┬───────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────┐
│ 本机资源                                              │
│ MySQL、原始视频、代理文件、素材、中间文件、成片、日志 │
└──────────────────────────────────────────────────────┘
```

## 4. 架构原则

### 4.1 Java 是业务事实来源

所有业务状态写入 MySQL：

- 用户和权限
- 项目与版本
- 脚本版本
- 分镜
- 素材元数据
- 剪辑清单
- 任务状态
- 渲染结果

Python 不连接 MySQL，也不能修改业务表。

### 4.2 Python 是能力执行层

Python 只接收受控任务，返回结构化结果：

- 脚本生成和改稿
- 分镜规划
- 语音转写
- 媒体探测和转码
- 视频渲染
- 封面候选

Python 不负责项目配置、权限和业务状态机。

### 4.3 Vue 只访问 Java

Vue 不直接调用 Python，不接触内部 Token，不拼接本机文件路径。

### 4.4 共享契约先于实现

跨语言数据必须通过版本化契约：

- 公开 API：Java OpenAPI
- 能力任务：内部 OpenAPI
- 剪辑清单：JSON Schema
- 状态与错误码：共享枚举文档

不靠“双方记得字段一样”维护跨语言协议。

### 4.5 本机优先

第一版按单机部署设计：

- 不做微服务注册中心
- 不引入消息中间件
- 不使用对象存储
- API 和 Worker 使用本机目录交换大文件
- 所有临时文件可以清理和重建

## 5. Java 模块

Java 控制面使用 Maven 多模块：

```text
frametalk-boot
├── frametalk-interfaces
├── frametalk-infrastructure
└── frametalk-core
    └── frametalk-commons
```

依赖只能向下：

```text
frametalk-boot
  ├──> frametalk-interfaces ──> frametalk-core ──> frametalk-commons
  └──> frametalk-infrastructure ──> frametalk-core ──> frametalk-commons
```

### 5.1 模块职责

| 模块 | 职责 | 禁止事项 |
| --- | --- | --- |
| `frametalk-commons` | 基础异常、错误码、ID、时间等纯 Java 构件 | 不依赖 Spring、MyBatis、Servlet |
| `frametalk-core` | 领域模型、领域端口、仓储接口、应用服务 | 不依赖 Spring、MyBatis、Web |
| `frametalk-infrastructure` | MyBatis、MySQL、Flyway、本地磁盘、能力调度、Scheduler | 不写 Controller，不定义业务规则 |
| `frametalk-interfaces` | REST Controller、DTO、参数校验、内部能力 API | 不直接访问 Mapper，不写事务 |
| `frametalk-boot` | Spring Boot 启动、装配、配置、ArchUnit 检查 | 不放业务实现 |

### 5.2 核心业务包

`frametalk-core` 按业务模块分包：

```text
identity
project
script
scene
asset
edit
render
job
capability
storage
```

每个模块内部按以下方式组织：

```text
domain/
├── model/
├── port/
└── repository/
application/
├── command/
└── query/
```

具体规则见 [代码分包规则](docs/package-boundaries.md)。

## 6. Python 能力模块

Python 工程：

```text
frametalk-capability/
├── src/frametalk_capability/
│   ├── client/          Java 内部 API 客户端
│   ├── worker/          领取、心跳、取消和回写
│   ├── capabilities/    能力实现与注册
│   ├── providers/       LLM/ASR 供应商适配
│   ├── media/           FFmpeg、ffprobe、代理文件
│   ├── rendering/       Remotion 调用和产物校验
│   └── contracts/       Pydantic 契约
├── tests/
└── pyproject.toml
```

Python 能力至少包括：

```text
script.generate
script.rewrite
scene.plan
transcript.create
media.probe
media.transcode_proxy
edit.render_preview
edit.render_final
cover.generate
```

能力实现由 registry 注册，Worker 不写具体业务分支。

## 7. Vue 前端模块

前端工程：

```text
frametalk-web/src/
├── app/                 应用壳、导航和全局装配
├── api/                 按业务域拆分的 HTTP 出口
├── components/
│   ├── ui/              基础视觉组件
│   ├── shell/           标题栏、侧边栏、布局
│   └── business/        业务组件
├── composables/         可复用交互逻辑
├── router/              路由和权限守卫
├── stores/              Pinia 业务状态
├── styles/              设计令牌和全局样式
├── types/               OpenAPI 生成类型
├── utils/               纯函数
└── views/               页面级组合
```

业务组件按领域继续拆分：

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

### 7.1 前端参考 WorkCove 的部分

参考：

- 语义设计令牌
- 紧凑但可读的信息密度
- macOS 原生空间关系
- `Ui*` 基础组件
- 浅色、深色和跟随系统
- 自定义下拉、弹窗和焦点行为
- Lucide 线性图标

不复制：

- WorkCove 的业务导航和信息架构
- WorkCove 的工作时钟、网址、Codex 等业务组件
- 没有桌面壳时显示假的红黄绿窗口按钮
- WorkCove 的版权 footer 和品牌标识
- 页面中一次性硬编码的颜色、圆角和阴影

具体约束见 [前端设计系统](docs/frontend-design-system.md)。

## 8. 共享契约

`contracts/` 存放跨语言契约：

```text
contracts/
├── capability-api.openapi.yaml
├── edit-manifest.schema.json
├── capability-result.schema.json
└── README.md
```

### 8.1 契约所有权

| 契约 | 所有者 | 使用方 |
| --- | --- | --- |
| 公开 OpenAPI | Java interfaces | Vue、测试工具 |
| 能力内部 OpenAPI | Java interfaces + Python client | Java、Python |
| EditManifest Schema | Java core + Python contracts | Java、Python、Remotion |
| Capability Result Schema | Java core + Python contracts | Java、Python |

### 8.2 变更规则

- Schema 必须带版本
- 删除字段视为破坏性变更
- 新增必填字段必须先升级使用方
- Java 和 Python 都要有契约测试
- 剪辑清单校验失败时禁止进入渲染

## 9. 关键执行链

### 9.1 生成脚本

```text
Vue -> Java 创建 script.generate
Java -> MySQL 写入 job_run
Python -> Java /internal/capability/jobs/claim
Python -> LLM
Python -> 写 result.json
Python -> Java complete
Java -> 校验结果并保存 script_version
Vue -> 轮询或 SSE 获取结果
```

### 9.2 真人视频转写

```text
Vue -> Java 分片上传视频
Java -> 本机磁盘落盘
Java -> ffprobe/媒体校验
Java -> 创建 transcript.create
Python -> 领取任务
Python -> faster-whisper
Python -> 写 transcript.json
Python -> Java complete
Java -> 保存 transcript_segment
```

### 9.3 自动粗剪

```text
Vue -> Java 保存编辑操作
Java -> 生成 EditManifest
Java -> 创建 edit.render_preview
Python -> 校验任务和 Manifest
Python -> FFmpeg + Remotion
Python -> 写预览文件和 result.json
Python -> Java complete
Java -> 保存 render_job
Vue -> 播放预览
```

## 10. 数据与文件边界

### 10.1 MySQL 保存

- 业务实体
- 版本号
- 任务状态
- 任务租约
- 文件相对路径
- 文件 SHA-256
- 剪辑清单
- 能力调用记录

### 10.2 本机磁盘保存

- 原始视频
- 音频
- 代理视频
- B-roll
- 图片
- 字幕
- 渲染中间产物
- 成片
- 日志

### 10.3 文件引用规则

- MySQL 只保存相对路径
- Java 负责把相对路径解析到受控根目录
- Python 只访问 Java 授权的工作目录
- 前端只能通过 Java 媒体接口访问文件
- 任何 `..`、绝对路径和符号链接逃逸都必须拒绝

## 11. 任务与并发

任务队列仍由 Java + MySQL 承担。

- Worker 主动领取
- 短事务 + `SKIP LOCKED`
- Java 管理租约
- Python 发送心跳
- 超时任务由 Java 恢复
- 单机默认渲染并发为 1

Java 不直接启动 Python 子进程。这样 Python 崩溃、重启或升级不会影响控制面。

## 12. 前端风格基线

视觉方向：

> macOS 原生工作空间的空间关系 + 视频创作工具的高信息密度。

核心布局：

```text
┌──────────────────────────────────────────────┐
│ 46px 顶栏：品牌、项目切换、任务、账户         │
├──────────────┬───────────────────────────────┤
│ 216px 侧栏    │ 工作区                        │
│ 项目导航      │ 脚本 / 预览 / 素材 / 时间轴    │
└──────────────┴───────────────────────────────┘
```

项目工作区使用可调整三栏：

```text
脚本与逐字稿 | 视频预览与属性 | 素材与任务
                    时间轴
```

关键约束：

- 白色画布为主
- 视频预览区允许使用深色背景
- 不使用大面积渐变和装饰光斑
- 不使用 emoji 功能图标
- 所有颜色和尺寸来自语义令牌
- 不使用负字距
- 小于 11px 的业务文字禁止出现
- 所有页面支持浅色、深色和移动端

## 13. 构建与验证

Java：

```bash
mvn clean verify
```

前端：

```bash
cd frametalk-web
pnpm install
pnpm typecheck
pnpm build
```

Python：

```bash
cd frametalk-capability
uv sync
uv run pytest
```

未安装 `uv` 时改用项目虚拟环境安装 `.[dev]`，不要修改系统 Python。

第一阶段的验证目标是：

- Java 多模块依赖可以构建
- Vue 工程可以类型检查和构建
- Python 包可以导入
- 共享契约文件存在且可解析

## 14. 实施顺序

1. 工程骨架、模块依赖和设计令牌
2. 身份、项目、脚本和任务表
3. 能力领取协议和 Python Worker
4. 脚本生成和改稿
5. 分片上传、媒体探测和 ASR
6. 剪辑清单、预览和导出
7. 素材库、封面和发布包

当前阶段已完成第 1 步，并实现登录注册、账号安全和基础设置页面。
