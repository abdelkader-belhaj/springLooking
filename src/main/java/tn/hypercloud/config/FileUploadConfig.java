package tn.hypercloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.File;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {
    
    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get absolute path of uploads directory
        File uploadDirFile = new File(uploadDir).getAbsoluteFile();
        
        // Ensure directory exists
        if (!uploadDirFile.exists()) {
            uploadDirFile.mkdirs();
        }
        
        // Register resource handler with absolute path
        // Use file:/// protocol with forward slashes for cross-platform compatibility
        String absolutePath = uploadDirFile.getAbsolutePath()
                .replace("\\", "/");
        
        // Add file:/// prefix for Windows, handle drive letters
        String resourceLocation = "file:///" + absolutePath;
        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(0);
        
        System.out.println("📁 File Upload Config - Serving uploads from: " + absolutePath);
        System.out.println("📁 Resource Location: " + resourceLocation);
    }
}
