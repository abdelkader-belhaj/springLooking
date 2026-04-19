# Face Auth AI Service (Python)

Small local AI service to integrate with your Spring Boot monolith.

## Stack

- Python 3.11
- FastAPI
- DeepFace
- Uvicorn

## What this service does

- Extracts face embedding from a live photo
- Verifies a probe image against a stored embedding
- Returns a similarity score and match decision

## Install

```bash
cd ai-face-service
python -m venv .venv
# Windows PowerShell
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## Run

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

Swagger UI:

- http://localhost:8001/docs

## Endpoints

### GET /health

Response:

```json
{ "status": "ok" }
```

### POST /v1/face/extract-embedding

Request:

```json
{
  "image_base64": "data:image/jpeg;base64,/9j/4AAQSk...",
  "detector_backend": "opencv",
  "model_name": "Facenet512"
}
```

Response:

```json
{
  "embedding": [0.012, -0.532, 0.104],
  "embedding_size": 512,
  "detector_backend": "opencv",
  "model_name": "Facenet512"
}
```

### POST /v1/face/verify

Request:

```json
{
  "image_base64": "data:image/jpeg;base64,/9j/4AAQSk...",
  "enrolled_embedding": [0.012, -0.532, 0.104],
  "threshold": 0.75,
  "detector_backend": "opencv",
  "model_name": "Facenet512"
}
```

Response:

```json
{
  "matched": true,
  "similarity": 0.83,
  "threshold": 0.75,
  "detector_backend": "opencv",
  "model_name": "Facenet512"
}
```

## Integration flow with Spring Boot

### Registration (email, name, password, live photo)

1. Angular sends registration form + live photo to Spring.
2. Spring hashes password.
3. Spring calls `POST /v1/face/extract-embedding`.
4. Spring stores user fields + encrypted embedding in DB.
5. Spring returns JWT/session as usual.

### Login with password

- Keep existing `/auth/login` flow unchanged.

### Login with webcam

1. Angular captures live frame and sends to Spring endpoint (example `/auth/face/login`).
2. Spring loads user embedding from DB.
3. Spring calls `POST /v1/face/verify`.
4. If `matched=true`, Spring issues JWT.

## Production notes

- Do not store raw face images after processing.
- Encrypt embedding at rest.
- Keep this service private, accessible only by Spring.
- Add timeout + retry + circuit breaker in Spring HTTP client.
- Add liveness check in v2 (anti-photo/anti-screen attack).
