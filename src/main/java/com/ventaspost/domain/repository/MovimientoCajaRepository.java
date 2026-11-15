package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.MovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {
    List<MovimientoCaja> findByTurnoCajaIdOrderByFechaMovimientoDesc(Long turnoCajaId);
}
