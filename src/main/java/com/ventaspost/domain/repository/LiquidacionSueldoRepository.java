package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.LiquidacionSueldo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LiquidacionSueldoRepository extends JpaRepository<LiquidacionSueldo, Long> {

    @EntityGraph(attributePaths = {"usuario", "sucursal", "conceptos"})
    List<LiquidacionSueldo> findByUsuarioIdOrderByFechaLiquidacionDescIdDesc(Long usuarioId);

    @EntityGraph(attributePaths = {"usuario", "sucursal", "conceptos"})
    List<LiquidacionSueldo> findBySucursalIdOrderByFechaLiquidacionDescIdDesc(Long sucursalId);

    @EntityGraph(attributePaths = {"usuario", "sucursal", "conceptos"})
    List<LiquidacionSueldo> findAllByOrderByFechaLiquidacionDescIdDesc();

    @EntityGraph(attributePaths = {"usuario", "sucursal"})
    Optional<LiquidacionSueldo> findFirstByUsuarioIdAndEstadoOrderByFechaHastaDescIdDesc(
            Long usuarioId,
            String estado
    );
}
