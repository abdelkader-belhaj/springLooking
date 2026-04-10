package tn.hypercloud.controller.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.transport.ChatMessageDTO;
import tn.hypercloud.dto.transport.DriverNotificationDTO;
import tn.hypercloud.dto.transport.LocationUpdateDTO;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.Localisation;
import tn.hypercloud.service.transport.IChauffeurService;
import tn.hypercloud.service.transport.ICourseService;
import tn.hypercloud.service.transport.MessageServiceImpl;

@Controller
@RequiredArgsConstructor
public class RealTimeTransportController {

    private final SimpMessagingTemplate messagingTemplate;
    private final IChauffeurService chauffeurService;
    private final ICourseService courseService;   // injecté pour usage futur
    private final MessageServiceImpl messageServiceImpl;

    /**
     * Driver envoie sa position toutes les 3-5 secondes (via WebSocket)
     */
    @MessageMapping("/location/update")
    @Transactional

    public void updateDriverLocation(@Payload LocationUpdateDTO update) {
        try {
            Chauffeur chauffeur = chauffeurService.getChauffeurById(update.getChauffeurId());

            Localisation pos = chauffeur.getPositionActuelle();

            if (pos == null) {
                // Création d'une nouvelle position (cascade ALL va la sauvegarder)
                pos = Localisation.builder()
                        .latitude(update.getLatitude())
                        .longitude(update.getLongitude())
                        .build();
                chauffeur.setPositionActuelle(pos);
            } else {
                // Mise à jour existante
                pos.setLatitude(update.getLatitude());
                pos.setLongitude(update.getLongitude());
            }

            chauffeurService.save(chauffeur);   // ← Important : cette méthode doit exister dans ton service

            // Diffusion en temps réel à tous les clients qui suivent cette course (carte Angular)
            String destination = "/topic/course/" + update.getCourseId() + "/location";
            messagingTemplate.convertAndSend(destination, update);

        } catch (Exception e) {
            System.err.println("❌ Erreur mise à jour position chauffeur : " + e.getMessage());
        }
    }

    /**
     * Méthode publique à appeler depuis tes Services (MatchingService, CourseService, etc.)
     * → Envoie notification sonore + visuelle au chauffeur
     */
    public void sendNotificationToDriver(Long chauffeurId, DriverNotificationDTO notification) {
        System.out.println("📨 [DEBUG] Envoi notification via WebSocket → chauffeur " + chauffeurId
                + " | type = " + notification.getType());

        Chauffeur chauffeur = chauffeurService.getChauffeurById(chauffeurId);
        String principalName = chauffeur.getUtilisateur().getEmail(); // = getUsername() côté UserDetails
        messagingTemplate.convertAndSendToUser(
                principalName,
                "/queue/notifications",
                notification
        );
    }
    @MessageMapping("/chat/send")
    @Transactional
    public void sendChatMessage(@Payload ChatMessageDTO message) {
        // Sauvegarde en base (tu peux créer un service MessageService plus tard)
        // Pour l'instant on diffuse directement

        String destination = "/topic/course/" + message.getCourseId() + "/chat";
        messagingTemplate.convertAndSend(destination, message);

        System.out.println("💬 Message envoyé dans la course " + message.getCourseId());
    }
    /**
     * Diffusion du message de chat
     */
    public void broadcastChatMessage(ChatMessageDTO message) {
        messagingTemplate.convertAndSend("/topic/course/" + message.getCourseId() + "/chat", message);
    }

    /**
     * Diffusion des accusés de réception (delivered + read)
     */
    public void broadcastReadReceipt(ChatMessageDTO receipt) {
        messagingTemplate.convertAndSend("/topic/course/" + receipt.getCourseId() + "/chat/receipts", receipt);
    }
    @MessageMapping("/chat/delivered")
    @Transactional
    public void messageDelivered(@Payload Long messageId) {
        messageServiceImpl.markMessageAsDelivered(messageId);
    }

    @MessageMapping("/chat/read")
    @Transactional
    public void messageRead(@Payload Long messageId) {
        messageServiceImpl.markMessageAsRead(messageId);
    }
}