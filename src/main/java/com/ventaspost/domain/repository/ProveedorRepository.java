package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    List<Proveedor> findAllByOrderByEsProveedorSistemaDescRazonSocialAsc();
    List<Proveedor> findByActivoTrueOrderByEsProveedorSistemaDescRazonSocialAsc();
    Optional<Proveedor> findByEsProveedorSistemaTrue();
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);
    boolean existsByCuit(String cuit);
    boolean existsByCuitAndIdNot(String cuit, Long id);
}
