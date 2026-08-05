package com.curapaste.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "pastes")
@Builder
public class Paste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;


    @Column(name = "short_id", nullable = false, unique = true, length = 12)
    private String shortId;


    private Instant createdAt;



    @Column(length = 512)
    private String contentLocation;

    @Column(nullable = false)
    private Integer sizeBytes;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

}
