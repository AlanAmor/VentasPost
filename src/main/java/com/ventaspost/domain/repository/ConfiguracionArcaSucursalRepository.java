package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.ConfiguracionArcaSucursal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfiguracionArcaSucursalRepository extends JpaRepository<ConfiguracionArcaSucursal, Long> {

    Optional<ConfiguracionArcaSucursal> findBySucursalId(Long sucursalId);

    List<ConfiguracionArcaSucursal> findAllByOrderBySucursalIdAsc();
}
