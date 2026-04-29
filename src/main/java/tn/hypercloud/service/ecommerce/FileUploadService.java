package tn.hypercloud.service.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadService {
    
    @Value("${file.upload.dir:uploads}")
    private String uploadDir;
    
    @Value("${file.upload.max-size:5242880}")
    private long maxFileSize;
    
    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "webp"};
    private static final String[] ALLOWED_MIME_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    /**
     * Upload image file for product
     * @param file Multipart file from request
     * @param productId Product ID for organizing files
     * @return Relative file path to store in database (e.g., "uploads/products/1/filename.jpg")
     */
    public String uploadProductImage(MultipartFile file, Long productId) throws IOException {
        validateFile(file);
        
        // Create directory structure: uploads/products/{productId}
        Path productDir = Paths.get(uploadDir, "products", productId.toString());
        Files.createDirectories(productDir);
        
        // Generate unique filename
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID().toString() + "." + fileExtension;
        
        // Save file
        Path filePath = productDir.resolve(uniqueFileName);
        Files.write(filePath, file.getBytes());
        
        // Return relative path for database storage
        return "uploads/products/" + productId + "/" + uniqueFileName;
    }

    /**
     * Delete image file
     * @param imagePath Relative path to delete
     */
    public void deleteProductImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return;
        }
        try {
            Path filePath = Paths.get(imagePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log but don't throw - file might already be deleted
            System.err.println("Could not delete file: " + imagePath);
        }
    }

    /**
     * Update product image (delete old, save new)
     */
    public String updateProductImage(MultipartFile file, Long productId, String oldImagePath) throws IOException {
        // Delete old image if exists
        if (oldImagePath != null && !oldImagePath.isEmpty()) {
            deleteProductImage(oldImagePath);
        }
        
        // Upload new image
        return uploadProductImage(file, productId);
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new IOException("File size exceeds maximum allowed size: " + maxFileSize + " bytes");
        }
        
        // Get file extension - it's mandatory
        String originalFileName = file.getOriginalFilename();
        String extension = getFileExtension(originalFileName).toLowerCase();
        
        // Validate extension
        boolean validExtension = false;
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equals(extension)) {
                validExtension = true;
                break;
            }
        }
        
        if (!validExtension) {
            throw new IOException("Invalid file extension. Allowed: JPG, JPEG, PNG, GIF, WEBP");
        }
        
        // Validate MIME type (if available) - fallback to extension if null
        String mimeType = file.getContentType();
        if (mimeType != null && !mimeType.isEmpty()) {
            boolean validMimeType = false;
            String lowerMimeType = mimeType.toLowerCase();
            
            // Check for common MIME types (case-insensitive)
            if (lowerMimeType.contains("jpeg") || lowerMimeType.contains("jpg") ||
                lowerMimeType.contains("png") || lowerMimeType.contains("gif") ||
                lowerMimeType.contains("webp")) {
                validMimeType = true;
            }
            
            if (!validMimeType) {
                throw new IOException("Invalid MIME type: " + mimeType + ". Allowed: image/jpeg, image/png, image/gif, image/webp");
            }
        }
        // If mimeType is null, we still allow it if extension is valid (already checked above)
    }

    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        
        if (!fileName.contains(".")) {
            throw new IllegalArgumentException("Filename must have an extension");
        }
        
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}
