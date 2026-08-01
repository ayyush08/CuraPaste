package com.curapaste.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "pastes",
        indexes = {
                @Index(name = "idx_paste_expires_at",columnList = "expires_at")
        }
)
@Builder
public class Paste {

    @Id
    @Column(name = "id",nullable = false,length = 8)
    private String id;

    @Column(name = "created_at",nullable = false,updatable = false)
    private Instant createdAt;


    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "size_bytes",nullable = false)
    private long sizeBytes;

    @Column(name = "delete_token_hash",nullable = false,length = 64,unique = true)
    private String deleteTokenHash;
}
