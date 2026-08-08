package com.curapaste.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class CreatePasteRequest {
    @NotBlank(message = "Content cannot be empty")
    private String content;

    private Long expiresInSeconds;

    private boolean burnAfterRead = false;

    private String password;
}
