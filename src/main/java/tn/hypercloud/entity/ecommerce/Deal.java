package tn.hypercloud.entity.ecommerce;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "deals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Budget budget;

    @Column(length = 500)
    private String image;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Environment environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Duration duration;

    @Column(name = "favorites_count", nullable = false)
    private int favoritesCount = 0;

    public enum Region       { north, south, center, east_coast, sahara }
    public enum Budget       { low, medium, high }
    public enum ActivityType { solo, duo, group, flexible }
    public enum Environment  { indoor, outdoor, both }
    public enum Category     { adventure, culture_history, food, relaxation,
                                water_sports, crafts, nature_hiking, heritage, photography }
    public enum Duration     { one_hour, two_hours, three_hours, half_day,
                                full_day, two_days, three_days_plus, weekend }
}
