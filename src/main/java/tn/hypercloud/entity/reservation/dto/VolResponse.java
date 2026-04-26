package tn.hypercloud.entity.reservation.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data @Builder
public class VolResponse {
    private Integer id;
    private String societeNom;
    private String numero;
    private String depart;
    private String arrivee;
    private LocalDate dateDepart;
    private LocalTime heureDepart;
    private BigDecimal prix;
    private int places;
    private java.util.List<EscaleResponse> escales;
    private OffreResponse offre;
    private Integer retard;
}