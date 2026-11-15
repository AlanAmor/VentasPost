package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.EstadoTurnoCaja;
import com.ventaspost.domain.model.TurnoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Long> {
    Optional<TurnoCaja> findFirstBySucursalIdAndUsuarioIdAndEstadoOrderByFechaAperturaDesc(
            Long sucursalId, Long usuarioId, EstadoTurnoCaja estado);

    List<TurnoCaja> findBySucursalIdOrderByFechaAperturaDesc(Long sucursalId);
    List<TurnoCaja> findAllByOrderByFechaAperturaDesc();
}
