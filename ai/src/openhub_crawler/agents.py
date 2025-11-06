"""Agent orchestration layer for running multiple portal connectors."""

from __future__ import annotations

import logging
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Sequence

from .portals.base import CrawlStats, PortalConnector
from .portals.danang import DaNangConnector
from .portals.dongthap import DongThapConnector
from .portals.hcm import HoChiMinhConnector
from .storage import LocalDataRepository


log = logging.getLogger(__name__)


@dataclass
class AgentResult:
    """Summary of a single connector run.

    Attributes:
        connector_slug (str): Connector identifier slug.
        datasets_processed (int): Number of datasets processed.
        resources_downloaded (int): Number of resources downloaded.
    """


class AgentRegistry:
    """Registry exposing available connectors by slug."""

    def __init__(self) -> None:
        """Initialize the registry with bundled portal connectors."""
        self._connectors: Dict[str, PortalConnector] = {
            "dong-thap": DongThapConnector(),
            "hcm": HoChiMinhConnector(),
            "da-nang": DaNangConnector(),
        }

    def get(self, slug: str) -> PortalConnector:
        """Return the connector instance for the given slug.

        Args:
            slug (str): Connector slug.

        Returns:
            PortalConnector: The connector instance.

        Raises:
            KeyError: If the slug is not present in the registry.
        """
        if slug not in self._connectors:
            raise KeyError(f"Unknown connector '{slug}'. Available: {list(self._connectors)}")
        return self._connectors[slug]

    def list(self) -> Sequence[str]:
        """List all available connector slugs.

        Returns:
            Sequence[str]: Tuple of slugs in registry order.
        """
        return tuple(self._connectors.keys())


class CrawlAgentManager:
    """Coordinator that runs multiple connectors and manages storage roots."""

    def __init__(self, repository_root: str | Path | None = None) -> None:
        """Create a manager that orchestrates multiple connectors.

        Args:
            repository_root (str | Path | None): Root directory for data storage.
                If None, defaults to the "storage" directory in the CWD.
        """
        self.registry = AgentRegistry()
        self.repository_root = Path(repository_root) if repository_root else None

    def run_connectors(self, connector_slugs: Iterable[str]) -> List[AgentResult]:
        """Execute the specified connectors sequentially and collect statistics.

        Args:
            connector_slugs (Iterable[str]): Slugs of connectors to run.

        Returns:
            List[AgentResult]: Results in execution order.

        Side Effects:
            Logs progress for each connector and creates a LocalDataRepository per connector.
        """
        results: List[AgentResult] = []
        for slug in connector_slugs:
            connector = self.registry.get(slug)
            log.info("Running connector %s", connector.describe())
            repository_path = self._resolve_repository_path(slug)
            repository = LocalDataRepository(repository_path)
            stats = self._run_connector(connector, repository)
            results.append(
                AgentResult(
                    connector_slug=slug,
                    datasets_processed=stats.datasets_processed,
                    resources_downloaded=stats.resources_downloaded,
                )
            )
        return results

    def _run_connector(self, connector: PortalConnector, repository: LocalDataRepository) -> CrawlStats:
        """Run a connector, handling unimplemented placeholders gracefully.

        Args:
            connector (PortalConnector): Connector to execute.
            repository (LocalDataRepository): Target repository for outputs.

        Returns:
            CrawlStats: Collected statistics. Defaults to an empty `CrawlStats` if unimplemented.

        Behavior:
            Calls `connector.run_full_crawl(repository)` and catches `NotImplementedError`
            to avoid interrupting the overall run.
        """
        try:
            return connector.run_full_crawl(repository)
        except NotImplementedError as exc:
            log.warning("Connector %s is not implemented: %s", connector.describe(), exc)
            return CrawlStats()

    def _resolve_repository_path(self, slug: str) -> Path:
        """Compute the storage path for a connector's outputs.

        Args:
            slug (str): Connector slug.

        Returns:
            Path: Directory path for metadata and resource files.
        """
        if self.repository_root:
            return self.repository_root / slug
        return Path("storage") / slug


__all__ = ["CrawlAgentManager", "AgentResult"]
