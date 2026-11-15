package com.ventaspost.service;

import com.ventaspost.common.exception.BusinessException;
import com.ventaspost.common.exception.ResourceNotFoundException;
import com.ventaspost.domain.model.Producto;
import com.ventaspost.domain.model.ProductoSucursal;
import com.ventaspost.domain.model.Sucursal;
import com.ventaspost.domain.repository.ProductoRepository;
import com.ventaspost.domain.repository.ProductoSucursalRepository;
import com.ventaspost.domain.repository.SucursalRepository;
import com.ventaspost.web.dto.sucursal.SucursalRequestDto;
import com.ventaspost.web.dto.sucursal.SucursalResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class SucursalService {

    private final SucursalRepository sucursalRepository;
    private final ProductoRepository productoRepository;
    private final ProductoSucursalRepository productoSucursalRepository;

    @Transactional(readOnly = true)
    public List<SucursalResponseDto> listarTodas() {
        return sucursalRepository.findAllByOrderByCodigoAsc().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SucursalResponseDto obtenerPorId(Long id) {
        Sucursal sucursal = buscarEntidadPorId(id);
        return toResponseDto(sucursal);
    }

    @Transactional
    public SucursalResponseDto crear(SucursalRequestDto dto) {
        String codigoNormalizado = dto.codigo().trim().toUpperCase();
        String cuitNormalizado = normalizarCuit(dto.cuit());

        if (sucursalRepository.existsByCodigoIgnoreCase(codigoNormalizado)) {
            throw new BusinessException("Ya existe una sucursal con el código: " + codigoNormalizado, HttpStatus.CONFLICT);
        }

        // Regla 5.1: Unicidad sobre (cuit, punto_venta_arca)
        if (sucursalRepository.existsByCuitAndPuntoVentaArca(cuitNormalizado, dto.puntoVentaArca())) {
            throw new BusinessException(
                    "Ya existe una sucursal bajo el CUIT " + cuitNormalizado
                            + " con el Punto de Venta ARCA N° " + dto.puntoVentaArca(),
                    HttpStatus.CONFLICT
            );
        }

        Sucursal sucursal = new Sucursal();
        mapearDatos(sucursal, dto, codigoNormalizado, cuitNormalizado);
        sucursal.setActivo(true);

        Sucursal guardada = sucursalRepository.save(sucursal);

        // Inicializar existencias en 0 y stock mínimo por defecto del producto para la nueva sucursal
        List<Producto> productosExistentes = productoRepository.findAll();
        for (Producto prod : productosExistentes) {
            ProductoSucursal ps = new ProductoSucursal();
            ps.setProducto(prod);
            ps.setSucursal(guardada);
            ps.setStockActual(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
            ps.setStockMinimo(prod.getStockMinimoDefecto());
            ps.setActivo(true);
            productoSucursalRepository.save(ps);
        }

        return toResponseDto(guardada);
    }

    @Transactional
    public SucursalResponseDto actualizar(Long id, SucursalRequestDto dto) {
        Sucursal sucursal = buscarEntidadPorId(id);
        String codigoNormalizado = dto.codigo().trim().toUpperCase();
        String cuitNormalizado = normalizarCuit(dto.cuit());

        if (sucursalRepository.existsByCodigoIgnoreCaseAndIdNot(codigoNormalizado, id)) {
            throw new BusinessException("Ya existe otra sucursal con el código: " + codigoNormalizado, HttpStatus.CONFLICT);
        }

        // Regla 5.1: Unicidad sobre (cuit, punto_venta_arca)
        if (sucursalRepository.existsByCuitAndPuntoVentaArcaAndIdNot(cuitNormalizado, dto.puntoVentaArca(), id)) {
            throw new BusinessException(
                    "Ya existe otra sucursal bajo el CUIT " + cuitNormalizado
                            + " con el Punto de Venta ARCA N° " + dto.puntoVentaArca(),
                    HttpStatus.CONFLICT
            );
        }

        mapearDatos(sucursal, dto, codigoNormalizado, cuitNormalizado);
        Sucursal actualizada = sucursalRepository.save(sucursal);
        return toResponseDto(actualizada);
    }

    @Transactional
    public SucursalResponseDto cambiarEstadoActivo(Long id, boolean nuevoEstado) {
        Sucursal sucursal = buscarEntidadPorId(id);
        if (!nuevoEstado) {
            long activasCount = sucursalRepository.findByActivoTrueOrderByCodigoAsc().stream()
                    .filter(s -> !s.getId().equals(id))
                    .count();
            if (activasCount == 0) {
                throw new BusinessException("No se puede desactivar la única sucursal activa del sistema.");
            }
        }
        sucursal.setActivo(nuevoEstado);
        return toResponseDto(sucursalRepository.save(sucursal));
    }

    private Sucursal buscarEntidadPorId(Long id) {
        return sucursalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + id));
    }

    private void mapearDatos(Sucursal sucursal, SucursalRequestDto dto, String codigoNormalizado, String cuitNormalizado) {
        sucursal.setCodigo(codigoNormalizado);
        sucursal.setNombre(dto.nombre().trim());
        sucursal.setRazonSocial(dto.razonSocial().trim());
        sucursal.setCuit(cuitNormalizado);
        sucursal.setCondicionIva(dto.condicionIva());
        sucursal.setPuntoVentaArca(dto.puntoVentaArca());
        sucursal.setIngresosBrutos(limpiarTextoOpcional(dto.ingresosBrutos()));
        sucursal.setFechaInicioActividades(dto.fechaInicioActividades());
        sucursal.setDireccion(dto.direccion().trim());
        sucursal.setLocalidad(limpiarTextoOpcional(dto.localidad()));
        sucursal.setProvincia(limpiarTextoOpcional(dto.provincia()));
        sucursal.setTelefono(limpiarTextoOpcional(dto.telefono()));
        sucursal.setEmail(limpiarTextoOpcional(dto.email()));
        sucursal.setVendeAlPublico(Boolean.TRUE.equals(dto.vendeAlPublico()));
        sucursal.setTieneDeposito(Boolean.TRUE.equals(dto.tieneDeposito()));
        sucursal.setEsCasaCentral(Boolean.TRUE.equals(dto.esCasaCentral()));
    }

    private String normalizarCuit(String cuitRaw) {
        String soloDigitos = cuitRaw.replaceAll("\\D", "");
        if (soloDigitos.length() == 11) {
            return soloDigitos.substring(0, 2) + "-" + soloDigitos.substring(2, 10) + "-" + soloDigitos.substring(10);
        }
        return cuitRaw.trim();
    }

    private String limpiarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private SucursalResponseDto toResponseDto(Sucursal s) {
        return new SucursalResponseDto(
                s.getId(),
                s.getCodigo(),
                s.getNombre(),
                s.getRazonSocial(),
                s.getCuit(),
                s.getCondicionIva(),
                s.getCondicionIva() != null ? s.getCondicionIva().getDescripcion() : null,
                s.getPuntoVentaArca(),
                s.getIngresosBrutos(),
                s.getFechaInicioActividades(),
                s.getDireccion(),
                s.getLocalidad(),
                s.getProvincia(),
                s.getTelefono(),
                s.getEmail(),
                s.isVendeAlPublico(),
                s.isTieneDeposito(),
                s.isEsCasaCentral(),
                s.isActivo(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }

    public SucursalService(SucursalRepository sucursalRepository, ProductoRepository productoRepository, ProductoSucursalRepository productoSucursalRepository) {
        this.sucursalRepository = sucursalRepository;
        this.productoRepository = productoRepository;
        this.productoSucursalRepository = productoSucursalRepository;
    }

}
