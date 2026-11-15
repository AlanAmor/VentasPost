package com.ventaspost.web.dto.estadistica;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardEstadisticoResponseDto(
        Long sucursalFiltroId,
        String sucursalFiltroNombre,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        String filtroFacturacion,
        boolean puedeVerCostosYGanancias,

        // KPIs Obligatorios (Día Hoy, Mes Actual, Año Actual y Período Filtrado)
        BigDecimal ventasHoyMonto,
        long ventasHoyCantidad,
        BigDecimal ventasMesActualMonto,
        long ventasMesActualCantidad,
        BigDecimal ventasAnioActualMonto,
        long ventasAnioActualCantidad,

        BigDecimal ventasPeriodoMonto,
        long ventasPeriodoCantidad,
        BigDecimal ticketPromedioPeriodo,
        BigDecimal unidadesPromedioPorTicket,

        // Métrica 1: Margen de Ganancia Real (Precio Venta Neto vs Costo Histórico con IVA)
        BigDecimal costoTotalMercaderiaVendida,
        BigDecimal gananciaBrutaRealMonto,
        BigDecimal margenGananciaBrutaPorcentaje,

        // Reglas 2.1 y 2.2: Control Fiscal ARCA y Ventas Anuladas en el Período
        long ventasFacturadasArcaCantidad,
        BigDecimal ventasFacturadasArcaMonto,
        long ventasNoFacturadasCantidad,
        BigDecimal ventasNoFacturadasMonto,
        long ventasAnuladasCantidad,
        BigDecimal ventasAnuladasMonto,
        List<ResumenComprobanteArcaDto> desgloseComprobantesArca,

        // Series Temporales (Por Día en el período y Por Mes en el año)
        List<PuntoTemporalVentaDto> evolucionPorDia,
        List<PuntoTemporalVentaDto> evolucionPorMes,

        // Rankings de Productos Más Vendidos (Por Unidades y Por Facturación)
        List<ProductoRankingDto> rankingPorUnidades,
        List<ProductoRankingDto> rankingPorFacturacion,

        // Métrica 3: Distribución de Ingresos por Medio de Pago
        List<MedioPagoEstadisticaDto> distribucionMediosPago,

        // Métrica 4: Rentabilidad por Categoría y por Proveedor
        List<RubroEstadisticaDto> rendimientoPorCategoria,
        List<RubroEstadisticaDto> rendimientoPorProveedor,

        // Métrica 5: Horas Pico y Días de la Semana
        List<HoraPicoDto> ventasPorHora,
        List<DiaSemanaDto> ventasPorDiaSemana,

        // Métrica 6: Valuación de Inventario y Productos sin Rotación (Stock Inmovilizado)
        BigDecimal capitalTotalStockCosto,
        BigDecimal valorTotalStockVenta,
        BigDecimal gananciaPotencialStock,
        int cantidadProductosSinRotacion,
        List<ProductoSinRotacionDto> productosSinRotacion,

        // Métrica 7: Rendimiento por Vendedor/Cajero y Comparativa de Sucursales
        List<VendedorEstadisticaDto> rendimientoPorVendedor,
        List<SucursalEstadisticaDto> comparativaSucursales
) {
    public record ResumenComprobanteArcaDto(
            String tipoComprobante,
            String naturaleza,
            long cantidad,
            BigDecimal importeTotal
    ) {}

    public record PuntoTemporalVentaDto(
            String clave,
            String etiqueta,
            long cantidadVentas,
            BigDecimal totalFacturado,
            BigDecimal gananciaBruta
    ) {}

    public record ProductoRankingDto(
            Long productoId,
            String codigoBarras,
            String nombre,
            String marca,
            String categoria,
            String unidadMedida,
            BigDecimal unidadesVendidas,
            BigDecimal totalFacturado,
            BigDecimal costoTotalHistorico,
            BigDecimal gananciaBruta,
            BigDecimal margenPorcentaje
    ) {}

    public record MedioPagoEstadisticaDto(
            String codigo,
            String nombre,
            long cantidadOperaciones,
            BigDecimal montoBaseTotal,
            BigDecimal montoAjusteTotal,
            BigDecimal montoFinalCobrado,
            BigDecimal porcentajeParticipacion
    ) {}

    public record RubroEstadisticaDto(
            String nombre,
            BigDecimal unidadesVendidas,
            BigDecimal totalFacturado,
            BigDecimal gananciaBruta,
            BigDecimal porcentajeParticipacion
    ) {}

    public record HoraPicoDto(
            int hora,
            String etiquetaHora,
            long cantidadVentas,
            BigDecimal totalFacturado
    ) {}

    public record DiaSemanaDto(
            int ordenDia,
            String nombreDia,
            long cantidadVentas,
            BigDecimal totalFacturado
    ) {}

    public record ProductoSinRotacionDto(
            Long productoId,
            String codigoBarras,
            String nombre,
            String categoria,
            String marca,
            String unidadMedida,
            BigDecimal stockActual,
            BigDecimal precioVenta,
            BigDecimal costoConIva,
            BigDecimal capitalInmovilizadoCosto,
            BigDecimal valorInmovilizadoVenta
    ) {}

    public record VendedorEstadisticaDto(
            Long usuarioId,
            String username,
            String nombreCompleto,
            long cantidadVentasRealizadas,
            BigDecimal totalFacturado,
            BigDecimal ticketPromedio,
            long cantidadAnulaciones,
            BigDecimal montoAnulaciones
    ) {}

    public record SucursalEstadisticaDto(
            Long sucursalId,
            String codigo,
            String nombre,
            long cantidadVentas,
            BigDecimal totalFacturado,
            BigDecimal gananciaBruta,
            BigDecimal porcentajeParticipacion
    ) {}
}
