package tn.hypercloud.entity.reservation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BilletPublicResponse {
    private String reference;
    private String touristeNom;
    private String depart;
    private String arrivee;
    private String numeroVol;
    private String dateDepart;
    private String heureDepart;
    private byte nbPassagers;
    private String typeBillet;
    private String prixTotal;
    private String imageBase64;
}