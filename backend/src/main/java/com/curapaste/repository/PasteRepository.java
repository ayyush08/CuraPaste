package com.curapaste.repository;

import com.curapaste.entities.Paste;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasteRepository extends JpaRepository<Paste,Long> {
    Optional<Paste> findByShortId(String shortId);

    boolean existsByShortId(String shortId);
}
