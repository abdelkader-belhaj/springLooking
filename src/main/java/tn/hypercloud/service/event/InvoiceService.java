package tn.hypercloud.service.event;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    @Value("${app.company.name:TunisiaTour}")
    private String companyName;

    @Value("${app.company.email:contact@tunisiatour.tn}")
    private String companyEmail;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    /**
     * Génère un PDF facture professionnel
     */
    public byte[] generateInvoicePdf(
            String clientName,
            String clientEmail,
            String eventTitle,
            LocalDateTime eventDate,
            String eventAddress,
            int numberOfTickets,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            Integer reservationId
    ) throws Exception {
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        document.open();

        // ========== En-tête
        document.add(createHeader());
        document.add(new Paragraph("\n"));

        // ========== Info facture
        document.add(createInvoiceInfo(eventDate));
        document.add(new Paragraph("\n"));

        // ========== Info client
        document.add(createClientInfo(clientName, clientEmail));
        document.add(new Paragraph("\n"));

        // ========== Événement
        document.add(createEventSection(eventTitle, eventDate, eventAddress));
        document.add(new Paragraph("\n"));

        // ========== Tableau items
        document.add(createItemsTable(eventTitle, numberOfTickets, unitPrice, totalPrice));
        document.add(new Paragraph("\n"));

        // ========== Total
        document.add(createTotalSection(totalPrice));
        document.add(new Paragraph("\n\n"));

        // ========== Footer
        document.add(createFooter());

        document.close();
        return out.toByteArray();
    }

    public byte[] generateTicketsPdf(
            String clientName,
            String eventTitle,
            LocalDateTime eventDate,
            String eventAddress,
            Integer reservationId,
            List<TicketPdfItem> tickets
    ) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        document.open();

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (int i = 0; i < tickets.size(); i++) {
            TicketPdfItem ticket = tickets.get(i);

            if (i > 0) {
                document.newPage();
            }

            Paragraph title = new Paragraph("BILLET ELECTRONIQUE", new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, new BaseColor(15, 118, 110)));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            Paragraph details = new Paragraph();
            details.setAlignment(Element.ALIGN_LEFT);
            details.add(new Phrase("Client: " + safe(clientName) + "\n", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
            details.add(new Phrase("Evenement: " + safe(eventTitle) + "\n", new Font(Font.FontFamily.HELVETICA, 11)));
            details.add(new Phrase("Date: " + eventDate.format(dateTimeFormatter) + "\n", new Font(Font.FontFamily.HELVETICA, 11)));
            details.add(new Phrase("Lieu: " + safe(eventAddress) + "\n", new Font(Font.FontFamily.HELVETICA, 11)));
            details.add(new Phrase("Ticket: " + ticket.getTicketNumber() + "/" + tickets.size() + "\n", new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD)));
            details.add(new Phrase("Code ticket: " + safe(ticket.getTicketCode()) + "\n", new Font(Font.FontFamily.COURIER, 11, Font.BOLD)));
            document.add(details);
            document.add(new Paragraph("\n"));

            String qrPayload = frontendBaseUrl + "/ticket/" + reservationId + "?code=" + ticket.getTicketCode();
            Image qrImage = Image.getInstance(generateQrPng(qrPayload));
            qrImage.scaleAbsolute(210, 210);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            document.add(qrImage);

            Paragraph hint = new Paragraph("Presentez ce QR a l'entree", new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.GRAY));
            hint.setAlignment(Element.ALIGN_CENTER);
            hint.setSpacingBefore(10f);
            document.add(hint);

            Paragraph footer = new Paragraph("\nEmis le " + LocalDate.now() + " - " + companyName, new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
        }

        document.close();
        return out.toByteArray();
    }

    private PdfPTable createHeader() throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Logo / Nom entreprise
        PdfPCell cell1 = new PdfPCell();
        cell1.setHorizontalAlignment(Element.ALIGN_LEFT);
        Paragraph p = new Paragraph(companyName, new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD, BaseColor.DARK_GRAY));
        cell1.addElement(p);
        cell1.setBorder(0);
        table.addCell(cell1);

        // Texte droite
        PdfPCell cell2 = new PdfPCell();
        cell2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph subtitle = new Paragraph("FACTURE", new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, new BaseColor(15, 118, 110)));
        cell2.addElement(subtitle);

        Paragraph email = new Paragraph(companyEmail, new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.GRAY));
        cell2.addElement(email);

        cell2.setBorder(0);
        table.addCell(cell2);

        return table;
    }

    private Paragraph createInvoiceInfo(LocalDateTime eventDate) {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_LEFT);

        p.add("Ref Facture: ");
        String ref = "FAC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Chunk refChunk = new Chunk(ref, new Font(Font.FontFamily.COURIER, 11, Font.BOLD));
        p.add(refChunk);

        p.add(" | Date: ");
        String dateStr = eventDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        Chunk dateChunk = new Chunk(dateStr, new Font(Font.FontFamily.COURIER, 11, Font.BOLD));
        p.add(dateChunk);

        return p;
    }

    private Paragraph createClientInfo(String clientName, String clientEmail) {
        Paragraph p = new Paragraph("CLIENT", new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.DARK_GRAY));
        p.add(new Paragraph(clientName, new Font(Font.FontFamily.HELVETICA, 10)));
        p.add(new Paragraph(clientEmail, new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC)));
        return p;
    }

    private Paragraph createEventSection(String eventTitle, LocalDateTime eventDate, String eventAddress) {
        Paragraph p = new Paragraph("DÉTAILS DE L'ÉVÉNEMENT", new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.DARK_GRAY));
        
        String dateStr = eventDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        p.add(new Paragraph("Titre: " + eventTitle, new Font(Font.FontFamily.HELVETICA, 10)));
        p.add(new Paragraph("Date: " + dateStr, new Font(Font.FontFamily.HELVETICA, 10)));
        p.add(new Paragraph("Lieu: " + eventAddress, new Font(Font.FontFamily.HELVETICA, 10)));
        
        return p;
    }

    private PdfPTable createItemsTable(
            String eventTitle,
            int numberOfTickets,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) throws DocumentException {
        
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{40, 20, 20, 20});
        table.setSpacingBefore(10f);

        // Header
        addHeaderCell(table, "Description");
        addHeaderCell(table, "Quantité");
        addHeaderCell(table, "Prix unitaire");
        addHeaderCell(table, "Total");

        // Row
        PdfPCell cell1 = new PdfPCell(new Phrase(eventTitle, new Font(Font.FontFamily.HELVETICA, 10)));
        cell1.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell1.setPadding(8);
        table.addCell(cell1);

        PdfPCell cell2 = new PdfPCell(new Phrase(String.valueOf(numberOfTickets), new Font(Font.FontFamily.HELVETICA, 10)));
        cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell2.setPadding(8);
        table.addCell(cell2);

        PdfPCell cell3 = new PdfPCell(new Phrase(unitPrice.toPlainString() + " TND", new Font(Font.FontFamily.HELVETICA, 10)));
        cell3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell3.setPadding(8);
        table.addCell(cell3);

        PdfPCell cell4 = new PdfPCell(new Phrase(totalPrice.toPlainString() + " TND", new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        cell4.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell4.setPadding(8);
        table.addCell(cell4);

        return table;
    }

    private Paragraph createTotalSection(BigDecimal totalPrice) {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_RIGHT);

        String totalStr = totalPrice.toPlainString() + " TND";
        Chunk total = new Chunk("MONTANT TOTAL: " + totalStr, 
                new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(15, 118, 110)));
        p.add(total);

        return p;
    }

    private Paragraph createFooter() {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Phrase("© 2026 " + companyName + " - Confirmée informatiquement", 
                new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY)));
        return p;
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, 
                new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE)));
        cell.setBackgroundColor(new BaseColor(15, 118, 110));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        table.addCell(cell);
    }

    private byte[] generateQrPng(String content) throws Exception {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 320, 320, hints);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException e) {
            throw new Exception("QR generation failed", e);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TicketPdfItem {
        private Integer ticketNumber;
        private String ticketCode;
    }
}
