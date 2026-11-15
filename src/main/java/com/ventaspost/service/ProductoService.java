package com.ventaspost.service;

import com.ventaspost.common.exception.BusinessException;
import com.ventaspost.common.exception.ResourceNotFoundException;
import com.ventaspost.domain.model.*;
import com.ventaspost.domain.repository.ProductoRepository;
import com.ventaspost.domain.repository.ProductoSucursalRepository;
import com.ventaspost.domain.repository.SucursalRepository;
import com.ventaspost.security.CustomUserDetails;
import com.ventaspost.web.dto.producto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoSucursalRepository productoSucursalRepository;
    private final SucursalRepository sucursalRepository;
    private final CatalogoMaestroService catalogoMaestroService;
    private final ProveedorService proveedorService;

    @Transactional(readOnly = true)
    public List<ProductoResponseDto> listarProductos(
            String busqueda,
            Long categoriaId,
            Long marcaId,
            Long proveedorId,
            Boolean soloBajoStock,
            Boolean soloActivos,
            Long sucursalContextoId,
            CustomUserDetails operador) {

        boolean mostrarCostos = puedeVerCostosYMargenes(operador);
        String termino = busqueda != null ? busqueda.trim().toLowerCase() : "";

        return productoRepository.findAllByOrderByNombreAsc().stream()
                .filter(p -> !Boolean.TRUE.equals(soloActivos) || p.isActivo())
                .filter(p -> categoriaId == null || p.getCategoria().getId().equals(categoriaId))
                .filter(p -> marcaId == null || p.getMarca().getId().equals(marcaId))
                .filter(p -> proveedorId == null || p.getProveedor().getId().equals(proveedorId))
                .filter(p -> {
                    if (termino.isEmpty()) {
                        return true;
                    }
                    return p.getNombre().toLowerCase().contains(termino)
                            || p.getCodigoBarras().toLowerCase().contains(termino)
                            || (p.getCodigoProveedorSku() != null && p.getCodigoProveedorSku().toLowerCase().contains(termino))
                            || p.getMarca().getNombre().toLowerCase().contains(termino);
                })
                .map(p -> construirResponseDto(p, sucursalContextoId, mostrarCostos))
                .filter(dto -> !Boolean.TRUE.equals(soloBajoStock) || dto.alertaBajoStock())
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponseDto obtenerPorId(Long id, Long sucursalContextoId, CustomUserDetails operador) {
        Producto p = buscarEntidadPorId(id);
        return construirResponseDto(p, sucursalContextoId, puedeVerCostosYMargenes(operador));
    }

    /**
     * Regla 3.1: Generador automático de código de barras interno EAN-13 válido (prefijo 20).
     */
    @Transactional(readOnly = true)
    public String generarCodigoBarrasInternoUnico() {
        for (int intento = 0; intento < 50; intento++) {
            long aleatorio10Digitos = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
            String base12 = "20" + aleatorio10Digitos;
            int digitoVerificador = calcularDigitoVerificadorEan13(base12);
            String ean13 = base12 + digitoVerificador;
            if (!productoRepository.existsByCodigoBarrasIgnoreCase(ean13)) {
                return ean13;
            }
        }
        throw new BusinessException("No fue posible generar un código de barras interno único. Intente nuevamente.");
    }

    @Transactional
    public ProductoResponseDto crear(
            ProductoRequestDto dto,
            Long sucursalContextoId,
            CustomUserDetails operador) {

        validarPermisoEdicionCatalogoGlobal(operador);

        boolean generarInterno = Boolean.TRUE.equals(dto.generarCodigoInterno())
                || dto.codigoBarras() == null
                || dto.codigoBarras().isBlank();

        String codigoBarrasFinal = generarInterno
                ? generarCodigoBarrasInternoUnico()
                : dto.codigoBarras().trim();

        if (productoRepository.existsByCodigoBarrasIgnoreCase(codigoBarrasFinal)) {
            throw new BusinessException("Ya existe un producto con el código de barras: " + codigoBarrasFinal, HttpStatus.CONFLICT);
        }

        validarDecimalesSegunUnidad(dto.unidadMedida(), dto.stockMinimoDefecto(), "stock mínimo por defecto");

        Producto producto = new Producto();
        producto.setCodigoBarras(codigoBarrasFinal);
        producto.setEsCodigoInterno(generarInterno);
        producto.setActivo(true);

        mapearDatosProducto(producto, dto);
        Producto guardado = productoRepository.save(producto);

        sincronizarSucursalesProducto(guardado, dto.sucursalesConfig(), null, false, false);

        return construirResponseDto(guardado, sucursalContextoId, puedeVerCostosYMargenes(operador));
    }

    @Transactional
    public ProductoResponseDto actualizar(
            Long id,
            ProductoRequestDto dto,
            Long sucursalContextoId,
            CustomUserDetails operador) {

        validarPermisoEdicionCatalogoGlobal(operador);
        Producto producto = buscarEntidadPorId(id);

        String codigoBarrasFinal = (dto.codigoBarras() != null && !dto.codigoBarras().isBlank())
                ? dto.codigoBarras().trim()
                : producto.getCodigoBarras();

        if (productoRepository.existsByCodigoBarrasIgnoreCaseAndIdNot(codigoBarrasFinal, id)) {
            throw new BusinessException("Ya existe otro producto con el código de barras: " + codigoBarrasFinal, HttpStatus.CONFLICT);
        }

        validarDecimalesSegunUnidad(dto.unidadMedida(), dto.stockMinimoDefecto(), "stock mínimo por defecto");

        BigDecimal precioVentaGeneralAnterior = producto.getPrecioVentaGeneral();
        producto.setCodigoBarras(codigoBarrasFinal);

        mapearDatosProducto(producto, dto);
        Producto actualizado = productoRepository.save(producto);

        boolean restablecerSucursal = Boolean.TRUE.equals(dto.restablecerPreciosSucursalAlGeneral());
        boolean proporcionalSucursal = dto.actualizarPreciosSucursalProporcional() == null
                || Boolean.TRUE.equals(dto.actualizarPreciosSucursalProporcional());

        sincronizarSucursalesProducto(
                actualizado,
                dto.sucursalesConfig(),
                precioVentaGeneralAnterior,
                proporcionalSucursal,
                restablecerSucursal
        );

        return construirResponseDto(actualizado, sucursalContextoId, puedeVerCostosYMargenes(operador));
    }

    /**
     * Permite a un Encargado de Sucursal o Admin ajustar el precio local (override),
     * stock mínimo, stock actual y ubicación en góndola de una sucursal puntual.
     */
    @Transactional
    public ProductoResponseDto actualizarConfiguracionSucursal(
            Long productoId,
            ConfiguracionSucursalProductoRequestDto dto,
            Long sucursalContextoId,
            CustomUserDetails operador) {

        validarPermisoConfiguracionSucursal(operador, dto.sucursalId());
        Producto producto = buscarEntidadPorId(productoId);

        validarDecimalesSegunUnidad(producto.getUnidadMedida(), dto.stockActual(), "stock actual");
        validarDecimalesSegunUnidad(producto.getUnidadMedida(), dto.stockMinimo(), "stock mínimo");

        Sucursal sucursal = sucursalRepository.findById(dto.sucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + dto.sucursalId()));

        ProductoSucursal ps = productoSucursalRepository.findByProductoIdAndSucursalId(productoId, dto.sucursalId())
                .orElseGet(() -> {
                    ProductoSucursal nuevo = new ProductoSucursal();
                    nuevo.setProducto(producto);
                    nuevo.setSucursal(sucursal);
                    return nuevo;
                });

        ps.setStockActual(dto.stockActual().setScale(3, RoundingMode.HALF_UP));
        ps.setStockMinimo(dto.stockMinimo().setScale(3, RoundingMode.HALF_UP));
        if (Boolean.TRUE.equals(dto.usarPrecioGeneral())) {
            ps.setPrecioVentaSucursal(null);
        } else if (dto.precioVentaSucursal() != null) {
            ps.setPrecioVentaSucursal(dto.precioVentaSucursal().setScale(2, RoundingMode.HALF_UP));
        }
        ps.setUbicacionGondola(limpiarTextoOpcional(dto.ubicacionGondola()));
        productoSucursalRepository.save(ps);

        return construirResponseDto(producto, sucursalContextoId, puedeVerCostosYMargenes(operador));
    }

    /**
     * Regla 2.3: Actualización masiva de precios con redondeo configurable a gusto del Admin.
     */
    @Transactional
    public ActualizacionMasivaPreciosResponseDto actualizarPreciosMasivo(
            ActualizacionMasivaPreciosRequestDto dto,
            CustomUserDetails operador) {

        validarPermisoEdicionCatalogoGlobal(operador);

        if (dto.porcentajeVariacion().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("El porcentaje de variación debe ser distinto de 0.");
        }
        if (dto.porcentajeVariacion().compareTo(new BigDecimal("-95.00")) < 0) {
            throw new BusinessException("El porcentaje de reducción no puede superar el -95%.");
        }

        BigDecimal factor = BigDecimal.ONE.add(
                dto.porcentajeVariacion().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)
        );

        List<Producto> candidatos = productoRepository.findAllByOrderByNombreAsc().stream()
                .filter(Producto::isActivo)
                .filter(p -> dto.proveedorId() == null || p.getProveedor().getId().equals(dto.proveedorId()))
                .filter(p -> dto.categoriaId() == null || p.getCategoria().getId().equals(dto.categoriaId()))
                .filter(p -> dto.marcaId() == null || p.getMarca().getId().equals(dto.marcaId()))
                .toList();

        if (candidatos.isEmpty()) {
            throw new BusinessException("No se encontraron productos activos que coincidan con los filtros seleccionados.");
        }

        boolean restablecerSuc = Boolean.TRUE.equals(dto.restablecerPreciosPersonalizadosSucursal());
        boolean aplicarASuc = dto.aplicarPorcentajeAPreciosPersonalizadosSucursal() == null
                || Boolean.TRUE.equals(dto.aplicarPorcentajeAPreciosPersonalizadosSucursal());

        for (Producto p : candidatos) {
            if (Boolean.TRUE.equals(dto.actualizarTambienPrecioCompra())) {
                BigDecimal nuevoCosto = p.getPrecioCompra().multiply(factor).setScale(4, RoundingMode.HALF_UP);
                p.setPrecioCompra(nuevoCosto);
            }

            BigDecimal precioBruto = p.getPrecioVentaGeneral().multiply(factor);
            BigDecimal nuevoPrecioVenta = aplicarRedondeoConfigurable(
                    precioBruto,
                    dto.modoRedondeo(),
                    dto.multiploRedondeo()
            );
            p.setPrecioVentaGeneral(nuevoPrecioVenta);

            // Recalcular el % de ganancia real tras el redondeo (Regla 2.1 A)
            BigDecimal costoFinalConIva = p.calcularCostoFinalConIva();
            if (costoFinalConIva.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal margenReal = nuevoPrecioVenta.subtract(costoFinalConIva)
                        .divide(costoFinalConIva, 6, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
                p.setPorcentajeGanancia(margenReal);
            }

            productoRepository.save(p);

            // Regla 1.1 A: Actualizar o restablecer precios personalizados de sucursales
            List<ProductoSucursal> filasSucursal = productoSucursalRepository.findByProductoIdOrderBySucursalCodigoAsc(p.getId());
            for (ProductoSucursal ps : filasSucursal) {
                if (ps.getPrecioVentaSucursal() != null) {
                    if (restablecerSuc) {
                        ps.setPrecioVentaSucursal(null);
                        productoSucursalRepository.save(ps);
                    } else if (aplicarASuc) {
                        BigDecimal nuevoPrecioSuc = aplicarRedondeoConfigurable(
                                ps.getPrecioVentaSucursal().multiply(factor),
                                dto.modoRedondeo(),
                                dto.multiploRedondeo()
                        );
                        ps.setPrecioVentaSucursal(nuevoPrecioSuc);
                        productoSucursalRepository.save(ps);
                    }
                }
            }
        }

        return new ActualizacionMasivaPreciosResponseDto(
                candidatos.size(),
                dto.porcentajeVariacion(),
                dto.modoRedondeo().getDescripcion(),
                dto.multiploRedondeo(),
                LocalDateTime.now()
        );
    }

    @Transactional
    public ProductoResponseDto cambiarEstadoActivo(
            Long id,
            boolean nuevoEstado,
            Long sucursalContextoId,
            CustomUserDetails operador) {

        validarPermisoEdicionCatalogoGlobal(operador);
        Producto p = buscarEntidadPorId(id);
        p.setActivo(nuevoEstado);
        return construirResponseDto(productoRepository.save(p), sucursalContextoId, puedeVerCostosYMargenes(operador));
    }

    private void mapearDatosProducto(Producto p, ProductoRequestDto dto) {
        Categoria categoria = catalogoMaestroService.buscarCategoriaPorId(dto.categoriaId());
        Marca marca = catalogoMaestroService.buscarMarcaPorId(dto.marcaId());

        // Regla 4.2: Si no se envía proveedorId, usar el Proveedor General del sistema
        Proveedor proveedor = (dto.proveedorId() != null)
                ? proveedorService.buscarEntidadPorId(dto.proveedorId())
                : proveedorService.obtenerProveedorPorDefectoSistema();

        p.setCodigoProveedorSku(limpiarTextoOpcional(dto.codigoProveedorSku()));
        p.setNombre(dto.nombre().trim());
        p.setDescripcion(limpiarTextoOpcional(dto.descripcion()));
        p.setCategoria(categoria);
        p.setMarca(marca);
        p.setProveedor(proveedor);
        p.setUnidadMedida(dto.unidadMedida());
        p.setPrecioCompra(dto.precioCompra().setScale(4, RoundingMode.HALF_UP));
        p.setIvaIncluidoCompra(Boolean.TRUE.equals(dto.ivaIncluidoCompra()));
        p.setAlicuotaIva(dto.alicuotaIva().setScale(2, RoundingMode.HALF_UP));
        p.setPrecioVentaGeneral(dto.precioVentaGeneral().setScale(2, RoundingMode.HALF_UP));
        p.setStockMinimoDefecto(dto.stockMinimoDefecto().setScale(3, RoundingMode.HALF_UP));

        // Regla 2.1 A: Recalcular margen real exacto si hay costo > 0, o guardar el porcentaje enviado
        BigDecimal costoFinalConIva = p.calcularCostoFinalConIva();
        if (costoFinalConIva.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal gananciaReal = p.getPrecioVentaGeneral().subtract(costoFinalConIva)
                    .divide(costoFinalConIva, 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
            p.setPorcentajeGanancia(gananciaReal);
        } else {
            p.setPorcentajeGanancia(dto.porcentajeGanancia().setScale(2, RoundingMode.HALF_UP));
        }
    }

    private void sincronizarSucursalesProducto(
            Producto producto,
            List<ProductoSucursalItemRequestDto> configList,
            BigDecimal precioGeneralAnterior,
            boolean actualizarProporcional,
            boolean restablecerAlGeneral) {

        List<Sucursal> todasSucursales = sucursalRepository.findAllByOrderByCodigoAsc();
        Map<Long, ProductoSucursalItemRequestDto> configMap = (configList == null)
                ? Collections.emptyMap()
                : configList.stream().collect(Collectors.toMap(ProductoSucursalItemRequestDto::sucursalId, c -> c, (a, b) -> a));

        BigDecimal factorCambioPrecio = null;
        if (precioGeneralAnterior != null
                && precioGeneralAnterior.compareTo(BigDecimal.ZERO) > 0
                && producto.getPrecioVentaGeneral().compareTo(precioGeneralAnterior) != 0) {
            factorCambioPrecio = producto.getPrecioVentaGeneral()
                    .divide(precioGeneralAnterior, 6, RoundingMode.HALF_UP);
        }

        for (Sucursal suc : todasSucursales) {
            ProductoSucursal ps = productoSucursalRepository
                    .findByProductoIdAndSucursalId(producto.getId(), suc.getId())
                    .orElseGet(() -> {
                        ProductoSucursal nuevo = new ProductoSucursal();
                        nuevo.setProducto(producto);
                        nuevo.setSucursal(suc);
                        nuevo.setStockActual(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
                        nuevo.setStockMinimo(producto.getStockMinimoDefecto());
                        return nuevo;
                    });

            ProductoSucursalItemRequestDto itemReq = configMap.get(suc.getId());
            if (itemReq != null) {
                validarDecimalesSegunUnidad(producto.getUnidadMedida(), itemReq.stockActual(), "stock actual en " + suc.getCodigo());
                validarDecimalesSegunUnidad(producto.getUnidadMedida(), itemReq.stockMinimo(), "stock mínimo en " + suc.getCodigo());

                ps.setStockActual(itemReq.stockActual().setScale(3, RoundingMode.HALF_UP));
                ps.setStockMinimo(itemReq.stockMinimo().setScale(3, RoundingMode.HALF_UP));
                ps.setPrecioVentaSucursal(itemReq.precioVentaSucursal() != null
                        ? itemReq.precioVentaSucursal().setScale(2, RoundingMode.HALF_UP)
                        : null);
                ps.setUbicacionGondola(limpiarTextoOpcional(itemReq.ubicacionGondola()));
            } else if (ps.getId() != null && ps.getPrecioVentaSucursal() != null) {
                if (restablecerAlGeneral) {
                    ps.setPrecioVentaSucursal(null);
                } else if (actualizarProporcional && factorCambioPrecio != null) {
                    ps.setPrecioVentaSucursal(
                            ps.getPrecioVentaSucursal().multiply(factorCambioPrecio).setScale(2, RoundingMode.HALF_UP)
                    );
                }
            }

            productoSucursalRepository.save(ps);
        }
    }

    private BigDecimal aplicarRedondeoConfigurable(
            BigDecimal valor,
            ModoRedondeo modoRedondeo,
            BigDecimal multiplo) {

        if (modoRedondeo == null || modoRedondeo == ModoRedondeo.SIN_REDONDEO
                || multiplo == null || multiplo.compareTo(BigDecimal.ZERO) <= 0) {
            return valor.setScale(2, RoundingMode.HALF_UP);
        }

        RoundingMode rm = switch (modoRedondeo) {
            case MULTIPLO_SUPERIOR -> RoundingMode.CEILING;
            case MULTIPLO_MAS_CERCANO -> RoundingMode.HALF_UP;
            case MULTIPLO_INFERIOR -> RoundingMode.FLOOR;
            default -> RoundingMode.HALF_UP;
        };

        BigDecimal cociente = valor.divide(multiplo, 0, rm);
        BigDecimal resultado = cociente.multiply(multiplo).setScale(2, RoundingMode.HALF_UP);
        return resultado.compareTo(BigDecimal.ZERO) > 0 ? resultado : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private void validarDecimalesSegunUnidad(UnidadMedida unidad, BigDecimal cantidad, String nombreCampo) {
        if (unidad != null && !unidad.isPermiteDecimales() && cantidad != null) {
            if (cantidad.stripTrailingZeros().scale() > 0) {
                throw new BusinessException(
                        "La unidad de medida " + unidad.getNombre() + " no admite cantidades decimales en " + nombreCampo + "."
                );
            }
        }
    }

    private int calcularDigitoVerificadorEan13(String doceDigitos) {
        int suma = 0;
        for (int i = 0; i < 12; i++) {
            int digito = Character.getNumericValue(doceDigitos.charAt(i));
            suma += (i % 2 == 0) ? digito : digito * 3;
        }
        int resto = suma % 10;
        return (resto == 0) ? 0 : (10 - resto);
    }

    private ProductoResponseDto construirResponseDto(
            Producto p,
            Long sucursalContextoId,
            boolean mostrarCostos) {

        List<ProductoSucursal> filasSucursal = productoSucursalRepository.findByProductoIdOrderBySucursalCodigoAsc(p.getId());
        List<ProductoSucursalItemResponseDto> sucursalesDto = filasSucursal.stream()
                .map(ps -> new ProductoSucursalItemResponseDto(
                        ps.getSucursal().getId(),
                        ps.getSucursal().getCodigo(),
                        ps.getSucursal().getNombre(),
                        ps.getStockActual(),
                        ps.getStockMinimo(),
                        ps.getPrecioVentaSucursal(),
                        ps.obtenerPrecioVentaEfectivo(),
                        ps.getPrecioVentaSucursal() != null,
                        ps.esBajoStock(),
                        ps.getUbicacionGondola()
                ))
                .toList();

        BigDecimal stockContextoActual;
        BigDecimal stockContextoMinimo;
        BigDecimal precioVentaContextoEfectivo = p.getPrecioVentaGeneral();
        boolean tienePrecioPersonalizadoContexto = false;
        boolean alertaBajoStock;

        if (sucursalContextoId != null) {
            Optional<ProductoSucursalItemResponseDto> sucActivaOpt = sucursalesDto.stream()
                    .filter(s -> s.sucursalId().equals(sucursalContextoId))
                    .findFirst();

            if (sucActivaOpt.isPresent()) {
                ProductoSucursalItemResponseDto item = sucActivaOpt.get();
                stockContextoActual = item.stockActual();
                stockContextoMinimo = item.stockMinimo();
                precioVentaContextoEfectivo = item.precioVentaEfectivo();
                tienePrecioPersonalizadoContexto = item.tienePrecioPersonalizado();
                alertaBajoStock = item.bajoStock();
            } else {
                stockContextoActual = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
                stockContextoMinimo = p.getStockMinimoDefecto();
                alertaBajoStock = true;
            }
        } else {
            // Modo Consolidado: suma el stock total de todas las sucursales y alerta si alguna sucursal está en bajo stock
            stockContextoActual = sucursalesDto.stream()
                    .map(ProductoSucursalItemResponseDto::stockActual)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stockContextoMinimo = sucursalesDto.stream()
                    .map(ProductoSucursalItemResponseDto::stockMinimo)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            alertaBajoStock = sucursalesDto.isEmpty() || sucursalesDto.stream().anyMatch(ProductoSucursalItemResponseDto::bajoStock);
        }

        return new ProductoResponseDto(
                p.getId(),
                p.getCodigoBarras(),
                p.getCodigoProveedorSku(),
                p.isEsCodigoInterno(),
                p.getNombre(),
                p.getDescripcion(),
                p.getCategoria().getId(),
                p.getCategoria().getNombre(),
                p.getMarca().getId(),
                p.getMarca().getNombre(),
                p.getProveedor().getId(),
                p.getProveedor().getRazonSocial(),
                p.getUnidadMedida(),
                p.getUnidadMedida().getAbreviatura(),
                p.getUnidadMedida().isPermiteDecimales(),
                mostrarCostos ? p.getPrecioCompra() : null,
                mostrarCostos ? p.isIvaIncluidoCompra() : null,
                mostrarCostos ? p.calcularCostoFinalConIva() : null,
                mostrarCostos ? p.getPorcentajeGanancia() : null,
                p.getAlicuotaIva(),
                p.getPrecioVentaGeneral(),
                p.getStockMinimoDefecto(),
                stockContextoActual,
                stockContextoMinimo,
                precioVentaContextoEfectivo,
                tienePrecioPersonalizadoContexto,
                alertaBajoStock,
                p.isActivo(),
                sucursalesDto,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    private boolean puedeVerCostosYMargenes(CustomUserDetails operador) {
        if (operador == null) {
            return false;
        }
        return operador.tieneRol(CodigoRol.ADMIN)
                || operador.tienePermiso(PermisoGranular.CAMBIAR_PRECIOS_GLOBAL)
                || operador.tienePermiso(PermisoGranular.CAMBIAR_PRECIOS_SUCURSAL);
    }

    private void validarPermisoEdicionCatalogoGlobal(CustomUserDetails operador) {
        if (!operador.tieneRol(CodigoRol.ADMIN)
                && !operador.tienePermiso(PermisoGranular.CAMBIAR_PRECIOS_GLOBAL)
                && !operador.tienePermiso(PermisoGranular.GESTIONAR_STOCK_SUCURSAL)) {
            throw new BusinessException(
                    "No posee permisos para crear o modificar artículos del catálogo general.",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private void validarPermisoConfiguracionSucursal(CustomUserDetails operador, Long sucursalId) {
        if (operador.tieneRol(CodigoRol.ADMIN) || operador.isAccesoTodasSucursales()) {
            return;
        }
        if (!operador.tienePermiso(PermisoGranular.CAMBIAR_PRECIOS_SUCURSAL)
                && !operador.tienePermiso(PermisoGranular.GESTIONAR_STOCK_SUCURSAL)) {
            throw new BusinessException(
                    "No tiene permisos para modificar precios o stock de la sucursal.",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private Producto buscarEntidadPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
    }

    private String limpiarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    public ProductoService(ProductoRepository productoRepository, ProductoSucursalRepository productoSucursalRepository, SucursalRepository sucursalRepository, CatalogoMaestroService catalogoMaestroService, ProveedorService proveedorService) {
        this.productoRepository = productoRepository;
        this.productoSucursalRepository = productoSucursalRepository;
        this.sucursalRepository = sucursalRepository;
        this.catalogoMaestroService = catalogoMaestroService;
        this.proveedorService = proveedorService;
    }

}
