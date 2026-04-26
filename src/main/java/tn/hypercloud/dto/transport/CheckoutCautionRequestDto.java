package tn.hypercloud.dto.transport;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutCautionRequestDto(
        List<String> photoUrls,
        boolean constatDommages,
        String descriptionDommages,
        BigDecimal montantDommages,
        BigDecimal montantCautionRetenu,
        BigDecimal montantCautionRestitue
) {}