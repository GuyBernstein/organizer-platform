package com.enhanceai.platform.service;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Configuration
@Service
public class FileStorageService {

    private final String storagePath;

    public FileStorageService(@Value("${file.upload.directory:${user.home}/Documents/shahafwebsite}") String storagePath) {
        this.storagePath = storagePath;
    }

    @PostConstruct
    public void init() {
        try {
            if (storagePath == null) {
                throw new IllegalStateException("Upload directory path is not set");
            }

            Path uploadPath = Paths.get(storagePath);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Created upload directory: " + uploadPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    // Existing method for MultipartFile
    public String storeFileInFilesystem(MultipartFile file, String contentId) throws IOException {
        return storeFile(file.getInputStream(), file.getOriginalFilename(), contentId);
    }

    // New method for File
    public String storeFileInFilesystem(File file, String contentId) throws IOException {
        return storeFile(new FileInputStream(file), file.getName(), contentId);
    }

    // Common implementation
    private String storeFile(InputStream inputStream, String originalFilename, String contentId) throws IOException {
        // Create storage directory if it doesn't exist
        File directory = new File(storagePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Generate file path
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filePath = storagePath + File.separator + contentId + fileExtension;

        // Copy file to storage location
        Files.copy(inputStream, Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);

        return filePath;
    }

}
