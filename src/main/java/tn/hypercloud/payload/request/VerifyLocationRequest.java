package tn.hypercloud.payload.request;

import lombok.Data;

@Data
public class VerifyLocationRequest {
    private Integer logementId;
    private Double clientLatitude;
    private Double clientLongitude;
}