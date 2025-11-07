"""MongoDB persistence layer for crawler metadata."""

from __future__ import annotations

import logging
import os
from datetime import UTC, datetime
from typing import Any, Dict, Optional, TYPE_CHECKING

from bson import ObjectId
from pymongo import MongoClient, errors

if TYPE_CHECKING:
    from .storage import DownloadedResource

log = logging.getLogger(__name__)


class MongoSink:
    """Lightweight wrapper to persist crawl metadata into MongoDB."""

    def __init__(self, uri: str, db_name: str) -> None:
        self.client = MongoClient(uri, serverSelectionTimeoutMS=5000, connectTimeoutMS=5000)
        self.db = self.client[db_name]

    @classmethod
    def from_env(cls) -> Optional["MongoSink"]:
        """Create a sink using `MONGO_URI` and `MONGO_DB` environment variables."""
        uri = os.getenv("MONGO_URI")
        db_name = os.getenv("MONGO_DB")
        if not uri or not db_name:
            log.info("MongoSink disabled: missing MONGO_URI or MONGO_DB")
            return None
        try:
            sink = cls(uri, db_name)
            sink.client.admin.command("ping")
            log.info("MongoSink connected to %s/%s", uri, db_name)
            return sink
        except Exception as exc:  # noqa: BLE001
            log.warning("MongoSink disabled due to connection error: %s", exc)
            return None

    def close(self) -> None:
        """Close the underlying MongoDB client."""
        self.client.close()

    def new_run_id(self) -> ObjectId:
        """Return a new ObjectId for linking datasets/resources to crawl runs."""
        return ObjectId()

    def log_crawl_run(self, run_id: ObjectId, payload: Dict[str, Any]) -> Optional[str]:
        """Insert a crawl run document and return its identifier."""
        document = dict(payload)
        document["_id"] = run_id
        document.setdefault("created_at", datetime.now(UTC).isoformat())
        return self._safe_call(lambda: str(self.db["crawl_runs"].insert_one(document).inserted_id))

    def upsert_dataset(
        self,
        *,
        slug: str,
        province: str,
        dataset_id: str,
        dataset: Dict[str, Any],
        fetched_at: str,
        run_id: Optional[str],
    ) -> None:
        """Upsert dataset metadata."""
        if not dataset_id:
            return

        dataset_doc = self._normalize_dataset(dataset)
        title = dataset_doc.get("title") or dataset_doc.get("name") or dataset_id
        description = dataset_doc.get("notes") or dataset_doc.get("description")
        license_name = dataset_doc.get("license_title") or dataset_doc.get("license_id") or dataset_doc.get("license_url")
        created = (
            dataset_doc.get("metadata_created")
            or dataset_doc.get("created")
            or dataset_doc.get("created_time")
        )
        modified = (
            dataset_doc.get("metadata_modified")
            or dataset_doc.get("modified")
            or dataset_doc.get("updated_time")
        )
        tags = self._extract_tags(dataset_doc)
        category = dataset_doc.get("category") or self._extract_category(dataset_doc)
        source_name = dataset_doc.get("source") or self._extract_source(dataset_doc) or province
        provider = dataset_doc.get("provider") or dataset_doc.get("maintainer") or dataset_doc.get("author") or source_name
        data_url = (
            dataset_doc.get("dataUrl")
            or dataset_doc.get("data_url")
            or dataset_doc.get("url")
            or dataset_doc.get("landingPage")
        )
        view_count = int(dataset_doc.get("viewCount") or dataset_doc.get("view_count") or 0)
        download_count = int(dataset_doc.get("downloadCount") or dataset_doc.get("download_count") or 0)
        document = {
            "slug": slug,
            "province": province,
            "source_dataset_id": dataset_id,
            "title": title,
            "description": description,
            "tags": tags,
            "category": category,
            "license": license_name,
            "source": source_name,
            "provider": provider,
            "dataUrl": data_url,
            "created": created,
            "modified": modified,
            "last_crawled_at": fetched_at,
            "run_id": run_id,
            "viewCount": view_count,
            "downloadCount": download_count,
        }
        filter_doc = {"slug": slug, "source_dataset_id": dataset_id}
        update = {
            "$set": document,
            "$setOnInsert": {"first_seen_at": fetched_at},
        }
        self._safe_call(lambda: self.db["datasets"].update_one(filter_doc, update, upsert=True))

    def upsert_resource(
        self,
        *,
        slug: str,
        province: str,
        dataset_id: str,
        resource: Dict[str, Any],
        downloaded: "DownloadedResource",
        fetched_at: str,
        run_id: Optional[str],
    ) -> None:
        """Upsert resource metadata."""
        resource_id = str(resource.get("id") or "")
        if not dataset_id or not resource_id:
            return

        storage_path = str(downloaded.local_path)
        normalized_path = str(downloaded.normalized_path) if downloaded.normalized_path else None
        document = {
            "_id": f"{slug}:{dataset_id}:{resource_id}",
            "dataset_id": dataset_id,
            "slug": slug,
            "province": province,
            "source_resource_id": resource_id,
            "format": resource.get("format"),
            "category": downloaded.category.value if downloaded.category else None,
            "download_url": resource.get("url"),
            "storage_path": storage_path,
            "normalized": bool(downloaded.normalized_path),
            "normalized_path": normalized_path,
            "last_crawled_at": fetched_at,
            "run_id": run_id,
        }
        filter_doc = {"_id": document["_id"]}
        update = {
            "$set": document,
            "$setOnInsert": {"first_seen_at": fetched_at},
        }
        self._safe_call(lambda: self.db["resources"].update_one(filter_doc, update, upsert=True))

    def _safe_call(self, func):
        """Execute a MongoDB operation and log failures without raising."""
        try:
            return func()
        except errors.PyMongoError as exc:
            log.warning("MongoSink write failed: %s", exc)
        except Exception as exc:  # noqa: BLE001
            log.warning("MongoSink unexpected error: %s", exc)
        return None

    @staticmethod
    def _extract_tags(dataset: Dict[str, Any]) -> list[str]:
        """Normalize CKAN tag structures to a list of names."""
        tags_field = dataset.get("tags")
        if not isinstance(tags_field, list):
            return []
        tags: list[str] = []
        for item in tags_field:
            if isinstance(item, dict):
                name = item.get("display_name") or item.get("name")
            else:
                name = str(item)
            if name:
                tags.append(name)
        return tags

    @staticmethod
    def _extract_category(dataset: Dict[str, Any]) -> Optional[str]:
        """Derive a category/group/topic name if available."""
        if "topic" in dataset and isinstance(dataset["topic"], dict):
            return dataset["topic"].get("name")
        groups = dataset.get("groups")
        if isinstance(groups, list):
            for group in groups:
                if isinstance(group, dict):
                    name = group.get("title") or group.get("name")
                    if name:
                        return name
        categories = dataset.get("categories")
        if isinstance(categories, list) and categories:
            return categories[0]
        return None

    @staticmethod
    def _extract_source(dataset: Dict[str, Any]) -> Optional[str]:
        """Extract a source/organization label if present."""
        org = dataset.get("organization")
        if isinstance(org, dict):
            name = org.get("title") or org.get("name")
            if name:
                return name
        return dataset.get("author") or dataset.get("maintainer")

    @staticmethod
    def _normalize_dataset(dataset: Any) -> Dict[str, Any]:
        """Return a dict representation regardless of the incoming type."""
        if isinstance(dataset, dict):
            return dataset
        if isinstance(dataset, list):
            for item in dataset:
                if isinstance(item, dict):
                    return item
            return {}
        if hasattr(dataset, "model_dump"):
            try:
                return dataset.model_dump(mode="json", by_alias=True)
            except Exception:  # noqa: BLE001
                return {}
        if hasattr(dataset, "__dict__"):
            return dict(getattr(dataset, "__dict__"))
        return {}


__all__ = ["MongoSink"]
