package tn.hypercloud.dto.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventActivityResponse {
    private Integer id;
    private String title;
    private String description;
    private BigDecimal price;
    private int capacity;
    private int availableSeats;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String city;
    private String address;
    private Double latitude;
    private Double longitude;
    private String imageUrl;
    private String type;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime moderatedAt;
    private String moderatedByEmail;
    private String moderationReason;
    private String cancellationReason;
    private Integer categoryId;
    private String categoryName;
    private Long organizerId;
    private String organizerName;
}