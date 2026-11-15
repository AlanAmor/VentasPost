package com.ventaspost.service;

import com.ventaspost.common.exception.BusinessException;
import com.ventaspost.common.exception.ResourceNotFoundException;
import com.ventaspost.domain.model.*;
import com.ventaspost.domain.repository.*;
import com.ventaspost.security.CustomUserDetails;
import com.ventaspost.web.dto.venta.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class VentaPosService {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal TOLERANCIA_REDONDEO = new BigDecimal("0.05");

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final ProductoSucursalRepository productoSucursalRepository;
    private final ConfiguracionMedioPagoRepository medioPagoRepository;
    private final TurnoCajaRepository turnoCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajaService cajaService;
    private final ArcaFacturacionService arcaFacturacionService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<ConfiguracionMedioPagoResponseDto> listarMediosPago(boolean soloActivos) {
        List<ConfiguracionMedioPago> lista = soloActivos
                ? medioPagoRepository.findByActivoTrueOrderByIdAsc()
                : medioPagoRepository.findAllByOrderByIdAsc();
        return lista.stream()
                .map(m -> new ConfiguracionMedioPagoResponseDto(
                        m.getId(),
                        m.getCodigo(),
                        m.getNombre(),
                        m.getPorcentajeAjuste(),
                        m.isRequiereReferencia(),
                        m.isPermiteVuelto(),
                        m.isActivo()
                ))
                .toList();
    }

    @Transactional
    public ConfiguracionMedioPagoResponseDto actualizarMedioPago(Long id, ConfiguracionMedioPagoRequestDto dto) {
        ConfiguracionMedioPago medio = medioPagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medio de pago no encontrado: " + id));
        medio.setPorcentajeAjuste(dto.porcentajeAjuste().setScale(2, RoundingMode.HALF_UP));
        medio.setRequiereReferencia(Boolean.TRUE.equals(dto.requiereReferencia()));
        medio.setActivo(Boolean.TRUE.equals(dto.activo()));
        ConfiguracionMedioPago guardado = medioPagoRepository.save(medio);
        return new ConfiguracionMedioPagoResponseDto(
                guardado.getId(),
                guardado.getCodigo(),
                guardado.getNombre(),
                guardado.getPorcentajeAjuste(),
                guardado.isRequiereReferencia(),
                guardado.isPermiteVuelto(),
                guardado.isActivo()
        );
    }

    @Transactional(readOnly = true)
    public List<VentaResponseDto> listarVentas(
            Long sucursalContextoId,
            Long turnoCajaId,
            EstadoFacturacionVenta estadoFacturacion,
            CustomUserDetails operador) {

        List<Venta> ventas;
        if (turnoCajaId != null) {
            ventas = ventaRepository.findByTurnoCajaIdOrderByFechaVentaDesc(turnoCajaId);
        } else if (sucursalContextoId != null) {
            ventas = ventaRepository.findBySucursalIdOrderByFechaVentaDesc(sucursalContextoId);
        } else {
            ventas = ventaRepository.findAllByOrderByFechaVentaDesc();
        }

        boolean puedeVerTodasSucursal = operador.tieneRol(CodigoRol.ADMIN)
                || operador.tieneRol(CodigoRol.ENCARGADO_SUCURSAL)
                || operador.tienePermiso(PermisoGranular.VER_REPORTES_SUCURSAL)
                || operador.tienePermiso(PermisoGranular.AUDITAR_CAJAS_SUCURSAL);

        return ventas.stream()
                .filter(v -> puedeVerTodasSucursal || v.getUsuario().getId().equals(operador.getUsuarioId()))
                .filter(v -> estadoFacturacion == null || v.getEstadoFacturacion() == estadoFacturacion)
                .limit(100)
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public VentaResponseDto obtenerPorId(Long id) {
        Venta v = ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con ID: " + id));
        return toResponseDto(v);
    }

    @Transactional
    public VentaResponseDto registrarVenta(
            RegistrarVentaRequestDto dto,
            Long sucursalActivaId,
            CustomUserDetails operador) {

        // Regla 4.1 y 4.2 A: Debe existir un Turno de Caja Abierto del operador en la sucursal activa
        TurnoCaja turnoAbierto = cajaService.obtenerEntidadTurnoAbiertoRequerido(sucursalActivaId, operador.getUsuarioId());
        Sucursal sucursal = turnoAbierto.getSucursal();
        Usuario cajero = turnoAbierto.getUsuario();

        // Regla 2.2: Validar permiso o autorización de supervisor si hay descuentos manuales
        BigDecimal descGlobalPorc = dto.descuentoManualPorcentaje() != null
                ? dto.descuentoManualPorcentaje().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        boolean hayDescuentoManual = descGlobalPorc.compareTo(BigDecimal.ZERO) > 0
                || dto.items().stream().anyMatch(i -> i.descuentoItemPorcentaje() != null
                && i.descuentoItemPorcentaje().compareTo(BigDecimal.ZERO) > 0);

        String supervisorAutorizador = null;
        if (hayDescuentoManual) {
            supervisorAutorizador = validarAutorizacionPermisoPos(
                    operador,
                    sucursalActivaId,
                    PermisoGranular.APLICAR_DESCUENTOS_ESPECIALES,
                    dto.supervisorUsername(),
                    dto.supervisorPassword()
            );
        }

        Venta venta = new Venta();
        venta.setNumeroComprobanteInterno(generarNumeroComprobanteInterno(sucursal));
        venta.setSucursal(sucursal);
        venta.setTurnoCaja(turnoAbierto);
        venta.setUsuario(cajero);
        venta.setFechaVenta(LocalDateTime.now());
        venta.setClienteNombre(dto.clienteNombre() != null && !dto.clienteNombre().isBlank()
                ? dto.clienteNombre().trim()
                : "Consumidor Final");
        venta.setClienteDocumento(limpiarTextoOpcional(dto.clienteDocumento()));
        venta.setClienteCondicionIva(dto.clienteCondicionIva() != null
                ? dto.clienteCondicionIva()
                : CondicionIva.CONSUMIDOR_FINAL);
        venta.setEstadoVenta(EstadoVenta.REALIZADA);
        venta.setEstadoFacturacion(EstadoFacturacionVenta.NO_FACTURADA);
        venta.setSupervisorAutorizadorUsername(supervisorAutorizador);
        venta.setObservaciones(limpiarTextoOpcional(dto.observaciones()));
        venta.setActivo(true);

        BigDecimal subtotalItems = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // Procesar ítems y descontar stock en la sucursal (Regla 3.1 A: permite quedar en negativo registrando el alerta)
        for (VentaItemRequestDto itemReq : dto.items()) {
            Producto producto = productoRepository.findById(itemReq.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + itemReq.productoId()));
            if (!producto.isActivo()) {
                throw new BusinessException("El producto '" + producto.getNombre() + "' se encuentra inactivo.");
            }

            BigDecimal cantidad = itemReq.cantidad().setScale(3, RoundingMode.HALF_UP);
            if (!producto.getUnidadMedida().isPermiteDecimales() && cantidad.stripTrailingZeros().scale() > 0) {
                throw new BusinessException(
                        "El producto '" + producto.getNombre() + "' se vende por "
                                + producto.getUnidadMedida().getNombre() + " y no admite cantidades decimales."
                );
            }

            ProductoSucursal ps = productoSucursalRepository
                    .findByProductoIdAndSucursalId(producto.getId(), sucursalActivaId)
                    .orElseGet(() -> {
                        ProductoSucursal nuevoPs = new ProductoSucursal();
                        nuevoPs.setProducto(producto);
                        nuevoPs.setSucursal(sucursal);
                        nuevoPs.setStockActual(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
                        nuevoPs.setStockMinimo(producto.getStockMinimoDefecto());
                        return nuevoPs;
                    });

            BigDecimal precioUnitario = ps.obtenerPrecioVentaEfectivo().setScale(2, RoundingMode.HALF_UP);
            BigDecimal costoUnitarioConIva = producto.calcularCostoFinalConIva().setScale(2, RoundingMode.HALF_UP);
            BigDecimal costoTotalConIva = costoUnitarioConIva.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            BigDecimal brutoLinea = precioUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);

            BigDecimal descItemPorc = itemReq.descuentoItemPorcentaje() != null
                    ? itemReq.descuentoItemPorcentaje().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            BigDecimal descItemMonto = brutoLinea.multiply(descItemPorc)
                    .divide(CIEN, 2, RoundingMode.HALF_UP);
            BigDecimal subtotalNetoLinea = brutoLinea.subtract(descItemMonto).setScale(2, RoundingMode.HALF_UP);

            BigDecimal stockPrevio = ps.getStockActual();
            BigDecimal stockPosterior = stockPrevio.subtract(cantidad).setScale(3, RoundingMode.HALF_UP);
            boolean vendidoSinStock = stockPosterior.compareTo(BigDecimal.ZERO) < 0;

            ps.setStockActual(stockPosterior);
            productoSucursalRepository.save(ps);

            VentaItem vi = new VentaItem();
            vi.setVenta(venta);
            vi.setProducto(producto);
            vi.setCodigoBarrasHistorico(producto.getCodigoBarras());
            vi.setNombreProductoHistorico(producto.getNombre());
            vi.setUnidadMedida(producto.getUnidadMedida());
            vi.setCantidad(cantidad);
            vi.setPrecioUnitario(precioUnitario);
            vi.setCostoUnitarioConIvaHistorico(costoUnitarioConIva);
            vi.setAlicuotaIva(producto.getAlicuotaIva());
            vi.setDescuentoItemPorcentaje(descItemPorc);
            vi.setDescuentoItemMonto(descItemMonto);
            vi.setSubtotalNetoItem(subtotalNetoLinea);
            vi.setCostoTotalConIvaHistorico(costoTotalConIva);
            vi.setStockPrevioSucursal(stockPrevio);
            vi.setStockPosteriorSucursal(stockPosterior);
            vi.setVendidoSinStock(vendidoSinStock);
            vi.setActivo(true);

            venta.getItems().add(vi);
            subtotalItems = subtotalItems.add(subtotalNetoLinea);
        }

        BigDecimal descuentoManualMonto = subtotalItems.multiply(descGlobalPorc)
                .divide(CIEN, 2, RoundingMode.HALF_UP);
        BigDecimal netoAPagarBase = subtotalItems.subtract(descuentoManualMonto).setScale(2, RoundingMode.HALF_UP);

        // Regla 1.1 A y 2.1 A: Validar suma de pagos base y calcular recargos/descuentos por medio de pago
        BigDecimal sumaMontoBasePagos = dto.pagos().stream()
                .map(p -> p.montoBase().setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);

        if (sumaMontoBasePagos.subtract(netoAPagarBase).abs().compareTo(TOLERANCIA_REDONDEO) > 0) {
            throw new BusinessException(
                    "La suma de los montos base de pago ($" + sumaMontoBasePagos
                            + ") no coincide con el neto a cubrir del carrito ($" + netoAPagarBase + ")."
            );
        }

        BigDecimal totalAjusteMediosPago = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalFinalVenta = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (VentaPagoRequestDto pagoReq : dto.pagos()) {
            ConfiguracionMedioPago configMedio = medioPagoRepository.findByCodigo(pagoReq.medioPago())
                    .orElseThrow(() -> new BusinessException("Medio de pago no configurado: " + pagoReq.medioPago()));

            if (!configMedio.isActivo()) {
                throw new BusinessException("El medio de pago " + configMedio.getNombre() + " se encuentra deshabilitado.");
            }
            if (configMedio.isRequiereReferencia()
                    && (pagoReq.referenciaOperacion() == null || pagoReq.referenciaOperacion().isBlank())) {
                throw new BusinessException(
                        "El medio de pago " + configMedio.getNombre() + " requiere ingresar número de cupón/referencia."
                );
            }

            BigDecimal montoBase = pagoReq.montoBase().setScale(2, RoundingMode.HALF_UP);
            BigDecimal porcAjuste = configMedio.getPorcentajeAjuste().setScale(2, RoundingMode.HALF_UP);
            BigDecimal montoAjuste = montoBase.multiply(porcAjuste).divide(CIEN, 2, RoundingMode.HALF_UP);
            BigDecimal montoFinalCobrado = montoBase.add(montoAjuste).setScale(2, RoundingMode.HALF_UP);

            BigDecimal recibidoEfectivo = null;
            BigDecimal vueltoEfectivo = null;
            if (pagoReq.medioPago() == CodigoMedioPago.EFECTIVO) {
                if (pagoReq.montoRecibidoEfectivo() != null) {
                    recibidoEfectivo = pagoReq.montoRecibidoEfectivo().setScale(2, RoundingMode.HALF_UP);
                    if (recibidoEfectivo.compareTo(montoFinalCobrado) < 0) {
                        throw new BusinessException(
                                "El efectivo recibido ($" + recibidoEfectivo
                                        + ") es menor al monto en efectivo a cobrar ($" + montoFinalCobrado + ")."
                        );
                    }
                    vueltoEfectivo = recibidoEfectivo.subtract(montoFinalCobrado).setScale(2, RoundingMode.HALF_UP);
                } else {
                    recibidoEfectivo = montoFinalCobrado;
                    vueltoEfectivo = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                }
            }

            VentaPago vp = new VentaPago();
            vp.setVenta(venta);
            vp.setMedioPago(pagoReq.medioPago());
            vp.setMontoBase(montoBase);
            vp.setPorcentajeAjusteAplicado(porcAjuste);
            vp.setMontoAjuste(montoAjuste);
            vp.setMontoFinalCobrado(montoFinalCobrado);
            vp.setMontoRecibidoEfectivo(recibidoEfectivo);
            vp.setVueltoEfectivo(vueltoEfectivo);
            vp.setCuotas(pagoReq.cuotas() != null && pagoReq.cuotas() >= 1 ? pagoReq.cuotas() : 1);
            vp.setReferenciaOperacion(limpiarTextoOpcional(pagoReq.referenciaOperacion()));
            vp.setActivo(true);

            venta.getPagos().add(vp);
            totalAjusteMediosPago = totalAjusteMediosPago.add(montoAjuste);
            totalFinalVenta = totalFinalVenta.add(montoFinalCobrado);

            // Imputar el neto cobrado al Turno de Caja abierto
            imputarPagoATurnoCaja(turnoAbierto, pagoReq.medioPago(), montoFinalCobrado, true);
        }

        venta.setSubtotalItems(subtotalItems);
        venta.setDescuentoManualPorcentaje(descGlobalPorc);
        venta.setDescuentoManualMonto(descuentoManualMonto);
        venta.setAjusteMediosPagoMonto(totalAjusteMediosPago);
        venta.setTotalFinal(totalFinalVenta);

        turnoAbierto.recalcularEfectivoEsperado();
        turnoCajaRepository.save(turnoAbierto);

        Venta guardada = ventaRepository.save(venta);
        return toResponseDto(guardada);
    }

    /**
     * Regla 5.1: Anulación de Venta con reintegro automático de stock a la sucursal y reversa en Caja.
     */
    @Transactional
    public VentaResponseDto anularVenta(
            Long ventaId,
            AnularVentaRequestDto dto,
            CustomUserDetails operador) {

        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con ID: " + ventaId));

        if (venta.getEstadoVenta() == EstadoVenta.ANULADA) {
            throw new BusinessException("La venta ya se encuentra anulada.");
        }

        String autorizador = validarAutorizacionPermisoPos(
                operador,
                venta.getSucursal().getId(),
                PermisoGranular.ANULAR_VENTAS,
                dto.supervisorUsername(),
                dto.supervisorPassword()
        );

        // 1. Reintegrar unidades al stock de la sucursal
        for (VentaItem item : venta.getItems()) {
            productoSucursalRepository
                    .findByProductoIdAndSucursalId(item.getProducto().getId(), venta.getSucursal().getId())
                    .ifPresent(ps -> {
                        ps.setStockActual(ps.getStockActual().add(item.getCantidad()).setScale(3, RoundingMode.HALF_UP));
                        productoSucursalRepository.save(ps);
                    });
        }

        // 2. Descontar los pagos del turno de caja asociado
        TurnoCaja turno = venta.getTurnoCaja();
        for (VentaPago pago : venta.getPagos()) {
            imputarPagoATurnoCaja(turno, pago.getMedioPago(), pago.getMontoFinalCobrado(), false);
        }
        turno.recalcularEfectivoEsperado();
        turnoCajaRepository.save(turno);

        // 3. Si la venta ya estaba FACTURADA en ARCA, emitir automáticamente la Nota de Crédito Electrónica (Regla 4.1 A)
        String usuarioResponsableAnulacion = autorizador != null ? autorizador : operador.getUsername();
        if (venta.getEstadoFacturacion() == EstadoFacturacionVenta.FACTURADA) {
            arcaFacturacionService.emitirNotaCreditoPorAnulacion(venta, usuarioResponsableAnulacion);
        }

        // 4. Marcar venta como ANULADA
        venta.setEstadoVenta(EstadoVenta.ANULADA);
        venta.setMotivoAnulacion(dto.motivoAnulacion().trim());
        venta.setFechaAnulacion(LocalDateTime.now());
        venta.setUsuarioAnulacionUsername(usuarioResponsableAnulacion);

        return toResponseDto(ventaRepository.save(venta));
    }

    private void imputarPagoATurnoCaja(TurnoCaja turno, CodigoMedioPago medio, BigDecimal monto, boolean sumar) {
        BigDecimal delta = sumar ? monto : monto.negate();
        switch (medio) {
            case EFECTIVO -> turno.setTotalVentasEfectivo(turno.getTotalVentasEfectivo().add(delta));
            case TRANSFERENCIA -> turno.setTotalVentasTransferencia(turno.getTotalVentasTransferencia().add(delta));
            case QR_BILLETERA -> turno.setTotalVentasQr(turno.getTotalVentasQr().add(delta));
            case TARJETA_DEBITO -> turno.setTotalVentasDebito(turno.getTotalVentasDebito().add(delta));
            case TARJETA_CREDITO -> turno.setTotalVentasCredito(turno.getTotalVentasCredito().add(delta));
        }
    }

    private String validarAutorizacionPermisoPos(
            CustomUserDetails operador,
            Long sucursalId,
            PermisoGranular permisoRequerido,
            String supervisorUsername,
            String supervisorPassword) {

        if (operador.tieneRol(CodigoRol.ADMIN) || operador.tienePermiso(permisoRequerido)) {
            return operador.getUsername();
        }

        if (supervisorUsername == null || supervisorUsername.isBlank()
                || supervisorPassword == null || supervisorPassword.isBlank()) {
            throw new BusinessException(
                    "Esta operación requiere el permiso '" + permisoRequerido.getDescripcion()
                            + "' o autorización en pantalla de un Encargado/Administrador.",
                    HttpStatus.FORBIDDEN
            );
        }

        Usuario supervisor = usuarioRepository.findByUsernameIgnoreCase(supervisorUsername.trim())
                .orElseThrow(() -> new BusinessException("Credenciales de supervisor inválidas.", HttpStatus.UNAUTHORIZED));

        if (!supervisor.isActivo() || supervisor.isCuentaBloqueada()) {
            throw new BusinessException("La cuenta del supervisor no está habilitada o está bloqueada.", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(supervisorPassword, supervisor.getPasswordHash())) {
            throw new BusinessException("Credenciales de supervisor inválidas.", HttpStatus.UNAUTHORIZED);
        }

        boolean alcanceSucursal = supervisor.tieneRol(CodigoRol.ADMIN)
                || supervisor.isAccesoTodasSucursales()
                || supervisor.getSucursalesHabilitadas().stream().anyMatch(s -> s.getId().equals(sucursalId) && s.isActivo());

        if (!alcanceSucursal || !supervisor.tienePermiso(permisoRequerido)) {
            throw new BusinessException(
                    "El supervisor ingresado no tiene permiso o alcance en esta sucursal para autorizar la operación.",
                    HttpStatus.FORBIDDEN
            );
        }

        return supervisor.getUsername();
    }

    private String generarNumeroComprobanteInterno(Sucursal sucursal) {
        String pv = String.format("%04d", sucursal.getPuntoVentaArca());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        int rnd = ThreadLocalRandom.current().nextInt(10, 99);
        return "X-" + pv + "-" + timestamp + rnd;
    }

    private String limpiarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private VentaResponseDto toResponseDto(Venta v) {
        List<VentaResponseDto.VentaItemResponseDto> itemsDto = v.getItems().stream()
                .map(i -> new VentaResponseDto.VentaItemResponseDto(
                        i.getId(),
                        i.getProducto().getId(),
                        i.getCodigoBarrasHistorico(),
                        i.getNombreProductoHistorico(),
                        i.getUnidadMedida(),
                        i.getUnidadMedida().getAbreviatura(),
                        i.getCantidad(),
                        i.getPrecioUnitario(),
                        i.getAlicuotaIva(),
                        i.getDescuentoItemPorcentaje(),
                        i.getDescuentoItemMonto(),
                        i.getSubtotalNetoItem(),
                        i.getStockPrevioSucursal(),
                        i.getStockPosteriorSucursal(),
                        i.isVendidoSinStock()
                ))
                .toList();

        List<VentaResponseDto.VentaPagoResponseDto> pagosDto = v.getPagos().stream()
                .map(p -> new VentaResponseDto.VentaPagoResponseDto(
                        p.getId(),
                        p.getMedioPago(),
                        p.getMedioPago().getDescripcion(),
                        p.getMontoBase(),
                        p.getPorcentajeAjusteAplicado(),
                        p.getMontoAjuste(),
                        p.getMontoFinalCobrado(),
                        p.getMontoRecibidoEfectivo(),
                        p.getVueltoEfectivo(),
                        p.getCuotas(),
                        p.getReferenciaOperacion()
                ))
                .toList();

        return new VentaResponseDto(
                v.getId(),
                v.getNumeroComprobanteInterno(),
                v.getSucursal().getId(),
                v.getSucursal().getCodigo(),
                v.getSucursal().getNombre(),
                v.getSucursal().getDireccion(),
                v.getSucursal().getCuit(),
                v.getSucursal().getPuntoVentaArca(),
                v.getTurnoCaja().getId(),
                v.getUsuario().getId(),
                v.getUsuario().getUsername(),
                v.getUsuario().getNombre() + " " + v.getUsuario().getApellido(),
                v.getFechaVenta(),
                v.getClienteNombre(),
                v.getClienteDocumento(),
                v.getClienteCondicionIva(),
                v.getClienteCondicionIva().getDescripcion(),
                v.getSubtotalItems(),
                v.getDescuentoManualPorcentaje(),
                v.getDescuentoManualMonto(),
                v.getAjusteMediosPagoMonto(),
                v.getTotalFinal(),
                v.getEstadoVenta(),
                v.getEstadoVenta().getDescripcion(),
                v.getEstadoFacturacion(),
                v.getEstadoFacturacion().getDescripcion(),
                v.getUltimoErrorArca(),
                v.getCantidadIntentosArca(),
                v.getSupervisorAutorizadorUsername(),
                v.getMotivoAnulacion(),
                v.getFechaAnulacion(),
                v.getUsuarioAnulacionUsername(),
                v.getObservaciones(),
                itemsDto,
                pagosDto,
                arcaFacturacionService.listarComprobantesPorVenta(v.getId())
        );
    }

    public VentaPosService(VentaRepository ventaRepository, ProductoRepository productoRepository, ProductoSucursalRepository productoSucursalRepository, ConfiguracionMedioPagoRepository medioPagoRepository, TurnoCajaRepository turnoCajaRepository, UsuarioRepository usuarioRepository, CajaService cajaService, ArcaFacturacionService arcaFacturacionService, PasswordEncoder passwordEncoder) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.productoSucursalRepository = productoSucursalRepository;
        this.medioPagoRepository = medioPagoRepository;
        this.turnoCajaRepository = turnoCajaRepository;
        this.usuarioRepository = usuarioRepository;
        this.cajaService = cajaService;
        this.arcaFacturacionService = arcaFacturacionService;
        this.passwordEncoder = passwordEncoder;
    }

}
