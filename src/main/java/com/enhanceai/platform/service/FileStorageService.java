package com.enhanceai.platform.service;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

@Configuration
@Service
public class FileStorageService {

    private final String uploadDir;

    public FileStorageService(@Value("${file.upload.directory:${user.home}/Documents/shahafwebsite}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @PostConstruct
    public void init() {
        try {
            if (uploadDir == null) {
                throw new IllegalStateException("Upload directory path is not set");
            }

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Created upload directory: " + uploadPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    public String storeFileInFilesystem(MultipartFile file, String fileId) throws IOException {
        // Create year/month based directory structure
        String datePath = new SimpleDateFormat("yyyy/MM").format(new Date());
        Path directory = Paths.get(uploadDir, datePath);
        Files.createDirectories(directory);

        // Store file with unique name
        Path filePath = directory.resolve(fileId + getFileExtension(file));
        Files.copy(file.getInputStream(), filePath);

        return filePath.toString();
    }

    private String getFileExtension(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName != null && originalFileName.contains(".")) {
            return originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return "";
    }
}
