package tn.hypercloud.entity.reservation.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class VolRequest {
    private String numero;
    private String depart;
    private String arrivee;
    private LocalDate dateDepart;
    private LocalTime heureDepart;
    private BigDecimal prix;
    private int places;
    private java.util.List<EscaleRequest> escales;
    private Integer offreId;
    private Integer retard;
}