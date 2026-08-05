package com.curapaste.controllers;


import com.curapaste.entities.Paste;
import com.curapaste.services.PasteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/pastes")
public class PasteController {

    private final PasteService pasteService;


    public PasteController(PasteService service) {
        this.pasteService = service;
    }

    @PostMapping
    public ResponseEntity<Paste> create(@RequestBody Map<String,String> body){
        return ResponseEntity.status(HttpStatus.CREATED).body(
                pasteService.createPaste(body.get("content"))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Paste> getPaste(@PathVariable String id){
        return ResponseEntity.status(HttpStatus.FOUND).body(pasteService.getPaste(id));
    }
}
