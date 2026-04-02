package tn.hypercloud.controller.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.transport.DriverNotificationDTO;
import tn.hypercloud.dto.transport.LocationUpdateDTO;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.Localisation;
import tn.hypercloud.service.transport.IChauffeurService;
import tn.hypercloud.service.transport.ICourseService;

@Controller
@RequiredArgsConstructor
public class RealTimeTransportController {

    private final SimpMessagingTemplate messagingTemplate;
    private final IChauffeurService chauffeurService;
    private final ICourseService courseService;   // injecté pour usage futur

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

        messagingTemplate.convertAndSendToUser(
                chauffeurId.toString(),
                "/queue/notifications",
                notification
        );
    }
}