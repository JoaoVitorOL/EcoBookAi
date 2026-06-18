package com.ecobook.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class UploadStorageWebConfig implements WebMvcConfigurer {

    private final String uploadDir;

    /**
     * Creates the web resource configuration bound to the upload directory.
     * @param uploadDir configured upload directory path
     */
    public UploadStorageWebConfig(@Value("${storage.upload-dir}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    /**
     * Registers the resource handlers that expose stored uploads.
     * @param registry Spring resource handler registry to customize
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        String resourceLocation = uploadRoot.toUri().toString();
        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}
