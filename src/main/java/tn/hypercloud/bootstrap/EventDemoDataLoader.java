package tn.hypercloud.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tn.hypercloud.entity.event.EventActivity;
import tn.hypercloud.entity.event.EventCategory;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.event.EventActivityRepository;
import tn.hypercloud.repository.event.EventCategoryRepository;
import tn.hypercloud.repository.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Insère des événements / activités de démo (images + organisateur) si la table est vide.
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class EventDemoDataLoader implements CommandLineRunner {

    private final EventActivityRepository eventActivityRepository;
    private final EventCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (eventActivityRepository.count() > 0) {
            return;
        }

        User org = userRepository.findByRole(Role.ORGANISATEUR).stream()
                .findFirst()
                .orElseGet(this::createDemoOrganizer);

        EventCategory catCulture = category("Festivals & Culture",
                "Concerts et festivals", EventCategory.CategoryType.EVENT);
        EventCategory catNature = category("Nature & Aventure",
                "Randonnées et outdoor", EventCategory.CategoryType.ACTIVITY);

        EventCategory catMer = category("Mer & détente",
                "Sports nautiques", EventCategory.CategoryType.ACTIVITY);
        EventCategory catArtisan = category("Artisanat & traditions",
                "Ateliers locaux", EventCategory.CategoryType.ACTIVITY);

        List<DemoEvent> demos = List.of(
                new DemoEvent(
                        "Festival International de Carthage — soirée d’ouverture",
                        "Une soirée exceptionnelle au site antique de Carthage avec artistes tunisiens et invités internationaux.",
                        "https://tse3.mm.bing.net/th/id/OIP.qtOgWIbqTHZWEknSBDfp0AHaD8?pid=Api&P=0&h=180",
                        EventActivity.EventType.EVENT,
                        catCulture,
                        "Amphithéâtre de Carthage, Tunis",
                        "Carthage",
                        45,
                        120,
                        BigDecimal.valueOf(85),
                        days(30),
                        days(30).plusHours(4)),
                new DemoEvent(
                        "Randonnée guidée — Djebel Zaghouan & sources",
                        "Journée nature avec guide certifié, pique-nique local et panorama sur la dorsale tunisienne.",
                        "https://images.unsplash.com/photo-1551632811-561732d1e306?w=1200&q=80",
                        EventActivity.EventType.ACTIVITY,
                        catNature,
                        "Départ parking Office du tourisme, Zaghouan",
                        "Zaghouan",
                        25,
                        25,
                        BigDecimal.valueOf(55),
                        days(14),
                        days(14).plusHours(8)),

                new DemoEvent(
                        "Nuits du Jazz à Sidi Bou Saïd",
                        "Concerts en plein air face à la Méditerranée, sélection jazz fusion et world music.",
                        "https://tunisiagotravel.com/blog/wp-content/uploads/2021/08/Sidi-Bousaid-1-3-1350x900-1.jpg",
                        EventActivity.EventType.EVENT,
                        catCulture,
                        "Palais Ennejma Ezzahra, Sidi Bou Saïd",
                        "Sidi Bou Saïd",
                        200,
                        200,
                        BigDecimal.valueOf(65),
                        days(21),
                        days(21).plusHours(5)),
                new DemoEvent(
                        "Plongée sous-marine — réserve de Tabarka",
                        "Baptême ou perfectionnement avec moniteurs PADI, matériel fourni.",
                        "https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=1200&q=80",
                        EventActivity.EventType.ACTIVITY,
                        catMer,
                        "Club nautique Tabarka",
                        "Tabarka",
                        12,
                        12,
                        BigDecimal.valueOf(120),
                        days(10),
                        days(10).plusHours(6)),
                new DemoEvent(
                        "Quad & coucher de soleil — dunes de Douz",
                        "Demi-journée en buggy/quad avec pause thé à la menthe dans le désert.",
                        "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200&q=80",
                        EventActivity.EventType.ACTIVITY,
                        catNature,
                        "Douz, porte du Sahara",
                        "Douz",
                        20,
                        20,
                        BigDecimal.valueOf(95),
                        days(18),
                        days(18).plusHours(5)),
                new DemoEvent(
                        "Salon du livre de Tunis — pass week-end",
                        "Rencontres d’auteurs, dédicaces et espace jeunesse.",
                        "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=1200&q=80",
                        EventActivity.EventType.EVENT,
                        catCulture,
                        "Palais des congrès, Le Lac",
                        "Tunis",
                        500,
                        500,
                        BigDecimal.valueOf(15),
                        days(7),
                        days(7).plusHours(3)),
                new DemoEvent(
                        "Atelier poterie de Sejnane — journée immersive",
                        "Initiation aux techniques UNESCO avec artisanes locales.",
                        "https://images.unsplash.com/photo-1565193566173-7a0ee3dbe261?w=1200&q=80",
                        EventActivity.EventType.ACTIVITY,
                        catArtisan,
                        "Village de Sejnane, Bizerte",
                        "Sejnane",
                        15,
                        15,
                        BigDecimal.valueOf(70),
                        days(25),
                        days(25).plusHours(9)),
                new DemoEvent(
                        "Marathon international de Tunis",
                        "Parcours 42 km et 10 km, village partenaires et médailles finisher.",
                        "https://images.unsplash.com/photo-1452626038306-9aae5e071dd3?w=1200&q=80",
                        EventActivity.EventType.EVENT,
                        catCulture,
                        "Avenue Habib Bourguiba, Tunis",
                        "Tunis",
                        3000,
                        3000,
                        BigDecimal.valueOf(35),
                        days(45),
                        days(45).plusDays(1)),
                new DemoEvent(
                        "Yoga sunrise — plage de Gammarth",
                        "Séance douce au lever du soleil, tapis fournis, jus détox offert.",
                        "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=1200&q=80",
                        EventActivity.EventType.ACTIVITY,
                        catMer,
                        "Plage La Brise, Gammarth",
                        "Gammarth",
                        30,
                        30,
                        BigDecimal.valueOf(40),
                        nextSaturday(),
                        nextSaturday().plusHours(3))   // ✅ parenthèse ici
        );

        for (DemoEvent d : demos) {
            EventActivity e = EventActivity.builder()
                    .title(d.title)
                    .description(d.description)
                    .imageUrl(d.imageUrl)
                    .type(d.eventType)
                    .status(EventActivity.EventStatus.PUBLISHED)
                    .category(d.category)
                    .organizer(org)
                    .price(d.price)
                    .capacity(d.capacity)
                    .availableSeats(d.available)
                    .startDate(d.start)
                    .endDate(d.end)
                    .city(d.city)
                    .address(d.address)
                    .latitude(null)
                    .longitude(null)
                    .build();
            eventActivityRepository.save(e);
        }

        log.info("EventDemoDataLoader: {} événements / activités de démo créés.", demos.size());
    }

    private User createDemoOrganizer() {
        User u = User.builder()
                .username("Organisateur Looking")
                .email("demo.organisateur@looking.tn")
                .password(passwordEncoder.encode("DemoLooking2026!"))
                .role(Role.ORGANISATEUR)
                .enabled(true)
                .phone("+216 12 345 678")
                .build();
        return userRepository.save(u);
    }

    private EventCategory category(String name, String desc, EventCategory.CategoryType type) {
        if (categoryRepository.existsByName(name)) {
            return categoryRepository.findAll().stream()
                    .filter(c -> name.equals(c.getName()))
                    .findFirst()
                    .orElseThrow();
        }
        EventCategory c = EventCategory.builder()
                .name(name)
                .description(desc)
                .type(type)
                .build();
        return categoryRepository.save(c);
    }

    private static LocalDateTime days(int plusDays) {
        return LocalDateTime.now().plusDays(plusDays).withHour(18).withMinute(0).withSecond(0).withNano(0);
    }
    private static LocalDateTime nextSaturday() {
        LocalDateTime now = LocalDateTime.now();
        int days = (6 - now.getDayOfWeek().getValue() + 7) % 7;
        if (days == 0) days = 7;
        return now.plusDays(days).withHour(20).withMinute(0).withSecond(0).withNano(0);
    }


    private record DemoEvent(
            String title,
            String description,
            String imageUrl,
            EventActivity.EventType eventType,
            EventCategory category,
            String address,
            String city,
            int capacity,
            int available,
            BigDecimal price,
            LocalDateTime start,
            LocalDateTime end
    ) {}
}
