package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.ReservationLocation;

public interface PdfService {
    String generateContractPdf(ReservationLocation reservation);
    String addSignatureToPdf(String existingPdfPath, String base64Signature, String signedBy);
}