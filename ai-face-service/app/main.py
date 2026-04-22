from fastapi import FastAPI, HTTPException

from app.face_engine import cosine_similarity, extract_embedding
from app.schemas import (
    ErrorResponse,
    ExtractEmbeddingRequest,
    ExtractEmbeddingResponse,
    HealthResponse,
    VerifyFaceRequest,
    VerifyFaceResponse,
)

app = FastAPI(
    title="Face Auth AI Service",
    version="1.0.0",
    description="Local AI service for facial enrollment and verification",
)


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="ok")


@app.post(
    "/v1/face/extract-embedding",
    response_model=ExtractEmbeddingResponse,
    responses={400: {"model": ErrorResponse}},
)
def extract_embedding_endpoint(payload: ExtractEmbeddingRequest) -> ExtractEmbeddingResponse:
    try:
        embedding = extract_embedding(
            image_base64=payload.image_base64,
            model_name=payload.model_name,
            detector_backend=payload.detector_backend,
        )
        return ExtractEmbeddingResponse(
            embedding=embedding,
            embedding_size=len(embedding),
            detector_backend=payload.detector_backend,
            model_name=payload.model_name,
        )
    except Exception as ex:
        raise HTTPException(
            status_code=400,
            detail=f"Unable to extract embedding: {str(ex)}",
        ) from ex


@app.post(
    "/v1/face/verify",
    response_model=VerifyFaceResponse,
    responses={400: {"model": ErrorResponse}},
)
def verify_face_endpoint(payload: VerifyFaceRequest) -> VerifyFaceResponse:
    try:
        probe_embedding = extract_embedding(
            image_base64=payload.image_base64,
            model_name=payload.model_name,
            detector_backend=payload.detector_backend,
        )
        sim = cosine_similarity(probe_embedding, payload.enrolled_embedding)

        return VerifyFaceResponse(
            matched=sim >= payload.threshold,
            similarity=sim,
            threshold=payload.threshold,
            detector_backend=payload.detector_backend,
            model_name=payload.model_name,
        )
    except Exception as ex:
        raise HTTPException(
            status_code=400,
            detail=f"Unable to verify face: {str(ex)}",
        ) from ex
