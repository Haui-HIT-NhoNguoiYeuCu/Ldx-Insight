"""Portal connector abstractions for OpenHub crawler agents."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable, Protocol

from ..storage import DownloadedResource, LocalDataRepository


@dataclass
class CrawlStats:
    datasets_processed: int = 0
    resources_downloaded: int = 0


class PortalConnector(Protocol):
    """Connector interface each portal implementation must follow."""

    slug: str
    display_name: str

    def run_full_crawl(self, repository: LocalDataRepository) -> CrawlStats:
        """Execute a full crawl for this portal."""
        ...  # pragma: no cover

    def describe(self) -> str:
        """Return human-readable description used in logs."""
        return f"{self.display_name} ({self.slug})"
