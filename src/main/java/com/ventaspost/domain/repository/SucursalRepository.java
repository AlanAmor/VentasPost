package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SucursalRepository extends JpaRepository<Sucursal, Long> {
    List<Sucursal> findAllByOrderByCodigoAsc();
    List<Sucursal> findByActivoTrueOrderByCodigoAsc();
    Optional<Sucursal> findByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    // Regla 5.1: Unicidad fiscal sobre (cuit, punto_venta_arca)
    boolean existsByCuitAndPuntoVentaArca(String cuit, Integer puntoVentaArca);
    boolean existsByCuitAndPuntoVentaArcaAndIdNot(String cuit, Integer puntoVentaArca, Long id);
}
