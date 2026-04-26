package tn.hypercloud.payload.request;

import lombok.*;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReservationRequest {
    private Integer   idLogement;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private int       nbPersonnes;
    private BigDecimal prixFinalNegocie;
}