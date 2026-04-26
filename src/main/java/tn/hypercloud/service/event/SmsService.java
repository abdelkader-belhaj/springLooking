package tn.hypercloud.service.event;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.hypercloud.service.event.PhoneNumberUtil;

@Service
@Slf4j
public class SmsService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromNumber;

    // Initialise Twilio au démarrage de l'application
    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        log.info("Twilio initialized ✅");
    }

    // Envoyer SMS annulation event au client
    public void sendEventCancellationSms(
            String toPhone,
            String clientName,
            String eventTitle,
            String formattedDate,
            String formattedAmount) {

        String normalizedToPhone = PhoneNumberUtil.normalizeForTwilio(toPhone);
        if (normalizedToPhone == null) {
            log.warn("Phone number is null/invalid (raw='{}'), SMS not sent", toPhone);
            return;
        }

        try {
            String messageBody = "Bonjour " + clientName + ",\n\n"
                    + "Nous vous informons que l'evenement :\n"
                    + "[ " + eventTitle + " ]\n"
                    + "Prevu le : " + formattedDate + "\n"
                    + "a ete annule.\n\n"
                    + "REMBOURSEMENT\n"
                    + "Le montant de " + formattedAmount + " TND\n"
                    + "sera rembourse sous 3 a 5 jours\n"
                    + "ouvrables sur votre compte.\n\n"
                    + "Nous nous excusons pour\n"
                    + "la gene occasionnee.\n\n"
                    + "Pour toute question :\n"
                    + "support@hypercloud.tn\n\n"
                    + "Cordialement,\n"
                    + "L'equipe HyperCloud\n"
                    + "---------------------------";

            Message.creator(
                    new PhoneNumber(normalizedToPhone),
                    new PhoneNumber(fromNumber),
                    messageBody
            ).create();

            log.info("SMS sent to {}", normalizedToPhone);
        } catch (ApiException e) {
            log.error("Twilio SMS failed to {} (code={}): {}",
                    normalizedToPhone,
                    e.getCode(),
                    e.getMessage());
        } catch (Exception e) {
            log.error("SMS send failed to {}: {}", normalizedToPhone, e.getMessage());
        }
    }
}