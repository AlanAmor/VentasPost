package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.IntentoFacturacionArca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntentoFacturacionArcaRepository extends JpaRepository<IntentoFacturacionArca, Long> {

    List<IntentoFacturacionArca> findByVentaIdOrderByFechaIntentoDesc(Long ventaId);
}
