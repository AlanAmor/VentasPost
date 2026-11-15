package com.ventaspost.service;

import com.ventaspost.common.exception.BusinessException;
import com.ventaspost.domain.model.*;
import com.ventaspost.domain.repository.*;
import com.ventaspost.security.CustomUserDetails;
import com.ventaspost.web.dto.estadistica.DashboardEstadisticoResponseDto;
import com.ventaspost.web.dto.estadistica.DashboardEstadisticoResponseDto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EstadisticaNegocioService {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final String[] NOMBRES_DIAS = {
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    };
    private static final String[] NOMBRES_MESES = {
            "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    };

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final ProductoSucursalRepository productoSucursalRepository;
    private final SucursalRepository sucursalRepository;
    private final ComprobanteFiscalArcaRepository comprobanteArcaRepository;

    @Transactional(readOnly = true)
    public DashboardEstadisticoResponseDto obtenerDashboard(
            Long sucursalIdParam,
            LocalDate fechaDesdeParam,
            LocalDate fechaHastaParam,
            String filtroFacturacionParam,
            CustomUserDetails operador) {

        validarAccesoReportes(operador, sucursalIdParam);

        boolean puedeVerCostos = operador.tieneRol(CodigoRol.ADMIN)
                || operador.tienePermiso(PermisoGranular.CAMBIAR_PRECIOS_GLOBAL)
                || operador.tienePermiso(PermisoGranular.CAMBIAR_PRECIOS_SUCURSAL);

        Long sucursalEfectivaId = determinarSucursalEfectiva(operador, sucursalIdParam);
        String sucursalNombre = "Todas las Sucursales (Consolidado)";
        if (sucursalEfectivaId != null) {
            sucursalNombre = sucursalRepository.findById(sucursalEfectivaId)
                    .map(s -> s.getCodigo() + " - " + s.getNombre())
                    .orElse("Sucursal #" + sucursalEfectivaId);
        }

        LocalDate hoy = LocalDate.now();
        LocalDate fechaHasta = fechaHastaParam != null ? fechaHastaParam : hoy;
        LocalDate fechaDesde = fechaDesdeParam != null ? fechaDesdeParam : fechaHasta.minusDays(29);
        if (fechaDesde.isAfter(fechaHasta)) {
            LocalDate tmp = fechaDesde;
            fechaDesde = fechaHasta;
            fechaHasta = tmp;
        }

        String filtroFacturacion = normalizarFiltroFacturacion(filtroFacturacionParam);

        // 1. Obtener ventas del año en curso (para KPIs Hoy, Mes Actual, Año Actual y Evolución Mensual)
        LocalDate inicioAnio = LocalDate.of(hoy.getYear(), 1, 1);
        LocalDate desdeConsultaGeneral = fechaDesde.isBefore(inicioAnio) ? fechaDesde : inicioAnio;
        LocalDate hastaConsultaGeneral = fechaHasta.isAfter(hoy) ? fechaHasta : hoy;

        List<Venta> ventasUniverso = sucursalEfectivaId != null
                ? ventaRepository.findBySucursalIdAndFechaVentaBetweenOrderByFechaVentaAsc(
                        sucursalEfectivaId,
                        desdeConsultaGeneral.atStartOfDay(),
                        hastaConsultaGeneral.atTime(LocalTime.MAX)
                )
                : ventaRepository.findByFechaVentaBetweenOrderByFechaVentaAsc(
                        desdeConsultaGeneral.atStartOfDay(),
                        hastaConsultaGeneral.atTime(LocalTime.MAX)
                );

        // KPIs Hoy, Mes Actual y Año Actual (respetando filtroFacturacion y excluyendo ANULADAS - Regla 2.2)
        BigDecimal ventasHoyMonto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long ventasHoyCant = 0;
        BigDecimal ventasMesMonto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long ventasMesCant = 0;
        BigDecimal ventasAnioMonto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long ventasAnioCant = 0;

        Map<YearMonth, AcumuladorTemporal> mapaEvolucionMensual = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(hoy.getYear(), m);
            mapaEvolucionMensual.put(ym, new AcumuladorTemporal(
                    ym.toString(),
                    NOMBRES_MESES[m - 1] + " " + hoy.getYear()
            ));
        }

        for (Venta v : ventasUniverso) {
            if (v.getEstadoVenta() != EstadoVenta.REALIZADA) continue;
            if (!cumpleFiltroFacturacion(v, filtroFacturacion)) continue;

            LocalDate fv = v.getFechaVenta().toLocalDate();
            BigDecimal totalV = v.getTotalFinal().setScale(2, RoundingMode.HALF_UP);
            BigDecimal costoV = calcularCostoTotalVenta(v);
            BigDecimal gananciaV = totalV.subtract(costoV).setScale(2, RoundingMode.HALF_UP);

            if (fv.equals(hoy)) {
                ventasHoyMonto = ventasHoyMonto.add(totalV);
                ventasHoyCant++;
            }
            if (fv.getYear() == hoy.getYear() && fv.getMonth() == hoy.getMonth()) {
                ventasMesMonto = ventasMesMonto.add(totalV);
                ventasMesCant++;
            }
            if (fv.getYear() == hoy.getYear()) {
                ventasAnioMonto = ventasAnioMonto.add(totalV);
                ventasAnioCant++;
                YearMonth ym = YearMonth.from(fv);
                AcumuladorTemporal acMes = mapaEvolucionMensual.get(ym);
                if (acMes != null) {
                    acMes.cantidad++;
                    acMes.facturado = acMes.facturado.add(totalV);
                    acMes.ganancia = acMes.ganancia.add(gananciaV);
                }
            }
        }

        // 2. Filtrar ventas dentro del rango [fechaDesde, fechaHasta] seleccionado
        LocalDate finalDesde = fechaDesde;
        LocalDate finalHasta = fechaHasta;
        List<Venta> ventasRangoTodas = ventasUniverso.stream()
                .filter(v -> {
                    LocalDate d = v.getFechaVenta().toLocalDate();
                    return !d.isBefore(finalDesde) && !d.isAfter(finalHasta);
                })
                .toList();

        // Contadores fiscales y de anulaciones en el período (Reglas 2.1 y 2.2)
        long cantAnuladas = 0;
        BigDecimal montoAnuladas = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long cantFacturadasArca = 0;
        BigDecimal montoFacturadasArca = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long cantNoFacturadas = 0;
        BigDecimal montoNoFacturadas = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        Map<Long, AcumuladorVendedor> mapaVendedores = new LinkedHashMap<>();

        for (Venta v : ventasRangoTodas) {
            Usuario u = v.getUsuario();
            AcumuladorVendedor acVend = mapaVendedores.computeIfAbsent(
                    u.getId(),
                    id -> new AcumuladorVendedor(u.getId(), u.getUsername(), u.getNombre() + " " + u.getApellido())
            );

            if (v.getEstadoVenta() == EstadoVenta.ANULADA) {
                cantAnuladas++;
                montoAnuladas = montoAnuladas.add(v.getTotalFinal());
                acVend.cantAnulaciones++;
                acVend.montoAnulaciones = acVend.montoAnulaciones.add(v.getTotalFinal());
            } else {
                if (v.getEstadoFacturacion() == EstadoFacturacionVenta.FACTURADA) {
                    cantFacturadasArca++;
                    montoFacturadasArca = montoFacturadasArca.add(v.getTotalFinal());
                } else {
                    cantNoFacturadas++;
                    montoNoFacturadas = montoNoFacturadas.add(v.getTotalFinal());
                }
            }
        }

        // Ventas válidas del período que cumplen el filtroFacturacion
        List<Venta> ventasValidasPeriodo = ventasRangoTodas.stream()
                .filter(v -> v.getEstadoVenta() == EstadoVenta.REALIZADA)
                .filter(v -> cumpleFiltroFacturacion(v, filtroFacturacion))
                .toList();

        BigDecimal ventasPeriodoMonto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long ventasPeriodoCant = ventasValidasPeriodo.size();
        BigDecimal costoTotalPeriodo = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalUnidadesVendidasPeriodo = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);

        // Estructuras de agregación del período
        Map<LocalDate, AcumuladorTemporal> mapaPorDia = new LinkedHashMap<>();
        DateTimeFormatter fmtDia = DateTimeFormatter.ofPattern("dd/MM");
        LocalDate cursorDia = fechaDesde;
        while (!cursorDia.isAfter(fechaHasta)) {
            mapaPorDia.put(cursorDia, new AcumuladorTemporal(cursorDia.toString(), cursorDia.format(fmtDia)));
            cursorDia = cursorDia.plusDays(1);
        }

        Map<Long, AcumuladorProducto> mapaProductos = new HashMap<>();
        Set<Long> productosConVentaEnPeriodo = new HashSet<>();
        Map<CodigoMedioPago, AcumuladorMedioPago> mapaMediosPago = new EnumMap<>(CodigoMedioPago.class);
        for (CodigoMedioPago cod : CodigoMedioPago.values()) {
            mapaMediosPago.put(cod, new AcumuladorMedioPago(cod.name(), cod.getDescripcion()));
        }

        Map<String, AcumuladorRubro> mapaCategorias = new HashMap<>();
        Map<String, AcumuladorRubro> mapaProveedores = new HashMap<>();
        Map<Long, AcumuladorSucursal> mapaSucursales = new LinkedHashMap<>();

        long[] cantPorHora = new long[24];
        BigDecimal[] montoPorHora = new BigDecimal[24];
        Arrays.fill(montoPorHora, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        long[] cantPorDiaSemana = new long[7];
        BigDecimal[] montoPorDiaSemana = new BigDecimal[7];
        Arrays.fill(montoPorDiaSemana, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        for (Venta v : ventasValidasPeriodo) {
            BigDecimal totalVenta = v.getTotalFinal().setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotalItems = v.getSubtotalItems();
            BigDecimal factorAjusteGlobal = (subtotalItems != null && subtotalItems.compareTo(BigDecimal.ZERO) > 0)
                    ? totalVenta.divide(subtotalItems, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ONE;

            BigDecimal costoVenta = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            for (VentaItem item : v.getItems()) {
                Producto prod = item.getProducto();
                productosConVentaEnPeriodo.add(prod.getId());

                BigDecimal ingresoNetoItem = item.getSubtotalNetoItem().multiply(factorAjusteGlobal).setScale(2, RoundingMode.HALF_UP);
                BigDecimal costoHistItem = (item.getCostoTotalConIvaHistorico() != null
                        && item.getCostoTotalConIvaHistorico().compareTo(BigDecimal.ZERO) > 0)
                        ? item.getCostoTotalConIvaHistorico().setScale(2, RoundingMode.HALF_UP)
                        : prod.calcularCostoFinalConIva().multiply(item.getCantidad()).setScale(2, RoundingMode.HALF_UP);

                BigDecimal gananciaItem = ingresoNetoItem.subtract(costoHistItem).setScale(2, RoundingMode.HALF_UP);
                costoVenta = costoVenta.add(costoHistItem);
                totalUnidadesVendidasPeriodo = totalUnidadesVendidasPeriodo.add(item.getCantidad());

                // Producto
                AcumuladorProducto acProd = mapaProductos.computeIfAbsent(prod.getId(), id -> new AcumuladorProducto(
                        prod.getId(),
                        prod.getCodigoBarras(),
                        prod.getNombre(),
                        prod.getMarca().getNombre(),
                        prod.getCategoria().getNombre(),
                        prod.getUnidadMedida().getAbreviatura()
                ));
                acProd.unidades = acProd.unidades.add(item.getCantidad());
                acProd.facturado = acProd.facturado.add(ingresoNetoItem);
                acProd.costo = acProd.costo.add(costoHistItem);
                acProd.ganancia = acProd.ganancia.add(gananciaItem);

                // Categoría
                String catNombre = prod.getCategoria().getNombre();
                AcumuladorRubro acCat = mapaCategorias.computeIfAbsent(catNombre, AcumuladorRubro::new);
                acCat.unidades = acCat.unidades.add(item.getCantidad());
                acCat.facturado = acCat.facturado.add(ingresoNetoItem);
                acCat.ganancia = acCat.ganancia.add(gananciaItem);

                // Proveedor
                String provNombre = prod.getProveedor().getRazonSocial();
                AcumuladorRubro acProv = mapaProveedores.computeIfAbsent(provNombre, AcumuladorRubro::new);
                acProv.unidades = acProv.unidades.add(item.getCantidad());
                acProv.facturado = acProv.facturado.add(ingresoNetoItem);
                acProv.ganancia = acProv.ganancia.add(gananciaItem);
            }

            BigDecimal gananciaVenta = totalVenta.subtract(costoVenta).setScale(2, RoundingMode.HALF_UP);
            ventasPeriodoMonto = ventasPeriodoMonto.add(totalVenta);
            costoTotalPeriodo = costoTotalPeriodo.add(costoVenta);

            // Evolución diaria
            AcumuladorTemporal acDia = mapaPorDia.get(v.getFechaVenta().toLocalDate());
            if (acDia != null) {
                acDia.cantidad++;
                acDia.facturado = acDia.facturado.add(totalVenta);
                acDia.ganancia = acDia.ganancia.add(gananciaVenta);
            }

            // Hora pico y día de semana
            int hora = v.getFechaVenta().getHour();
            cantPorHora[hora]++;
            montoPorHora[hora] = montoPorHora[hora].add(totalVenta);

            DayOfWeek dow = v.getFechaVenta().getDayOfWeek();
            int idxDia = dow.getValue() - 1; // 0 = Lunes .. 6 = Domingo
            cantPorDiaSemana[idxDia]++;
            montoPorDiaSemana[idxDia] = montoPorDiaSemana[idxDia].add(totalVenta);

            // Medios de pago
            for (VentaPago p : v.getPagos()) {
                AcumuladorMedioPago acMp = mapaMediosPago.get(p.getMedioPago());
                if (acMp != null) {
                    acMp.operaciones++;
                    acMp.montoBase = acMp.montoBase.add(p.getMontoBase());
                    acMp.montoAjuste = acMp.montoAjuste.add(p.getMontoAjuste());
                    acMp.montoFinal = acMp.montoFinal.add(p.getMontoFinalCobrado());
                }
            }

            // Vendedor
            AcumuladorVendedor acVend = mapaVendedores.get(v.getUsuario().getId());
            if (acVend != null) {
                acVend.cantRealizadas++;
                acVend.totalFacturado = acVend.totalFacturado.add(totalVenta);
            }

            // Sucursal
            Sucursal suc = v.getSucursal();
            AcumuladorSucursal acSuc = mapaSucursales.computeIfAbsent(
                    suc.getId(),
                    id -> new AcumuladorSucursal(suc.getId(), suc.getCodigo(), suc.getNombre())
            );
            acSuc.cantidad++;
            acSuc.facturado = acSuc.facturado.add(totalVenta);
            acSuc.ganancia = acSuc.ganancia.add(gananciaVenta);
        }

        BigDecimal gananciaBrutaPeriodo = ventasPeriodoMonto.subtract(costoTotalPeriodo).setScale(2, RoundingMode.HALF_UP);
        BigDecimal margenPorcentajePeriodo = ventasPeriodoMonto.compareTo(BigDecimal.ZERO) > 0
                ? gananciaBrutaPeriodo.multiply(CIEN).divide(ventasPeriodoMonto, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal ticketPromedio = ventasPeriodoCant > 0
                ? ventasPeriodoMonto.divide(BigDecimal.valueOf(ventasPeriodoCant), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal unidadesPromTicket = ventasPeriodoCant > 0
                ? totalUnidadesVendidasPeriodo.divide(BigDecimal.valueOf(ventasPeriodoCant), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // 3. Construir Rankings Top Productos (Por Unidades y Por Facturación)
        List<ProductoRankingDto> todosProductosVendidos = mapaProductos.values().stream()
                .map(ap -> {
                    BigDecimal margenP = ap.facturado.compareTo(BigDecimal.ZERO) > 0
                            ? ap.ganancia.multiply(CIEN).divide(ap.facturado, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    return new ProductoRankingDto(
                            ap.id,
                            ap.codigoBarras,
                            ap.nombre,
                            ap.marca,
                            ap.categoria,
                            ap.unidad,
                            ap.unidades,
                            ap.facturado,
                            puedeVerCostos ? ap.costo : null,
                            puedeVerCostos ? ap.ganancia : null,
                            puedeVerCostos ? margenP : null
                    );
                })
                .toList();

        List<ProductoRankingDto> rankingPorUnidades = todosProductosVendidos.stream()
                .sorted(Comparator.comparing(ProductoRankingDto::unidadesVendidas).reversed())
                .limit(15)
                .toList();

        List<ProductoRankingDto> rankingPorFacturacion = todosProductosVendidos.stream()
                .sorted(Comparator.comparing(ProductoRankingDto::totalFacturado).reversed())
                .limit(15)
                .toList();

        // 4. Distribución Medios de Pago
        BigDecimal finalVentasPeriodoMonto = ventasPeriodoMonto;
        List<MedioPagoEstadisticaDto> distribucionMedios = mapaMediosPago.values().stream()
                .map(mp -> {
                    BigDecimal part = finalVentasPeriodoMonto.compareTo(BigDecimal.ZERO) > 0
                            ? mp.montoFinal.multiply(CIEN).divide(finalVentasPeriodoMonto, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    return new MedioPagoEstadisticaDto(
                            mp.codigo,
                            mp.nombre,
                            mp.operaciones,
                            mp.montoBase,
                            mp.montoAjuste,
                            mp.montoFinal,
                            part
                    );
                })
                .sorted(Comparator.comparing(MedioPagoEstadisticaDto::montoFinalCobrado).reversed())
                .toList();

        // 5. Rentabilidad por Categoría y por Proveedor
        List<RubroEstadisticaDto> listaCategorias = construirListaRubros(mapaCategorias.values(), ventasPeriodoMonto, puedeVerCostos);
        List<RubroEstadisticaDto> listaProveedores = construirListaRubros(mapaProveedores.values(), ventasPeriodoMonto, puedeVerCostos);

        // 6. Horas Pico y Días de la Semana
        List<HoraPicoDto> listaHoras = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            listaHoras.add(new HoraPicoDto(
                    h,
                    String.format("%02d:00", h),
                    cantPorHora[h],
                    montoPorHora[h]
            ));
        }

        List<DiaSemanaDto> listaDiasSemana = new ArrayList<>(7);
        for (int d = 0; d < 7; d++) {
            listaDiasSemana.add(new DiaSemanaDto(
                    d + 1,
                    NOMBRES_DIAS[d],
                    cantPorDiaSemana[d],
                    montoPorDiaSemana[d]
            ));
        }

        // 7. Valuación de Inventario y Productos sin Rotación / Stock Inmovilizado (Métrica 6)
        BigDecimal capitalTotalStockCosto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorTotalStockVenta = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        List<ProductoSinRotacionDto> sinRotacionList = new ArrayList<>();

        List<Producto> catalogoActivo = productoRepository.findByActivoTrueOrderByNombreAsc();
        for (Producto prod : catalogoActivo) {
            List<ProductoSucursal> stocksProd = sucursalEfectivaId != null
                    ? productoSucursalRepository.findByProductoIdAndSucursalId(prod.getId(), sucursalEfectivaId)
                    .map(List::of).orElse(List.of())
                    : productoSucursalRepository.findByProductoId(prod.getId());

            BigDecimal stockTotalProd = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
            BigDecimal valorVentaProd = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal precioRef = prod.getPrecioVentaGeneral();

            for (ProductoSucursal ps : stocksProd) {
                if (ps.getSucursal().isActivo() && ps.getStockActual().compareTo(BigDecimal.ZERO) > 0) {
                    stockTotalProd = stockTotalProd.add(ps.getStockActual());
                    precioRef = ps.obtenerPrecioVentaEfectivo();
                    valorVentaProd = valorVentaProd.add(
                            ps.getStockActual().multiply(ps.obtenerPrecioVentaEfectivo()).setScale(2, RoundingMode.HALF_UP)
                    );
                }
            }

            if (stockTotalProd.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal costoUnitConIva = prod.calcularCostoFinalConIva().setScale(2, RoundingMode.HALF_UP);
                BigDecimal costoStockProd = stockTotalProd.multiply(costoUnitConIva).setScale(2, RoundingMode.HALF_UP);

                capitalTotalStockCosto = capitalTotalStockCosto.add(costoStockProd);
                valorTotalStockVenta = valorTotalStockVenta.add(valorVentaProd);

                if (!productosConVentaEnPeriodo.contains(prod.getId())) {
                    sinRotacionList.add(new ProductoSinRotacionDto(
                            prod.getId(),
                            prod.getCodigoBarras(),
                            prod.getNombre(),
                            prod.getCategoria().getNombre(),
                            prod.getMarca().getNombre(),
                            prod.getUnidadMedida().getAbreviatura(),
                            stockTotalProd,
                            precioRef,
                            puedeVerCostos ? costoUnitConIva : null,
                            puedeVerCostos ? costoStockProd : null,
                            valorVentaProd
                    ));
                }
            }
        }

        sinRotacionList.sort(Comparator.comparing(ProductoSinRotacionDto::valorInmovilizadoVenta).reversed());
        BigDecimal gananciaPotencialStock = valorTotalStockVenta.subtract(capitalTotalStockCosto).setScale(2, RoundingMode.HALF_UP);

        // 8. Desglose de Comprobantes Fiscales ARCA emitidos en el período
        List<ComprobanteFiscalArca> comprobantesArca = sucursalEfectivaId != null
                ? comprobanteArcaRepository.findBySucursalIdOrderByIdDesc(sucursalEfectivaId)
                : comprobanteArcaRepository.findAllByOrderByIdDesc();

        Map<TipoComprobanteArca, ResumenComprobanteArcaDto> mapaCompArca = new EnumMap<>(TipoComprobanteArca.class);
        for (ComprobanteFiscalArca c : comprobantesArca) {
            if (!c.getFechaEmision().isBefore(fechaDesde) && !c.getFechaEmision().isAfter(fechaHasta)) {
                ResumenComprobanteArcaDto prev = mapaCompArca.get(c.getTipoComprobante());
                if (prev == null) {
                    mapaCompArca.put(c.getTipoComprobante(), new ResumenComprobanteArcaDto(
                            c.getTipoComprobante().getDescripcion(),
                            c.getNaturalezaComprobante(),
                            1,
                            c.getImporteTotal()
                    ));
                } else {
                    mapaCompArca.put(c.getTipoComprobante(), new ResumenComprobanteArcaDto(
                            prev.tipoComprobante(),
                            prev.naturaleza(),
                            prev.cantidad() + 1,
                            prev.importeTotal().add(c.getImporteTotal())
                    ));
                }
            }
        }

        // 9. Rendimiento por Vendedor y Comparativa de Sucursales
        List<VendedorEstadisticaDto> listaVendedores = mapaVendedores.values().stream()
                .map(av -> new VendedorEstadisticaDto(
                        av.id,
                        av.username,
                        av.nombreCompleto,
                        av.cantRealizadas,
                        av.totalFacturado,
                        av.cantRealizadas > 0
                                ? av.totalFacturado.divide(BigDecimal.valueOf(av.cantRealizadas), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        av.cantAnulaciones,
                        av.montoAnulaciones
                ))
                .sorted(Comparator.comparing(VendedorEstadisticaDto::totalFacturado).reversed())
                .toList();

        List<SucursalEstadisticaDto> listaSucursales = mapaSucursales.values().stream()
                .map(as -> {
                    BigDecimal part = finalVentasPeriodoMonto.compareTo(BigDecimal.ZERO) > 0
                            ? as.facturado.multiply(CIEN).divide(finalVentasPeriodoMonto, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    return new SucursalEstadisticaDto(
                            as.id,
                            as.codigo,
                            as.nombre,
                            as.cantidad,
                            as.facturado,
                            puedeVerCostos ? as.ganancia : null,
                            part
                    );
                })
                .sorted(Comparator.comparing(SucursalEstadisticaDto::totalFacturado).reversed())
                .toList();

        List<PuntoTemporalVentaDto> evolucionDiaDto = mapaPorDia.values().stream()
                .map(at -> new PuntoTemporalVentaDto(
                        at.clave,
                        at.etiqueta,
                        at.cantidad,
                        at.facturado,
                        puedeVerCostos ? at.ganancia : null
                ))
                .toList();

        List<PuntoTemporalVentaDto> evolucionMesDto = mapaEvolucionMensual.values().stream()
                .map(at -> new PuntoTemporalVentaDto(
                        at.clave,
                        at.etiqueta,
                        at.cantidad,
                        at.facturado,
                        puedeVerCostos ? at.ganancia : null
                ))
                .toList();

        return new DashboardEstadisticoResponseDto(
                sucursalEfectivaId,
                sucursalNombre,
                fechaDesde,
                fechaHasta,
                filtroFacturacion,
                puedeVerCostos,
                ventasHoyMonto,
                ventasHoyCant,
                ventasMesMonto,
                ventasMesCant,
                ventasAnioMonto,
                ventasAnioCant,
                ventasPeriodoMonto,
                ventasPeriodoCant,
                ticketPromedio,
                unidadesPromTicket,
                puedeVerCostos ? costoTotalPeriodo : null,
                puedeVerCostos ? gananciaBrutaPeriodo : null,
                puedeVerCostos ? margenPorcentajePeriodo : null,
                cantFacturadasArca,
                montoFacturadasArca,
                cantNoFacturadas,
                montoNoFacturadas,
                cantAnuladas,
                montoAnuladas,
                new ArrayList<>(mapaCompArca.values()),
                evolucionDiaDto,
                evolucionMesDto,
                rankingPorUnidades,
                rankingPorFacturacion,
                distribucionMedios,
                listaCategorias,
                listaProveedores,
                listaHoras,
                listaDiasSemana,
                puedeVerCostos ? capitalTotalStockCosto : null,
                valorTotalStockVenta,
                puedeVerCostos ? gananciaPotencialStock : null,
                sinRotacionList.size(),
                sinRotacionList.stream().limit(30).toList(),
                listaVendedores,
                listaSucursales
        );
    }

    /**
     * Regla 3.1 A: Exportador de Reportes a CSV compatible con Microsoft Excel en Español (UTF-8 con BOM y separador ';').
     */
    @Transactional(readOnly = true)
    public String generarReporteCsvExcel(
            String tipoReporte,
            Long sucursalIdParam,
            LocalDate fechaDesdeParam,
            LocalDate fechaHastaParam,
            String filtroFacturacionParam,
            CustomUserDetails operador) {

        DashboardEstadisticoResponseDto dash = obtenerDashboard(
                sucursalIdParam,
                fechaDesdeParam,
                fechaHastaParam,
                filtroFacturacionParam,
                operador
        );

        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM UTF-8 para apertura directa en Excel
        String tipo = tipoReporte != null ? tipoReporte.toUpperCase() : "RANKING_PRODUCTOS";

        switch (tipo) {
            case "VENTAS_DETALLE" -> {
                Long sucursalEfectivaId = dash.sucursalFiltroId();
                List<Venta> ventas = sucursalEfectivaId != null
                        ? ventaRepository.findBySucursalIdAndFechaVentaBetweenOrderByFechaVentaAsc(
                                sucursalEfectivaId,
                                dash.fechaDesde().atStartOfDay(),
                                dash.fechaHasta().atTime(LocalTime.MAX)
                        )
                        : ventaRepository.findByFechaVentaBetweenOrderByFechaVentaAsc(
                                dash.fechaDesde().atStartOfDay(),
                                dash.fechaHasta().atTime(LocalTime.MAX)
                        );

                sb.append("Comprobante Interno;Fecha y Hora;Sucursal;Cajero;Cliente;Documento;Condicion IVA;Estado Venta;Estado Facturacion;Medios de Pago;Total Final ($)");
                if (dash.puedeVerCostosYGanancias()) {
                    sb.append(";Costo Mercaderia ($);Ganancia Bruta ($)");
                }
                sb.append("\r\n");

                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                for (Venta v : ventas) {
                    if (!cumpleFiltroFacturacion(v, dash.filtroFacturacion())) continue;
                    String medios = String.join(" + ", v.getPagos().stream()
                            .map(p -> p.getMedioPago().getDescripcion() + ": $" + p.getMontoFinalCobrado())
                            .toList());
                    BigDecimal costoV = calcularCostoTotalVenta(v);
                    BigDecimal ganV = v.getTotalFinal().subtract(costoV);

                    sb.append(escaparCsv(v.getNumeroComprobanteInterno())).append(';')
                            .append(v.getFechaVenta().format(fmt)).append(';')
                            .append(escaparCsv(v.getSucursal().getCodigo() + " - " + v.getSucursal().getNombre())).append(';')
                            .append(escaparCsv(v.getUsuario().getUsername())).append(';')
                            .append(escaparCsv(v.getClienteNombre())).append(';')
                            .append(escaparCsv(v.getClienteDocumento() != null ? v.getClienteDocumento() : "")).append(';')
                            .append(escaparCsv(v.getClienteCondicionIva().getDescripcion())).append(';')
                            .append(v.getEstadoVenta().name()).append(';')
                            .append(v.getEstadoFacturacion().name()).append(';')
                            .append(escaparCsv(medios)).append(';')
                            .append(formatearNumeroCsv(v.getTotalFinal()));

                    if (dash.puedeVerCostosYGanancias()) {
                        sb.append(';').append(formatearNumeroCsv(costoV))
                                .append(';').append(formatearNumeroCsv(ganV));
                    }
                    sb.append("\r\n");
                }
            }
            case "COMPROBANTES_ARCA" -> {
                Long sucursalEfectivaId = dash.sucursalFiltroId();
                List<ComprobanteFiscalArca> comps = sucursalEfectivaId != null
                        ? comprobanteArcaRepository.findBySucursalIdOrderByIdDesc(sucursalEfectivaId)
                        : comprobanteArcaRepository.findAllByOrderByIdDesc();

                sb.append("Fecha Emision;Sucursal;Tipo Comprobante;Numero Formateado;CAE;Vencimiento CAE;Receptor Nombre;Receptor Doc;Condicion IVA Receptor;Neto Gravado ($);Exento ($);IVA 21% ($);IVA 10.5% ($);IVA 27% ($);IVA Total ($);Importe Total ($)\r\n");
                for (ComprobanteFiscalArca c : comps) {
                    if (c.getFechaEmision().isBefore(dash.fechaDesde()) || c.getFechaEmision().isAfter(dash.fechaHasta())) {
                        continue;
                    }
                    sb.append(c.getFechaEmision()).append(';')
                            .append(escaparCsv(c.getSucursal().getNombre())).append(';')
                            .append(escaparCsv(c.getTipoComprobante().getDescripcion())).append(';')
                            .append(escaparCsv(c.getNumeroFormateado())).append(';')
                            .append(escaparCsv(c.getCae())).append(';')
                            .append(c.getCaeFechaVencimiento()).append(';')
                            .append(escaparCsv(c.getReceptorNombreRazonSocial())).append(';')
                            .append(escaparCsv(c.getReceptorNumeroDoc())).append(';')
                            .append(escaparCsv(c.getReceptorCondicionIva().getDescripcion())).append(';')
                            .append(formatearNumeroCsv(c.getImporteNetoGravado())).append(';')
                            .append(formatearNumeroCsv(c.getImporteExento())).append(';')
                            .append(formatearNumeroCsv(c.getImporteIva21())).append(';')
                            .append(formatearNumeroCsv(c.getImporteIva105())).append(';')
                            .append(formatearNumeroCsv(c.getImporteIva27())).append(';')
                            .append(formatearNumeroCsv(c.getImporteIvaTotal())).append(';')
                            .append(formatearNumeroCsv(c.getImporteTotal())).append("\r\n");
                }
            }
            case "STOCK_INMOVILIZADO" -> {
                sb.append("Codigo de Barras;Producto;Marca;Categoria;Unidad;Stock Actual;Precio Venta ($)");
                if (dash.puedeVerCostosYGanancias()) {
                    sb.append(";Costo c/IVA ($);Capital Inmovilizado a Costo ($)");
                }
                sb.append(";Valorizado a Venta ($)\r\n");

                for (ProductoSinRotacionDto p : dash.productosSinRotacion()) {
                    sb.append(escaparCsv(p.codigoBarras())).append(';')
                            .append(escaparCsv(p.nombre())).append(';')
                            .append(escaparCsv(p.marca())).append(';')
                            .append(escaparCsv(p.categoria())).append(';')
                            .append(escaparCsv(p.unidadMedida())).append(';')
                            .append(formatearNumeroCsv(p.stockActual())).append(';')
                            .append(formatearNumeroCsv(p.precioVenta()));
                    if (dash.puedeVerCostosYGanancias()) {
                        sb.append(';').append(formatearNumeroCsv(p.costoConIva()))
                                .append(';').append(formatearNumeroCsv(p.capitalInmovilizadoCosto()));
                    }
                    sb.append(';').append(formatearNumeroCsv(p.valorInmovilizadoVenta())).append("\r\n");
                }
            }
            default -> {
                sb.append("Codigo de Barras;Producto;Marca;Categoria;Unidad;Unidades Vendidas;Total Facturado ($)");
                if (dash.puedeVerCostosYGanancias()) {
                    sb.append(";Costo Historico Total ($);Ganancia Bruta ($);Margen (%)");
                }
                sb.append("\r\n");

                for (ProductoRankingDto p : dash.rankingPorFacturacion()) {
                    sb.append(escaparCsv(p.codigoBarras())).append(';')
                            .append(escaparCsv(p.nombre())).append(';')
                            .append(escaparCsv(p.marca())).append(';')
                            .append(escaparCsv(p.categoria())).append(';')
                            .append(escaparCsv(p.unidadMedida())).append(';')
                            .append(formatearNumeroCsv(p.unidadesVendidas())).append(';')
                            .append(formatearNumeroCsv(p.totalFacturado()));
                    if (dash.puedeVerCostosYGanancias()) {
                        sb.append(';').append(formatearNumeroCsv(p.costoTotalHistorico()))
                                .append(';').append(formatearNumeroCsv(p.gananciaBruta()))
                                .append(';').append(formatearNumeroCsv(p.margenPorcentaje()));
                    }
                    sb.append("\r\n");
                }
            }
        }

        return sb.toString();
    }

    private void validarAccesoReportes(CustomUserDetails operador, Long sucursalIdParam) {
        if (operador == null) {
            throw new BusinessException("Sesión no válida.", HttpStatus.UNAUTHORIZED);
        }
        if (operador.tieneRol(CodigoRol.ADMIN) || operador.tienePermiso(PermisoGranular.VER_REPORTES_CONSOLIDADOS)) {
            return;
        }
        if (!operador.tienePermiso(PermisoGranular.VER_REPORTES_SUCURSAL)
                && !operador.tieneRol(CodigoRol.ENCARGADO_SUCURSAL)) {
            throw new BusinessException(
                    "No posee permisos para acceder al Panel Estadístico de Negocio.",
                    HttpStatus.FORBIDDEN
            );
        }
        if (sucursalIdParam != null && !operador.isAccesoTodasSucursales()
                && !operador.getSucursalesHabilitadasIds().contains(sucursalIdParam)) {
            throw new BusinessException("No tiene acceso a los reportes de la sucursal solicitada.", HttpStatus.FORBIDDEN);
        }
    }

    private Long determinarSucursalEfectiva(CustomUserDetails operador, Long sucursalIdParam) {
        if (operador.tieneRol(CodigoRol.ADMIN) || operador.tienePermiso(PermisoGranular.VER_REPORTES_CONSOLIDADOS)) {
            return sucursalIdParam;
        }
        if (sucursalIdParam != null && operador.getSucursalesHabilitadasIds().contains(sucursalIdParam)) {
            return sucursalIdParam;
        }
        return operador.getSucursalesHabilitadasIds().stream().findFirst().orElse(null);
    }

    private String normalizarFiltroFacturacion(String raw) {
        if (raw == null || raw.isBlank()) {
            return "TODAS_REALIZADAS";
        }
        String up = raw.trim().toUpperCase();
        if ("SOLO_FACTURADAS_ARCA".equals(up) || "SOLO_NO_FACTURADAS".equals(up)) {
            return up;
        }
        return "TODAS_REALIZADAS";
    }

    private boolean cumpleFiltroFacturacion(Venta v, String filtroFacturacion) {
        return switch (filtroFacturacion) {
            case "SOLO_FACTURADAS_ARCA" -> v.getEstadoFacturacion() == EstadoFacturacionVenta.FACTURADA;
            case "SOLO_NO_FACTURADAS" -> v.getEstadoFacturacion() != EstadoFacturacionVenta.FACTURADA;
            default -> true;
        };
    }

    private BigDecimal calcularCostoTotalVenta(Venta v) {
        BigDecimal totalCosto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (VentaItem item : v.getItems()) {
            BigDecimal costoItem = (item.getCostoTotalConIvaHistorico() != null
                    && item.getCostoTotalConIvaHistorico().compareTo(BigDecimal.ZERO) > 0)
                    ? item.getCostoTotalConIvaHistorico()
                    : item.getProducto().calcularCostoFinalConIva().multiply(item.getCantidad());
            totalCosto = totalCosto.add(costoItem.setScale(2, RoundingMode.HALF_UP));
        }
        return totalCosto;
    }

    private List<RubroEstadisticaDto> construirListaRubros(
            Collection<AcumuladorRubro> coleccion,
            BigDecimal ventasTotales,
            boolean puedeVerCostos) {
        return coleccion.stream()
                .map(r -> {
                    BigDecimal part = ventasTotales.compareTo(BigDecimal.ZERO) > 0
                            ? r.facturado.multiply(CIEN).divide(ventasTotales, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    return new RubroEstadisticaDto(
                            r.nombre,
                            r.unidades,
                            r.facturado,
                            puedeVerCostos ? r.ganancia : null,
                            part
                    );
                })
                .sorted(Comparator.comparing(RubroEstadisticaDto::totalFacturado).reversed())
                .toList();
    }

    private String escaparCsv(String val) {
        if (val == null) return "";
        String limpio = val.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ");
        if (limpio.contains(";") || limpio.contains("\"")) {
            return "\"" + limpio + "\"";
        }
        return limpio;
    }

    private String formatearNumeroCsv(BigDecimal num) {
        if (num == null) return "0,00";
        return num.toPlainString().replace('.', ',');
    }

    private static class AcumuladorTemporal {
        final String clave;
        final String etiqueta;
        long cantidad = 0;
        BigDecimal facturado = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal ganancia = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        AcumuladorTemporal(String clave, String etiqueta) {
            this.clave = clave;
            this.etiqueta = etiqueta;
        }
    }

    private static class AcumuladorProducto {
        final Long id;
        final String codigoBarras;
        final String nombre;
        final String marca;
        final String categoria;
        final String unidad;
        BigDecimal unidades = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        BigDecimal facturado = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal costo = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal ganancia = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        AcumuladorProducto(Long id, String codigoBarras, String nombre, String marca, String categoria, String unidad) {
            this.id = id;
            this.codigoBarras = codigoBarras;
            this.nombre = nombre;
            this.marca = marca;
            this.categoria = categoria;
            this.unidad = unidad;
        }
    }

    private static class AcumuladorMedioPago {
        final String codigo;
        final String nombre;
        long operaciones = 0;
        BigDecimal montoBase = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoAjuste = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoFinal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        AcumuladorMedioPago(String codigo, String nombre) {
            this.codigo = codigo;
            this.nombre = nombre;
        }
    }

    private static class AcumuladorRubro {
        final String nombre;
        BigDecimal unidades = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        BigDecimal facturado = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal ganancia = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        AcumuladorRubro(String nombre) {
            this.nombre = nombre;
        }
    }

    private static class AcumuladorVendedor {
        final Long id;
        final String username;
        final String nombreCompleto;
        long cantRealizadas = 0;
        BigDecimal totalFacturado = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long cantAnulaciones = 0;
        BigDecimal montoAnulaciones = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        AcumuladorVendedor(Long id, String username, String nombreCompleto) {
            this.id = id;
            this.username = username;
            this.nombreCompleto = nombreCompleto;
        }
    }

    private static class AcumuladorSucursal {
        final Long id;
        final String codigo;
        final String nombre;
        long cantidad = 0;
        BigDecimal facturado = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal ganancia = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        AcumuladorSucursal(Long id, String codigo, String nombre) {
            this.id = id;
            this.codigo = codigo;
            this.nombre = nombre;
        }
    }

    public EstadisticaNegocioService(VentaRepository ventaRepository, ProductoRepository productoRepository, ProductoSucursalRepository productoSucursalRepository, SucursalRepository sucursalRepository, ComprobanteFiscalArcaRepository comprobanteArcaRepository) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.productoSucursalRepository = productoSucursalRepository;
        this.sucursalRepository = sucursalRepository;
        this.comprobanteArcaRepository = comprobanteArcaRepository;
    }

}
