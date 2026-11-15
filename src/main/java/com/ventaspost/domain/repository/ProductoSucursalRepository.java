package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.ProductoSucursal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoSucursalRepository extends JpaRepository<ProductoSucursal, Long> {
    List<ProductoSucursal> findByProductoIdOrderBySucursalCodigoAsc(Long productoId);
    List<ProductoSucursal> findByProductoId(Long productoId);
    List<ProductoSucursal> findBySucursalId(Long sucursalId);
    Optional<ProductoSucursal> findByProductoIdAndSucursalId(Long productoId, Long sucursalId);
}
