package tn.hypercloud.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;
import tn.hypercloud.payload.response.TwoFactorSetupResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

@Service
public class TwoFactorService {

    private static final String ISSUER = "Looking";
    private static final int SECRET_LENGTH_BYTES = 20;
    private static final int OTP_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSecret() {
        byte[] buffer = new byte[SECRET_LENGTH_BYTES];
        secureRandom.nextBytes(buffer);
        return toBase32(buffer);
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null) {
            return false;
        }

        String normalizedCode = code.replaceAll("\\s+", "");
        if (!normalizedCode.matches("\\d{" + OTP_DIGITS + "}")) {
            return false;
        }

        long timeWindow = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        for (long offset = -1; offset <= 1; offset++) {
            if (normalizedCode.equals(generateCode(secret, timeWindow + offset))) {
                return true;
            }
        }

        return false;
    }

    public TwoFactorSetupResponse buildSetupResponse(String accountName, String secret, boolean enabled) {
        String otpauthUri = buildOtpAuthUri(accountName, secret);

        TwoFactorSetupResponse response = new TwoFactorSetupResponse();
        response.setIssuer(ISSUER);
        response.setAccountName(accountName);
        response.setSecret(secret);
        response.setOtpauthUri(otpauthUri);
        response.setQrCodeDataUrl(buildQrCodeDataUrl(otpauthUri));
        response.setEnabled(enabled);
        return response;
    }

    private String buildOtpAuthUri(String accountName, String secret) {
        String issuer = urlEncode(ISSUER);
        String label = urlEncode(ISSUER + ":" + accountName);
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + issuer
                + "&algorithm=SHA1&digits=" + OTP_DIGITS
                + "&period=" + TIME_STEP_SECONDS;
    }

    private String generateCode(String secret, long timeWindow) {
        try {
            byte[] key = fromBase32(secret);
            byte[] data = ByteBuffer.allocate(8).putLong(timeWindow).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, OTP_DIGITS);
            return String.format(Locale.ROOT, "%0" + OTP_DIGITS + "d", otp);
        } catch (Exception ex) {
            throw new RuntimeException("Impossible de verifier le code 2FA");
        }
    }

    private String buildQrCodeDataUrl(String content) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 280, 280, hints);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception ex) {
            throw new RuntimeException("Impossible de generer le QR code 2FA", ex);
        }
    }

    private String toBase32(byte[] data) {
        final char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
        StringBuilder result = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = data[0] >= 0 ? data[0] : data[0] + 256;
        int next = 1;
        int bitsLeft = 8;

        while (bitsLeft > 0 || next < data.length) {
            if (bitsLeft < 5) {
                if (next < data.length) {
                    buffer <<= 8;
                    buffer |= data[next++] & 0xFF;
                    bitsLeft += 8;
                } else {
                    int pad = 5 - bitsLeft;
                    buffer <<= pad;
                    bitsLeft += pad;
                }
            }

            int index = 0x1F & (buffer >> (bitsLeft - 5));
            bitsLeft -= 5;
            result.append(alphabet[index]);
        }

        while (result.length() % 8 != 0) {
            result.append('=');
        }

        return result.toString();
    }

    private byte[] fromBase32(String base32) {
        String normalized = base32.replace("=", "").replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int buffer = 0;
        int bitsLeft = 0;

        for (char character : normalized.toCharArray()) {
            int value = decodeBase32Char(character);
            buffer <<= 5;
            buffer |= value & 0x1F;
            bitsLeft += 5;

            if (bitsLeft >= 8) {
                outputStream.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }

        return outputStream.toByteArray();
    }

    private int decodeBase32Char(char character) {
        if (character >= 'A' && character <= 'Z') {
            return character - 'A';
        }

        if (character >= '2' && character <= '7') {
            return character - '2' + 26;
        }

        throw new IllegalArgumentException("Caractere Base32 invalide: " + character);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}