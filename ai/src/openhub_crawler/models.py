"""Typed response models for Dong Thap dataset metadata."""

from __future__ import annotations

from datetime import datetime
from typing import List, Optional

from pydantic import BaseModel, Field


class TopicSummary(BaseModel):
    id: int = Field(alias="_id")
    name: str
    icon: Optional[str] = None
    avatar: Optional[str] = None
    is_active: int
    created_time: datetime
    updated_time: Optional[datetime] = None
    type: int


class DatasetResourceFile(BaseModel):
    id: Optional[str] = None
    name: Optional[str] = None
    format: Optional[str] = None
    size: Optional[str] = None


class DatasetResourceApiColumn(BaseModel):
    id: str
    name: str
    dataType: Optional[str] = None
    isOutput: Optional[bool] = None


class DatasetResourceApiTable(BaseModel):
    id: Optional[str] = None
    name: Optional[str] = None
    columns: List[DatasetResourceApiColumn] = Field(default_factory=list)


class DatasetResourceApi(BaseModel):
    _id: Optional[int] = None
    type: Optional[str] = None
    url: Optional[str] = None
    table: Optional[DatasetResourceApiTable] = None


class DatasetResource(BaseModel):
    resource_id: Optional[int] = None
    file: Optional[DatasetResourceFile | str] = None
    api: Optional[DatasetResourceApi | str] = None


class DatasetTag(BaseModel):
    id: int
    name: str


class DatasetSummary(BaseModel):
    id: int
    parent_id: int
    title: str
    description: Optional[str] = None
    licence: Optional[str] = None
    author: Optional[str] = None
    topicId: Optional[str] = None
    topicName: Optional[str] = None
    created_time: Optional[datetime] = None
    updated_time: Optional[datetime] = None
    resources: Optional[str] = None
    tags: List[DatasetTag] = Field(default_factory=list)


class DatasetDetail(BaseModel):
    parent_id: int
    title: str
    description: Optional[str] = None
    licence: Optional[str] = None
    author: Optional[str] = None
    created_time: Optional[datetime] = None
    updated_time: Optional[datetime] = None
    lastModifiedDate: Optional[int] = None
    download_count: Optional[int] = None
    topic: Optional[DatasetTag] = None
    tags: List[DatasetTag] = Field(default_factory=list)
    resources: List[DatasetResource] = Field(default_factory=list)
