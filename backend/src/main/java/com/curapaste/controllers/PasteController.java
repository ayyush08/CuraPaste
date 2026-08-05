package com.curapaste.controllers;


import com.curapaste.dto.CreatePasteRequest;
import com.curapaste.dto.PasteResponse;
import com.curapaste.entities.Paste;
import com.curapaste.services.PasteService;
import jakarta.validation.Valid;
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
    public ResponseEntity<PasteResponse> create(@Valid  @RequestBody CreatePasteRequest requestBody){
        return ResponseEntity.status(HttpStatus.CREATED).body(
                pasteService.createPaste(requestBody)
        );
    }

    @GetMapping("/{shortId}")
    public ResponseEntity<PasteResponse> getPaste(@PathVariable String shortId) {
        return ResponseEntity.ok(
                pasteService.getPaste(shortId)
        );
    }
}
