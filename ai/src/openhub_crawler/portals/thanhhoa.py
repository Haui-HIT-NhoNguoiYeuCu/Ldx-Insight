"""Thanh Hóa portal connector (CKAN)."""

from __future__ import annotations

from .ckan import CkanConnector


class ThanhHoaConnector(CkanConnector):
    def __init__(self) -> None:
        super().__init__(
            slug="thanh-hoa",
            display_name="Thanh Hóa Open Data",
            base_url="https://opendata.thanhhoa.gov.vn",
        )


__all__ = ["ThanhHoaConnector"]