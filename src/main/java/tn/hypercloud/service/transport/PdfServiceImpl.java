package tn.hypercloud.service.transport;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.transport.ReservationLocation;
import tn.hypercloud.entity.transport.enums.DepositStatus;
import tn.hypercloud.entity.transport.enums.ReservationStatus;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.awt.Color;
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

            Color primary = new Color(0, 57, 116);
            Color accent = new Color(217, 119, 6);
            Color border = new Color(226, 232, 240);
            Color surface = new Color(248, 251, 255);
            Color muted = new Color(100, 116, 139);

            Font eyebrowFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Font.NORMAL, accent);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20f, Font.NORMAL, primary);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, Font.NORMAL, primary);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10f, Font.NORMAL, new Color(51, 65, 85));
            Font strongFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, Font.NORMAL, new Color(15, 23, 42));
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8f, Font.NORMAL, muted);

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

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String period = (res.getDateDebut() != null ? res.getDateDebut().format(formatter) : "-")
                + " - "
                + (res.getDateFin() != null ? res.getDateFin().format(formatter) : "-");

            PdfPTable header = new PdfPTable(new float[] { 3.1f, 1.9f });
            header.setWidthPercentage(100);
            header.setSpacingAfter(10f);

            PdfPCell leftHeader = new PdfPCell();
            leftHeader.setBorder(Rectangle.BOTTOM);
            leftHeader.setBorderColor(border);
            leftHeader.setBorderWidth(1.5f);
            leftHeader.setPadding(8f);
            leftHeader.addElement(new Paragraph("SERVEURHUB MOBILITY", eyebrowFont));
            leftHeader.addElement(new Paragraph("CONTRAT DE LOCATION", titleFont));
            leftHeader.addElement(new Paragraph("Contrat généré automatiquement pour la réservation", FontFactory.getFont(FontFactory.HELVETICA, 10f, Font.NORMAL, muted)));
            leftHeader.addElement(new Paragraph("Réservation #" + res.getIdReservation() + "  |  Émis le " + java.time.LocalDate.now(), bodyFont));

            PdfPCell rightHeader = new PdfPCell();
            rightHeader.setBorder(Rectangle.NO_BORDER);
            rightHeader.setPadding(0f);
            rightHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);

            PdfPCell statusBadge = new PdfPCell(new Phrase("CONTRAT ACTIF", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, Font.NORMAL, Color.WHITE)));
            statusBadge.setHorizontalAlignment(Element.ALIGN_CENTER);
            statusBadge.setPadding(8f);
            statusBadge.setBorder(Rectangle.NO_BORDER);
            statusBadge.setBackgroundColor(primary);
            PdfPTable statusTable = new PdfPTable(1);
            statusTable.setWidthPercentage(100);
            statusTable.addCell(statusBadge);

            PdfPTable meta = new PdfPTable(1);
            meta.setWidthPercentage(100);
            meta.setSpacingBefore(8f);
            meta.addCell(metaCell("Agence", safeText(res.getAgenceLocation() != null ? res.getAgenceLocation().getNomAgence() : "Agence de location"), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Font.NORMAL, muted), bodyFont, border));
            meta.addCell(metaCell("Client", safeText(clientName), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Font.NORMAL, muted), bodyFont, border));
            meta.addCell(metaCell("Véhicule", safeText((vehicleBrand + " " + vehicleModel).trim()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Font.NORMAL, muted), bodyFont, border));
            meta.addCell(metaCell("Période", safeText(period), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Font.NORMAL, muted), bodyFont, border));

            rightHeader.addElement(statusTable);
            rightHeader.addElement(meta);

            header.addCell(leftHeader);
            header.addCell(rightHeader);
            document.add(header);

            PdfPTable hero = new PdfPTable(new float[] { 1.4f, 1.4f, 1.2f });
            hero.setWidthPercentage(100);
            hero.setSpacingAfter(12f);
            hero.addCell(summaryCard("Client", clientName, "Réservation #" + res.getIdReservation(), surface, border, primary, bodyFont, strongFont));
            hero.addCell(summaryCard("Véhicule", safeText((vehicleBrand + " " + vehicleModel).trim()), "Immatriculation: " + safeText(vehiclePlate), surface, border, primary, bodyFont, strongFont));
            hero.addCell(summaryCard("Période", period, "Phase: " + safeText(res.getPaymentPhase()), surface, border, primary, bodyFont, strongFont));
            document.add(hero);

            document.add(new Paragraph("Détails véhicule", sectionFont));
            PdfPTable vehicleTable = new PdfPTable(2);
            vehicleTable.setWidthPercentage(100);
            vehicleTable.setSpacingBefore(6f);
            vehicleTable.setSpacingAfter(10f);
            vehicleTable.setWidths(new float[] { 3f, 2f });
            addRow(vehicleTable, "Type", vehicleType);
            addRow(vehicleTable, "Capacité passagers", vehicleCapacity);
            addRow(vehicleTable, "Prix véhicule", money(vehiclePrice));
            addRow(vehicleTable, "Tarif journalier", money(dailyRate));
            document.add(vehicleTable);

            document.add(new Paragraph("Récapitulatif financier", sectionFont));
            BigDecimal total = safeMoney(res.getPrixTotal());
            BigDecimal advance = safeMoney(res.getAdvanceAmount());
            BigDecimal deposit = safeMoney(res.getDepositAmount());
            BigDecimal upfront = advance.add(deposit).setScale(2, RoundingMode.HALF_UP);
            BigDecimal finalPayment = total.subtract(advance).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            PdfPTable financeTable = new PdfPTable(2);
            financeTable.setWidthPercentage(100);
            financeTable.setSpacingBefore(6f);
            financeTable.setSpacingAfter(10f);
            financeTable.setWidths(new float[] { 3f, 2f });
            addRow(financeTable, "Prix total location", money(total));
            addRow(financeTable, "Avance (30%)", money(advance));
            addRow(financeTable, "Caution", money(deposit));
            addRow(financeTable, "Paiement initial (avance + caution)", money(upfront));
            addRow(financeTable, "Paiement final prévu", money(finalPayment));
            addRow(financeTable, "Phase paiement", safeText(res.getPaymentPhase()));
            document.add(financeTable);

            document.add(new Paragraph("Clauses d'annulation", sectionFont));
            document.add(new Paragraph("• Avant paiement initial: annulation simple, aucun remboursement.", bodyFont));
            document.add(new Paragraph("• Après paiement initial et avant confirmation agence: remboursement total (avance + caution).", bodyFont));
            document.add(new Paragraph("• Après confirmation agence: caution remboursée, avance non remboursable.", bodyFont));
            document.add(new Paragraph("• Refus agence (permis non approuvé): remboursement total client.", bodyFont));

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Conditions Générales", sectionFont));
            document.add(new Paragraph("• Le locataire s'engage à restituer le véhicule en bon état.", bodyFont));
            document.add(new Paragraph("• Toute dégradation constatée est à la charge du locataire.", bodyFont));
            document.add(new Paragraph("• Une facture PDF est disponible pour chaque règlement financier et pour toute annulation avec remboursement.", bodyFont));

            document.add(new Paragraph(" "));

            PdfPTable signatureTable = new PdfPTable(2);
            signatureTable.setWidthPercentage(100);
            signatureTable.setSpacingBefore(8f);
            signatureTable.setWidths(new float[] { 1f, 1f });
            signatureTable.addCell(signatureBox("Agence", safeText(res.getAgenceLocation() != null ? res.getAgenceLocation().getNomAgence() : "Agence de location"), safeText(res.getAgenceLocation() != null ? res.getAgenceLocation().getAdresse() : "Adresse non renseignée"), primary, border, muted));
            signatureTable.addCell(signatureBox("Client", safeText(clientName), "Signature électronique", primary, border, muted));
            document.add(signatureTable);

            Paragraph footer = new Paragraph("Document contractuel généré automatiquement. Réservation #" + res.getIdReservation() + " · " + period, footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(8f);
            document.add(footer);

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

            Color primary = new Color(0, 57, 116);
            Color accent = new Color(217, 119, 6);
            Color border = new Color(226, 232, 240);
            Color surface = new Color(248, 251, 255);
            Color muted = new Color(100, 116, 139);

            Font eyebrowFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Font.NORMAL, accent);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20f, Font.NORMAL, primary);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, Font.NORMAL, primary);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Font.NORMAL, muted);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10f, Font.NORMAL, new Color(51, 65, 85));
            Font strongFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, Font.NORMAL, new Color(15, 23, 42));
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8f, Font.NORMAL, muted);

            String titleText = cancellationWithRefund
                ? "FACTURE D'ANNULATION / REMBOURSEMENT"
                : "FACTURE FINALE LOCATION";

            PdfPTable header = new PdfPTable(new float[] { 3.1f, 1.9f });
            header.setWidthPercentage(100);
            header.setSpacingAfter(10f);

            PdfPCell leftHeader = new PdfPCell();
            leftHeader.setBorder(Rectangle.BOTTOM);
            leftHeader.setBorderColor(border);
            leftHeader.setBorderWidth(1.5f);
            leftHeader.setPadding(8f);
            leftHeader.addElement(new Paragraph("SERVEURHUB MOBILITY", eyebrowFont));
            leftHeader.addElement(new Paragraph(titleText, titleFont));
            leftHeader.addElement(new Paragraph("Facture finale de clôture générée automatiquement", FontFactory.getFont(FontFactory.HELVETICA, 10, muted)));
            leftHeader.addElement(new Paragraph("Réservation #" + res.getIdReservation() + "  |  Émise le " + java.time.LocalDate.now(), bodyFont));
            if (cancellationWithRefund) {
                leftHeader.addElement(new Paragraph("Motif: " + resolveCancellationReasonLabel(phase), bodyFont));
            }

            PdfPCell rightHeader = new PdfPCell();
            rightHeader.setBorder(Rectangle.NO_BORDER);
            rightHeader.setPadding(0f);
            rightHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);

            String badgeLabel = cancellationWithRefund
                ? "REMBOURSEMENT"
                : resolveClosingBadgeLabel(res.getDepositStatus(), res.getStatut());
            PdfPCell badge = new PdfPCell(new Phrase(badgeLabel, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, Font.NORMAL, Color.WHITE)));
            badge.setHorizontalAlignment(Element.ALIGN_CENTER);
            badge.setPadding(8f);
            badge.setBorder(Rectangle.NO_BORDER);
            badge.setBackgroundColor(cancellationWithRefund ? accent : primary);

            PdfPTable badgeTable = new PdfPTable(1);
            badgeTable.setWidthPercentage(100);
            badgeTable.addCell(badge);

            PdfPTable meta = new PdfPTable(1);
            meta.setWidthPercentage(100);
            meta.setSpacingBefore(8f);
            meta.addCell(metaCell("Agence", safeText(res.getAgenceLocation() != null ? res.getAgenceLocation().getNomAgence() : "Agence de location"), labelFont, bodyFont, border));
            meta.addCell(metaCell("Véhicule", safeText(res.getVehiculeAgence() != null ? (String.valueOf(res.getVehiculeAgence().getMarque()) + " " + String.valueOf(res.getVehiculeAgence().getModele())).trim() : "Véhicule"), labelFont, bodyFont, border));
            meta.addCell(metaCell("Phase", safeText(res.getPaymentPhase() != null ? res.getPaymentPhase() : "-"), labelFont, bodyFont, border));
            meta.addCell(metaCell("Période", safeText(periodForInvoice(res)), labelFont, bodyFont, border));

            rightHeader.addElement(badgeTable);
            rightHeader.addElement(meta);

            header.addCell(leftHeader);
            header.addCell(rightHeader);
            document.add(header);

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

                PdfPTable hero = new PdfPTable(new float[] { 1.45f, 1.45f, 1.1f });
                hero.setWidthPercentage(100);
                hero.setSpacingAfter(12f);
                hero.addCell(summaryCard("Client", clientName, "Réservation #" + res.getIdReservation(), surface, border, primary, bodyFont, strongFont));
                hero.addCell(summaryCard("Véhicule", vehicleLabel, safeText(res.getVehiculeAgence() != null ? res.getVehiculeAgence().getNumeroPlaque() : "-"), surface, border, primary, bodyFont, strongFont));
                hero.addCell(summaryCard("Période", period, cancellationWithRefund ? "Facture avec remboursement" : "Facture de clôture", surface, border, primary, bodyFont, strongFont));
                document.add(hero);

                document.add(new Paragraph("Détails financiers", sectionFont));

            BigDecimal total = res.getPrixTotal() != null ? res.getPrixTotal() : BigDecimal.ZERO;
            BigDecimal advance = res.getAdvanceAmount() != null ? res.getAdvanceAmount() : BigDecimal.ZERO;
            BigDecimal finalPayment = total.subtract(advance).max(BigDecimal.ZERO);
            BigDecimal deposit = res.getDepositAmount() != null ? res.getDepositAmount() : BigDecimal.ZERO;
            BigDecimal damageAmount = safeMoney(res.getMontantDommages());
            BigDecimal retainedAmount = safeMoney(res.getMontantCautionRetenu());
            BigDecimal restoredAmount = safeMoney(res.getMontantCautionRestitue());
            String damageReason = safeDamageReason(res.getDescriptionDommages());
            BigDecimal refundedAdvance = BigDecimal.ZERO;
            BigDecimal refundedDeposit = BigDecimal.ZERO;
            BigDecimal lostAdvance = BigDecimal.ZERO;
            String refundNote = "Aucun remboursement appliqué.";

            if (!cancellationWithRefund && res.getStatut() == ReservationStatus.COMPLETED) {
                if (retainedAmount.compareTo(BigDecimal.ZERO) <= 0 && restoredAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    if (res.getDepositStatus() == DepositStatus.RELEASED) {
                        restoredAmount = safeMoney(deposit);
                    } else if (res.getDepositStatus() == DepositStatus.FORFEITED) {
                        retainedAmount = safeMoney(deposit);
                    }
                }

                if (retainedAmount.compareTo(BigDecimal.ZERO) < 0) {
                    retainedAmount = BigDecimal.ZERO;
                }
                if (retainedAmount.compareTo(deposit) > 0) {
                    retainedAmount = safeMoney(deposit);
                }

                if (restoredAmount.compareTo(BigDecimal.ZERO) < 0 || restoredAmount.compareTo(deposit) > 0) {
                    restoredAmount = safeMoney(deposit.subtract(retainedAmount).max(BigDecimal.ZERO));
                }

                if (retainedAmount.add(restoredAmount).compareTo(safeMoney(deposit)) != 0) {
                    restoredAmount = safeMoney(deposit.subtract(retainedAmount).max(BigDecimal.ZERO));
                }

                if (damageAmount.compareTo(BigDecimal.ZERO) <= 0 && retainedAmount.compareTo(BigDecimal.ZERO) > 0) {
                    damageAmount = retainedAmount;
                }

                String cautionOutcome = resolveCautionOutcomeLabel(deposit, retainedAmount, restoredAmount);
                if ("Remboursement total".equals(cautionOutcome)) {
                    refundNote = "Clôture location: caution remboursée en totalité.";
                } else if ("Remboursement partiel".equals(cautionOutcome)) {
                    refundNote = "Clôture location: caution remboursée partiellement après retenue des dommages.";
                } else if ("Aucun remboursement".equals(cautionOutcome)) {
                    refundNote = "Clôture location: caution totalement retenue (aucun remboursement).";
                } else {
                    refundNote = "Clôture location: caution ajustée selon l'état des lieux.";
                }
            }

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

            PdfPTable table = new PdfPTable(new float[] { 3.1f, 2f });
            table.setWidthPercentage(100);
            table.setSpacingBefore(6f);
            table.setSpacingAfter(10f);

            addRow(table, "Prix total location", money(total));
            addRow(table, "Avance payée", money(advance));
            addRow(table, "Paiement final", money(finalPayment));
            addRow(table, "Caution", money(deposit));
            addRow(table, "Statut caution", mapDepositStatusLabel(res.getDepositStatus()));
            addRow(table, "Motif dégâts", damageReason);

            if (cancellationWithRefund) {
                addRow(table, "Avance remboursée", money(refundedAdvance));
                addRow(table, "Caution remboursée", money(refundedDeposit));
                addRow(table, "Montant perdu", money(totalLost));
                addRow(table, "Total remboursé", money(totalRefunded));
            } else if (res.getStatut() == ReservationStatus.COMPLETED) {
                addRow(table, "Montant dommages", money(damageAmount));
                addRow(table, "Caution retenue", money(retainedAmount));
                addRow(table, "Caution restituée", money(restoredAmount));
                addRow(table, "Issue caution", resolveCautionOutcomeLabel(deposit, retainedAmount, restoredAmount));
                addRow(table, "Caution remboursée", resolveCautionRefundedDisplay(deposit, retainedAmount, restoredAmount));
            }

            addRow(table, "Total net location", money(netLocationPaid));
            addRow(table, "Note remboursement", refundNote);

            document.add(table);

            Paragraph note = new Paragraph("Cette facture résume les mouvements financiers de la location: avance, solde final, caution et remboursement.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, muted));
            note.setSpacingAfter(8f);
            document.add(note);

            document.add(new Paragraph("Mentions et conditions", sectionFont));
            document.add(new Paragraph("• Les montants affichés couvrent la période réservée et la caution associée au dossier.", bodyFont));
            document.add(new Paragraph("• La caution est restituée selon le résultat de l'état des lieux et les règles de restitution appliquées au dossier.", bodyFont));
            document.add(new Paragraph("• En cas de solde restant dû, le paiement final doit être confirmé avant la clôture administrative.", bodyFont));

            document.add(new Paragraph(" "));

            PdfPTable signatureTable = new PdfPTable(2);
            signatureTable.setWidthPercentage(100);
            signatureTable.setSpacingBefore(8f);
            signatureTable.setWidths(new float[] { 1f, 1f });
            signatureTable.addCell(signatureBox("Agence", safeText(res.getAgenceLocation() != null ? res.getAgenceLocation().getNomAgence() : "Agence de location"), safeText(res.getAgenceLocation() != null ? res.getAgenceLocation().getAdresse() : "Adresse non renseignée"), primary, border, muted));
            signatureTable.addCell(signatureBox("Client", clientName, safeText(res.getClient() != null ? res.getClient().getUsername() : "Client"), primary, border, muted));

            document.add(signatureTable);

            Paragraph footer = new Paragraph("Document généré automatiquement à partir du dossier de location. Réservation #" + res.getIdReservation() + " · " + period, footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(8f);
            document.add(footer);

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

    private PdfPCell metaCell(String label, String value, Font labelFont, Font valueFont, Color borderColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0f);

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setBorderColor(borderColor);
        labelCell.setPadding(0f);
        labelCell.setPaddingBottom(2f);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(0f);
        valueCell.setPaddingTop(3f);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(labelCell);
        table.addCell(valueCell);
        cell.addElement(table);
        return cell;
    }

    private PdfPCell summaryCard(String title, String value, String note, Color backgroundColor, Color borderColor, Color titleColor, Font valueFont, Font strongFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(backgroundColor);
        cell.setBorderColor(borderColor);
        cell.setPadding(12f);

        Paragraph heading = new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, titleColor));
        heading.setSpacingAfter(6f);
        cell.addElement(heading);
        cell.addElement(new Paragraph(value, strongFont));
        cell.addElement(new Paragraph(note, valueFont));

        return cell;
    }

    private PdfPCell signatureBox(String title, String line1, String line2, Color primary, Color borderColor, Color muted) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(borderColor);
        cell.setPadding(0f);

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell header = new PdfPCell(new Phrase(title.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Font.NORMAL, new Color(15, 23, 42))));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setBorderColor(borderColor);
        header.setPadding(6f);
        header.setBackgroundColor(new Color(240, 244, 248));

        PdfPCell body = new PdfPCell();
        body.setBorderColor(borderColor);
        body.setPadding(14f);
        body.setMinimumHeight(82f);
        body.setHorizontalAlignment(Element.ALIGN_CENTER);
        body.addElement(new Paragraph(title.equalsIgnoreCase("Client") ? "SIGNÉ" : "VALIDÉ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, Font.NORMAL, primary)));
        body.addElement(new Paragraph(line1, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, Font.NORMAL, new Color(15, 23, 42))));
        body.addElement(new Paragraph(line2, FontFactory.getFont(FontFactory.HELVETICA, 9f, Font.NORMAL, muted)));

        table.addCell(header);
        table.addCell(body);
        cell.addElement(table);
        return cell;
    }

    private String periodForInvoice(ReservationLocation res) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return (res.getDateDebut() != null ? res.getDateDebut().format(formatter) : "-")
            + " - "
            + (res.getDateFin() != null ? res.getDateFin().format(formatter) : "-");
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value;
    }

    private String safeDamageReason(String value) {
        if (value == null || value.isBlank()) {
            return "Aucun dommage constaté";
        }

        return value;
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

    private String resolveClosingBadgeLabel(DepositStatus status, ReservationStatus reservationStatus) {
        if (reservationStatus == ReservationStatus.COMPLETED) {
            if (status == DepositStatus.RELEASED) {
                return "CAUTION RESTITUÉE";
            }
            if (status == DepositStatus.FORFEITED) {
                return "CAUTION AJUSTÉE";
            }
            return "CLÔTURÉE";
        }

        if (status == DepositStatus.RELEASED) {
            return "ACQUITTÉE";
        }

        return "EN ATTENTE";
    }

    private String resolveCautionOutcomeLabel(BigDecimal deposit, BigDecimal retained, BigDecimal restored) {
        if (safeMoney(deposit).compareTo(BigDecimal.ZERO) <= 0) {
            return "Aucune caution configurée";
        }

        if (safeMoney(retained).compareTo(BigDecimal.ZERO) <= 0 && safeMoney(restored).compareTo(safeMoney(deposit)) >= 0) {
            return "Remboursement total";
        }

        if (safeMoney(retained).compareTo(BigDecimal.ZERO) > 0 && safeMoney(restored).compareTo(BigDecimal.ZERO) > 0) {
            return "Remboursement partiel";
        }

        if (safeMoney(restored).compareTo(BigDecimal.ZERO) <= 0 && safeMoney(retained).compareTo(safeMoney(deposit)) >= 0) {
            return "Aucun remboursement";
        }

        return "Ajustement caution";
    }

    private String resolveCautionRefundedDisplay(BigDecimal deposit, BigDecimal retained, BigDecimal restored) {
        String outcome = resolveCautionOutcomeLabel(deposit, retained, restored);
        if ("Remboursement total".equals(outcome)) {
            return "OUI (total)";
        }
        if ("Remboursement partiel".equals(outcome)) {
            return "OUI (partiel)";
        }
        if ("Aucun remboursement".equals(outcome)) {
            return "NON";
        }
        return "-";
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