"""Shared CKAN portal client and connector utilities."""

from __future__ import annotations

import logging
from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path
from typing import Dict, Iterable, Iterator, List, Optional, Tuple

import httpx

from ..storage import DownloadedResource, LocalDataRepository, classify_resource
from ..transform import normalize_resource
from .base import CrawlStats


log = logging.getLogger(__name__)


@dataclass
class CkanDataset:
    """Lightweight wrapper around a CKAN dataset document.

    Attributes:
        raw (Dict): The raw dataset payload as returned by CKAN endpoints
            such as `package_search` or `package_show`.
    """
    raw: Dict

    @property
    def identifier(self) -> str:
        """Return a stable identifier for the dataset.

        Preference order:
            1) `id`
            2) `name`
            3) empty string if neither is present

        Returns:
            str: Stable dataset identifier.
        """
        data = self._as_dict()
        return data.get("id") or data.get("name") or ""

    @property
    def title(self) -> str:
        """Return the dataset title with a safe fallback.

        Returns:
            str: Human-friendly title if available, otherwise `name` or `identifier`.
        """
        data = self._as_dict()
        title = data.get("title") or data.get("name")
        return title or self.identifier

    @property
    def resources(self) -> Iterable[Dict]:
        """Yield the raw resource dictionaries bundled with the dataset.

        Returns:
            Iterable[Dict]: List of resource dicts, or an empty list if absent.
        """
        data = self._as_dict()
        resources = data.get("resources")
        if isinstance(resources, list):
            return resources
        return []

    def _as_dict(self) -> Dict:
        """Expose the raw payload as a dictionary.

        Handles cases where `raw` may be a list containing a single dict.

        Returns:
            Dict: A best-effort dictionary view of the raw payload.
        """
        if isinstance(self.raw, dict):
            return self.raw
        if isinstance(self.raw, list):
            for item in self.raw:
                if isinstance(item, dict):
                    return item
        return {}


class CkanClient:
    """Minimal CKAN API client for common dataset and resource operations."""

    def __init__(self, base_url: str, timeout: float = 60.0) -> None:
        """Initialise a CKAN API client.

        Args:
            base_url (str): Base URL of the CKAN portal, e.g. "https://data.example.org".
            timeout (float): Per-request timeout in seconds. Defaults to 60.0.
        """
        self.base_url = base_url.rstrip("/")
        self._client = httpx.Client(base_url=self.base_url, timeout=timeout, headers={"User-Agent": "OpenHubCrawler/1.0"})

    def close(self) -> None:
        """Close the underlying HTTP session.

        Releases pooled connections and related resources.
        """
        self._client.close()

    def package_search(self, start: int = 0, rows: int = 100) -> Tuple[List[CkanDataset], int]:
        """Return a page of dataset results along with the total count.

        Args:
            start (int): Zero-based offset. Defaults to 0.
            rows (int): Page size. Defaults to 100.

        Returns:
            Tuple[List[CkanDataset], int]: Pair of (datasets, total_count).

        Raises:
            httpx.HTTPStatusError: On non-2xx HTTP responses.
            RuntimeError: If the CKAN API signals `success=False`.
        """
        response = self._client.get(
            "/api/3/action/package_search",
            params={"start": start, "rows": rows},
        )
        response.raise_for_status()
        payload = response.json()
        if not payload.get("success"):
            raise RuntimeError(f"CKAN package_search error: {payload}")
        result = payload["result"]
        datasets = [CkanDataset(raw=item) for item in result.get("results", [])]
        return datasets, result.get("count", len(datasets))

    def package_list(self, offset: int = 0, limit: int = 100) -> List[str]:
        """Return a list of dataset names using the `package_list` endpoint.

        Args:
            offset (int): Zero-based offset. Defaults to 0.
            limit (int): Maximum number of names to return. Defaults to 100.

        Returns:
            List[str]: Dataset names as strings.

        Raises:
            httpx.HTTPStatusError: On non-2xx HTTP responses.
            RuntimeError: If the CKAN API signals `success=False`.
        """
        response = self._client.get(
            "/api/3/action/package_list",
            params={"offset": offset, "limit": limit},
        )
        response.raise_for_status()
        payload = response.json()
        if not payload.get("success"):
            raise RuntimeError(f"CKAN package_list error: {payload}")
        return payload["result"]

    def package_show(self, dataset_id: str) -> CkanDataset:
        """Fetch a single dataset document.

        Args:
            dataset_id (str): Dataset `id` or `name`.

        Returns:
            CkanDataset: Wrapped dataset payload.

        Raises:
            httpx.HTTPStatusError: On non-2xx HTTP responses.
            RuntimeError: If the CKAN API signals `success=False`.
        """
        response = self._client.get(
            "/api/3/action/package_show",
            params={"id": dataset_id},
        )
        response.raise_for_status()
        payload = response.json()
        if not payload.get("success"):
            raise RuntimeError(f"CKAN package_show error: {payload}")
        return CkanDataset(raw=payload["result"])

    def download_resource(self, resource: Dict, destination: str) -> Optional[str]:
        """Download a single resource to the destination path.

        Streams the resource URL to disk.

        Args:
            resource (Dict): Resource dictionary containing at least `url` and optionally `id`.
            destination (str): File path to write to.

        Returns:
            Optional[str]: Destination path if written, or None if skipped.

        Raises:
            httpx.HTTPStatusError: On non-2xx HTTP responses.
        """
        url = resource.get("url")
        if not url:
            log.warning("Resource without URL skipped: %s", resource.get("id"))
            return None
        with self._client.stream("GET", url) as response:
            response.raise_for_status()
            with open(destination, "wb") as fh:
                for chunk in response.iter_bytes():
                    fh.write(chunk)
        return destination


class CkanConnector:
    """Connector implementation targeting a CKAN-compatible portal."""

    def __init__(self, slug: str, display_name: str, base_url: str) -> None:
        """Create a connector that operates against a CKAN-compatible portal.

        Args:
            slug (str): Short connector identifier used for storage paths.
            display_name (str): Human-friendly portal name for logs.
            base_url (str): Base URL of the CKAN portal.
        """
        self.slug = slug
        self.display_name = display_name
        self.client = CkanClient(base_url)

    def describe(self) -> str:
        """Return a human readable description used in logs.

        Returns:
            str: A string like "Portal Name (slug)".
        """
        return f"{self.display_name} ({self.slug})"

    def run_full_crawl(self, repository: LocalDataRepository) -> CrawlStats:
        """Execute a full crawl of the portal and persist metadata and resources.

        Steps:
            1) Iterate datasets in pages.
            2) Save each page's listing into metadata.
            3) For each dataset, download all resources and save dataset detail.
            4) Optionally normalize supported resource files.

        Args:
            repository (LocalDataRepository): Target data repository.

        Returns:
            CrawlStats: Accumulated counts for datasets and resources.

        Side Effects:
            Writes metadata, resources, and normalized outputs to `repository`.
            Emits structured logs for progress and errors.
        """
        repository.prepare()
        stats = CrawlStats()
        try:
            for page_index, datasets in enumerate(self._iter_datasets()):
                fetched_at = datetime.now(UTC).isoformat()
                repository.save_dataset_page(
                    page_index=page_index,
                    datasets=[dataset.raw for dataset in datasets],
                    fetched_at=fetched_at,
                )
                for dataset in datasets:
                    downloaded = self._download_dataset(repository, dataset)
                    repository.save_dataset_detail(
                        dataset.raw,
                        fetched_at=fetched_at,
                        resources=downloaded,
                    )
                    stats.datasets_processed += 1
                    stats.resources_downloaded += len(downloaded)
        finally:
            self.client.close()
        return stats

    def _iter_datasets(self) -> Iterator[List[CkanDataset]]:
        """Yield datasets in pages honoring portal pagination.

        Primary strategy:
            Use `package_search` with paging until all results are consumed.

        Fallback:
            If `package_search` fails, switch to `package_list` + `package_show`.

        Yields:
            List[CkanDataset]: A page of dataset wrappers each iteration.
        """
        start = 0
        rows = 100
        try:
            while True:
                datasets, total = self.client.package_search(start=start, rows=rows)
                if not datasets:
                    break
                yield datasets
                start += rows
                if start >= total:
                    break
        except httpx.HTTPStatusError as exc:
            log.warning("package_search unavailable (%s). Falling back to package_list", exc)
            offset = 0
            limit = rows
            while True:
                names = self.client.package_list(offset=offset, limit=limit)
                if not names:
                    break
                datasets = [self.client.package_show(name) for name in names]
                yield datasets
                offset += limit
                if len(names) < limit:
                    break

    def _download_dataset(
        self,
        repository: LocalDataRepository,
        dataset: CkanDataset,
    ) -> List[DownloadedResource]:
        """Download every resource for a dataset and normalize supported files.

        For each resource:
            - Infer a category from `format` or filename.
            - Compute a deterministic destination under the repository.
            - Download if missing, otherwise skip.
            - Attempt normalization for supported types and record paths.

        Args:
            repository (LocalDataRepository): Target repository used for path resolution.
            dataset (CkanDataset): Dataset wrapper containing raw metadata and resources.

        Returns:
            List[DownloadedResource]: Records describing downloaded and normalized files.
        """
        downloaded: List[DownloadedResource] = []
        dataset_id = dataset.identifier
        if not dataset_id:
            log.warning("Skipping dataset without identifier: %s", dataset.raw)
            return downloaded
        for resource in dataset.resources:
            resource_id = resource.get("id")
            name = resource.get("name") or resource.get("title") or resource_id
            if not resource_id or not name:
                continue
            category = classify_resource(
                type(
                    "TempFile",
                    (),
                    {"format": resource.get("format"), "name": name},
                )()
            )
            destination = repository.resource_destination(dataset_id, name, category)
            dataset_key = str(dataset_id)
            if destination.exists():
                log.info(
                    "Skip download for %s (dataset %s) – file already exists",
                    resource_id,
                    dataset_id,
                )
                normalized_path = normalize_resource(
                    repository,
                    dataset_key,
                    str(resource_id),
                    destination,
                    category,
                )
                downloaded.append(
                    DownloadedResource(
                        resource_id=str(resource_id),
                        filename=name,
                        category=category,
                        local_path=destination,
                        normalized_path=normalized_path,
                    )
                )
                continue
            try:
                downloaded_path = self.client.download_resource(resource, str(destination))
            except Exception as exc:  # noqa: BLE001
                log.warning(
                    "Failed to download resource %s from dataset %s: %s",
                    resource_id,
                    dataset_id,
                    exc,
                )
                continue
            if not downloaded_path:
                continue
            normalized_path = normalize_resource(
                repository,
                dataset_key,
                str(resource_id),
                Path(downloaded_path),
                category,
            )
            downloaded.append(
                DownloadedResource(
                    resource_id=str(resource_id),
                    filename=name,
                    category=category,
                    local_path=destination,
                    normalized_path=normalized_path,
                )
            )
        return downloaded


__all__ = ["CkanConnector", "CkanClient"]
