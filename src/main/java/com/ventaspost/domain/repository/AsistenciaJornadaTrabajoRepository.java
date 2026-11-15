package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.AsistenciaJornadaTrabajo;
import com.ventaspost.domain.model.EstadoJornadaTrabajo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaJornadaTrabajoRepository extends JpaRepository<AsistenciaJornadaTrabajo, Long> {

    @EntityGraph(attributePaths = {"usuario", "sucursal", "liquidacion"})
    List<AsistenciaJornadaTrabajo> findByUsuarioIdAndFechaJornadaBetweenOrderByFechaJornadaAsc(
            Long usuarioId,
            LocalDate desde,
            LocalDate hasta
    );

    @EntityGraph(attributePaths = {"usuario", "sucursal", "liquidacion"})
    List<AsistenciaJornadaTrabajo> findByFechaJornadaBetweenOrderByFechaJornadaDescIdDesc(
            LocalDate desde,
            LocalDate hasta
    );

    @EntityGraph(attributePaths = {"usuario", "sucursal", "liquidacion"})
    List<AsistenciaJornadaTrabajo> findBySucursalIdAndFechaJornadaBetweenOrderByFechaJornadaDescIdDesc(
            Long sucursalId,
            LocalDate desde,
            LocalDate hasta
    );

    @EntityGraph(attributePaths = {"usuario", "sucursal", "liquidacion"})
    List<AsistenciaJornadaTrabajo> findByLiquidacionIdOrderByFechaJornadaAsc(Long liquidacionId);

    @EntityGraph(attributePaths = {"usuario", "sucursal"})
    Optional<AsistenciaJornadaTrabajo> findFirstByUsuarioIdAndEstadoJornadaOrderByIdDesc(
            Long usuarioId,
            EstadoJornadaTrabajo estadoJornada
    );

    boolean existsByUsuarioIdAndFechaJornadaAndActivoTrue(Long usuarioId, LocalDate fechaJornada);
}
