package tn.hypercloud.dto.event;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventCategoryResponse {
    private Integer id;
    private String name;
    private String description;
    private String type;
}