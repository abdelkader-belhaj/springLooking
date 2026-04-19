package tn.hypercloud.payload.request;

import jakarta.validation.constraints.NotBlank;

public class TwoFactorCodeRequest {

    @NotBlank(message = "Code 2FA obligatoire")
    private String code;

    public TwoFactorCodeRequest() {
    }

    public TwoFactorCodeRequest(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}