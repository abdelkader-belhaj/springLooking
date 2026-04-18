package tn.hypercloud.service.transport;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.transport.ReservationLocation;
import tn.hypercloud.entity.transport.enums.DepositStatus;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class PdfServiceImpl implements PdfService {

    private static final String UPLOAD_DIR = "uploads/contracts/";
    private static final String INVOICE_UPLOAD_DIR = "uploads/invoices/";

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
                document.add(new Paragraph("Réservation #" + res.getIdReservation()));
                document.add(new Paragraph("Date d'émission: " + java.time.LocalDate.now()));
            document.add(new Paragraph(" "));

                String clientName = res.getClient() != null && res.getClient().getUsername() != null
                    ? res.getClient().getUsername()
                    : "Client";

                String vehicleBrand = res.getVehiculeAgence() != null && res.getVehiculeAgence().getMarque() != null
                    ? res.getVehiculeAgence().getMarque()
                    : "-";
                String vehicleModel = res.getVehiculeAgence() != null && res.getVehiculeAgence().getModele() != null
                    ? res.getVehiculeAgence().getModele()
                    : "-";
                String vehiclePlate = res.getVehiculeAgence() != null && res.getVehiculeAgence().getNumeroPlaque() != null
                    ? res.getVehiculeAgence().getNumeroPlaque()
                    : "-";
                String vehicleType = res.getVehiculeAgence() != null && res.getVehiculeAgence().getTypeVehicule() != null
                    ? String.valueOf(res.getVehiculeAgence().getTypeVehicule())
                    : "-";
                String vehicleCapacity = res.getVehiculeAgence() != null && res.getVehiculeAgence().getCapacitePassagers() != null
                    ? String.valueOf(res.getVehiculeAgence().getCapacitePassagers())
                    : "-";
                BigDecimal vehiclePrice = res.getVehiculeAgence() != null && res.getVehiculeAgence().getPrixVehicule() != null
                    ? res.getVehiculeAgence().getPrixVehicule()
                    : BigDecimal.ZERO;
                BigDecimal dailyRate = res.getVehiculeAgence() != null && res.getVehiculeAgence().getPrixJour() != null
                    ? res.getVehiculeAgence().getPrixJour()
                    : BigDecimal.ZERO;

                document.add(new Paragraph("Client: " + clientName));
                document.add(new Paragraph("Véhicule: " + vehicleBrand + " " + vehicleModel));
                document.add(new Paragraph("Immatriculation: " + vehiclePlate));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String period = (res.getDateDebut() != null ? res.getDateDebut().format(formatter) : "-")
                    + " - "
                    + (res.getDateFin() != null ? res.getDateFin().format(formatter) : "-");
                document.add(new Paragraph("Période: " + period));
                document.add(new Paragraph(" "));

                Paragraph vehicleDetailsTitle = new Paragraph("Détails véhicule", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
                document.add(vehicleDetailsTitle);

                PdfPTable vehicleTable = new PdfPTable(2);
                vehicleTable.setWidthPercentage(100);
                vehicleTable.setSpacingBefore(6f);
                vehicleTable.setWidths(new float[]{3f, 2f});
                addRow(vehicleTable, "Type", vehicleType);
                addRow(vehicleTable, "Capacité passagers", vehicleCapacity);
                addRow(vehicleTable, "Prix véhicule", money(vehiclePrice));
                addRow(vehicleTable, "Tarif journalier", money(dailyRate));
                document.add(vehicleTable);
                document.add(new Paragraph(" "));

                Paragraph financeTitle = new Paragraph("Récapitulatif financier", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
                document.add(financeTitle);

                BigDecimal total = safeMoney(res.getPrixTotal());
                BigDecimal advance = safeMoney(res.getAdvanceAmount());
                BigDecimal deposit = safeMoney(res.getDepositAmount());
                BigDecimal upfront = advance.add(deposit).setScale(2, RoundingMode.HALF_UP);
                BigDecimal finalPayment = total.subtract(advance).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

                PdfPTable financeTable = new PdfPTable(2);
                financeTable.setWidthPercentage(100);
                financeTable.setSpacingBefore(6f);
                financeTable.setWidths(new float[]{3f, 2f});
                addRow(financeTable, "Prix total location", money(total));
                addRow(financeTable, "Avance (30%)", money(advance));
                addRow(financeTable, "Caution", money(deposit));
                addRow(financeTable, "Paiement initial (avance + caution)", money(upfront));
                addRow(financeTable, "Paiement final prévu", money(finalPayment));
                addRow(financeTable, "Phase paiement", res.getPaymentPhase() != null ? res.getPaymentPhase() : "-");
                document.add(financeTable);
                document.add(new Paragraph(" "));

                Paragraph cancellationTitle = new Paragraph("Règlement d'annulation", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
                document.add(cancellationTitle);
                document.add(new Paragraph("1) Avant paiement initial: annulation simple, aucun remboursement."));
                document.add(new Paragraph("2) Après paiement initial et avant confirmation agence: remboursement total (avance + caution)."));
                document.add(new Paragraph("3) Après confirmation agence: remboursement de la caution seulement, avance non remboursable."));
                document.add(new Paragraph("4) Refus agence (permis non approuvé): remboursement total client."));
                document.add(new Paragraph(" "));

                document.add(new Paragraph("Conditions Générales de Vente", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            document.add(new Paragraph("• Le locataire s’engage à restituer le véhicule en bon état."));
            document.add(new Paragraph("• Toute dégradation sera à la charge du locataire."));
                document.add(new Paragraph("• Une facture PDF est disponible pour chaque règlement financier et pour toute annulation avec remboursement."));

            document.close();
            return filePath;

        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF", e);
        }
    }

    @Override
    public String generateFinalInvoicePdf(ReservationLocation res) {
        try {
            Files.createDirectories(Path.of(INVOICE_UPLOAD_DIR));

            String phase = String.valueOf(res.getPaymentPhase() == null ? "" : res.getPaymentPhase()).toUpperCase();
            boolean cancellationWithRefund = isCancellationWithRefund(phase);

            String fileName = cancellationWithRefund
                    ? "facture_annulation_remboursement_" + res.getIdReservation() + ".pdf"
                    : "facture_location_" + res.getIdReservation() + ".pdf";
            String filePath = INVOICE_UPLOAD_DIR + fileName;

            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

                String titleText = cancellationWithRefund
                    ? "FACTURE D'ANNULATION / REMBOURSEMENT"
                    : "FACTURE FINALE LOCATION";
                Paragraph title = new Paragraph(titleText, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("Réservation #" + res.getIdReservation()));
            document.add(new Paragraph("Date d'émission: " + java.time.LocalDate.now()));
            if (cancellationWithRefund) {
                document.add(new Paragraph("Motif: " + resolveCancellationReasonLabel(phase)));
            }
            document.add(new Paragraph(" "));

            String clientName = res.getClient() != null && res.getClient().getUsername() != null
                    ? res.getClient().getUsername()
                    : "Client";

            String vehicleLabel = res.getVehiculeAgence() != null
                    ? (String.valueOf(res.getVehiculeAgence().getMarque()) + " " + String.valueOf(res.getVehiculeAgence().getModele())).trim()
                    : "Véhicule";

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String period = (res.getDateDebut() != null ? res.getDateDebut().format(formatter) : "-")
                    + " - "
                    + (res.getDateFin() != null ? res.getDateFin().format(formatter) : "-");

            document.add(new Paragraph("Client: " + clientName));
            document.add(new Paragraph("Véhicule: " + vehicleLabel));
            document.add(new Paragraph("Période: " + period));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Détails financiers", sectionFont));

            BigDecimal total = res.getPrixTotal() != null ? res.getPrixTotal() : BigDecimal.ZERO;
            BigDecimal advance = res.getAdvanceAmount() != null ? res.getAdvanceAmount() : BigDecimal.ZERO;
            BigDecimal finalPayment = total.subtract(advance).max(BigDecimal.ZERO);
            BigDecimal deposit = res.getDepositAmount() != null ? res.getDepositAmount() : BigDecimal.ZERO;
            BigDecimal refundedAdvance = BigDecimal.ZERO;
            BigDecimal refundedDeposit = BigDecimal.ZERO;
            BigDecimal lostAdvance = BigDecimal.ZERO;
            String refundNote = "Aucun remboursement appliqué.";

            if ("CANCELLED_REFUNDED_TOTAL".equals(phase) || "CANCELLED_BY_AGENCY_REFUND_TOTAL".equals(phase)) {
                refundedAdvance = advance;
                refundedDeposit = deposit;
                refundNote = "Annulation avec remboursement total (avance + caution).";
            } else if ("CANCELLED_DEPOSIT_REFUNDED_ADVANCE_LOST".equals(phase)) {
                refundedAdvance = BigDecimal.ZERO;
                refundedDeposit = deposit;
                lostAdvance = advance;
                refundNote = "Annulation après confirmation: caution remboursée, avance perdue.";
            }

            BigDecimal totalRefunded = refundedAdvance.add(refundedDeposit).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalLost = lostAdvance.setScale(2, RoundingMode.HALF_UP);
            BigDecimal netLocationPaid = total.subtract(refundedAdvance).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(8f);
            table.setWidths(new float[]{3f, 2f});

            addRow(table, "Prix total location", money(total));
            addRow(table, "Avance payée", money(advance));
            addRow(table, "Paiement final", money(finalPayment));
            addRow(table, "Total payé location", money(total));
            addRow(table, "Caution", money(deposit));
            addRow(table, "Statut caution", mapDepositStatusLabel(res.getDepositStatus()));
            addRow(table, "Caution remboursée", res.getDepositStatus() == DepositStatus.RELEASED ? "Oui" : "Non");
            addRow(table, "Phase paiement", res.getPaymentPhase() != null ? res.getPaymentPhase() : "-");
            addRow(table, "Avance remboursée", money(refundedAdvance));
            addRow(table, "Caution remboursée (montant)", money(refundedDeposit));
            addRow(table, "Total remboursé", money(totalRefunded));
            addRow(table, "Montant perdu (non remboursé)", money(totalLost));
            addRow(table, "Total net location après remboursement", money(netLocationPaid));
            addRow(table, "Note remboursement", refundNote);

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Cette facture résume les mouvements financiers de la location (avance, solde final, caution et remboursement)."));

            document.close();
            return filePath;
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération facture finale PDF", e);
        }
    }

    private void addRow(PdfPTable table, String label, String value) {
        PdfPCell left = new PdfPCell(new Phrase(label));
        left.setPadding(8f);
        left.setBorderColor(new java.awt.Color(220, 226, 234));

        PdfPCell right = new PdfPCell(new Phrase(value));
        right.setPadding(8f);
        right.setBorderColor(new java.awt.Color(220, 226, 234));

        table.addCell(left);
        table.addCell(right);
    }

    private String mapDepositStatusLabel(DepositStatus status) {
        if (status == null) {
            return "En attente";
        }

        switch (status) {
            case HELD:
                return "Bloquée";
            case RELEASED:
                return "Remboursée";
            case FORFEITED:
                return "Retenue";
            case PENDING:
            default:
                return "En attente";
        }
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String money(BigDecimal value) {
        return safeMoney(value) + " TND";
    }

    private boolean isCancellationWithRefund(String phase) {
        return "CANCELLED_REFUNDED_TOTAL".equals(phase)
                || "CANCELLED_BY_AGENCY_REFUND_TOTAL".equals(phase)
                || "CANCELLED_DEPOSIT_REFUNDED_ADVANCE_LOST".equals(phase);
    }

    private String resolveCancellationReasonLabel(String phase) {
        if ("CANCELLED_BY_AGENCY_REFUND_TOTAL".equals(phase)) {
            return "Annulation par l'agence (permis refusé) avec remboursement total";
        }
        if ("CANCELLED_DEPOSIT_REFUNDED_ADVANCE_LOST".equals(phase)) {
            return "Annulation après confirmation agence: caution remboursée, avance non remboursée";
        }
        if ("CANCELLED_REFUNDED_TOTAL".equals(phase)) {
            return "Annulation avant confirmation agence (client/agence) avec remboursement total";
        }
        return "Annulation avec ajustement financier";
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