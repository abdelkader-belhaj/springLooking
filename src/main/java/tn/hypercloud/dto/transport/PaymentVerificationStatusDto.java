package tn.hypercloud.dto.transport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.hypercloud.entity.transport.enums.AnnulePar;
import tn.hypercloud.entity.transport.enums.PaiementStatut;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVerificationStatusDto {
    private Long courseId;
    private boolean clientConfirmed;
    private boolean driverVerified;
    private boolean paymentCreated;
    private boolean cancelled;
    private String verificationCode;
    private String paymentIntentId;
    private PaiementStatut paymentStatut;
    private BigDecimal montantBrut;
    private BigDecimal montantPreautorise;
    private BigDecimal montantRestant;
    private BigDecimal penaltyAmount;
    private BigDecimal refundAmount;
    private AnnulePar cancelledBy;
    private String cancellationReason;
    private LocalDateTime clientConfirmedAt;
    private LocalDateTime driverVerifiedAt;
}
