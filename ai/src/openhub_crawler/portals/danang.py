"""Da Nang portal connector implementation (HTML scraping + API metadata).

This connector targets https://congdulieu.vn and extracts dataset listings
and detail pages primarily via HTML scraping, with optional API metadata capture.
It persists listing pages, dataset-level metadata, and synthetic API resources
into a `LocalDataRepository`.
"""

from __future__ import annotations

import base64
import json
import logging
import re
from datetime import UTC, datetime
from typing import Dict, List, Optional

import httpx

from ..storage import DownloadedResource, LocalDataRepository, ResourceCategory
from .base import CrawlStats


log = logging.getLogger(__name__)


class DaNangConnector:
    """Connector that scrapes the congdulieu.vn portal.

    Responsibilities:
        - Fetch paginated listing pages under `/dichvu`.
        - Parse dataset entries from HTML fragments.
        - Visit dataset detail pages and extract labeled fields.
        - Persist a synthetic resource describing the dataset's API endpoint
          when present.

    Attributes:
        slug (str): Storage-safe connector identifier.
        display_name (str): Human-friendly portal name used in logs.
        client (httpx.Client): Stateful HTTP client with default headers and timeout.
    """

    slug = "da-nang"
    display_name = "Đà Nẵng Open Data"

    _BASE_URL = "https://congdulieu.vn"
    _LIST_PATH = "/dichvu"
    _DETAIL_PREFIX = "/dulieuchitiet/"
    _DEFAULT_HEADERS = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/123.0 Safari/537.36",
        "Accept-Language": "vi-VN,vi;q=0.9",
    }
    _MAX_PAGES = 5

    _DATASET_LINK_PATTERN = re.compile(
        r"zclass:'link',label:'(?P<label>[^']+?)',href:'(?P<href>/dulieuchitiet/\d+)'",
        re.DOTALL,
    )
    _DATE_PATTERN = re.compile(r"value:'\s*([0-9]{2}-[0-9]{2}-[0-9]{4}[^']*)'", re.DOTALL)
    _API_URL_PATTERN = re.compile(r"https://congdulieu.vn/api/dataset/[A-Za-z0-9=\?&/_-]+")

    _FIELD_LABELS: Dict[str, str] = {
        "managing_agency": "Cơ quan chủ quản:",
        "providing_agency": "Cơ quan cung cấp:",
        "field": "Lĩnh vực:",
        "update_frequency": "Tần suất cung cấp:",
        "description": "Mô tả:",
        "dataset_identifier": "Mã định danh tập dữ liệu:",
        "tags": "Tag:",
        "contact_person": "Người liên hệ:",
        "contact_info": "Liên hệ (Email/số điện thoại):",
        "created_at": "Ngày tạo:",
        "updated_at": "Ngày cập nhật:",
        "license": "Giấy phép:",
    }

    def __init__(self) -> None:
        """Configure an HTTP client ready to interact with the portal.

        The client follows redirects and sends realistic headers to reduce the
        chance of being blocked. Network timeouts are conservative.
        """
        self.client = httpx.Client(
            base_url=self._BASE_URL,
            headers=self._DEFAULT_HEADERS,
            timeout=60.0,
            follow_redirects=True,
        )

    def describe(self) -> str:
        """Return a descriptive name for logging output.

        Returns:
            str: A string like "Đà Nẵng Open Data (da-nang)".
        """
        return f"{self.display_name} ({self.slug})"

    def close(self) -> None:
        """Close the underlying HTTP session.

        Releases pooled connections and related resources.
        """
        self.client.close()

    def run_full_crawl(self, repository: LocalDataRepository) -> CrawlStats:
        """Scrape dataset listings and persist structured metadata.

        Process:
            1) Iterate paginated listing pages up to `_MAX_PAGES`.
            2) Parse dataset cards and deduplicate by internal ID.
            3) Persist each listing page to the repository.
            4) For each dataset, fetch the detail page, build a metadata payload,
               and persist any synthetic API resource metadata.

        Args:
            repository (LocalDataRepository): Repository where outputs are written.

        Returns:
            CrawlStats: Accumulated counters for datasets and resources.

        Side Effects:
            Writes files under `metadata/` and `resources/` within the repository.
        """
        repository.prepare()
        stats = CrawlStats()
        seen_ids: set[str] = set()
        page = 1

        try:
            while True:
                if page > self._MAX_PAGES:
                    log.info("Reached configured page limit (%s)", self._MAX_PAGES)
                    break
                html = self._fetch_list_page(page)
                dataset_items = self._parse_dataset_list(html, page_index=page - 1)
                dataset_items = [item for item in dataset_items if item["id"] not in seen_ids]
                if not dataset_items:
                    break

                fetched_at = datetime.now(UTC).isoformat()
                repository.save_dataset_page(
                    page_index=page - 1,
                    datasets=dataset_items,
                    fetched_at=fetched_at,
                )

                for item in dataset_items:
                    try:
                        detail_html = self._fetch_detail_page(item["detail_path"])
                        dataset_payload, resources = self._build_dataset_payload(repository, item, detail_html)
                        repository.save_dataset_detail(dataset_payload, fetched_at=fetched_at, resources=resources)
                        stats.datasets_processed += 1
                        stats.resources_downloaded += len(resources)
                        seen_ids.add(item["id"])
                    except Exception as exc:  # noqa: BLE001
                        log.warning("Failed to process dataset %s: %s", item["id"], exc)

                page += 1
        finally:
            self.close()

        return stats

    # ------------------------------------------------------------------
    # Fetch helpers
    # ------------------------------------------------------------------
    def _fetch_list_page(self, page: int) -> str:
        """Retrieve a listing page as decoded HTML.

        Args:
            page (int): 1-based page index for the listing.

        Returns:
            str: HTML content with best-effort unescaping applied.
        """
        params = {"p": page}
        response = self.client.get(self._LIST_PATH, params=params)
        response.raise_for_status()
        return self._decode_html(response.text)

    def _fetch_detail_page(self, detail_path: str) -> str:
        """Retrieve the raw HTML for a specific dataset page.

        Args:
            detail_path (str): Path beginning with `/_DETAIL_PREFIX`.

        Returns:
            str: HTML content with best-effort unescaping applied.
        """
        response = self.client.get(detail_path)
        response.raise_for_status()
        return self._decode_html(response.text)

    @staticmethod
    def _decode_html(raw: str) -> str:
        """Best-effort transform for escaped HTML fragments.

        Attempts to decode unicode escape sequences. Falls back to the raw
        string if decoding fails.

        Args:
            raw (str): Raw HTML text.

        Returns:
            str: Decoded HTML text.
        """
        try:
            return raw.encode("utf-8").decode("unicode_escape")
        except UnicodeDecodeError:
            return raw

    # ------------------------------------------------------------------
    # Parsing helpers
    # ------------------------------------------------------------------
    def _parse_dataset_list(self, html: str, page_index: int) -> List[Dict[str, str]]:
        """Parse dataset entries from the listing page.

        Uses `_DATASET_LINK_PATTERN` to locate dataset links and extracts:
        - internal `id` from the detail URL
        - normalized title
        - absolute detail URL
        - nearby published date when it can be resolved

        Args:
            html (str): Listing page HTML.
            page_index (int): Zero-based page index for auditing.

        Returns:
            List[Dict[str, str]]: Parsed items ready for persistence.
        """
        items: List[Dict[str, str]] = []
        seen: set[str] = set()

        for match in self._DATASET_LINK_PATTERN.finditer(html):
            raw_title = match.group("label")
            title = self._normalize_text(raw_title)
            if not title or title.upper() == "WEB":
                continue

            href = match.group("href")
            dataset_id = href.split("/")[-1]
            if dataset_id in seen:
                continue

            published_at = self._extract_published_at(html, match.end())

            items.append(
                {
                    "id": dataset_id,
                    "title": title,
                    "detail_path": href,
                    "detail_url": self._absolute_url(href),
                    "published_at": published_at,
                    "page_index": page_index,
                }
            )
            seen.add(dataset_id)

        return items

    def _extract_published_at(self, html: str, start_idx: int) -> Optional[str]:
        """Extract the published date located near a match index.

        Scans a short window after the link position for a `value:'DD-MM-YYYY'`
        pattern and returns the raw string if found.

        Args:
            html (str): Source HTML.
            start_idx (int): Start index from which to scan forward.

        Returns:
            Optional[str]: Raw date string if found, otherwise None.
        """
        snippet = html[start_idx:start_idx + 800]
        match = self._DATE_PATTERN.search(snippet)
        if match:
            value = match.group(1).strip()
            return value or None
        return None

    # ------------------------------------------------------------------
    # Detail parsing / persistence
    # ------------------------------------------------------------------
    def _build_dataset_payload(
        self,
        repository: LocalDataRepository,
        item: Dict[str, str],
        detail_html: str,
    ) -> tuple[Dict[str, object], List[DownloadedResource]]:
        """Build a dataset metadata payload and associated resource records.

        Extracts labeled fields from the detail HTML and, if present, persists a
        synthetic API metadata file as a `DownloadedResource`.

        Args:
            repository (LocalDataRepository): Repository for persistence.
            item (Dict[str, str]): Parsed listing item with IDs and URLs.
            detail_html (str): Full HTML of the dataset detail page.

        Returns:
            tuple[Dict[str, object], List[DownloadedResource]]: Payload ready for
            `save_dataset_detail` and any synthetic resource records created.
        """
        fields: Dict[str, Optional[str]] = {}
        for key, label in self._FIELD_LABELS.items():
            fields[key] = self._extract_field(detail_html, label)

        api_url = self._extract_api_url(detail_html)
        base64_id = base64.b64encode(item["id"].encode()).decode()

        payload: Dict[str, object] = {
            "id": item["id"],
            "title": item["title"],
            "detail_url": item["detail_url"],
            "api_url": api_url,
            "api_base64_id": base64_id,
            "published_at": item.get("published_at"),
            "page_index": item.get("page_index"),
            "metadata_fields": {k: v for k, v in fields.items() if v},
        }

        resources: List[DownloadedResource] = []

        if api_url:
            resources.append(self._persist_api_metadata(repository, item["id"], api_url, base64_id))

        return payload, resources

    def _extract_field(self, html: str, label: str) -> Optional[str]:
        """Extract a metadata field from the detail HTML using its label.

        Strategy:
            1) Find the index of the label string.
            2) Search forward for `value:'...'/` pairs.
            3) Return the first non-empty, non-placeholder value.

        Args:
            html (str): Source HTML.
            label (str): Vietnamese label literal to search for.

        Returns:
            Optional[str]: Cleaned field value or None.
        """
        idx = html.find(label)
        if idx == -1:
            return None
        after = html[idx + len(label):]
        for match in re.finditer(r"value:'([^']*)'", after):
            value = self._normalize_text(match.group(1))
            lowered = value.lower()
            if (
                not value
                or value == label
                or value.endswith(":")
                or "đang xử lý" in lowered
                or lowered.startswith("<!--")
                or "recaptcha" in lowered
            ):
                continue
            if lowered.startswith("<"):
                continue
            return value
        return None

    def _extract_api_url(self, html: str) -> Optional[str]:
        """Locate an API URL within the dataset detail HTML.

        Args:
            html (str): Source HTML.

        Returns:
            Optional[str]: The first matching API URL if present.
        """
        match = self._API_URL_PATTERN.search(html)
        if match:
            return match.group(0)
        return None

    def _persist_api_metadata(
        self,
        repository: LocalDataRepository,
        dataset_id: str,
        api_url: str,
        base64_id: str,
    ) -> DownloadedResource:
        """Persist API endpoint metadata as a synthetic resource.

        Writes a small JSON document containing the dataset ID, base64 variant,
        and the discovered API URL. The file is stored under the `API` category.

        Args:
            repository (LocalDataRepository): Repository for path resolution.
            dataset_id (str): Dataset identifier in the portal.
            api_url (str): Discovered API endpoint URL.
            base64_id (str): Base64-encoded dataset identifier.

        Returns:
            DownloadedResource: Descriptor pointing to the written JSON file.
        """
        metadata = {
            "dataset_id": dataset_id,
            "dataset_base64_id": base64_id,
            "api_url": api_url,
            "notes": "API có thể yêu cầu xác thực/captcha trước khi truy vấn dữ liệu.",
        }
        destination = repository.resource_destination(dataset_id, "api_endpoint.json", ResourceCategory.API)
        destination.parent.mkdir(parents=True, exist_ok=True)
        with destination.open("w", encoding="utf-8") as fh:
            json.dump(metadata, fh, ensure_ascii=False, indent=2)

        return DownloadedResource(
            resource_id=f"api:{dataset_id}",
            filename="api_endpoint.json",
            category=ResourceCategory.API,
            local_path=destination,
            normalized_path=destination,
        )

    # ------------------------------------------------------------------
    # Utility helpers
    # ------------------------------------------------------------------
    def _absolute_url(self, path: str) -> str:
        """Resolve paths into absolute URLs using the portal base.

        Args:
            path (str): Either an absolute URL or a portal-relative path.

        Returns:
            str: Absolute URL string.
        """
        if path.startswith("http"):
            return path
        return f"{self._BASE_URL}{path}"

    @staticmethod
    def _normalize_text(value: str) -> str:
        """Collapse whitespace and escape sequences into readable text.

        Args:
            value (str): Raw string possibly containing escape sequences.

        Returns:
            str: Cleaned single-line text.
        """
        return (
            value.replace("\\n", " ")
            .replace("\\r", " ")
            .replace("\n", " ")
            .replace("\r", " ")
            .strip()
        )


__all__ = ["DaNangConnector"]
