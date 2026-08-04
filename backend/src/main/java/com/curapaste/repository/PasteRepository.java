package com.curapaste.repository;

import com.curapaste.entities.Paste;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasteRepository extends JpaRepository<Paste,Long> {
}
