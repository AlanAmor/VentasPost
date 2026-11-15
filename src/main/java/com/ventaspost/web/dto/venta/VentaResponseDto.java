package com.ventaspost.web.dto.venta;

import com.ventaspost.domain.model.*;
import com.ventaspost.web.dto.arca.ComprobanteFiscalArcaResponseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaResponseDto(
        Long id,
        String numeroComprobanteInterno,
        Long sucursalId,
        String sucursalCodigo,
        String sucursalNombre,
        String sucursalDireccion,
        String sucursalCuit,
        Integer sucursalPuntoVentaArca,
        Long turnoCajaId,
        Long usuarioId,
        String usuarioUsername,
        String usuarioNombreCompleto,
        LocalDateTime fechaVenta,
        String clienteNombre,
        String clienteDocumento,
        CondicionIva clienteCondicionIva,
        String clienteCondicionIvaDescripcion,
        BigDecimal subtotalItems,
        BigDecimal descuentoManualPorcentaje,
        BigDecimal descuentoManualMonto,
        BigDecimal ajusteMediosPagoMonto,
        BigDecimal totalFinal,
        EstadoVenta estadoVenta,
        String estadoVentaDescripcion,
        EstadoFacturacionVenta estadoFacturacion,
        String estadoFacturacionDescripcion,
        String ultimoErrorArca,
        Integer cantidadIntentosArca,
        String supervisorAutorizadorUsername,
        String motivoAnulacion,
        LocalDateTime fechaAnulacion,
        String usuarioAnulacionUsername,
        String observaciones,
        List<VentaItemResponseDto> items,
        List<VentaPagoResponseDto> pagos,
        List<ComprobanteFiscalArcaResponseDto> comprobantesArca
) {
    public record VentaItemResponseDto(
            Long id,
            Long productoId,
            String codigoBarras,
            String nombreProducto,
            UnidadMedida unidadMedida,
            String unidadMedidaAbreviatura,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal alicuotaIva,
            BigDecimal descuentoItemPorcentaje,
            BigDecimal descuentoItemMonto,
            BigDecimal subtotalNetoItem,
            BigDecimal stockPrevioSucursal,
            BigDecimal stockPosteriorSucursal,
            boolean vendidoSinStock
    ) {}

    public record VentaPagoResponseDto(
            Long id,
            CodigoMedioPago medioPago,
            String medioPagoDescripcion,
            BigDecimal montoBase,
            BigDecimal porcentajeAjusteAplicado,
            BigDecimal montoAjuste,
            BigDecimal montoFinalCobrado,
            BigDecimal montoRecibidoEfectivo,
            BigDecimal vueltoEfectivo,
            Integer cuotas,
            String referenciaOperacion
    ) {}
}
