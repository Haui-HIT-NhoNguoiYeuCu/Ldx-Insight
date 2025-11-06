"""Local storage helpers for OpenHub crawler data repository."""

from __future__ import annotations

import json
from dataclasses import dataclass
from enum import Enum
from pathlib import Path
from typing import Iterable, List, Optional

from .models import DatasetDetail, DatasetResourceFile, DatasetSummary, TopicSummary


class ResourceCategory(str, Enum):
    """Canonical buckets for dataset resource file types.

    Values mirror common file formats encountered on open data portals and drive
    where raw files are stored under `resources/<category>/...`.
    """
    CSV = "csv"
    XLS = "xls"
    XLSX = "xlsx"
    XML = "xml"
    PDF = "pdf"
    DOC = "doc"
    DOCX = "docx"
    JSON = "json"
    ZIP = "zip"
    IMAGE = "image"
    API = "api"
    OTHER = "other"


def safe_filename(value: str) -> str:
    """Return a filesystem-safe filename.

    Replaces characters invalid on common filesystems with underscores and
    collapses spaces to single underscores, preserving order.

    Args:
        value: Arbitrary filename string.

    Returns:
        str: Sanitized filename safe to concatenate into paths.
    """
    invalid_chars = '<>:"/\\|?*'
    cleaned = "".join("_" if ch in invalid_chars else ch for ch in value)
    return cleaned.strip().replace(" ", "_")


def _guess_extension(name: Optional[str]) -> Optional[str]:
    """Extract a lowercase extension (without the dot) if present.

    Args:
        name: Filename or path string.

    Returns:
        Optional[str]: The extension without the leading dot, or None if missing.
    """
    if not name or "." not in name:
        return None
    return name.rsplit(".", 1)[1].lower()


def classify_resource(file_meta: Optional[DatasetResourceFile | dict | object]) -> ResourceCategory:
    """Classify a resource into a `ResourceCategory`.

    Heuristics:
        1) Prefer explicit `format` field if available.
        2) Otherwise infer from the filename extension.
        3) Fallback to `OTHER` if no mapping matches.

    The function accepts multiple shapes to stay resilient to upstream model
    or dict payloads.

    Args:
        file_meta: A `DatasetResourceFile`, a dict-like payload, an object with
            `format`/`name` attributes, or None.

    Returns:
        ResourceCategory: Best-effort category for the resource.
    """
    format_hint = ""
    name_hint = None

    if isinstance(file_meta, DatasetResourceFile):
        format_hint = (file_meta.format or "").lower()
        name_hint = file_meta.name
    elif isinstance(file_meta, dict):
        format_hint = (file_meta.get("format") or "").lower()
        name_hint = file_meta.get("name") or file_meta.get("title")
    elif file_meta is not None:
        format_hint = getattr(file_meta, "format", "") or ""
        name_hint = getattr(file_meta, "name", None)

    ext_hint = _guess_extension(name_hint)

    def match(value: Optional[str]) -> Optional[ResourceCategory]:
        if not value:
            return None
        value = value.lower()
        mapping = {
            "csv": ResourceCategory.CSV,
            "xls": ResourceCategory.XLS,
            "xlsx": ResourceCategory.XLSX,
            "xml": ResourceCategory.XML,
            "pdf": ResourceCategory.PDF,
            "doc": ResourceCategory.DOC,
            "docx": ResourceCategory.DOCX,
            "json": ResourceCategory.JSON,
            "zip": ResourceCategory.ZIP,
            "rar": ResourceCategory.ZIP,
            "jpg": ResourceCategory.IMAGE,
            "jpeg": ResourceCategory.IMAGE,
            "png": ResourceCategory.IMAGE,
        }
        return mapping.get(value)

    category = match(format_hint) or match(ext_hint)
    return category or ResourceCategory.OTHER


@dataclass
class DownloadedResource:
    """Metadata about a downloaded resource file on local disk.

    Attributes:
        resource_id: Identifier of the resource within the dataset.
        filename: Original or server-provided filename.
        category: Classified `ResourceCategory`.
        local_path: Absolute or repository-relative path to the raw file.
        normalized_path: Optional path to a normalized representation (e.g., JSON).
    """
    resource_id: str
    filename: str
    category: ResourceCategory
    local_path: Path
    normalized_path: Optional[Path] = None

    def model_dump(self) -> dict:
        """Serialize this record into a plain JSON-serializable dict.

        Returns:
            dict: Minimal payload suitable for inclusion in metadata files.
        """
        return {
            "resource_id": self.resource_id,
            "filename": self.filename,
            "category": self.category.value,
            "local_path": str(self.local_path),
            "normalized_path": str(self.normalized_path) if self.normalized_path else None,
        }


class LocalDataRepository:
    """Organize metadata and resource binaries in a local directory tree.

    Layout under `root`:
        - metadata/
            - topics.json
            - dataset_pages/page_XXX.json
            - datasets/<dataset_identifier>.json
        - resources/<category>/<parent_id>/<safe_filename>
        - normalized/<parent_id>/<resource_id>.json

    The repository is content-addressed by portal identifiers and file categories
    to provide deterministic locations across runs.
    """

    def __init__(self, root: Path) -> None:
        """Initialize repository paths without touching the filesystem.

        Args:
            root: Root directory of the repository.
        """
        self.root = Path(root)
        self.metadata_root = self.root / "metadata"
        self.dataset_pages_dir = self.metadata_root / "dataset_pages"
        self.datasets_dir = self.metadata_root / "datasets"
        self.resources_root = self.root / "resources"
        self.normalized_root = self.root / "normalized"

    def prepare(self) -> None:
        """Ensure required directory structure exists.

        Creates core directories and per-category subfolders under `resources/`.
        Idempotent and safe to call multiple times.
        """
        for path in [
            self.metadata_root,
            self.dataset_pages_dir,
            self.datasets_dir,
            self.resources_root,
            self.normalized_root,
        ]:
            path.mkdir(parents=True, exist_ok=True)
        for category in ResourceCategory:
            (self.resources_root / category.value).mkdir(parents=True, exist_ok=True)

    # Metadata helpers
    def save_topics(self, topics: Iterable[TopicSummary], fetched_at: str) -> Path:
        """Persist the fetched topic catalogue to disk.

        Args:
            topics: Iterable of `TopicSummary`.
            fetched_at: ISO-8601 timestamp string for auditability.

        Returns:
            Path: Location of the written JSON file.
        """
        topic_list = list(topics)
        payload = {
            "fetched_at": fetched_at,
            "count": len(topic_list),
            "items": [topic.model_dump(mode="json", by_alias=True) for topic in topic_list],
        }
        path = self.metadata_root / "topics.json"
        _write_json(path, payload)
        return path

    def save_dataset_page(
        self, page_index: int, datasets: Iterable[DatasetSummary], fetched_at: str
    ) -> Path:
        """Persist a page of dataset search results.

        Args:
            page_index: Zero-based page index.
            datasets: Iterable of `DatasetSummary` or dict-like items.
            fetched_at: ISO-8601 timestamp string.

        Returns:
            Path: Location of the written page JSON file.
        """
        dataset_list = list(datasets)
        items_payload = []
        for dataset in dataset_list:
            if hasattr(dataset, "model_dump"):
                items_payload.append(dataset.model_dump(mode="json", by_alias=True))
            else:
                items_payload.append(dataset)
        payload = {
            "fetched_at": fetched_at,
            "page_index": page_index,
            "count": len(dataset_list),
            "items": items_payload,
        }
        path = self.dataset_pages_dir / f"page_{page_index:03d}.json"
        _write_json(path, payload)
        return path

    def save_dataset_detail(
        self,
        detail: DatasetDetail | dict,
        fetched_at: str,
        resources: Optional[List[DownloadedResource]] = None,
    ) -> Path:
        """Persist a dataset detail payload together with downloaded resources.

        Accepts either a typed `DatasetDetail` or a loose dict payload. Tries to
        derive a stable filename from `parent_id`, `id`, or `name`; falls back to
        `"dataset"` when unavailable.

        Args:
            detail: Dataset detail object or dict payload.
            fetched_at: ISO-8601 timestamp string.
            resources: Optional list of `DownloadedResource` to include.

        Returns:
            Path: Location of the written dataset JSON file.
        """
        if isinstance(detail, DatasetDetail):
            dataset_payload = detail.model_dump(mode="json", by_alias=True)
            dataset_identifier = str(detail.parent_id)
        else:
            dataset_dict = detail
            if isinstance(detail, list):
                dataset_dict = next((item for item in detail if isinstance(item, dict)), {})
            dataset_payload = dataset_dict
            identifier = (
                dataset_dict.get("parent_id")
                if isinstance(dataset_dict, dict)
                else None
            )
            if identifier is None and isinstance(dataset_dict, dict):
                identifier = dataset_dict.get("id") or dataset_dict.get("name")
            dataset_identifier = str(identifier) if identifier is not None else ""
            if not dataset_identifier or dataset_identifier.lower() == "none":
                dataset_identifier = "dataset"

        payload = {
            "fetched_at": fetched_at,
            "dataset": dataset_payload,
            "resources": [res.model_dump() for res in resources or []],
        }
        path = self.datasets_dir / f"{dataset_identifier}.json"
        _write_json(path, payload)
        return path

    # Resource helpers
    def resource_destination(
        self,
        parent_id: int | str,
        filename: str,
        category: ResourceCategory,
    ) -> Path:
        """Compute the destination path for a raw resource file.

        The final path is `resources/<category>/<parent_id>/<safe_filename>`.

        Args:
            parent_id: Portal dataset identifier.
            filename: Original filename to be sanitized.
            category: Classified `ResourceCategory`.

        Returns:
            Path: Full path where the file should be stored. Parent directories
            are created if missing.
        """
        safe_name = safe_filename(filename)
        target_dir = self.resources_root / category.value / str(parent_id)
        target_dir.mkdir(parents=True, exist_ok=True)
        return target_dir / safe_name

    def normalized_destination(
        self,
        parent_id: int | str,
        resource_id: str,
        suffix: str = "json",
    ) -> Path:
        """Compute the destination path for normalized output.

        The final path is `normalized/<parent_id>/<resource_id>.<suffix>`.

        Args:
            parent_id: Portal dataset identifier.
            resource_id: Resource identifier used to build a stable filename.
            suffix: File suffix for the normalized artifact. Defaults to "json".

        Returns:
            Path: Full path for the normalized file. Parent directories are created.
        """
        target_dir = self.normalized_root / str(parent_id)
        target_dir.mkdir(parents=True, exist_ok=True)
        filename = safe_filename(resource_id) + f".{suffix}"
        return target_dir / filename


def _write_json(path: Path, payload: dict) -> None:
    """Write a JSON payload to disk using UTF-8 encoding.

    Args:
        path: Destination file path.
        payload: JSON-serializable structure.

    Side Effects:
        Creates parent directories as needed and overwrites existing files.
    """
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as fh:
        json.dump(payload, fh, ensure_ascii=False, indent=2)
