package tn.hypercloud.entity.reservation.dto;

import lombok.Data;
import tn.hypercloud.entity.reservation.Panier;

@Data
public class ReservationRequest {
    private Integer volAllerId;
    private Integer volRetourId;      // null = aller simple
    private Panier.TypeBillet typeBillet;
    private byte nbPassagers;
}