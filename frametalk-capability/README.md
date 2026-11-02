# 帧言 Python 能力层

Python 只负责能力执行，不连接 MySQL，不拥有业务状态。

## 包边界

```text
client/        Java 内部 API 客户端
worker/        任务领取、心跳和能力注册
capabilities/  LLM、ASR、媒体和渲染能力
providers/     模型供应商适配
media/         FFmpeg 和 ffprobe
rendering/     Remotion 调用和产物校验
contracts/     Pydantic 契约
```

## 本地检查

```bash
cd frametalk-capability
uv sync --extra dev
uv run pytest
uv run ruff check .
uv run mypy
```

如果本机尚未安装 `uv`，可以使用标准的虚拟环境：

```bash
python3 -m venv .venv
.venv/bin/pip install -e '.[dev]'
.venv/bin/pytest
.venv/bin/ruff check .
.venv/bin/mypy
```

当前阶段只定义包结构与契约，不包含实际能力实现。
