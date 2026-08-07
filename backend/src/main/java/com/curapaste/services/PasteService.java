package com.curapaste.services;


import com.curapaste.config.StorageProperties;
import com.curapaste.dto.CreatePasteRequest;
import com.curapaste.dto.PasteResponse;
import com.curapaste.entities.Paste;
import com.curapaste.repository.PasteRepository;
import com.curapaste.services.storage.ContentStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@Service
public class PasteService {

    private static final int MAX_ID_RETRIES = 3;
    private final PasteRepository pasteRepository;
    private final ContentStorageService contentStorageService;
    private final StorageProperties storageProperties;

    private final CacheService cacheService;

    private final IdGeneratorService idGenerator;

    public PasteService(PasteRepository repo,
                        IdGeneratorService idGenerator,
                        ContentStorageService contentStorageService,
                        StorageProperties storageProperties,
                        CacheService cacheService) {
        this.pasteRepository = repo;
        this.idGenerator = idGenerator;
        this.contentStorageService = contentStorageService;
        this.storageProperties = storageProperties;
        this.cacheService = cacheService;
    }

    public PasteResponse createPaste(CreatePasteRequest requestBody){
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

        p = pasteRepository.save(p);

        PasteResponse pasteResponse = toResponse(p);

        cacheService.set(pasteResponse);
        return pasteResponse;
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

    public PasteResponse getPaste(String shortId){


        return cacheService.get(shortId).orElseGet(()->{
            System.out.println("Cache MISS -> Loading from database");
            Paste p =  pasteRepository.findByShortId(shortId).orElseThrow(
                    ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paste not found!")
            );

            PasteResponse pasteResponse = toResponse(p);

            cacheService.set(pasteResponse);

            return pasteResponse;
        });
    }

    private PasteResponse toResponse(Paste p){
        String content;

        if(p.getContentLocation() == null){
            content = p.getContent();
        }
        else{
            content = contentStorageService.fetch(p.getContentLocation());
        }

        return new PasteResponse(
                p.getShortId(),
                content,
                p.getCreatedAt()
        );
    }
}
