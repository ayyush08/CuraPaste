package com.curapaste.services;


import com.curapaste.entities.Paste;
import com.curapaste.repository.PasteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasteService {


    private final PasteRepository pasteRepository;

    public PasteService(PasteRepository repo){
        this.pasteRepository = repo;
    }

    public Paste createPaste(String content){
        Paste p = new Paste();
        p.setContent(content);
        return pasteRepository.save(p);
    }

    public Paste getPaste(Long id){
        return pasteRepository.findById(id).orElseThrow(
                ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paste not found!")
        );
    }
}
