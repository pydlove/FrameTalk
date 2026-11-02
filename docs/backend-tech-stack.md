# 帧言后端技术选型

总体分层和代码边界以 [架构与分包](../ARCHITECTURE.md) 为准。

## 0. 方案定位

按分层要求，本项目采用：

```text
Vue 配置与操作界面
        |
        v
Java 控制面：项目、配置、权限、素材元数据、任务编排、MySQL
        |
        | 内部 HTTP
        v
Python 能力面：AI 生成、ASR、视频分析、FFmpeg、Remotion
        |
        v
本机磁盘：原始视频、素材、中间文件、成片
```

这里的“配置层”理解为控制面，负责业务状态和用户操作，不负责模型推理和媒体计算。

核心约束不变：

1. 关系数据库只能使用 MySQL。
2. 不使用 Redis。
3. 不使用对象存储、MinIO 或云文件存储。
4. 所有媒体文件保存在本机磁盘。
5. 真人出镜，不接 TTS、数字人和唇形同步。
6. 第一版部署在单台电脑上。

## 1. 总体技术选型

### 1.1 Java 控制面

| 项目 | 选型 |
| --- | --- |
| JDK | Java 17 LTS |
| 框架 | Spring Boot 3.5.x |
| 构建 | Maven |
| Web | Spring Web MVC |
| 权限 | Spring Security |
| 会话 | Spring Session JDBC |
| 数据访问 | MyBatis-Plus + 显式 SQL |
| 数据库 | MySQL 8.0+ |
| 数据迁移 | Flyway |
| 参数校验 | Jakarta Validation |
| API 文档 | springdoc-openapi |
| 任务调度 | Spring Scheduler + MySQL 任务表 |
| JSON | Jackson |
| 日志 | SLF4J + Logback JSON |
| 测试 | JUnit 5 + Spring Boot Test |

Java 负责任务状态机、任务租约、项目版本和所有数据库写入，Python 不直接连接 MySQL。

### 1.2 Vue 前端

| 项目 | 选型 |
| --- | --- |
| 框架 | Vue 3 |
| 语言 | TypeScript |
| 构建 | Vite |
| 路由 | Vue Router |
| 状态 | Pinia |
| 图标 | @lucide/vue |
| UI 体系 | 自有 Ui* 基础组件 + 语义设计令牌，不使用通用组件库 |
| HTTP | 基于 OpenAPI 生成的类型化客户端 |

Vue 只调用 Java 的公开 API，不直接调用 Python，也不能访问本机媒体目录。

### 1.3 Python 能力面

| 项目 | 选型 |
| --- | --- |
| 版本 | Python 3.12 |
| 服务框架 | FastAPI，用于健康检查和内部诊断接口 |
| 数据校验 | Pydantic 2 |
| HTTP 客户端 | httpx |
| 包管理 | uv |
| ASR | faster-whisper |
| 视频处理 | FFmpeg |
| 动态包装 | Remotion CLI |
| 画面分析 | MediaPipe/OpenCV |
| 文本模型 | 通过自研 Model Gateway 接入 |
| 测试 | pytest |

Python 不持有业务数据库连接，不负责用户权限和项目配置。

### 1.4 本地任务队列

不使用 Redis 和 Celery。

任务队列由 Java + MySQL 实现：

- Java 提供内部任务领取接口
- Python Worker 主动领取任务
- MySQL 保存任务、租约、进度和结果
- Java 负责超时任务恢复
- Java 负责把能力结果写回业务表

## 2. 职责边界

| 能力 | Java 控制面 | Python 能力面 | Vue |
| --- | --- | --- | --- |
| 用户登录 | 负责 | 不负责 | 调用接口 |
| 项目 CRUD | 负责 | 不负责 | 提供界面 |
| 脚本版本 | 负责 | 不负责 | 显示和编辑 |
| 脚本生成 | 创建任务 | 调用 LLM 生成 | 触发和展示 |
| 脚本改稿 | 保存新版本 | 执行改稿 | 显示差异 |
| 分镜配置 | 保存和校验 | 生成建议 | 编辑确认 |
| 素材元数据 | 负责 | 分析素材 | 管理素材 |
| 文件上传 | 负责落盘 | 不负责 | 选择文件 |
| 视频转写 | 创建任务 | 执行 ASR | 显示任务进度 |
| 视频剪辑 | 保存剪辑清单 | 执行 FFmpeg/Remotion | 预览和微调 |
| 成片导出 | 记录产物 | 生成本地文件 | 下载成片 |
| 模型配置 | 保存业务配置 | 读取运行配置 | 管理模型 |

### 2.1 为什么 Python 不直接连 MySQL

Python 只接收一个受控的任务描述，返回结构化结果。这样可以：

- 避免 Python 和 Java 各自维护一套数据库模型
- 避免 Python 修改业务状态
- 方便替换模型和渲染实现
- 方便后续把 Python Worker 放到另一台机器
- 让 Java 成为唯一的业务事实来源

### 2.2 为什么使用 Java 作为控制面

Java 适合处理：

- 权限和会话
- 配置和版本
- 事务和审计
- 任务状态机
- 对外 API
- 文件元数据管理

Python 适合处理：

- LLM 调用
- Prompt 和输出校验
- 语音识别
- 图像和视频分析
- FFmpeg 参数处理
- Remotion 调用

## 3. 部署结构

第一版在单机上运行以下进程：

```text
frametalk-web       Vue 构建产物，由 Java 或本地 Web Server 提供
frametalk-control   Spring Boot，公开 API
frametalk-ai        Python Worker，处理 ai 队列
frametalk-asr       Python Worker，处理 transcribe 队列
frametalk-media     Python Worker，处理 media 队列
frametalk-render    Python + Node/Chromium Worker，处理 render 队列
mysql            MySQL 8.0+
```

如果资源有限，也可以把 Python Worker 合并成一个进程，通过不同并发配置隔离，但生产时建议至少拆分 `render` 与 `ai`。

### 3.1 进程关系

```text
Browser
   |
   v
Vue
   |
   v
Spring Boot 控制面
   |          \
   |           \---> MySQL
   |
   +---- 内部领取/回写接口 <---- Python Worker
                                  |
                                  +--> FFmpeg / ASR / Remotion
                                  |
                                  +--> 本机磁盘
```

### 3.2 网络绑定

- 公开 API 绑定本机可访问地址
- Python 内部接口只允许本机访问
- 内部接口使用独立 Token 或 HMAC
- Vue 不接触内部 Token
- Python 不直接暴露任务执行接口给公网

## 4. 代码目录

建议仓库结构：

```text
FrameTalk/
├── pom.xml
├── frametalk-commons/
├── frametalk-core/
├── frametalk-infrastructure/
├── frametalk-interfaces/
├── frametalk-boot/
├── frametalk-capability/
│   ├── pyproject.toml
│   └── src/frametalk_capability/
│       ├── client/
│       ├── worker/
│       ├── capabilities/
│       ├── integrations/
│       ├── media/
│       ├── rendering/
│       └── contracts/
├── frametalk-web/
│   ├── package.json
│   └── src/
├── contracts/
├── data/
│   ├── media/
│   ├── jobs/
│   ├── tmp/
│   └── logs/
├── docs/
└── README.md
```

`data/` 不进 Git，由 `FRAMETALK_DATA_DIR` 配置。

## 5. Java 控制面设计

### 5.1 模块划分

```text
auth          登录、登出、会话
project       项目、版本、状态
script        脚本和脚本版本
scene         分镜和镜头配置
asset         素材元数据和文件索引
edit          剪辑清单
render        预览和成片
job           任务状态机和租约
capability    任务领取、进度和结果回写
storage       本地文件路径、Range 读取、清理
```

模块之间通过 service 调用，不跨模块直接操作别人的表。

### 5.2 技术约定

- Controller 只负责 HTTP 入参出参
- Service 负责业务用例和事务
- Mapper 负责 SQL
- DTO 和数据库 Entity 分离
- 所有接口返回统一错误结构
- 所有任务写入必须带幂等键
- 数据库迁移由 Flyway 管理

### 5.3 会话

不使用 Redis Session，使用 Spring Session JDBC：

- Session 数据保存到 MySQL
- Cookie 使用 HttpOnly
- 写接口验证 CSRF
- 登出撤销 Session
- Session 过期由数据库和 Spring 管理

## 6. MySQL 设计

### 6.1 基本约定

- MySQL 8.0+，InnoDB
- `utf8mb4`
- 时间使用 UTC
- 大文本使用 `TEXT`
- 动态结构使用 `JSON`
- 文件和视频不存入 MySQL
- 所有表通过 Flyway 初始化

### 6.2 核心表

| 表 | 说明 |
| --- | --- |
| `app_user` | 用户 |
| `user_session` | Spring Session JDBC 使用的会话表 |
| `project` | 项目 |
| `project_revision` | 项目版本和乐观锁 |
| `script` | 脚本主记录 |
| `script_version` | 脚本历史版本 |
| `scene` | 结构化镜头 |
| `upload_session` | 分片上传会话 |
| `media_asset` | 原始素材、代理文件和成片索引 |
| `asset_candidate` | 镜头对应素材候选 |
| `transcript` | 转写记录 |
| `transcript_segment` | 句子和词级时间戳 |
| `edit_manifest` | 版本化剪辑清单 |
| `job_run` | 任务队列、租约、进度和错误 |
| `render_job` | 渲染产物 |
| `model_call_log` | 模型调用和成本 |
| `capability_config` | 模型、ASR、渲染参数等业务配置 |
| `audit_log` | 关键配置和版本变更 |

### 6.3 文件和数据库边界

`media_asset` 保存：

```text
id
owner_id
project_id
asset_type
original_name
relative_path
mime_type
size_bytes
sha256
width
height
duration_ms
status
created_at
```

文件内容只存在本机磁盘，MySQL 不保存视频或图片二进制。

### 6.4 项目版本

每个项目使用递增 `revision`：

```text
revision = 1, 2, 3, ...
```

客户端提交修改时携带当前版本。Java 使用乐观锁：

- 版本一致：保存并递增
- 版本不一致：返回 `409 Conflict`

任务绑定具体的项目版本。任务执行过程中，项目可以继续编辑，但渲染结果只对应它绑定的版本。

### 6.5 素材搜索

第一版使用 MySQL `FULLTEXT`：

- 文件名
- 用户标签
- 素材描述
- 脚本和台词文本

不使用 pgvector、Milvus 或独立向量服务。

后续如果确实需要语义检索，可以在本机使用 FAISS 索引，向量文件放在磁盘；索引只是派生数据，可以从 MySQL 重建。

## 7. MySQL 任务队列

### 7.1 核心字段

`job_run` 至少包含：

```text
id
queue_name
capability
status
priority
payload_json
project_id
project_revision
idempotency_key
attempts
max_attempts
run_after
lease_owner
lease_expires_at
heartbeat_at
progress
stage
result_manifest_json
error_code
error_message
created_at
updated_at
```

### 7.2 任务状态

```text
queued
running
succeeded
failed
cancelled
```

允许重试的任务从 `running` 回到 `queued`，但必须满足：

- 未达到 `max_attempts`
- 错误类型可重试
- `run_after` 到达

### 7.3 Java 领取任务

Java 在内部接口中执行短事务：

```sql
START TRANSACTION;

SELECT id
FROM job_run
WHERE queue_name = ?
  AND status = 'queued'
  AND run_after <= UTC_TIMESTAMP(6)
ORDER BY priority DESC, created_at ASC
LIMIT 1
FOR UPDATE SKIP LOCKED;

UPDATE job_run
SET status = 'running',
    lease_owner = ?,
    lease_expires_at = DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 60 SECOND),
    heartbeat_at = UTC_TIMESTAMP(6),
    attempts = attempts + 1
WHERE id = ?
  AND status = 'queued';

COMMIT;
```

领取事务只负责拿任务，不等待任务执行完成。

### 7.4 租约和心跳

- Python Worker 领取任务后获得 `lease_owner`
- Worker 每 15 秒调用心跳接口
- 默认租约 60 秒
- Java 定时扫描超时租约
- 超时任务重新入队或标记失败
- 同一个任务可能被重复执行，因此必须幂等

### 7.5 幂等键

幂等键建议为：

```text
capability + project_id + project_revision + input_hash
```

重复创建同一任务时：

- 返回已有任务
- 不重复调用模型
- 不重复渲染
- 如果已有结果，直接返回结果引用

### 7.6 单机并发

| 队列 | 默认并发 | 原因 |
| --- | --- | --- |
| `ai` | 1 到 2 | 受模型额度或本地显存限制 |
| `transcribe` | 1 | CPU/GPU 和内存占用高 |
| `media` | 1 | FFmpeg 和磁盘吞吐有限 |
| `render` | 1 | Chromium 和视频编码占用高 |
| `maintenance` | 1 | 清理、租约恢复和备份 |

并发数放入 `capability_config` 或配置文件，不写死在业务代码中。

## 8. Java 与 Python 的内部协议

Java 不直接执行 Python 代码，Python 也不访问 MySQL。双方通过本机 HTTP/JSON 交流，接口契约由 OpenAPI 管理。

### 8.1 Python 领取任务

```http
POST /internal/v1/capability/jobs/claim
Content-Type: application/json
X-Capability-Token: <token>
```

请求：

```json
{
  "workerId": "ai-worker-1",
  "queues": ["ai", "transcribe"],
  "capabilities": [
    "script.generate",
    "script.rewrite",
    "scene.plan",
    "transcript.create"
  ],
  "leaseSeconds": 60
}
```

响应：

```json
{
  "jobId": "10086",
  "capability": "script.generate",
  "idempotencyKey": "script.generate:42:7:abc",
  "leaseExpiresAt": "2026-11-02T12:00:00Z",
  "workDir": "jobs/10086/attempt-1",
  "payload": {
    "projectId": 42,
    "revision": 7,
    "brief": {
      "topic": "AI 如何赋能自媒体",
      "durationSeconds": 90
    }
  }
}
```

没有可执行任务时返回 `204 No Content`。

### 8.2 心跳

```http
POST /internal/v1/capability/jobs/{jobId}/heartbeat
```

```json
{
  "workerId": "ai-worker-1",
  "leaseSeconds": 60
}
```

Java 校验 `jobId + workerId + lease` 后再续租。

### 8.3 进度

```http
POST /internal/v1/capability/jobs/{jobId}/progress
```

```json
{
  "workerId": "ai-worker-1",
  "stage": "transcribing",
  "progress": 42
}
```

进度只用于展示，不作为任务完成的依据。

### 8.4 成功回写

```http
POST /internal/v1/capability/jobs/{jobId}/complete
```

```json
{
  "workerId": "ai-worker-1",
  "result": {
    "resultFile": "jobs/10086/attempt-1/result.json",
    "artifacts": [
      {
        "type": "transcript",
        "path": "jobs/10086/attempt-1/transcript.json",
        "sha256": "..."
      }
    ]
  }
}
```

对于脚本、分镜这类小结果，`result.json` 由 Java 读取并校验；对于视频和音频，只传递相对路径、哈希和媒体元数据。

### 8.5 失败回写

```http
POST /internal/v1/capability/jobs/{jobId}/fail
```

```json
{
  "workerId": "ai-worker-1",
  "errorCode": "ASR_FAILED",
  "message": "audio decode failed",
  "retryable": true
}
```

Java 根据 `retryable`、尝试次数和错误类型决定是否重新入队。

### 8.6 路径安全

Java 下发的 `workDir` 只能是受控根目录内的相对路径。Python 必须：

- 使用配置的 `FRAMETALK_DATA_DIR` 解析路径
- 拒绝 `..`、绝对路径和符号链接逃逸
- 只读写任务目录和明确授权的素材目录
- 在完成回写前校验每个产物的哈希

## 9. 能力清单

Python 第一批实现以下 capability：

| capability | 输入 | 输出 |
| --- | --- | --- |
| `script.generate` | 项目简报、风格配置 | 脚本版本 JSON |
| `script.rewrite` | 原脚本、改稿指令、目标平台 | 新脚本版本 |
| `scene.plan` | 脚本、镜头约束 | 结构化场景列表 |
| `transcript.create` | 真人视频或音频 | 逐字稿和词级时间戳 |
| `media.probe` | 媒体文件 | 编码、时长、分辨率、音轨信息 |
| `media.transcode_proxy` | 原始视频 | 低码率代理文件 |
| `edit.render_preview` | 剪辑清单 | 540p 预览视频 |
| `edit.render_final` | 剪辑清单 | 1080x1920 成片 |
| `cover.generate` | 项目、脚本、视频帧 | 封面候选 |

不实现：

- `tts.synthesize`
- `avatar.generate`
- `lip_sync`

## 10. 完整任务流程

### 10.1 生成脚本

```text
Vue 提交项目简报
  -> Java 保存 Brief
  -> Java 创建 job_run(script.generate)
  -> Python ai-worker 领取任务
  -> Python 调用 LLM，写入 result.json
  -> Python complete
  -> Java 校验结果并生成 script_version
  -> Vue 显示新版本
```

### 10.2 上传真人视频

```text
Vue 创建上传会话
  -> Java 返回 uploadId
  -> Vue 分片上传
  -> Java 合并到本机临时文件
  -> ffprobe 校验
  -> Java 移动到正式素材目录
  -> Java 创建 media_asset
```

### 10.3 转写和剪辑

```text
Vue 选择真人视频和脚本
  -> Java 创建 transcript.create 任务
  -> Python ASR Worker 领取任务
  -> Python 生成 transcript.json
  -> Java 保存 transcript_segment
  -> Java 组织默认剪辑清单
  -> Vue 按逐字稿编辑
  -> Java 将修改保存为新的 edit_manifest
```

### 10.4 渲染

```text
Vue 点击预览或导出
  -> Java 创建 render 任务
  -> Python Render Worker 领取任务
  -> FFmpeg 处理视频
  -> Remotion 处理字幕和包装
  -> 输出到工作目录
  -> Python 计算 SHA-256 并 complete
  -> Java 校验文件和媒体信息
  -> Java 登记 render_job
  -> Vue 播放或下载成片
```

## 11. 本地文件系统

### 11.1 目录

```text
FRAMETALK_DATA_DIR=./data
```

```text
data/
├── media/
│   └── users/{userId}/projects/{projectId}/
│       ├── sources/
│       ├── proxies/
│       ├── audio/
│       ├── assets/
│       ├── thumbnails/
│       └── renders/
├── jobs/
│   └── {jobId}/attempt-{n}/
├── tmp/
│   ├── uploads/
│   └── maintenance/
└── logs/
    ├── control/
    ├── ai/
    ├── asr/
    ├── media/
    └── render/
```

`data/` 不提交到 Git。

### 11.2 上传

没有对象存储，视频必须经过 Java 后端上传：

```text
POST /api/v1/uploads
PUT  /api/v1/uploads/{uploadId}/chunks/{chunkIndex}
POST /api/v1/uploads/{uploadId}/complete
POST /api/v1/uploads/{uploadId}/cancel
```

要求：

- 边接收边写临时文件
- 不能把完整视频读入 JVM 堆内存
- 分片建议 8 到 16 MiB
- 服务端记录分片序号和哈希
- 合并后使用 `ffprobe` 校验
- 校验成功后原子移动到正式目录
- 失败的上传临时文件定时清理

### 11.3 媒体访问

Java 提供鉴权的媒体流接口，支持 HTTP Range：

```text
GET /api/v1/media/{assetId}/stream
```

要求：

- 校验用户和项目权限
- 解析路径时限制在 `FRAMETALK_DATA_DIR` 内
- 禁止 `..`、NUL 字节和绝对路径
- 支持视频拖动
- 不把媒体目录直接挂给前端静态服务

### 11.4 磁盘保护

上传、转码和渲染前检查：

- 剩余空间是否足够
- 当前项目是否超出配额
- 临时目录是否超过上限
- 是否还有孤儿任务目录

建议：

- 上传预留文件大小的 1.2 倍
- 渲染预留成片大小的 3 倍
- 低于安全水位时拒绝新任务
- 定时清理超时上传和失败任务目录

### 11.5 备份

本机磁盘不是天然备份。至少提供：

- `mysqldump` 数据库备份
- `data/media` 归档
- `data/jobs` 中未完成任务的恢复策略
- 原始上传优先备份
- 成片可以重新渲染，原始素材不能丢

## 12. 视频处理和渲染

### 12.1 FFmpeg

负责：

- 媒体探测
- 音视频分离
- 代理文件
- 裁切和拼接
- 音频响度标准化
- 基础字幕烧录
- 编码和封装

### 12.2 Remotion

负责：

- 动态字幕
- 关键词花字
- 图表和步骤卡
- 转场包装
- 片头和片尾
- 封面模板
- 9:16 和 16:9 视觉适配

Render Worker 需要：

- Python
- Node.js 20+
- Chromium
- FFmpeg
- 中文字体
- Remotion CLI

### 12.3 剪辑清单

Java 保存剪辑清单，Python 执行：

```text
EditManifest
├── schemaVersion
├── projectId
├── projectRevision
├── output
├── tracks
│   ├── video
│   ├── overlay
│   ├── caption
│   └── audio
└── assets
```

每个片段包含：

```text
sourceAssetId
sourceIn
sourceOut
timelineIn
timelineOut
crop
transform
transition
```

Java 对剪辑清单做 Schema 校验，Python 渲染前再做一次校验。

### 12.4 预览策略

- 前端先播放代理文件
- 首次预览生成 540p 快速版本
- 用户确认后生成 1080x1920 成片
- 渲染任务使用任务目录隔离
- 临时文件成功后再原子重命名

## 13. 前端与 Java API

基础路径：

```text
/api/v1
```

主要公开接口：

```text
POST   /auth/login
POST   /auth/logout

POST   /projects
GET    /projects/{projectId}
PATCH  /projects/{projectId}

POST   /uploads
PUT    /uploads/{uploadId}/chunks/{chunkIndex}
POST   /uploads/{uploadId}/complete
POST   /uploads/{uploadId}/cancel

POST   /projects/{projectId}/scripts/generate
GET    /projects/{projectId}/scripts
PATCH  /scripts/{scriptId}
POST   /scripts/{scriptId}/scenes/generate

POST   /projects/{projectId}/transcriptions
GET    /projects/{projectId}/transcriptions

GET    /projects/{projectId}/edit-manifest
PUT    /projects/{projectId}/edit-manifest

POST   /projects/{projectId}/renders
GET    /renders/{renderId}

GET    /jobs/{jobId}
GET    /jobs/{jobId}/events
DELETE /jobs/{jobId}

GET    /media/{assetId}/stream
```

### 13.1 进度

第一版推荐前端轮询：

```text
GET /api/v1/jobs/{jobId}
```

如果后续需要实时体验，Java 可以通过 `SseEmitter` 推送：

```text
GET /api/v1/jobs/{jobId}/events
```

SSE 内部也只是查询 MySQL 中的任务状态，不依赖 Redis Pub/Sub。

### 13.2 类型契约

Java 生成 OpenAPI 文档，Vue 根据 OpenAPI 生成 TypeScript 类型。前端不手写重复的任务 DTO。

## 14. 安全设计

### 14.1 用户侧

- Spring Security
- HttpOnly Session Cookie
- CSRF 防护
- 项目级权限校验
- 媒体流鉴权
- 上传大小和类型限制

### 14.2 Java 与 Python 内部接口

- 只绑定本机地址
- 每次请求携带内部 Token
- 校验 `workerId`
- 校验任务租约
- 校验产物路径
- 不接受来自前端的任意文件路径
- 记录领取、心跳、完成和失败日志

### 14.3 媒体安全

- 使用 `ffprobe` 识别真实媒体
- 不信任扩展名和 Content-Type
- FFmpeg 使用参数数组，不拼接 shell 命令
- 对外部素材 URL 做 SSRF 防护
- 禁止执行用户上传的脚本和字体

## 15. 可观测性

控制面日志：

```text
data/logs/control/
```

能力面日志：

```text
data/logs/ai/
data/logs/asr/
data/logs/media/
data/logs/render/
```

统一日志字段：

- `request_id`
- `job_id`
- `worker_id`
- `project_id`
- `capability`
- `project_revision`
- `duration_ms`
- `error_code`

重点指标：

- 任务排队时间
- worker 心跳超时次数
- 任务成功率和重试次数
- ASR 实时率
- 渲染耗时
- 磁盘剩余空间
- MySQL 慢查询
- 模型调用成本

第一版不引入独立监控和日志平台，使用本机 JSON 日志和 MySQL 任务记录。日志必须轮转，避免占满磁盘。

## 16. 明确不选

| 技术 | 不采用原因 |
| --- | --- |
| Redis | 当前环境禁止使用 |
| Celery + Redis | 没有 Redis Broker |
| S3、MinIO、R2 | 只允许本机磁盘 |
| pgvector | MySQL 是唯一数据库 |
| MongoDB | 数据库只能使用 MySQL |
| Python 直接连 MySQL | 避免双写和业务模型分叉 |
| 微服务框架 | 当前是单机工具，不引入服务网格 |
| Kubernetes | 单机部署不需要 |
| TTS | 当前产品只做真人出镜 |
| 数字人 | 当前产品不需要 |
| LLM 直接生成 FFmpeg 命令 | 存在注入和不可控问题 |

## 17. 第一版验收标准

第一版完成后必须验证：

1. Java 能管理项目、配置、脚本版本和素材元数据。
2. Vue 能完成配置、编辑和任务状态查看。
3. 上传的真人视频能分片写入本机磁盘。
4. Java 能用 MySQL 任务表创建、领取、续租和恢复任务。
5. Python 不连接 MySQL，只通过内部接口领取任务和回写结果。
6. Python 能完成脚本生成、改稿、分镜和 ASR。
7. Python 能生成结构化转写和剪辑清单输入。
8. Java 能校验任务产物并更新业务表。
9. FFmpeg/Remotion 能输出预览和成片。
10. 媒体文件能通过 Java 鉴权接口读取。
11. 磁盘空间不足时能拒绝上传和渲染。
12. MySQL、媒体目录和日志能完成本机备份与恢复验证。

## 18. 方案边界

这套方案适合：

- 单台电脑上的个人创作工具
- 少量用户或单用户
- 先跑通脚本到成片的完整闭环
- 不希望维护 Redis 和对象存储

这套方案不适合直接承载：

- 多机分布式渲染
- 高并发 SaaS
- 无备份的单机生产环境
- 需要实时多人协作的时间轴编辑

后续扩展时应保留：

- Java `JobQueue` 接口
- Python `Capability` 注册机制
- `MediaStore` 本机存储接口
- `ModelGateway` 模型接口

如果以后迁移到多机，Java 控制面可以保留，Python 能力 Worker 可以独立部署，文件存储和任务队列再替换为对象存储与专用消息队列。
