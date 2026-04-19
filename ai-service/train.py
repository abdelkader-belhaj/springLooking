from datasets import load_dataset
from sklearn.pipeline import Pipeline
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report
import joblib
import os
import re

# =============================
# 1. DATASET FRANÇAIS : ALLOCINE
# =============================
print("📦 Chargement du dataset Allocine (français)...")
ds = load_dataset("allocine")

train_texts  = ds["train"]["review"]
train_labels = ds["train"]["label"]   # 0=NEGATIVE, 1=POSITIVE
test_texts   = ds["test"]["review"]
test_labels  = ds["test"]["label"]

print(f"✅ Train : {len(train_texts)} exemples")
print(f"✅ Test  : {len(test_texts)} exemples")

# =============================
# 2. NETTOYAGE
# =============================
def clean(text):
    text = text.lower()
    text = re.sub(r"http\S+", "", text)
    text = re.sub(r"[^a-zàâçéèêëîïôûùüÿñæœ0-9\s]", "", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text

print("🧹 Nettoyage des textes...")
train_texts = [clean(t) for t in train_texts]
test_texts  = [clean(t) for t in test_texts]

# =============================
# 3. PIPELINE
# =============================
print("🧠 Entraînement...")
pipeline = Pipeline([
    ("tfidf", TfidfVectorizer(
        max_features=80000,
        ngram_range=(1, 3),
        sublinear_tf=True,
        min_df=2,
        analyzer="word"
    )),
    ("clf", LogisticRegression(
        C=5.0,
        max_iter=1000,
        solver="saga",
        n_jobs=-1
    ))
])

pipeline.fit(train_texts, train_labels)

# =============================
# 4. EVALUATION
# =============================
print("\n📊 Évaluation :")
preds = pipeline.predict(test_texts)
print(classification_report(
    test_labels, preds,
    target_names=["NEGATIVE", "POSITIVE"]
))

# =============================
# 5. SAUVEGARDE
# =============================
os.makedirs("model", exist_ok=True)
joblib.dump(pipeline, "model/sentiment_pipeline.pkl")
print("✅ Modèle sauvegardé !")