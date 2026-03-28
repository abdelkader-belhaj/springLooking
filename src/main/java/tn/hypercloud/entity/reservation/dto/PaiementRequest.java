package tn.hypercloud.entity.reservation.dto;

import lombok.Data;

@Data
public class PaiementRequest {
    private Integer reservationId;
    private String methode;   // "carte", "paypal", "flouci"
}