"""Tests for MongoSink environment-based initialization."""

from __future__ import annotations

import os
import sys
import unittest
from unittest import mock
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC_DIR = ROOT / "src"
if str(SRC_DIR) not in sys.path:
    sys.path.insert(0, str(SRC_DIR))

from openhub_crawler.mongo_sink import MongoSink


class MongoSinkEnvTest(unittest.TestCase):
    """Ensure MongoSink respects environment configuration."""

    def setUp(self) -> None:
        self._original_uri = os.environ.pop("MONGO_URI", None)
        self._original_db = os.environ.pop("MONGO_DB", None)

    def tearDown(self) -> None:
        if self._original_uri is not None:
            os.environ["MONGO_URI"] = self._original_uri
        else:
            os.environ.pop("MONGO_URI", None)
        if self._original_db is not None:
            os.environ["MONGO_DB"] = self._original_db
        else:
            os.environ.pop("MONGO_DB", None)

    @mock.patch("openhub_crawler.mongo_sink.MongoClient")
    def test_from_env_returns_sink_when_configured(self, mock_client: mock.Mock) -> None:
        """MongoSink.from_env should initialize when env vars are provided."""
        os.environ["MONGO_URI"] = "mongodb://example.com:27017"
        os.environ["MONGO_DB"] = "crawler"
        client_instance = mock_client.return_value
        client_instance.admin.command.return_value = {"ok": 1}

        sink = MongoSink.from_env()

        self.assertIsNotNone(sink)
        mock_client.assert_called_once_with(
            "mongodb://example.com:27017",
            serverSelectionTimeoutMS=5000,
            connectTimeoutMS=5000,
        )
        client_instance.admin.command.assert_called_once_with("ping")


if __name__ == "__main__":
    unittest.main()
