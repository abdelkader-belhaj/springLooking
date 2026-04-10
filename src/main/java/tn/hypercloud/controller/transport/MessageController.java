package tn.hypercloud.controller.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.transport.ChatMessageDTO;
import tn.hypercloud.entity.transport.MessageTransport;
import tn.hypercloud.service.transport.IMessageService;

import java.util.List;

@RestController
@RequestMapping("/hypercloud/courses")
@RequiredArgsConstructor
public class MessageController {

    private final IMessageService messageService;

    /**
     * Récupère tout l'historique des messages d'une course
     * Exemple : GET http://localhost:8080/hypercloud/courses/7/messages
     */
    @GetMapping("/{courseId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(@PathVariable Long courseId) {
        List<MessageTransport> messages = messageService.getMessagesByCourse(courseId);

        // Conversion en DTO pour le frontend (plus léger et sans entités JPA)
        List<ChatMessageDTO> dtos = messages.stream()
                .map(msg -> ChatMessageDTO.builder()
                        .courseId(msg.getCourse().getIdCourse())
                        .senderId(msg.getSender().getId())
                        .senderRole(msg.getSender().getRole().name())   // CLIENT ou CHAUFFEUR
                        .contenu(msg.getContenu())
                        .dateEnvoi(msg.getDateEnvoi().toString())
                        .build())
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * (Optionnel) Envoyer un message via REST (fallback si WebSocket tombe)
     * POST http://localhost:8080/hypercloud/courses/7/messages
     */
    @PostMapping("/{courseId}/messages")
    public ResponseEntity<ChatMessageDTO> sendMessageViaRest(
            @PathVariable Long courseId,
            @RequestBody ChatMessageDTO dto) {

        dto.setCourseId(courseId);   // sécurité
        MessageTransport saved = messageService.saveMessage(dto);

        ChatMessageDTO response = ChatMessageDTO.builder()
                .courseId(saved.getCourse().getIdCourse())
                .senderId(saved.getSender().getId())
                .senderRole(saved.getSender().getRole().name())
                .contenu(saved.getContenu())
                .dateEnvoi(saved.getDateEnvoi().toString())
                .build();

        return ResponseEntity.ok(response);
    }
}