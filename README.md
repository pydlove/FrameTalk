# 帧言

帧言（FrameTalk）是面向真人出镜创作者的自媒体成片工作台，覆盖脚本、改稿、分镜、素材、文字化粗剪和成片导出。

> 让每一帧替你表达。

## 当前已实现

- 邮箱验证码注册、密码登录、保持登录和 HttpOnly Cookie 会话
- 修改密码并注销其他设备会话
- WorkCove 风格的登录、注册、应用壳和设置页
- 浅色、深色、跟随系统和紧凑侧边栏偏好
- MySQL 8 + Flyway 本地数据存储

## 架构

```text
Vue 3 交互面
      |
      v
Java 控制面 + MySQL
      |
      v
Python 能力面
      |
      v
本机磁盘
```

- Java 负责业务、配置、权限、MySQL 和任务编排。
- Python 负责 LLM、ASR、FFmpeg、Remotion 和媒体分析。
- Vue 只调用 Java API。
- 不使用 Redis、对象存储和 MinIO。

详细设计见 [ARCHITECTURE.md](ARCHITECTURE.md)。

## 工程结构

```text
FrameTalk/
├── frametalk-commons/      纯 Java 共享基础
├── frametalk-core/         领域与应用层
├── frametalk-infrastructure/ MyBatis、Flyway、文件与能力调度
├── frametalk-interfaces/   REST API 与 DTO
├── frametalk-boot/         Spring Boot 启动入口
├── frametalk-capability/   Python 能力 Worker
├── frametalk-web/          Vue 3 前端
├── contracts/           跨语言契约
├── data/                本机运行数据，不提交 Git
└── docs/                架构和开发规范
```

## 本地环境

- Java 17
- Maven 3.9+
- Node.js 20+ 与 pnpm
- Python 3.12+
- MySQL 8.0+

## 相关文档

- [架构与分包](ARCHITECTURE.md)
- [品牌规范](docs/brand.md)
- [本地开发](docs/local-development.md)
- [后端技术选型](docs/backend-tech-stack.md)
- [代码分包规则](docs/package-boundaries.md)
- [前端设计系统](docs/frontend-design-system.md)
