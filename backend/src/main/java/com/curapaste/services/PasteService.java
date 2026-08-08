package com.curapaste.services;


import com.curapaste.config.StorageProperties;
import com.curapaste.dto.CachedPaste;
import com.curapaste.dto.CreatePasteRequest;
import com.curapaste.dto.CreatePasteResponse;
import com.curapaste.dto.PasteResponse;
import com.curapaste.entities.Paste;
import com.curapaste.repository.PasteRepository;
import com.curapaste.services.storage.ContentStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
public class PasteService {

    private static final int MAX_ID_RETRIES = 3;
    private final PasteRepository pasteRepository;
    private final ContentStorageService contentStorageService;
    private final StorageProperties storageProperties;

    private final CacheService cacheService;

    private final IdGeneratorService idGenerator;

    private final PasswordEncoder encoder;

    public PasteService(PasteRepository repo,
                        IdGeneratorService idGenerator,
                        ContentStorageService contentStorageService,
                        StorageProperties storageProperties,
                        CacheService cacheService,
                        PasswordEncoder passwordEncoder) {
        this.pasteRepository = repo;
        this.idGenerator = idGenerator;
        this.contentStorageService = contentStorageService;
        this.storageProperties = storageProperties;
        this.cacheService = cacheService;
        this.encoder = passwordEncoder;
    }

    public CreatePasteResponse createPaste(CreatePasteRequest requestBody){
        String content = requestBody.getContent();
        Paste p = new Paste();
        p.setShortId(generateUniqueId());
        int size = content.getBytes(StandardCharsets.UTF_8).length;
        p.setSizeBytes(size);

        if(size > storageProperties.getInlineThresholdBytes()){
            p.setContentLocation(contentStorageService.store(p.getShortId(),content));
        }
        else{
            p.setContent(content);
        }

        p.setExpiresAt(requestBody.getExpiresInSeconds() != null
                ? Instant.now().plusSeconds(requestBody.getExpiresInSeconds()) : null);

        p.setBurnAfterRead(requestBody.isBurnAfterRead());


        if(requestBody.getPassword()!=null && !requestBody.getPassword().isBlank()){
            p.setPasswordHash(
                    encoder.encode(requestBody.getPassword())
            );

        }

        String rawDeleteToken = UUID.randomUUID().toString().replace("-","");

        p.setDeleteTokenHash(encoder.encode(rawDeleteToken));
        p = pasteRepository.save(p);


        CachedPaste cached = toCachedPaste(p);
        cacheService.set(cached);
        return new CreatePasteResponse(
                p.getShortId(),
                cached.getContent(),
                p.getCreatedAt(),
                rawDeleteToken
        );
    }

    private String generateUniqueId(){
        for(int attempt = 0; attempt < MAX_ID_RETRIES; attempt++){
            String id = idGenerator.generateId();
            if(!pasteRepository.existsByShortId(id)) return id;
        }
        // If you ever actually hit this, it means your traffic assumptions
        // are wrong by several orders of magnitude — treat it as a paging alert, not a retry-forever loop.
        throw new IllegalStateException("ID space contention — investigate traffic growth");
    }

    public PasteResponse getPaste(String shortId,String password){
        CachedPaste cached = cacheService.get(shortId)
                .orElseGet(() -> {

                    System.out.println("CACHE MISS -> Loading from database");

                    Paste paste = findAliveOrThrow(shortId);

                    CachedPaste cachedPaste = toCachedPaste(paste);
                    cacheService.set(cachedPaste);
                    return cachedPaste;
                });

        //Expired
        if (cached.getExpiresAt() != null
                && cached.getExpiresAt().isBefore(Instant.now())) {

            cacheService.evict(shortId);

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Paste is expired"
            );
        }

        if (cached.getPasswordHash() != null
                && (password == null
                || !encoder.matches(
                password,
                cached.getPasswordHash()
        ))) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Password required or incorrect"
            );
        }

        // Burn after read
        if (cached.isBurnAfterRead()) {

            int deleted =
                    pasteRepository.deleteAliveByShortId(shortId);

            if (deleted == 0) {
                throw new ResponseStatusException(
                        HttpStatus.GONE,
                        "Paste already consumed"
                );
            }

            cacheService.evict(shortId);

            return new PasteResponse(
                    cached.getShortId(),
                    cached.getContent(),
                    cached.getCreatedAt()
            );
        }

        return new PasteResponse(
                cached.getShortId(),
                cached.getContent(),
                cached.getCreatedAt()
        );
    }


    public void deletePaste(String shortId,String rawDeleteToken){
        Paste paste = findAliveOrThrow(shortId);

        if(rawDeleteToken==null || !encoder.matches(rawDeleteToken,paste.getDeleteTokenHash())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Invalid delete token");
        }

        pasteRepository.softDelete(paste.getShortId());
        cacheService.evict(shortId);
        //Delete content from storage if exists
        if(paste.getContentLocation()!=null){
            contentStorageService.delete(paste.getContentLocation());
        }



    }


    private Paste findAliveOrThrow(String shortId) {

        return pasteRepository.findAliveByShortId(shortId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Paste not found!"
                        )
                );
    }


    private CachedPaste toCachedPaste(Paste p) {

        String content;

        if (p.getContentLocation() == null) {
            content = p.getContent();
        } else {
            content = contentStorageService.fetch(
                    p.getContentLocation()
            );
        }

        return new CachedPaste(
                p.getShortId(),
                content,
                p.getCreatedAt(),
                p.getExpiresAt(),
                p.isBurnAfterRead(),
                p.getPasswordHash()
        );
    }
}
