package com.curapaste.services;


import com.curapaste.entities.Paste;
import com.curapaste.repository.PasteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasteService {

    private static final int MAX_ID_RETRIES = 3;
    private final PasteRepository pasteRepository;


    private final IdGeneratorService idGenerator;

    public PasteService(PasteRepository repo, IdGeneratorService idGenerator){
        this.pasteRepository = repo;
        this.idGenerator = idGenerator;
    }

    public Paste createPaste(String content){
        Paste p = new Paste();
        p.setContent(content);
        p.setShortId(generateUniqueId());
        return pasteRepository.save(p);
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

    public Paste getPaste(String shortId){
        return pasteRepository.findByShortId(shortId).orElseThrow(
                ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paste not found!")
        );
    }
}
