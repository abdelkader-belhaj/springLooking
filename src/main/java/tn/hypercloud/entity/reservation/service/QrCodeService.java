package tn.hypercloud.entity.reservation.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.reservation.QrCodeVol;
import tn.hypercloud.entity.reservation.ReservationVol;
import tn.hypercloud.repository.reservation.QrCodeVolRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QrCodeService {

    private final QrCodeVolRepository qrCodeRepo;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    // ============================================================
    //  GÉNÉRER ET SAUVEGARDER LE QR CODE D'UNE RÉSERVATION PAYÉE
    // ============================================================
    public QrCodeVol genererEtSauvegarder(ReservationVol res) {

        // Évite de régénérer si déjà existant
        return qrCodeRepo.findByReservationId(res.getId())
                .orElseGet(() -> {
                    String contenu = construireContenu(res);
                    String imageBase64 = genererImageBase64(contenu);

                    QrCodeVol qr = QrCodeVol.builder()
                            .reservation(res)
                            .contenu(contenu)
                            .imageBase64(imageBase64)
                            .build();

                    return qrCodeRepo.save(qr);
                });
    }

    // ============================================================
    //  RÉCUPÉRER LE QR CODE D'UNE RÉSERVATION
    // ============================================================
    public QrCodeVol getByReservationId(Integer reservationId) {
        return qrCodeRepo.findByReservationId(reservationId)
                .orElseThrow(() -> new RuntimeException(
                        "QR code introuvable pour la réservation #" + reservationId));
    }

    // ============================================================
    //  CONSTRUIRE LE CONTENU TEXTE DU QR CODE
    // ============================================================
    private String construireContenu(ReservationVol res) {
        // Le QR code pointe vers une page Angular avec la référence
        return "http://192.168.1.20:4200/billet/" + res.getReference();
    }

    // ============================================================
    //  GÉNÉRER L'IMAGE QR EN BASE64 AVEC ZXING
    // ============================================================
    private String genererImageBase64(String contenu) {
        try {
            QRCodeWriter writer = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // haute correction
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix bitMatrix = writer.encode(contenu, BarcodeFormat.QR_CODE, 300, 300, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] pngBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(pngBytes);

        } catch (WriterException | IOException e) {
            throw new RuntimeException("Erreur génération QR code : " + e.getMessage());
        }
    }
}