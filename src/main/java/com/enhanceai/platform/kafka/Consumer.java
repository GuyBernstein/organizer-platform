package com.enhanceai.platform.kafka;

import com.enhanceai.platform.model.FileProcessingMessage;
import com.enhanceai.platform.model.UserContent;
import com.enhanceai.platform.repository.UserContentRepository;
import com.enhanceai.platform.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.enhanceai.platform.kafka.Producer.APP_TOPIC;

@Component
@Slf4j
public class Consumer {
    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserContentRepository userContentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = {APP_TOPIC})
    public void listen(ConsumerRecord<?, ?> record) {
        try {
            Optional<?> kafkaMessage = Optional.ofNullable(record.value());

            if (kafkaMessage.isPresent()) {
                String messageStr = kafkaMessage.get().toString();
                FileProcessingMessage message = objectMapper.readValue(messageStr, FileProcessingMessage.class);

                // Process the file
                String filePath = processFile(message);

                // Find all contents for the user
                Iterable<UserContent> contents = userContentRepository.findByUserName(message.getUserName());


                // Find the content that matches our criteria (needs processing)
                Optional<UserContent> contentToUpdate = getUserContent(contents, message);

                if (contentToUpdate.isPresent()) {
                    UserContent content = contentToUpdate.get();
                    content.setContent(filePath);
                    content.setStatus("COMPLETED");
                    userContentRepository.save(content);
                    log.info("Updated content for user {} with file {}",
                            message.getUserName(), message.getFileName());
                }else {
                    log.error("No matching content found for user {} with file {}",
                            message.getUserName(), message.getFileName());
                }
            }
        } catch (Exception e) {
            log.error("Error processing file message", e);
            // Handle error - you might want to update the status to ERROR in Cassandra
        }
    }

    @NotNull
    private static Optional<UserContent> getUserContent(Iterable<UserContent> contents, FileProcessingMessage message) {
        return StreamSupport.stream(contents.spliterator(), false)
                .filter(content -> content.getStatus().equals("PROCESSING") &&
                        content.getFileName().equals(message.getFileName()) &&
                        content.getFileSize().equals(message.getFileSize()))
                .findFirst();
    }

    private String processFile(FileProcessingMessage message) throws IOException {
        // Create temporary file
        String originalFilename = message.getFileName();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        Path tempFile = Files.createTempFile(message.getContentId(), fileExtension);
        Files.write(tempFile, message.getFileContent());

        // Store file using the File version of the method
        String filePath = fileStorageService.storeFileInFilesystem(tempFile.toFile(), message.getContentId());

        // Clean up temporary file
        Files.deleteIfExists(tempFile);

        return filePath;
    }

}
