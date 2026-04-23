package tn.hypercloud.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeCheckoutSessionRequest {
    private Integer reservationId;
    private String promoCode;
}
