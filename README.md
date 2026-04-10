# FaceId
cd C:\empaclment\ai-face-service

1) Créer l'environnement virtuel


py -3.11 -m venv .venv


si py n'existe pas:


python -m venv .venv

2) Activer

   
.\.venv\Scripts\Activate.ps1

4) Installer les dépendances
python -m pip install --upgrade pip
pip install -r requirements.txt

5) Lancer l'API
python -m uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload --env-file .env
