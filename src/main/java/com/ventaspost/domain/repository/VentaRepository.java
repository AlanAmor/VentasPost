package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.Venta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @EntityGraph(attributePaths = {"sucursal", "turnoCaja", "usuarioCajero"})
    List<Venta> findBySucursalIdOrderByFechaVentaDesc(Long sucursalId);

    @EntityGraph(attributePaths = {"sucursal", "turnoCaja", "usuarioCajero"})
    List<Venta> findByTurnoCajaIdOrderByFechaVentaDesc(Long turnoCajaId);

    @EntityGraph(attributePaths = {"sucursal", "turnoCaja", "usuarioCajero"})
    List<Venta> findAllByOrderByFechaVentaDesc();

    @EntityGraph(attributePaths = {"sucursal", "turnoCaja", "usuarioCajero"})
    List<Venta> findByFechaVentaBetweenOrderByFechaVentaAsc(LocalDateTime desde, LocalDateTime hasta);

    @EntityGraph(attributePaths = {"sucursal", "turnoCaja", "usuarioCajero"})
    List<Venta> findBySucursalIdAndFechaVentaBetweenOrderByFechaVentaAsc(
            Long sucursalId,
            LocalDateTime desde,
            LocalDateTime hasta
    );
}
