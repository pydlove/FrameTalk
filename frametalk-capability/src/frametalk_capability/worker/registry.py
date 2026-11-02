from collections.abc import Awaitable, Callable
from dataclasses import dataclass
from typing import Any

CapabilityHandler = Callable[[dict[str, Any]], Awaitable[dict[str, Any]]]


@dataclass(frozen=True)
class CapabilityDefinition:
    name: str
    handler: CapabilityHandler


class CapabilityRegistry:
    """Maps capability names to handlers without branching in the worker loop."""

    def __init__(self) -> None:
        self._definitions: dict[str, CapabilityDefinition] = {}

    def register(self, name: str, handler: CapabilityHandler) -> None:
        if name in self._definitions:
            raise ValueError(f"Capability already registered: {name}")
        self._definitions[name] = CapabilityDefinition(name=name, handler=handler)

    def resolve(self, name: str) -> CapabilityHandler:
        try:
            return self._definitions[name].handler
        except KeyError as error:
            raise LookupError(f"Unknown capability: {name}") from error

    @property
    def names(self) -> tuple[str, ...]:
        return tuple(sorted(self._definitions))
