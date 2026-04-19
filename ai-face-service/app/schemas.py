import os
from typing import List, Optional

from pydantic import BaseModel, Field


DEFAULT_FACE_MODEL_NAME = os.getenv("FACE_MODEL_NAME", "SFace")
DEFAULT_FACE_DETECTOR_BACKEND = os.getenv("FACE_DETECTOR_BACKEND", "opencv")
DEFAULT_FACE_MATCH_THRESHOLD = float(os.getenv("FACE_MATCH_THRESHOLD", "0.75"))


class HealthResponse(BaseModel):
    status: str


class ExtractEmbeddingRequest(BaseModel):
    image_base64: str = Field(..., description="Base64 image from webcam/photo")
    detector_backend: str = Field(default=DEFAULT_FACE_DETECTOR_BACKEND)
    model_name: str = Field(default=DEFAULT_FACE_MODEL_NAME)


class ExtractEmbeddingResponse(BaseModel):
    embedding: List[float]
    embedding_size: int
    detector_backend: str
    model_name: str


class VerifyFaceRequest(BaseModel):
    image_base64: str = Field(..., description="Probe image in base64")
    enrolled_embedding: List[float] = Field(..., description="Embedding stored by Spring DB")
    threshold: float = Field(default=DEFAULT_FACE_MATCH_THRESHOLD)
    detector_backend: str = Field(default=DEFAULT_FACE_DETECTOR_BACKEND)
    model_name: str = Field(default=DEFAULT_FACE_MODEL_NAME)


class VerifyFaceResponse(BaseModel):
    matched: bool
    similarity: float
    threshold: float
    detector_backend: str
    model_name: str


class ErrorResponse(BaseModel):
    detail: str
    hint: Optional[str] = None
