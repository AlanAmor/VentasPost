package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.CodigoMedioPago;
import com.ventaspost.domain.model.ConfiguracionMedioPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfiguracionMedioPagoRepository extends JpaRepository<ConfiguracionMedioPago, Long> {
    List<ConfiguracionMedioPago> findAllByOrderByIdAsc();
    List<ConfiguracionMedioPago> findByActivoTrueOrderByIdAsc();
    Optional<ConfiguracionMedioPago> findByCodigo(CodigoMedioPago codigo);
}
