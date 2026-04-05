package tn.hypercloud.entity.reservation.dto;

import lombok.Builder;
import lombok.Data;
import tn.hypercloud.entity.reservation.Panier;
import tn.hypercloud.entity.reservation.PaiementVol;
import tn.hypercloud.entity.reservation.ReservationVol;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ReservationResponse {
    private Integer id;
    private String reference;
    private String touristeEmail;
    private VolResponse volAller;
    private VolResponse volRetour;
    private Panier.TypeBillet typeBillet;
    private byte nbPassagers;
    private BigDecimal prixTotal;
    private LocalDateTime dateReservation;
    private PaiementVol.StatutPaiement statutPaiement;

    // ← NOUVEAU
    private ReservationVol.StatutReservation statutReservation;
}