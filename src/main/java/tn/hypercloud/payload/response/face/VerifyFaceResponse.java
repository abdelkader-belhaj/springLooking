package tn.hypercloud.payload.response.face;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VerifyFaceResponse {

    private Boolean matched;
    private Double similarity;
    private Double threshold;

    @JsonProperty("detector_backend")
    private String detectorBackend;

    @JsonProperty("model_name")
    private String modelName;

    public VerifyFaceResponse() {
    }

    public Boolean getMatched() {
        return matched;
    }

    public void setMatched(Boolean matched) {
        this.matched = matched;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public String getDetectorBackend() {
        return detectorBackend;
    }

    public void setDetectorBackend(String detectorBackend) {
        this.detectorBackend = detectorBackend;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}