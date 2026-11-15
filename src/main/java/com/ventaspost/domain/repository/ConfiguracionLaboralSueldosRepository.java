package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.ConfiguracionLaboralSueldos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionLaboralSueldosRepository extends JpaRepository<ConfiguracionLaboralSueldos, Long> {

    Optional<ConfiguracionLaboralSueldos> findFirstByOrderByIdAsc();
}
