"""Ho Chi Minh City portal connector (CKAN)."""

from __future__ import annotations

from .ckan import CkanConnector


class HoChiMinhConnector(CkanConnector):
    def __init__(self) -> None:
        super().__init__(
            slug="hcm",
            display_name="TP. Hồ Chí Minh Open Data",
            base_url="https://opendata.hochiminhcity.gov.vn",
        )


__all__ = ["HoChiMinhConnector"]
