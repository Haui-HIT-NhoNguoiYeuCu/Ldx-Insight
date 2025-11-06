"""HTTP client wrappers for Dong Thap open data crawling."""

from __future__ import annotations

from pathlib import Path
from typing import Any, Dict, List, Optional

import httpx
from rich.console import Console
from tenacity import retry, stop_after_attempt, wait_exponential

from . import endpoints
from .models import DatasetDetail, DatasetSummary, TopicSummary


console = Console()


class DongThapOpenDataClient:
    """Thin wrapper around the portal endpoints.

    Purpose:
        Provide a small, typed interface over the Đồng Tháp open-data API:
        - Search datasets (paged).
        - Fetch dataset detail.
        - List active topics.
        - Download resource files.

    Notes:
        The client is synchronous and backed by a single `httpx.Client`.
        Selected calls are wrapped with `tenacity.retry` using exponential backoff.
    """

    def __init__(self, base_url: str = endpoints.BASE_URL, timeout: float = 30.0) -> None:
        """Create a session for the Dong Thap API with sensible defaults.

        Args:
            base_url (str): API base URL. Defaults to `endpoints.BASE_URL`.
            timeout (float): Per-request timeout in seconds. Defaults to 30.0.
        """
        self.base_url = base_url
        self._client = httpx.Client(base_url=base_url, timeout=timeout)

    def close(self) -> None:
        """Close the underlying HTTP client.

        Closes all keep-alive connections and releases resources.
        """
        self._client.close()

    def _post(self, path: str, json: Dict[str, Any]) -> Dict[str, Any]:
        """Perform a POST request and return the JSON payload.

        Args:
            path (str): Endpoint path relative to the base URL.
            json (Dict[str, Any]): JSON body to send.

        Returns:
            Dict[str, Any]: Parsed JSON response.

        Raises:
            httpx.HTTPStatusError: If the response status is not 2xx.
            httpx.RequestError: On network-level failures.
            ValueError: If response body is not valid JSON.
        """
        response = self._client.post(path, json=json)
        response.raise_for_status()
        return response.json()

    def _get(self, path: str, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Perform a GET request and return the JSON payload.

        Args:
            path (str): Endpoint path relative to the base URL.
            params (Optional[Dict[str, Any]]): Query parameters.

        Returns:
            Dict[str, Any]: Parsed JSON response.

        Raises:
            httpx.HTTPStatusError: If the response status is not 2xx.
            httpx.RequestError: On network-level failures.
            ValueError: If response body is not valid JSON.
        """
        response = self._client.get(path, params=params)
        response.raise_for_status()
        return response.json()

    @retry(wait=wait_exponential(multiplier=1, min=1, max=8), stop=stop_after_attempt(3))
    def search_datasets(self, limit: int = 100, offset: int = 0) -> List[DatasetSummary]:
        """Return a page of datasets as `DatasetSummary` objects.

        This is a convenience wrapper around `search_datasets_with_total`
        that discards the total count.

        Args:
            limit (int): Page size. Defaults to 100.
            offset (int): Zero-based offset. Defaults to 0.

        Returns:
            List[DatasetSummary]: Dataset summaries for the requested page.

        Retry:
            Retries up to 3 attempts with exponential backoff on exceptions.
        """
        datasets, _ = self.search_datasets_with_total(limit=limit, offset=offset)
        return datasets

    @retry(wait=wait_exponential(multiplier=1, min=1, max=8), stop=stop_after_attempt(3))
    def search_datasets_with_total(self, limit: int = 100, offset: int = 0) -> tuple[List[DatasetSummary], int]:
        """Return a page of datasets and the total count.

        Args:
            limit (int): Page size. Defaults to 100.
            offset (int): Zero-based offset. Defaults to 0.

        Returns:
            tuple[List[DatasetSummary], int]: A pair of
                (dataset summaries, total available count).

        Raises:
            RuntimeError: If the API signals a non-successful `error_code`.

        Retry:
            Retries up to 3 attempts with exponential backoff on exceptions.
        """
        payload = {
            "limit": limit,
            "offset": offset,
            "query": "",
            "topicId": "",
            "format": "",
            "licence": "",
            "sort_name": "created_time.keyword",
            "sort_type": "desc",
            "type_list": "",
            "account": "",
        }
        data = self._post(endpoints.DATASET_SEARCH, payload)
        if data.get("error_code") != "SUCCESSFUL":
            raise RuntimeError(f"Dataset search failed: {data}")
        items = data["data"]["data"]
        total = data["data"].get("total", len(items))
        return [DatasetSummary.model_validate(item["_source"]) for item in items], total

    @retry(wait=wait_exponential(multiplier=1, min=1, max=8), stop=stop_after_attempt(3))
    def get_dataset_detail(self, parent_id: int) -> DatasetDetail:
        """Fetch detailed metadata for a dataset.

        Args:
            parent_id (int): Dataset identifier used by the portal.

        Returns:
            DatasetDetail: Parsed dataset metadata.

        Raises:
            RuntimeError: If the API signals a non-successful `error_code`.

        Retry:
            Retries up to 3 attempts with exponential backoff on exceptions.
        """
        params = {"parent_id": parent_id, "account": ""}
        data = self._get(endpoints.DATASET_DETAIL, params=params)
        if data.get("error_code") != "SUCCESSFUL":
            raise RuntimeError(f"Dataset detail failed for {parent_id}: {data}")
        return DatasetDetail.model_validate(data["data"])

    @retry(wait=wait_exponential(multiplier=1, min=1, max=3), stop=stop_after_attempt(3))
    def list_topics(self) -> List[TopicSummary]:
        """Return the list of active topics.

        Returns:
            List[TopicSummary]: Active topics exposed by the portal.

        Raises:
            RuntimeError: If the API signals a non-successful `error_code`.

        Retry:
            Retries up to 3 attempts with exponential backoff on exceptions.
        """
        payload = {"type": 1, "is_active": 1, "sort": {"_id": 1}}
        data = self._post(endpoints.TOPIC_LIST, payload)
        if data.get("error_code") != "SUCCESSFUL":
            raise RuntimeError(f"Topic listing failed: {data}")
        return [TopicSummary.model_validate(raw) for raw in data["data"]]

    def download_resource_file(
        self,
        parent_id: int,
        resource_id: int,
        destination: Path,
        account: str = "",
    ) -> Path:
        """Download a dataset resource file to `destination`.

        Downloads the binary stream and writes it to disk. If the server
        provides a `Content-Disposition` filename, it will be used to rename
        the final file in the target directory.

        Args:
            parent_id (int): Parent dataset identifier.
            resource_id (int): Resource identifier within the dataset.
            destination (Path): Target path hint. The directory is created if missing.
            account (str): Optional account filter. Defaults to empty.

        Returns:
            Path: Final file path written to disk.

        Raises:
            httpx.HTTPStatusError: If the response status is not 2xx.
            httpx.RequestError: On network-level failures.

        Side Effects:
            Creates parent directories for `destination` if needed and writes the file.
        """
        params = {"parent_id": parent_id, "resource_id": resource_id, "account": account}
        destination.parent.mkdir(parents=True, exist_ok=True)
        with self._client.stream("GET", endpoints.RESOURCE_DOWNLOAD, params=params) as response:
            response.raise_for_status()
            filename = _extract_filename(response) or destination.name
            final_path = destination.with_name(filename)
            with final_path.open("wb") as fh:
                for chunk in response.iter_bytes():
                    fh.write(chunk)
        return final_path


def _extract_filename(response: httpx.Response) -> Optional[str]:
    """Extract a filename from the `Content-Disposition` header.

    Args:
        response (httpx.Response): HTTP response object.

    Returns:
        Optional[str]: Filename if present, otherwise None.

    Notes:
        This parses the simple `filename="..."` form and does not handle
        RFC 5987 extended parameters (e.g., `filename*=`).
    """
    content_disposition = response.headers.get("Content-Disposition")
    if not content_disposition:
        return None
    parts = content_disposition.split(";")
    for part in parts:
        part = part.strip()
        if part.lower().startswith("filename="):
            filename = part.split("=", 1)[1].strip('"')
            return filename
    return None


def main() -> None:
    """Minimal CLI demonstration.

    Steps:
        1) List active topics and report the count.
        2) Fetch the first page of datasets and print a small sample.
        3) Fetch detail for the first dataset and show a few fields.

    This function is intended for manual smoke-testing only.
    """
    client = DongThapOpenDataClient()
    try:
        console.log("[bold]Fetching topics…[/]")
        topics = client.list_topics()
        console.log(f"Received {len(topics)} active topics.")

        console.log("[bold]Fetching datasets (first page)…[/]")
        datasets = client.search_datasets(limit=10, offset=0)
        console.log(f"Received {len(datasets)} dataset summaries. Sample:")
        for ds in datasets[:3]:
            console.log(f"- {ds.parent_id}: {ds.title}")
        if datasets:
            detail = client.get_dataset_detail(datasets[0].parent_id)
            console.log(f"[bold]Detail for dataset {detail.parent_id}[/]")
            console.log(f"Title: {detail.title}")
            console.log(f"Resources: {len(detail.resources)}")
    finally:
        client.close()
if __name__ == "__main__":
    main()
