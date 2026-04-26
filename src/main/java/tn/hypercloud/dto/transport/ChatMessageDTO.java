package tn.hypercloud.dto.transport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private Long id;                    // ID du message (utile pour read receipt)
    private Long courseId;
    private Long senderId;
    private String senderRole;          // "CLIENT" ou "CHAUFFEUR"
    private String contenu;
    private String dateEnvoi;
    private boolean delivered;
    private boolean read;
    private LocalDateTime dateLecture;
    private boolean isRead;
}