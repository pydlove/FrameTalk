# 共享契约

跨 Java 和 Python 的协议放在此目录，不复制到任一端的私有目录。

## 文件

| 文件 | 用途 |
| --- | --- |
| `capability-api.openapi.yaml` | Python Worker 与 Java 控制面的内部任务协议 |
| `capability-result.schema.json` | 能力任务成功回写的产物清单 |
| `edit-manifest.schema.json` | 渲染器消费的结构化剪辑清单 |

## 规则

- 所有持久化结构都带 `schemaVersion`
- 路径必须是相对路径
- 禁止 `..`、绝对路径和反斜杠路径
- Java 和 Python 都必须有契约测试
- 契约变更必须同时检查后端、前端和渲染器
- 未知字段默认允许读取，写入时由各语言模型校验

公开的 `/api/v1/**` 接口由 Java 生成 OpenAPI，Vue 只根据该文档生成类型。
