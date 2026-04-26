package tn.hypercloud.dto.transport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverNotificationDTO {
    private String type;        // "NEW_COURSE", "COURSE_ACCEPTED", "CANCELLED", "ARRIVED", etc.
    private String message;
    private Long courseId;
    private String titre;       // Pour le toast
    private Object data;        // Données supplémentaires (ex: prix, client, etc.)
}