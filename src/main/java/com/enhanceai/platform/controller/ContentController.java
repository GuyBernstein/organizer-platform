package com.enhanceai.platform.controller;

import com.enhanceai.platform.model.RedisData;
import com.enhanceai.platform.util.Dates;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.enhanceai.platform.service.Redis;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/content")
@Api(tags = "Content Management API")
public class ContentController {
    private final Redis redis;
    private final ObjectMapper objectMapper;

    @Autowired
    public ContentController(Redis redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @ApiOperation(value = "Store text content", notes = "Stores text content with metadata in Redis")
    @RequestMapping(value = "/text", method = RequestMethod.POST)
    public ResponseEntity<RedisData> storeText(
            @ApiParam(value = "Text content to store", required = true)
            @RequestParam String content) throws JsonProcessingException {

        RedisData data = RedisData.RedisDataBuilder.aRedisData()
                .key("text:" + System.currentTimeMillis())
                .value(content)
                .createdAt(Dates.nowUTC())
                .build();

        if (redis.set(data.getKey(), objectMapper.writeValueAsString(data))) {
            return ResponseEntity.ok(data);
        }
        return ResponseEntity.badRequest().build();
    }

    @ApiOperation(value = "Retrieve content by key", notes = "Gets content and metadata from Redis")
    @RequestMapping(value = "/{key}", method = RequestMethod.GET)
    public ResponseEntity<RedisData> getContent(
            @ApiParam(value = "Content key", required = true)
            @PathVariable String key) throws JsonProcessingException {

        Object value = redis.get(key);
        if (value != null) {
            RedisData data = objectMapper.readValue(value.toString(), RedisData.class);
            return ResponseEntity.ok(data);
        }
        return ResponseEntity.notFound().build();
    }

    @ApiOperation(value = "Store file metadata", notes = "Stores file metadata in Redis and prepares for async processing")
    @PostMapping("/file")
    public ResponseEntity<RedisData> storeFile(
            @ApiParam(value = "File to process", required = true)
            @RequestParam MultipartFile file) throws JsonProcessingException {

        // Create metadata entry
        RedisData data = RedisData.RedisDataBuilder.aRedisData()
                .key("file:" + System.currentTimeMillis())
                .value(String.format("{\"filename\":\"%s\",\"size\":%d,\"contentType\":\"%s\"}",
                        file.getOriginalFilename(),
                        file.getSize(),
                        file.getContentType()))
                .createdAt(Dates.nowUTC())
                .build();

        if (redis.set(data.getKey(), objectMapper.writeValueAsString(data))) {
            // TODO: Send to Kafka for async processing
            // kafkaTemplate.send("file-processing", data.getKey(), objectMapper.writeValueAsString(data));
            return ResponseEntity.ok(data);
        }
        return ResponseEntity.badRequest().build();
    }

    @ApiOperation(value = "List all content keys", notes = "Gets all content keys stored in Redis")
    @GetMapping("/list")
    public ResponseEntity<String[]> listContent(
            @ApiParam(value = "Content type filter (text/file)")
            @RequestParam(required = false) String type) {
        // Note: This is a placeholder. Actual implementation would depend on your Redis key pattern
        // TODO: Implement key pattern matching based on your needs
        return ResponseEntity.ok(new String[]{"Sample implementation"});
    }
}