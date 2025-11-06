"""Centralised endpoint path definitions for the Dong Thap open data portal."""

from urllib.parse import urljoin


BASE_URL = "https://opendata.dongthap.gov.vn"

DATASET_SEARCH = "/dataset/search-home"
DATASET_DETAIL = "/xhr/admin/dataset/client-get-one-dataset"
TOPIC_LIST = "/xhr/public/common/TOPICS/getList"
RESOURCE_DETAIL = "/xhr/admin/resource/get-one/{resource_id}"
RESOURCE_DOWNLOAD = "/xhr/admin/dataset/download-file"


def absolute_url(path: str) -> str:
    """Return an absolute URL for the given path."""
    return urljoin(BASE_URL, path)