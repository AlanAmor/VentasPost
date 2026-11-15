package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.ComprobanteFiscalArca;
import com.ventaspost.domain.model.EntornoArca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ComprobanteFiscalArcaRepository extends JpaRepository<ComprobanteFiscalArca, Long> {

    List<ComprobanteFiscalArca> findByVentaIdOrderByIdAsc(Long ventaId);

    Optional<ComprobanteFiscalArca> findFirstByVentaIdAndNaturalezaComprobanteOrderByIdDesc(Long ventaId, String naturalezaComprobante);

    List<ComprobanteFiscalArca> findBySucursalIdOrderByIdDesc(Long sucursalId);

    List<ComprobanteFiscalArca> findAllByOrderByIdDesc();

    @Query("""
            SELECT COALESCE(MAX(c.numeroComprobante), 0)
            FROM ComprobanteFiscalArca c
            WHERE c.cuitEmisor = :cuitEmisor
              AND c.puntoVenta = :puntoVenta
              AND c.codigoAfipComprobante = :codigoAfipComprobante
              AND c.entornoArca = :entornoArca
            """)
    Long obtenerUltimoNumeroEmitidoLocal(
            @Param("cuitEmisor") String cuitEmisor,
            @Param("puntoVenta") Integer puntoVenta,
            @Param("codigoAfipComprobante") Integer codigoAfipComprobante,
            @Param("entornoArca") EntornoArca entornoArca
    );
}
