package com.curapaste.services.storage;


import com.curapaste.config.StorageProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Service
public class S3ContentStorageService implements ContentStorageService{
    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    public S3ContentStorageService(
            S3Client s3Client,
            StorageProperties storageProperties
    ) {
        this.s3Client = s3Client;
        this.storageProperties = storageProperties;
    }

    @Override
    public String store(String shortId,String content) {
        String key = buildKey(shortId);

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(storageProperties.getBucket())
                        .key(key)
                        .contentType("text/plain")
                        .build(),
                RequestBody.fromString(content, StandardCharsets.UTF_8)
        );

        return key;
    }

    @Override
    public String fetch(String location) {
       return s3Client.getObjectAsBytes(
               GetObjectRequest.builder()
                       .bucket(storageProperties.getBucket())
                       .key(location)
                       .build()
       ).asUtf8String();
    }

    @Override
    public void delete(String location) {

        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(storageProperties.getBucket())
                        .key(location)
                        .build()
        );
    }
    private String buildKey(String shortId) {

        LocalDate today = LocalDate.now();

        return "%d/%02d/%02d/%s.txt".formatted(
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                shortId
        );
    }

}
