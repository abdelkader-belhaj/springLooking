package tn.hypercloud.payload.request;

import jakarta.validation.constraints.NotBlank;

public class UpdateFaceIdRequest {

    @NotBlank(message = "Image base64 obligatoire")
    private String imageBase64;

    public UpdateFaceIdRequest() {
    }

    public UpdateFaceIdRequest(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
