package tn.hypercloud.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
public class LenientLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.isBlank()) return null;

        try {
            return LocalDateTime.parse(value, DATE_TIME);
        } catch (Exception ignored) {
        }

        try {
            return LocalDate.parse(value, DATE).atStartOfDay();
        } catch (Exception ignored) {
        }

        throw new IOException("Format date invalide: " + value + " (attendu yyyy-MM-dd ou yyyy-MM-ddTHH:mm:ss)");
    }
}