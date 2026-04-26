package tn.hypercloud.payload.request;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CategorieRequest {
    private String  nomCategorie;
    private String  description;
    private String  icone;
    private boolean statut;
}