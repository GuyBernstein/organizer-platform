package com.enhanceai.platform.controller;

import com.enhanceai.platform.kafka.Producer;
import com.enhanceai.platform.model.*;
import com.enhanceai.platform.repository.UserContentRepository;
import com.enhanceai.platform.repository.UserRepository;
import com.enhanceai.platform.service.MinioService;
import com.enhanceai.platform.service.Redis;
import com.enhanceai.platform.util.Dates;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.enhanceai.platform.model.UserBuilder.anUser;
import static com.enhanceai.platform.util.Dates.getCurMonth;
import static org.springframework.data.util.StreamUtils.createStreamFromIterator;

@RestController
@RequestMapping("/api/content")
@Api(tags = "Content Management API")
public class AppController {
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final Redis redis;
    private final ObjectMapper objectMapper;
    private final UserContentRepository userContentRepository;
    private final Producer producer;
    private final MinioService minioService;

    @Autowired
    public AppController(UserRepository userRepository, MongoTemplate mongoTemplate, Redis redis,
                         ObjectMapper objectMapper, UserContentRepository userContentRepository,
                         Producer producer, MinioService minioService) {
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.userContentRepository = userContentRepository;
        this.producer = producer;
        this.minioService = minioService;
    }

    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public User createUser(@RequestParam String name) {
        User user = anUser().withName(name).build();
        user = userRepository.insert(user);
        return user;
    }

    @RequestMapping(value = "/user/{name}", method = RequestMethod.GET)
    public User getUser(@RequestParam String name) {
        return userRepository.findFirstByName(name);
    }

    @RequestMapping(value = "/user/{name}/contents", method = RequestMethod.GET)
    public List<UserContentOut> getUserContents(@PathVariable String name) {
        return StreamSupport.stream(userContentRepository.findByUserName(name).spliterator(), false)
                .map(UserContentOut::of)
                .collect(Collectors.toList());
    }

    @ApiOperation(value = "Store text content", notes = "Stores text content with metadata in Redis")
    @PostMapping(value = "/text")
    public ResponseEntity<UserContentOut> storeText(
            @ApiParam(value = "Text content to store", required = true)
            @RequestBody TextDataIn content) throws JsonProcessingException {

        if(content.getUserName().isEmpty() || content.getText().isEmpty() || content.getType().isEmpty())
            throw new RuntimeException("Please provide username, text, and the type of the text");

        String contentId = UUID.randomUUID().toString();

        UserContent textContent = UserContent.UserContentBuilder.anUserContent()
                .userContentKey(UserContentKey.UserContentKeyBuilder.anUserContentKey()
                        .userName(content.getUserName()).creationTime(Dates.nowUTC())
                        .build())
                .contentId(contentId)
                .contentType("TEXT")
                .content(content.getText())
                .build();

        textContent = userContentRepository.save(textContent);

        if (redis.set(contentId, objectMapper.writeValueAsString(UserContentOut.of(textContent)))) {
            // Update MongoDB document
            Query query = Query.query(Criteria.where("name").is(content.getUserName()));
            Update update = new Update()
                    .inc("totalTextContents", 1)
                    .inc("textContents.content." + content.getType() + "." + getCurMonth() + ".count", 1)
                    .set("textContents.content." + content.getType() + "." + getCurMonth() + ".lastUpdated", Dates.nowUTC())
                    .push("textContents.content." + content.getType() + "." + getCurMonth() + ".contentIds", contentId);

            mongoTemplate.upsert(query, update, User.class);

            return ResponseEntity.ok(UserContentOut.of(textContent));
        }
        return ResponseEntity.badRequest().build();
    }

    @ApiOperation(value = "Store file metadata", notes = "Stores file metadata in Redis and prepares for async processing")
    @PostMapping("/file")
    public ResponseEntity<UserContentOut> storeFile(
            @ApiParam(value = "File to process", required = true)
            @RequestParam MultipartFile file,
            @RequestParam String userName,
            @RequestParam String category) throws IOException {

        if(userName.isEmpty() || file.isEmpty() || category.isEmpty())
            throw new RuntimeException("Please provide username, a file and a file category");

        String contentId = UUID.randomUUID().toString();
        String fileName = file.getOriginalFilename();
        Long fileSize = file.getSize();
        String mimeType = file.getContentType();

        FileProcessingMessage message = FileProcessingMessage.FileProcessingMessageBuilder.aFileProcessingMessage()
                .contentId(contentId)
                .userName(userName)
                .category(category)
                .fileName(fileName)
                .mimeType(mimeType)
                .fileSize(fileSize)
                .fileContent(file.getBytes())
                .creationTime(Dates.nowUTC())
                .build();


        UserContent cassandraData = UserContent.UserContentBuilder.anUserContent()
                .userContentKey(UserContentKey.UserContentKeyBuilder.anUserContentKey()
                        .userName(userName).creationTime(Dates.nowUTC())
                        .build())
                .contentId(contentId)
                .contentType("FILE")
                .mimeType(mimeType)
                .fileName(fileName)
                .fileSize(fileSize)
                .status("PROCESSING") // Add status field to track processing
                .build();

        cassandraData = userContentRepository.save(cassandraData);

        if (redis.setIfAbsent(contentId, objectMapper.writeValueAsString(UserContentOut.of(cassandraData)))) {
            // Update MongoDB document
            Query query = Query.query(Criteria.where("name").is(userName));
            Update update = new Update()
                    .inc("totalFiles", 1)
                    .inc("fileContents.content." + category + "." + getCurMonth() + ".count", 1)
                    .set("fileContents.content." + category + "." + getCurMonth() + ".lastUpdated", Dates.nowUTC())
                    .push("fileContents.content." + category + "." + getCurMonth() + ".contentIds", contentId);

            mongoTemplate.upsert(query, update, User.class);

            // Send to Kafka for async processing
            producer.send(message);

            return ResponseEntity.ok(UserContentOut.of(cassandraData));
        }
        return ResponseEntity.badRequest().build();
    }

    @ApiOperation(value = "Retrieve content by key", notes = "Gets content and metadata from Redis")
    @GetMapping(value = "/{key}")
    public ResponseEntity<UserContentOut> getContent(
            @ApiParam(value = "Content key", required = true)
            @PathVariable String key) throws JsonProcessingException {

        // Check if key exists
        if (!redis.hasKey(key)) {
            return ResponseEntity.notFound().build();
        }

        Object value = redis.get(key);
        UserContentOut data = objectMapper.readValue(value.toString(), UserContentOut.class);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/file/{userName}")
    public ResponseEntity<?> getFile(@PathVariable String userName) {
        try {
            // Find the content in Cassandra
            Optional<UserContent> content = userContentRepository.findLatestByUserName(userName);

            if (content.isPresent() && content.get().getStatus().equals("COMPLETED")) {
                UserContent userContent = content.get();
                String objectName = userContent.getContent();

                String presignedUrl = minioService.getPresignedUrl(objectName, 1); // 1 minutes expiry
                return ResponseEntity.ok(Collections.singletonMap("url", presignedUrl));
            }

            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @ApiOperation(value = "Delete content", notes = "Deletes content from Redis by key")
    @DeleteMapping("/{key}")
    public ResponseEntity<String> deleteContent(
            @ApiParam(value = "Content key to delete", required = true)
            @PathVariable String key) {

        // Check if key exists
        if (!redis.hasKey(key)) {
            return ResponseEntity.notFound().build();
        }

        // Delete the key
        redis.del(key);
        return ResponseEntity.ok("Content deleted successfully");
    }
}