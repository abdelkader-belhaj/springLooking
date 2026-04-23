package tn.hypercloud.entity.reservation.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class QrCodeVolResponse {
    private Integer id;
    private Integer reservationId;
    private String reference;          // référence de la réservation
    private String contenu;            // texte encodé dans le QR
    private String imageBase64;        // image PNG en Base64 → affichage direct HTML: data:image/png;base64,...
    private LocalDateTime dateGeneration;
}