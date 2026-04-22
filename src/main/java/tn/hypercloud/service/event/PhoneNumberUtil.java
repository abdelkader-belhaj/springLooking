package tn.hypercloud.service.event;

public final class PhoneNumberUtil {

    private PhoneNumberUtil() {
    }

    public static String normalizeForTwilio(String rawPhone) {
        if (rawPhone == null) {
            return null;
        }

        String cleaned = rawPhone.trim().replaceAll("[\\s\\-()]", "");
        if (cleaned.isEmpty()) {
            return null;
        }

        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2);
        }

        if (cleaned.startsWith("+")) {
            String digits = cleaned.substring(1);
            if (digits.matches("\\d{8,15}")) {
                return "+" + digits;
            }
            return null;
        }

        if (cleaned.matches("\\d{8}")) {
            return "+216" + cleaned;
        }

        if (cleaned.matches("216\\d{8}")) {
            return "+" + cleaned;
        }

        if (cleaned.matches("\\d{10,15}")) {
            return "+" + cleaned;
        }

        return null;
    }
}