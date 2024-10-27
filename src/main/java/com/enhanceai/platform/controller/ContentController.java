package com.enhanceai.platform.controller;

import com.enhanceai.platform.model.*;
import com.enhanceai.platform.repository.UserContentRepository;
import com.enhanceai.platform.repository.UserRepository;
import com.enhanceai.platform.service.FileStorageService;
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
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.enhanceai.platform.model.UserBuilder.anUser;
import static com.enhanceai.platform.util.Dates.getCurMonth;
import static org.springframework.data.util.StreamUtils.createStreamFromIterator;

@RestController
@RequestMapping("/api/content")
@Api(tags = "Content Management API")
public class ContentController {
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final Redis redis;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;
    private final UserContentRepository userContentRepository;


    @Autowired
    public ContentController(UserRepository userRepository, MongoTemplate mongoTemplate, Redis redis, ObjectMapper objectMapper, FileStorageService fileStorageService, UserContentRepository userContentRepository) {
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.fileStorageService = fileStorageService;
        this.userContentRepository = userContentRepository;
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
    public List<UserContentOut> getUserContents(@RequestParam String name) {
        var userContents = createStreamFromIterator( userContentRepository.findByUserName(name).iterator())
                .map(UserContentOut::of)
                .collect(Collectors.toList());
        return userContents;
    }

    @ApiOperation(value = "Store text content", notes = "Stores text content with metadata in Redis")
    @PostMapping(value = "/text")
    public ResponseEntity<UserContentOut> storeText(
            @ApiParam(value = "Text content to store", required = true)
            @RequestBody RedisDataIn content) throws JsonProcessingException {

        if(content.getUserName().isEmpty() || content.getText().isEmpty() || content.getType().isEmpty())
            throw new RuntimeException("Please provide username, text, and the type of the text");

        String contentId = UUID.randomUUID().toString();

        UserContent textContent = UserContent.UserContentBuilder.anUserContent()
                .userContentKey(UserContentKey.UserContentKeyBuilder.anUserContentKey()
                        .userName(content.getUserName()).creationTime(new Date())
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
                    .set("textContents.content." + content.getType() + "." + getCurMonth() + ".lastUpdated", new Date())
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
            @RequestParam String catagory) throws IOException {

        if(userName.isEmpty() || file.isEmpty() || catagory.isEmpty())
            throw new RuntimeException("Please provide username, a file and a file catagory");

        String contentId = UUID.randomUUID().toString();
        String filePath = fileStorageService.storeFileInFilesystem(file,contentId);
        String fileName = file.getOriginalFilename();
        Long fileSize = file.getSize();
        String mimeType = file.getContentType();

        UserContent fileContent = UserContent.UserContentBuilder.anUserContent()
                .userContentKey(UserContentKey.UserContentKeyBuilder.anUserContentKey()
                        .userName(userName).creationTime(new Date())
                        .build())
                .contentId(contentId)
                .contentType("FILE")
                .content(filePath)
                .mimeType(mimeType)
                .fileName(fileName)
                .fileSize(fileSize)
                .build();

        fileContent = userContentRepository.save(fileContent);

        if (redis.set(contentId, objectMapper.writeValueAsString(UserContentOut.of(fileContent)))) {
            // Update MongoDB document
            Query query = Query.query(Criteria.where("name").is(userName));
            Update update = new Update()
                    .inc("totalFiles", 1)
                    .inc("fileContents.content." + catagory + "." + getCurMonth() + ".count", 1)
                    .set("fileContents.content." + catagory + "." + getCurMonth() + ".lastUpdated", new Date())
                    .push("fileContents.content." + catagory + "." + getCurMonth() + ".contentIds", contentId);

            mongoTemplate.upsert(query, update, User.class);
            // User user = userRepository.findFirstByName(userName);
            // TODO: Send to Kafka for async processing
            // kafkaTemplate.send("file-processing", data.getKey(), objectMapper.writeValueAsString(data));
            return ResponseEntity.ok(UserContentOut.of(fileContent));
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

//        if (data.getKey() != null) {
//            String userName = data.getUserName();
//
//            if ( userName != null) {
//                incrementMongoField(userName, "totalTextContents");
//            }
//        }
//        else {
//            throw new RuntimeException(key + " not found");
//        }

        return ResponseEntity.ok(data);
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