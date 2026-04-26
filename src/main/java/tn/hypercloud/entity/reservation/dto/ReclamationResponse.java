package tn.hypercloud.entity.reservation.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReclamationResponse {
    private Integer id;
    private Integer reservationId;
    private String reservationReference;
    private String touristeEmail;
    private String priorite;
    private String statut;
    private String sujet;
    private String reponse;
    private LocalDateTime dateCreation;
    private LocalDateTime dateReponse;
    private Boolean clientLu;
}

