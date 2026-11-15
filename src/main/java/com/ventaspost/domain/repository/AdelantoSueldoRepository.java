package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.AdelantoSueldo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdelantoSueldoRepository extends JpaRepository<AdelantoSueldo, Long> {

    @EntityGraph(attributePaths = {"usuario", "sucursal", "liquidacion"})
    List<AdelantoSueldo> findByUsuarioIdAndDescontadoFalseAndActivoTrueOrderByFechaAdelantoAsc(Long usuarioId);

    @EntityGraph(attributePaths = {"usuario", "sucursal", "liquidacion"})
    List<AdelantoSueldo> findByUsuarioIdOrderByFechaAdelantoDescIdDesc(Long usuarioId);

    @EntityGraph(attributePaths = {"usuario", "sucursal", "liquidacion"})
    List<AdelantoSueldo> findAllByOrderByFechaAdelantoDescIdDesc();
}
