package com.curapaste.services;


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


    public void set(PasteResponse pasteResponse){
       try{
           redisTemplate.opsForValue().set(
                   cacheKey(pasteResponse.getShortId()),
                   toJson(pasteResponse),
                   Duration.ofHours(1) //add expiresAt in future here
           );
       }
       catch (Exception e){
           System.out.println("CACHE SET ERROR"+e);
       }
    }

    public Optional<PasteResponse> get(String shortId){
        try {
            String json = redisTemplate.opsForValue().get(cacheKey(shortId));
            return json == null ? Optional.empty() : Optional.of(fromJson(json));
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

    private String toJson(PasteResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize paste", e);
        }
    }

    private PasteResponse fromJson(String json) {
        try {
            return objectMapper.readValue(json, PasteResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize paste", e);
        }
    }

//    private Duration computeTtl(Instant expiresAt) {
//        if (expiresAt == null) return Duration.ofHours(1); // cap even "forever" pastes at 1h cache freshness
//        Duration untilExpiry = Duration.between(Instant.now(), expiresAt);
//        return untilExpiry.compareTo(Duration.ofHours(1)) > 0 ? Duration.ofHours(1) : untilExpiry;
//    }
}
