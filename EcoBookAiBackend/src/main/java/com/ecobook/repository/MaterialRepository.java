package com.ecobook.repository;

import com.ecobook.model.Material;
import com.ecobook.model.enums.StatusMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaterialRepository extends JpaRepository<Material, UUID> {
    List<Material> findByDoadorId(UUID doadorId);
    List<Material> findByStatus(StatusMaterial status);
    List<Material> findByCidadeAndBairro(String cidade, String bairro);
}
