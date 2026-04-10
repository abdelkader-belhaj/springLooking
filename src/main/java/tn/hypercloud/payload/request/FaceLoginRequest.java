package tn.hypercloud.payload.request;

import jakarta.validation.constraints.NotBlank;

public class FaceLoginRequest {

    @NotBlank(message = "Login obligatoire")
    private String login;

    @NotBlank(message = "Image base64 obligatoire")
    private String imageBase64;

    private Double threshold;

    public FaceLoginRequest() {
    }

    public FaceLoginRequest(String login, String imageBase64, Double threshold) {
        this.login = login;
        this.imageBase64 = imageBase64;
        this.threshold = threshold;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }
}