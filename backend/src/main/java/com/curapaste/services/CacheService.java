package com.curapaste.services;


import com.curapaste.dto.CachedPaste;
import com.curapaste.dto.PasteResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class CacheService {

    private final RedisTemplate<String,String> redisTemplate;
    private final ObjectMapper objectMapper;

    public CacheService(RedisTemplate<String,String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }


    public void set(CachedPaste paste){
       try{
           redisTemplate.opsForValue().set(
                   cacheKey(paste.getShortId()),
                   toJson(paste),
                   computeTtl(paste.getExpiresAt())
           );
       }
       catch (Exception e){
           System.out.println("CACHE SET ERROR"+e);
       }
    }

    public Optional<CachedPaste> get(String shortId){
        try {
            String json = redisTemplate.opsForValue().get(cacheKey(shortId));
            return json == null
                    ? Optional.empty()
                    : Optional.of(fromJson(json));
        } catch (Exception e) {
//            log.warn("Redis unavailable", e);
            System.out.println("CACHE GET ERROR: "+e);
            return Optional.empty();
        }
    }


    public void evict(String shortId){
        try {
            redisTemplate.delete(cacheKey(shortId));
        } catch (Exception e) {
            System.out.println("CACHE EVICT ERROR: " + e);
        }
    }


    private String cacheKey(String shortId) {
        return "paste:" + shortId;
    }

    private String toJson(CachedPaste paste) {
        try {
            return objectMapper.writeValueAsString(paste);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize paste", e);
        }
    }

    private CachedPaste fromJson(String json) {
        try {
            return objectMapper.readValue(json, CachedPaste.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize paste", e);
        }
    }

    private Duration computeTtl(Instant expiresAt) {

        // Paste doesn't expire.
        // Still don't keep it in Redis forever.
        if (expiresAt == null) {
            return Duration.ofHours(1);
        }

        Duration untilExpiry =
                Duration.between(Instant.now(), expiresAt);

        // Already expired.
        if (untilExpiry.isNegative()
                || untilExpiry.isZero()) {

            return Duration.ofSeconds(1);
        }

        // Never cache longer than 1 hour.
        return untilExpiry.compareTo(Duration.ofHours(1)) > 0
                ? Duration.ofHours(1)
                : untilExpiry;
    }
}
