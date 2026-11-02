from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="FRAMETALK_CAPABILITY_",
        env_file=".env",
        extra="ignore",
    )

    control_plane_url: str = "http://127.0.0.1:8791"
    internal_token: str = Field(default="", repr=False)
    worker_id: str = "capability-worker-1"
    queues: list[str] = Field(
        default_factory=lambda: ["ai", "transcribe", "media", "render"]
    )
    data_dir: Path = Path("./data")
    poll_interval_seconds: float = 1.0
    lease_seconds: int = 60
    heartbeat_interval_seconds: int = 15
