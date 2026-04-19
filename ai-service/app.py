from flask import Flask, request, jsonify
import joblib
import re

app = Flask(__name__)

pipeline = joblib.load("model/sentiment_pipeline.pkl")

def clean(text):
    text = text.lower()
    text = re.sub(r"http\S+", "", text)
    text = re.sub(r"[^a-zàâçéèêëîïôûùüÿñæœ0-9\s]", "", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text

@app.route("/analyze", methods=["POST"])
def analyze():
    data = request.get_json()
    text = data.get("text", "").strip()

    if not text:
        return jsonify({"error": "text is required"}), 400

    cleaned = clean(text)
    proba   = pipeline.predict_proba([cleaned])[0]

    neg_score = proba[0]
    pos_score = proba[1]

    # Seuil adaptatif
    diff = abs(pos_score - neg_score)

    if diff < 0.15:
        sentiment = "NEUTRAL"
    elif pos_score > neg_score:
        sentiment = "POSITIVE"
    else:
        sentiment = "NEGATIVE"

    return jsonify({
        "sentiment": sentiment,
        "score": round(float(max(proba)), 4)
    })

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)