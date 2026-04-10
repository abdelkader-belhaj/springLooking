package tn.hypercloud.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadDir = Path.of("uploads").toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/hypercloud/uploads/**")
                .addResourceLocations(uploadDir);

        // Optionnel (utile pour debug direct)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDir);
    }
}