package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @EntityGraph(attributePaths = {"categoria", "marca", "proveedor"})
    List<Producto> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = {"categoria", "marca", "proveedor"})
    List<Producto> findByActivoTrueOrderByNombreAsc();

    @EntityGraph(attributePaths = {"categoria", "marca", "proveedor"})
    Optional<Producto> findByCodigoBarrasIgnoreCase(String codigoBarras);

    boolean existsByCodigoBarrasIgnoreCase(String codigoBarras);
    boolean existsByCodigoBarrasIgnoreCaseAndIdNot(String codigoBarras, Long id);
}
