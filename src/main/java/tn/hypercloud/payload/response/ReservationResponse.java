package tn.hypercloud.payload.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReservationResponse {
    private Integer       idReservation;
    private Integer       idLogement;
    private String        nomLogement;
    private String        villeLogement;
    private Long          idClient;
    private String        nomClient;
    private LocalDate     dateDebut;
    private LocalDate     dateFin;
    private int           nbPersonnes;
    private BigDecimal    prixTotal;
    private String        statut;
    private LocalDateTime dateReservation;
    private boolean       canCancelOrModify;
    private Integer       capaciteLogement;
    private String        smartLockCode;
    private boolean       archived;
}