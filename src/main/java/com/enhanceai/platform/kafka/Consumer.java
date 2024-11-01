package com.enhanceai.platform.kafka;

import com.enhanceai.platform.model.FileProcessingMessage;
import com.enhanceai.platform.model.UserContent;
import com.enhanceai.platform.model.UserContentOut;
import com.enhanceai.platform.repository.UserContentRepository;
import com.enhanceai.platform.service.MinioService;
import com.enhanceai.platform.service.Redis;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.StreamSupport;

import static com.enhanceai.platform.kafka.Producer.APP_TOPIC;
@Component
@Slf4j
public class Consumer {

    @Autowired
    private MinioService minioService;

    @Autowired
    private UserContentRepository userContentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Redis redis;

    @Value("${spring.minio.bucket}")
    private String bucketName;

    @KafkaListener(topics = {APP_TOPIC})
    public void listen(ConsumerRecord<?, ?> record) {
        try {
            Optional<?> kafkaMessage = Optional.ofNullable(record.value());

            if (kafkaMessage.isPresent()) {
                String messageStr = kafkaMessage.get().toString();
                FileProcessingMessage message = objectMapper.readValue(messageStr, FileProcessingMessage.class);

                // Process the file
                String objectName = processFile(message);

                // Find all contents for the user
                Iterable<UserContent> contents = userContentRepository.findByUserName(message.getUserName());

                // Find the content that matches our criteria (needs processing)
                Optional<UserContent> contentToUpdate = getUserContent(contents, message);

                if (contentToUpdate.isPresent()) {
                    UserContent content = contentToUpdate.get();
                    // Store the MinIO object name instead of file path
                    content.setContent(objectName);
                    content.setStatus("COMPLETED");
                    content = userContentRepository.save(content);
                    if (!redis.set(content.getContentId(), objectMapper.writeValueAsString(UserContentOut.of(content)))) {
                        log.error("couldn't save content in Redis for user {} with file {}",
                                message.getUserName(), message.getFileName());
                    } else {
                        log.info("Updated content for user {} with file {} in both Cassandra and Redis",
                                message.getUserName(), message.getFileName());
                    }
                } else {
                    log.error("No matching content found for user {} with file {}",
                            message.getUserName(), message.getFileName());
                }
            }
        } catch (Exception e) {
            log.error("Error processing file message", e);
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

    private String processFile(FileProcessingMessage message) throws Exception {
        String objectName = message.getContentId() + "_" + message.getFileName();

        // Upload directly to MinIO using byte array
        minioService.uploadFile(message.getFileContent(), objectName, message.getMimeType());

        return objectName;
    }
}