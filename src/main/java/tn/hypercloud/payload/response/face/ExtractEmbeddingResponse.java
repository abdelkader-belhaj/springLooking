package tn.hypercloud.payload.response.face;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ExtractEmbeddingResponse {

    private List<Double> embedding;

    @JsonProperty("embedding_size")
    private Integer embeddingSize;

    @JsonProperty("detector_backend")
    private String detectorBackend;

    @JsonProperty("model_name")
    private String modelName;

    public ExtractEmbeddingResponse() {
    }

    public List<Double> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }

    public Integer getEmbeddingSize() {
        return embeddingSize;
    }

    public void setEmbeddingSize(Integer embeddingSize) {
        this.embeddingSize = embeddingSize;
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