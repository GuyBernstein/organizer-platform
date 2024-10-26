package com.enhanceai.platform.controller;

import com.enhanceai.platform.model.RedisData;
import com.enhanceai.platform.model.RedisDataIn;
import com.enhanceai.platform.model.User;
import com.enhanceai.platform.repository.UserRepository;
import com.enhanceai.platform.service.FileStorageService;
import com.enhanceai.platform.service.Redis;
import com.enhanceai.platform.util.Dates;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static com.enhanceai.platform.model.UserBuilder.anUser;
import static com.enhanceai.platform.util.Dates.getCurMonth;

@RestController
@RequestMapping("/api/content")
@Api(tags = "Content Management API")
public class ContentController {
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final Redis redis;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

    @Autowired
    public ContentController(UserRepository userRepository, MongoTemplate mongoTemplate, Redis redis, ObjectMapper objectMapper, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.fileStorageService = fileStorageService;
    }

    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public User createUser(@RequestParam String name) {
        User user = anUser().withName(name).build();
        user = userRepository.insert(user);
        return user;
    }

    @RequestMapping(value = "/user/{name}", method = RequestMethod.GET)
    public User getUser(@RequestParam String name) {
        User user = userRepository.findFirstByName(name);
        return user;
    }

    // Helper method for incrementing numeric fields
    private void incrementMongoField(String userName, String key){
        Query query = Query.query(Criteria.where("name").is(userName));
        Update update = new Update().inc(key, 1);
        mongoTemplate.updateFirst(query, update, "users");
    }

    private void pushMongoTextContent(String userName, String key, String text) {
        Query query = Query.query(Criteria.where("name").is(userName));
        Update update = new Update().push(key,text);
        mongoTemplate.updateFirst(query, update, "users");
    }

    @ApiOperation(value = "Store text content", notes = "Stores text content with metadata in Redis")
    @PostMapping(value = "/text")
    public ResponseEntity<RedisData> storeText(
            @ApiParam(value = "Text content to store", required = true)
            @RequestBody RedisDataIn content) throws JsonProcessingException {

        if(content.getUserName().isEmpty() || content.getText().isEmpty() || content.getType().isEmpty())
            throw new RuntimeException("Please provide username, text, and the type of the text");

        RedisData data = RedisData.RedisDataBuilder.aRedisData()
                .key("text-" + System.currentTimeMillis())
                .userName(content.getUserName())
                .value(content.getText())
                .createdAt(Dates.nowUTC())
                .catagory(content.getType())
                .build();

        if (redis.set(data.getKey(), objectMapper.writeValueAsString(data))) {
            incrementMongoField(content.getUserName(), "totalTextContents");
            incrementMongoField(content.getUserName(),
                    "textContents."  + data.getCatagory() + ".texts." + getCurMonth());
            pushMongoTextContent(content.getUserName(),
                    "textContents." + data.getCatagory() + ".contents", content.getText());
            return ResponseEntity.ok(data);
        }
        return ResponseEntity.badRequest().build();
    }

    @ApiOperation(value = "Retrieve content by key", notes = "Gets content and metadata from Redis")
    @GetMapping(value = "/{key}")
    public ResponseEntity<RedisData> getContent(
            @ApiParam(value = "Content key", required = true)
            @PathVariable String key) throws JsonProcessingException {

        // Check if key exists
        if (!redis.hasKey(key)) {
            return ResponseEntity.notFound().build();
        }

        Object value = redis.get(key);
        RedisData data = objectMapper.readValue(value.toString(), RedisData.class);

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

    @ApiOperation(value = "Store file metadata", notes = "Stores file metadata in Redis and prepares for async processing")
    @PostMapping("/file")
    public ResponseEntity<RedisData> storeFile(
            @ApiParam(value = "File to process", required = true)
            @RequestParam MultipartFile file,
            @RequestParam String userName,
            @RequestParam String catagory) throws IOException {

        if(userName.isEmpty() || file.isEmpty() || catagory.isEmpty())
            throw new RuntimeException("Please provide username, a file and a file catagory");

        String key = "file-" + System.currentTimeMillis();
        String filePath = fileStorageService.storeFileInFilesystem(file,key);

        RedisData data = RedisData.RedisDataBuilder.aRedisData()
                .key(key)
                .value(String.format("{filename: %s, size: %d, contentType: %s,storagePath: %s}",
                        file.getOriginalFilename(),
                        file.getSize(),
                        file.getContentType(),filePath
                        ))
                .catagory(catagory)
                .createdAt(Dates.nowUTC())
                .build();

        if (redis.set(data.getKey(), objectMapper.writeValueAsString(data))) {
            incrementMongoField(userName, "totalFiles");
            incrementMongoField(userName,
                    "fileContents."  + data.getCatagory() + ".files." + getCurMonth());
            pushMongoTextContent(userName,
                    "fileContents." + data.getCatagory() + ".contents", filePath);

            // User user = userRepository.findFirstByName(userName);
            // TODO: Send to Kafka for async processing
            // kafkaTemplate.send("file-processing", data.getKey(), objectMapper.writeValueAsString(data));
            return ResponseEntity.ok(data);
        }
        return ResponseEntity.badRequest().build();
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