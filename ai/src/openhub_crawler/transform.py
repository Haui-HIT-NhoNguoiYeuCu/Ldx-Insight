"""Utility helpers to normalize downloaded resources into a unified format."""

from __future__ import annotations

import json
import logging
from pathlib import Path
from typing import Optional

import pandas as pd

from .storage import LocalDataRepository, ResourceCategory


log = logging.getLogger(__name__)


def normalize_resource(
    repository: LocalDataRepository,
    dataset_id: str,
    resource_id: str,
    source_path: Path,
    category: ResourceCategory,
) -> Optional[Path]:
    """Normalize a raw resource file into line-oriented JSON when feasible.

    Supported conversions:
        - CSV → list[dict] via `pandas.read_csv`
        - XLS/XLSX → list[dict] via `pandas.read_excel`
        - JSON → pass-through with pretty formatting

    Unsupported categories are skipped without error.

    Args:
        repository: Target repository that provides the normalized destination.
        dataset_id: Portal dataset identifier used in the output path.
        resource_id: Resource identifier used as the normalized filename stem.
        source_path: Path to the raw downloaded file.
        category: Detected `ResourceCategory` of the source file.

    Returns:
        Optional[Path]: Path to the normalized JSON file, or None if skipped or failed.

    Side Effects:
        - Creates parent directories as needed.
        - Overwrites an existing normalized file only if it does not already exist
          at the computed destination (idempotent guard at the start).

    Logging:
        - Warns on missing source file or normalization failure.
        - Debug-logs when a category is intentionally skipped.
    """

    if not source_path.exists():
        log.warning("Skip normalization for missing file: %s", source_path)
        return None

    destination = repository.normalized_destination(dataset_id, resource_id)
    if destination.exists():
        return destination

    try:
        if category in {ResourceCategory.CSV}:
            df = pd.read_csv(source_path)
        elif category in {ResourceCategory.XLS, ResourceCategory.XLSX}:
            df = pd.read_excel(source_path)
        elif category == ResourceCategory.JSON:
            data = _load_json(source_path)
            destination.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
            return destination
        else:
            log.debug("Normalization skipped for format %s (%s)", category.value, source_path)
            return None

        records = df.to_dict(orient="records")
        destination.write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")
        return destination
    except Exception as exc:  # noqa: BLE001
        log.warning("Failed to normalize %s (%s): %s", resource_id, source_path, exc)
        if destination.exists():
            destination.unlink(missing_ok=True)
        return None


def _load_json(path: Path):
    """Load JSON from disk.

    Args:
        path: Path to a UTF-8 encoded JSON file.

    Returns:
        Any: Parsed Python object produced by `json.load`.
    """
    with path.open("r", encoding="utf-8") as fh:
        return json.load(fh)
