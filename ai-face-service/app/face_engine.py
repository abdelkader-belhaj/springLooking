import base64
from io import BytesIO
from typing import List

import numpy as np
from deepface import DeepFace
from PIL import Image


def _decode_base64_to_bgr(image_base64: str) -> np.ndarray:
    if "," in image_base64:
        image_base64 = image_base64.split(",", 1)[1]

    raw = base64.b64decode(image_base64)
    pil = Image.open(BytesIO(raw)).convert("RGB")
    rgb = np.array(pil)
    # DeepFace expects BGR when passing numpy arrays.
    bgr = rgb[:, :, ::-1]
    return bgr


def extract_embedding(image_base64: str, model_name: str, detector_backend: str) -> List[float]:
    image_bgr = _decode_base64_to_bgr(image_base64)
    reps = DeepFace.represent(
        img_path=image_bgr,
        model_name=model_name,
        detector_backend=detector_backend,
        enforce_detection=True,
        normalization="base",
    )

    if not reps:
        raise ValueError("No face detected in image")

    return reps[0]["embedding"]


def cosine_similarity(a: List[float], b: List[float]) -> float:
    va = np.array(a, dtype=np.float32)
    vb = np.array(b, dtype=np.float32)

    if va.shape != vb.shape:
        raise ValueError("Embedding vectors must have the same shape")

    denom = np.linalg.norm(va) * np.linalg.norm(vb)
    if denom == 0:
        raise ValueError("Invalid embedding norm")

    return float(np.dot(va, vb) / denom)
