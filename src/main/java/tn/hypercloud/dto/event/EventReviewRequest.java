package tn.hypercloud.dto.event;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventReviewRequest {

    @Min(value = 1, message = "La note minimum est 1")
    @Max(value = 5, message = "La note maximum est 5")
    private int rating;

    @NotBlank(message = "Le commentaire est obligatoire")
    @Size(min = 10, max = 2000, message = "Le commentaire doit contenir entre 10 et 2000 caractères")
    private String comment;
}
