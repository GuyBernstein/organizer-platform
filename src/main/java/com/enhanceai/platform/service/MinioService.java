package com.enhanceai.platform.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MinioService {

    @Value("${spring.minio.bucket}")
    private String bucketName;

    @Autowired
    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            log.error("Error initializing MinIO bucket", e);
        }
    }

    public void uploadFile(byte[] content, String objectName, String contentType) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(content);

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(bais, content.length, -1)
                        .contentType(contentType)
                        .build());
    }

    public byte[] downloadFile(String objectName) throws Exception {
        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build())) {
            return IOUtils.toByteArray(response);
        }
    }

    public String getPresignedUrl(String objectName, int expiryMinutes) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(expiryMinutes, TimeUnit.MINUTES)
                        .build());
    }
}
