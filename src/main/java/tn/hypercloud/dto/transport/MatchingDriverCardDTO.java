package tn.hypercloud.dto.transport;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingDriverCardDTO {
    private Long idMatching;
    private Long idDemande;
    private String statut;
    private Long chauffeurId;
    private BigDecimal prixEstime;
    private String typeVehicule;
    private String adresseDepart;
    private String adresseArrivee;
    private String clientNom;
}