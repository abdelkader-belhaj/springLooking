package tn.hypercloud.dto.event;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Test Postman sans paiement : envoi d'un email billet avec QR (lien vers {@link #ticketUrl}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventMailTestRequest {

    @NotBlank(message = "Email obligatoire")
    @Email(message = "Email invalide")
    private String email;

    /**
     * URL encodee dans le QR (ex. page Angular ticket).
     * Si vide, une URL de demo est generee avec {@code app.frontend.base-url}.
     */
    private String ticketUrl;
}
