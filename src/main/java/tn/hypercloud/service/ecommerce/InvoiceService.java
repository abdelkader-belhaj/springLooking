package tn.hypercloud.service.ecommerce;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.dto.ecommerce.OrderDTO;
import tn.hypercloud.dto.ecommerce.OrderDetailDTO;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service("ecommerceInvoiceService")
@RequiredArgsConstructor
public class InvoiceService {

    /**
     * Generate PDF invoice for an order
     */
    public byte[] generateInvoicePDF(OrderDTO order) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, outputStream);
        document.open();

        // Header
        Paragraph header = new Paragraph("FACTURE", new Font(Font.HELVETICA, 24, Font.BOLD));
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);

        // Space
        document.add(new Paragraph("\n"));

        // Order info table
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        
        addInfoCell(infoTable, "Numéro de Commande", true);
        addInfoCell(infoTable, order.getOrderNumber(), false);
        addInfoCell(infoTable, "Date", true);
        addInfoCell(infoTable, formatDate(order.getCreatedAt()), false);

        addInfoCell(infoTable, "Statut", true);
        addInfoCell(infoTable, getStatusLabel(order.getStatus()), false);
        addInfoCell(infoTable, "Statut de Paiement", true);
        addInfoCell(infoTable, getPaymentStatusLabel(order.getPaymentStatus()), false);

        document.add(infoTable);

        // Space
        document.add(new Paragraph("\n"));

        // Customer info
        Paragraph customerHeader = new Paragraph("INFORMATIONS CLIENT", new Font(Font.HELVETICA, 12, Font.BOLD));
        document.add(customerHeader);

        Paragraph customerInfo = new Paragraph();
        customerInfo.add("Nom: " + order.getClientName() + "\n");
        customerInfo.add("Email: " + order.getClientEmail() + "\n");
        customerInfo.add("Adresse de Livraison: " + order.getShippingAddress() + "\n");
        customerInfo.add("Méthode de Paiement: " + order.getPaymentMethod());
        document.add(customerInfo);

        // Space
        document.add(new Paragraph("\n"));

        // Items header
        Paragraph itemsHeader = new Paragraph("ARTICLES", new Font(Font.HELVETICA, 12, Font.BOLD));
        document.add(itemsHeader);

        // Items table
        PdfPTable itemsTable = new PdfPTable(4);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{4, 1, 2, 2});

        // Header row
        addTableHeaderCell(itemsTable, "Produit");
        addTableHeaderCell(itemsTable, "Qté");
        addTableHeaderCell(itemsTable, "Prix Unit.");
        addTableHeaderCell(itemsTable, "Sous-total");

        // Items rows
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            for (OrderDetailDTO detail : order.getOrderDetails()) {
                PdfPCell cell1 = new PdfPCell(new Paragraph(detail.getProductName()));
                cell1.setPadding(5);
                itemsTable.addCell(cell1);

                PdfPCell cell2 = new PdfPCell(new Paragraph(String.valueOf(detail.getQuantity())));
                cell2.setPadding(5);
                cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
                itemsTable.addCell(cell2);

                PdfPCell cell3 = new PdfPCell(new Paragraph(String.format("%.2f TND", detail.getUnitPrice())));
                cell3.setPadding(5);
                cell3.setHorizontalAlignment(Element.ALIGN_RIGHT);
                itemsTable.addCell(cell3);

                PdfPCell cell4 = new PdfPCell(new Paragraph(String.format("%.2f TND", detail.getSubtotal())));
                cell4.setPadding(5);
                cell4.setHorizontalAlignment(Element.ALIGN_RIGHT);
                itemsTable.addCell(cell4);
            }
        }

        document.add(itemsTable);

        // Totals table
        document.add(new Paragraph("\n"));
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(60);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell subtotalLabel = new PdfPCell(new Paragraph("Sous-total", new Font(Font.HELVETICA, Font.BOLD)));
        subtotalLabel.setPadding(5);
        subtotalLabel.setBorderWidth(0);
        totalsTable.addCell(subtotalLabel);

        PdfPCell subtotalValue = new PdfPCell(new Paragraph(String.format("%.2f TND", order.getSubtotal())));
        subtotalValue.setPadding(5);
        subtotalValue.setBorderWidth(0);
        subtotalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.addCell(subtotalValue);

        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            PdfPCell discountLabel = new PdfPCell(new Paragraph("Remise", new Font(Font.HELVETICA, Font.BOLD)));
            discountLabel.setPadding(5);
            discountLabel.setBorderWidth(0);
            totalsTable.addCell(discountLabel);

            PdfPCell discountValue = new PdfPCell(new Paragraph(String.format("− %.2f TND", order.getDiscountAmount())));
            discountValue.setPadding(5);
            discountValue.setBorderWidth(0);
            discountValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.addCell(discountValue);
        }

        PdfPCell totalLabel = new PdfPCell(new Paragraph("TOTAL", new Font(Font.HELVETICA, 14, Font.BOLD)));
        totalLabel.setPadding(5);
        totalLabel.setBorderWidth(1);
        totalsTable.addCell(totalLabel);

        PdfPCell totalValue = new PdfPCell(new Paragraph(String.format("%.2f TND", order.getTotalAmount()), new Font(Font.HELVETICA, 14, Font.BOLD)));
        totalValue.setPadding(5);
        totalValue.setBorderWidth(1);
        totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.addCell(totalValue);

        document.add(totalsTable);

        // Footer
        document.add(new Paragraph("\n\n"));
        Paragraph footer = new Paragraph("Merci pour votre commande!", new Font(Font.HELVETICA, 10, Font.ITALIC));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return outputStream.toByteArray();
    }

    private void addInfoCell(PdfPTable table, String content, boolean isBold) {
        PdfPCell cell = new PdfPCell(new Paragraph(content, isBold ? new Font(Font.HELVETICA, Font.BOLD) : new Font(Font.HELVETICA)));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addTableHeaderCell(PdfPTable table, String content) {
        PdfPCell cell = new PdfPCell(new Paragraph(content, new Font(Font.HELVETICA, Font.BOLD)));
        cell.setPadding(5);
        cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateTime.format(formatter);
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "pending" -> "En attente";
            case "paid" -> "Payé";
            case "shipped" -> "Expédié";
            case "delivered" -> "Livré";
            case "cancelled" -> "Annulé";
            default -> status;
        };
    }

    private String getPaymentStatusLabel(String status) {
        return switch (status) {
            case "pending" -> "En attente";
            case "paid" -> "Payé";
            case "failed" -> "Échoué";
            default -> status;
        };
    }
}
