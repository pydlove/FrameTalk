from frametalk_capability.config import Settings


def main() -> None:
    settings = Settings()
    print(
        "frametalk capability scaffold: "
        f"worker_id={settings.worker_id} queues={','.join(settings.queues)}"
    )


if __name__ == "__main__":
    main()
