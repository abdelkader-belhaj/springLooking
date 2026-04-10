package tn.hypercloud.service.transport;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.transport.ReservationLocation;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class PdfServiceImpl implements PdfService {

    private static final String UPLOAD_DIR = "uploads/contracts/";

    @Override
    public String generateContractPdf(ReservationLocation res) {
        try {
            Files.createDirectories(Path.of(UPLOAD_DIR));

            String fileName = "contrat_" + res.getIdReservation() + ".pdf";
            String filePath = UPLOAD_DIR + fileName;

            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // En-tête
            Paragraph title = new Paragraph("CONTRAT DE LOCATION - TunisiaTour",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // Informations
            document.add(new Paragraph("Client : " + res.getClient().getUsername()));
            document.add(new Paragraph("Véhicule : " + res.getVehiculeAgence().getMarque() + " "
                    + res.getVehiculeAgence().getModele()));
            document.add(new Paragraph("Immatriculation : " + res.getVehiculeAgence().getNumeroPlaque()));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            document.add(new Paragraph("Période : " + res.getDateDebut().format(formatter)
                    + " - " + res.getDateFin().format(formatter)));

            document.add(new Paragraph("Prix total : " + res.getPrixTotal() + " TND"));
            document.add(new Paragraph("Caution : " + res.getDepositAmount() + " TND"));

            document.add(new Paragraph("\nConditions Générales de Vente"));
            document.add(new Paragraph("• Le locataire s’engage à restituer le véhicule en bon état."));
            document.add(new Paragraph("• Toute dégradation sera à la charge du locataire."));

            document.close();
            return filePath;

        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF", e);
        }
    }
    @Override
    public String addSignatureToPdf(String pdfPath, String base64Signature, String signedBy) {
        try {
            String payload = base64Signature;
            if (payload.startsWith("data:image/")) {
                String[] parts = payload.split(",", 2);
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Signature base64 invalide");
                }
                payload = parts[1];
            }

            byte[] signatureBytes = Base64.getDecoder().decode(payload);

            PdfReader reader = new PdfReader(pdfPath);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfStamper stamper = new PdfStamper(reader, baos);

            PdfContentByte over = stamper.getOverContent(reader.getNumberOfPages());

            Image signature = Image.getInstance(signatureBytes);
            signature.scaleToFit(250, 100);
            signature.setAbsolutePosition(80, 120);
            over.addImage(signature);

            over.beginText();
            over.setFontAndSize(BaseFont.createFont(), 12);
            over.setTextMatrix(80, 100);
            over.showText("Signé le " + java.time.LocalDate.now() + " par : " + signedBy);
            over.endText();

            stamper.close();
            reader.close();

            try (FileOutputStream fos = new FileOutputStream(pdfPath)) {
                baos.writeTo(fos);
            }

            return pdfPath;
        } catch (Exception e) {
            throw new RuntimeException("Erreur ajout signature PDF", e);
        }
    }
}