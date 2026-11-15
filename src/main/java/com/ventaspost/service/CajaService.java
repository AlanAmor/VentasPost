package com.ventaspost.service;

import com.ventaspost.common.exception.BusinessException;
import com.ventaspost.common.exception.ResourceNotFoundException;
import com.ventaspost.domain.model.*;
import com.ventaspost.domain.repository.MovimientoCajaRepository;
import com.ventaspost.domain.repository.SucursalRepository;
import com.ventaspost.domain.repository.TurnoCajaRepository;
import com.ventaspost.domain.repository.UsuarioRepository;
import com.ventaspost.security.CustomUserDetails;
import com.ventaspost.web.dto.caja.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CajaService {

    private final TurnoCajaRepository turnoCajaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Optional<TurnoCajaResponseDto> obtenerTurnoAbiertoActual(Long sucursalId, Long usuarioId) {
        if (sucursalId == null || usuarioId == null) {
            return Optional.empty();
        }
        return turnoCajaRepository
                .findFirstBySucursalIdAndUsuarioIdAndEstadoOrderByFechaAperturaDesc(
                        sucursalId, usuarioId, EstadoTurnoCaja.ABIERTO
                )
                .map(this::toResponseDto);
    }

    @Transactional(readOnly = true)
    public List<TurnoCajaResponseDto> listarTurnos(Long sucursalContextoId, CustomUserDetails operador) {
        boolean puedeAuditar = operador.tieneRol(CodigoRol.ADMIN)
                || operador.tieneRol(CodigoRol.ENCARGADO_SUCURSAL)
                || operador.tienePermiso(PermisoGranular.AUDITAR_CAJAS_SUCURSAL);

        List<TurnoCaja> turnos = (sucursalContextoId != null)
                ? turnoCajaRepository.findBySucursalIdOrderByFechaAperturaDesc(sucursalContextoId)
                : turnoCajaRepository.findAllByOrderByFechaAperturaDesc();

        return turnos.stream()
                .filter(t -> puedeAuditar || t.getUsuario().getId().equals(operador.getUsuarioId()))
                .limit(50)
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public TurnoCajaResponseDto abrirTurno(
            AperturaCajaRequestDto dto,
            Long sucursalActivaId,
            CustomUserDetails operador) {

        validarPermisoOperarCaja(operador);
        if (sucursalActivaId == null) {
            throw new BusinessException(
                    "Debe seleccionar una sucursal específica antes de abrir un turno de caja (no disponible en Vista Consolidada)."
            );
        }

        Optional<TurnoCaja> abiertoExistente = turnoCajaRepository
                .findFirstBySucursalIdAndUsuarioIdAndEstadoOrderByFechaAperturaDesc(
                        sucursalActivaId, operador.getUsuarioId(), EstadoTurnoCaja.ABIERTO
                );

        if (abiertoExistente.isPresent()) {
            throw new BusinessException("Ya posee un turno de caja abierto en esta sucursal.", HttpStatus.CONFLICT);
        }

        Sucursal sucursal = sucursalRepository.findById(sucursalActivaId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + sucursalActivaId));
        Usuario usuario = usuarioRepository.findById(operador.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));

        BigDecimal montoInicial = dto.montoInicialEfectivo().setScale(2, RoundingMode.HALF_UP);

        TurnoCaja turno = new TurnoCaja();
        turno.setSucursal(sucursal);
        turno.setUsuario(usuario);
        turno.setEstado(EstadoTurnoCaja.ABIERTO);
        turno.setFechaApertura(LocalDateTime.now());
        turno.setMontoInicialEfectivo(montoInicial);
        turno.setEfectivoEsperado(montoInicial);
        turno.setObservacionesApertura(limpiarTextoOpcional(dto.observacionesApertura()));
        turno.setActivo(true);

        return toResponseDto(turnoCajaRepository.save(turno));
    }

    @Transactional
    public TurnoCajaResponseDto registrarMovimientoManual(
            MovimientoCajaRequestDto dto,
            Long sucursalActivaId,
            CustomUserDetails operador) {

        validarPermisoOperarCaja(operador);
        TurnoCaja turno = obtenerEntidadTurnoAbiertoRequerido(sucursalActivaId, operador.getUsuarioId());
        Usuario usuario = usuarioRepository.findById(operador.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));

        BigDecimal monto = dto.monto().setScale(2, RoundingMode.HALF_UP);

        if (dto.tipo() == TipoMovimientoCaja.EGRESO) {
            BigDecimal disponibleActual = turno.recalcularEfectivoEsperado();
            if (monto.compareTo(disponibleActual) > 0) {
                throw new BusinessException(
                        "El monto del retiro ($" + monto + ") supera el efectivo disponible en caja ($" + disponibleActual + ")."
                );
            }
            turno.setTotalEgresosManuales(turno.getTotalEgresosManuales().add(monto));
        } else {
            turno.setTotalIngresosManuales(turno.getTotalIngresosManuales().add(monto));
        }

        turno.recalcularEfectivoEsperado();
        turnoCajaRepository.save(turno);

        MovimientoCaja mov = new MovimientoCaja();
        mov.setTurnoCaja(turno);
        mov.setSucursal(turno.getSucursal());
        mov.setUsuario(usuario);
        mov.setTipo(dto.tipo());
        mov.setMonto(monto);
        mov.setMotivo(dto.motivo().trim());
        mov.setFechaMovimiento(LocalDateTime.now());
        mov.setActivo(true);
        movimientoCajaRepository.save(mov);

        return toResponseDto(turno);
    }

    /**
     * Regla 4.4 A: Cierre y Arqueo de Caja con comparación entre efectivo esperado y efectivo físico contado.
     */
    @Transactional
    public TurnoCajaResponseDto cerrarTurno(
            CierreCajaRequestDto dto,
            Long sucursalActivaId,
            CustomUserDetails operador) {

        validarPermisoOperarCaja(operador);
        TurnoCaja turno = obtenerEntidadTurnoAbiertoRequerido(sucursalActivaId, operador.getUsuarioId());

        BigDecimal esperado = turno.recalcularEfectivoEsperado();
        BigDecimal declarado = dto.efectivoDeclaradoCierre().setScale(2, RoundingMode.HALF_UP);
        BigDecimal diferencia = declarado.subtract(esperado).setScale(2, RoundingMode.HALF_UP);

        turno.setEfectivoDeclaradoCierre(declarado);
        turno.setDiferenciaEfectivo(diferencia);
        turno.setObservacionesCierre(limpiarTextoOpcional(dto.observacionesCierre()));
        turno.setFechaCierre(LocalDateTime.now());
        turno.setEstado(EstadoTurnoCaja.CERRADO);

        return toResponseDto(turnoCajaRepository.save(turno));
    }

    public TurnoCaja obtenerEntidadTurnoAbiertoRequerido(Long sucursalActivaId, Long usuarioId) {
        if (sucursalActivaId == null) {
            throw new BusinessException(
                    "Debe seleccionar una sucursal específica para operar en Caja / Punto de Venta."
            );
        }
        return turnoCajaRepository
                .findFirstBySucursalIdAndUsuarioIdAndEstadoOrderByFechaAperturaDesc(
                        sucursalActivaId, usuarioId, EstadoTurnoCaja.ABIERTO
                )
                .orElseThrow(() -> new BusinessException(
                        "Debe abrir su Turno de Caja en esta sucursal antes de realizar operaciones de venta o movimientos."
                ));
    }

    private void validarPermisoOperarCaja(CustomUserDetails operador) {
        if (!operador.tieneRol(CodigoRol.ADMIN)
                && !operador.tienePermiso(PermisoGranular.OPERAR_CAJA_PROPIA)
                && !operador.tienePermiso(PermisoGranular.AUDITAR_CAJAS_SUCURSAL)) {
            throw new BusinessException("No posee permisos para operar turnos de caja.", HttpStatus.FORBIDDEN);
        }
    }

    private String limpiarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private TurnoCajaResponseDto toResponseDto(TurnoCaja t) {
        List<MovimientoCajaResponseDto> movs = movimientoCajaRepository
                .findByTurnoCajaIdOrderByFechaMovimientoDesc(t.getId())
                .stream()
                .map(m -> new MovimientoCajaResponseDto(
                        m.getId(),
                        t.getId(),
                        m.getTipo(),
                        m.getTipo().getDescripcion(),
                        m.getMonto(),
                        m.getMotivo(),
                        m.getUsuario().getUsername(),
                        m.getFechaMovimiento()
                ))
                .toList();

        return new TurnoCajaResponseDto(
                t.getId(),
                t.getSucursal().getId(),
                t.getSucursal().getCodigo(),
                t.getSucursal().getNombre(),
                t.getUsuario().getId(),
                t.getUsuario().getUsername(),
                t.getUsuario().getNombre() + " " + t.getUsuario().getApellido(),
                t.getEstado(),
                t.getFechaApertura(),
                t.getFechaCierre(),
                t.getMontoInicialEfectivo(),
                t.getTotalVentasEfectivo(),
                t.getTotalVentasTransferencia(),
                t.getTotalVentasQr(),
                t.getTotalVentasDebito(),
                t.getTotalVentasCredito(),
                t.calcularTotalGeneralVentas(),
                t.getTotalIngresosManuales(),
                t.getTotalEgresosManuales(),
                t.recalcularEfectivoEsperado(),
                t.getEfectivoDeclaradoCierre(),
                t.getDiferenciaEfectivo(),
                t.getObservacionesApertura(),
                t.getObservacionesCierre(),
                movs
        );
    }

    public CajaService(TurnoCajaRepository turnoCajaRepository, MovimientoCajaRepository movimientoCajaRepository, SucursalRepository sucursalRepository, UsuarioRepository usuarioRepository) {
        this.turnoCajaRepository = turnoCajaRepository;
        this.movimientoCajaRepository = movimientoCajaRepository;
        this.sucursalRepository = sucursalRepository;
        this.usuarioRepository = usuarioRepository;
    }

}
