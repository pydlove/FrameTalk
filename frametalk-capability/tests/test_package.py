from frametalk_capability import __version__
from frametalk_capability.worker.registry import CapabilityRegistry


def test_package_version() -> None:
    assert __version__ == "0.1.0"


def test_registry_reports_unknown_capability() -> None:
    registry = CapabilityRegistry()

    try:
        registry.resolve("missing.capability")
    except LookupError as error:
        assert "missing.capability" in str(error)
    else:
        raise AssertionError("LookupError was not raised")
