package tn.hypercloud.payload.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorieResponse {

    private Integer idCategorie;
    private String nomCategorie;
    private String description;
    private String icone;
    private boolean statut;
    private LocalDateTime dateCreation;


    private Long nbLogements;


}