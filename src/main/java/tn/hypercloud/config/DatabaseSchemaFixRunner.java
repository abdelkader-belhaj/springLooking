package tn.hypercloud.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaFixRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE event_activity MODIFY COLUMN image_url LONGTEXT NULL");
            log.info("Schema fix applied: event_activity.image_url -> LONGTEXT");
        } catch (Exception ex) {
            // Non-blocking: app should still start even if schema change rights are missing.
            log.warn("Schema fix skipped for event_activity.image_url: {}", ex.getMessage());
        }
    }
}
