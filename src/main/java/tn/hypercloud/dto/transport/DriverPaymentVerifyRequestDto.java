package tn.hypercloud.dto.transport;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverPaymentVerifyRequestDto {
    private String verificationCode;
}
