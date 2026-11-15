package com.ventaspost.service;

import com.ventaspost.common.exception.BusinessException;
import com.ventaspost.common.exception.ResourceNotFoundException;
import com.ventaspost.domain.model.Proveedor;
import com.ventaspost.domain.repository.ProveedorRepository;
import com.ventaspost.web.dto.proveedor.ProveedorRequestDto;
import com.ventaspost.web.dto.proveedor.ProveedorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Transactional(readOnly = true)
    public List<ProveedorResponseDto> listarTodos(boolean soloActivos) {
        List<Proveedor> lista = soloActivos
                ? proveedorRepository.findByActivoTrueOrderByEsProveedorSistemaDescRazonSocialAsc()
                : proveedorRepository.findAllByOrderByEsProveedorSistemaDescRazonSocialAsc();
        return lista.stream().map(this::toResponseDto).toList();
    }

    @Transactional(readOnly = true)
    public ProveedorResponseDto obtenerPorId(Long id) {
        return toResponseDto(buscarEntidadPorId(id));
    }

    @Transactional
    public ProveedorResponseDto crear(ProveedorRequestDto dto) {
        String codigoNorm = dto.codigo().trim().toUpperCase();
        String cuitNorm = normalizarCuitOpcional(dto.cuit());

        if (proveedorRepository.existsByCodigoIgnoreCase(codigoNorm)) {
            throw new BusinessException("Ya existe un proveedor con el código: " + codigoNorm, HttpStatus.CONFLICT);
        }
        if (cuitNorm != null && proveedorRepository.existsByCuit(cuitNorm)) {
            throw new BusinessException("Ya existe un proveedor registrado con el CUIT: " + cuitNorm, HttpStatus.CONFLICT);
        }

        Proveedor proveedor = new Proveedor();
        mapearDatos(proveedor, dto, codigoNorm, cuitNorm);
        proveedor.setEsProveedorSistema(false);
        proveedor.setActivo(true);

        return toResponseDto(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorResponseDto actualizar(Long id, ProveedorRequestDto dto) {
        Proveedor proveedor = buscarEntidadPorId(id);
        String codigoNorm = dto.codigo().trim().toUpperCase();
        String cuitNorm = normalizarCuitOpcional(dto.cuit());

        if (proveedor.isEsProveedorSistema() && !proveedor.getCodigo().equalsIgnoreCase(codigoNorm)) {
            throw new BusinessException("No se puede modificar el código interno del Proveedor General del sistema.");
        }

        if (proveedorRepository.existsByCodigoIgnoreCaseAndIdNot(codigoNorm, id)) {
            throw new BusinessException("Ya existe otro proveedor con el código: " + codigoNorm, HttpStatus.CONFLICT);
        }
        if (cuitNorm != null && proveedorRepository.existsByCuitAndIdNot(cuitNorm, id)) {
            throw new BusinessException("Ya existe otro proveedor registrado con el CUIT: " + cuitNorm, HttpStatus.CONFLICT);
        }

        mapearDatos(proveedor, dto, codigoNorm, cuitNorm);
        return toResponseDto(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorResponseDto cambiarEstadoActivo(Long id, boolean nuevoEstado) {
        Proveedor proveedor = buscarEntidadPorId(id);
        if (proveedor.isEsProveedorSistema() && !nuevoEstado) {
            throw new BusinessException("No se puede desactivar el Proveedor General por defecto del sistema.");
        }
        proveedor.setActivo(nuevoEstado);
        return toResponseDto(proveedorRepository.save(proveedor));
    }

    public Proveedor obtenerProveedorPorDefectoSistema() {
        return proveedorRepository.findByEsProveedorSistemaTrue()
                .orElseThrow(() -> new IllegalStateException("No se encontró el Proveedor General del sistema."));
    }

    public Proveedor buscarEntidadPorId(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + id));
    }

    private void mapearDatos(Proveedor p, ProveedorRequestDto dto, String codigoNorm, String cuitNorm) {
        p.setCodigo(codigoNorm);
        p.setRazonSocial(dto.razonSocial().trim());
        p.setNombreFantasia(limpiarTextoOpcional(dto.nombreFantasia()));
        p.setCuit(cuitNorm);
        p.setCondicionIva(dto.condicionIva());
        p.setTelefono(limpiarTextoOpcional(dto.telefono()));
        p.setEmail(limpiarTextoOpcional(dto.email()));
        p.setDireccion(limpiarTextoOpcional(dto.direccion()));
        p.setLocalidad(limpiarTextoOpcional(dto.localidad()));
        p.setProvincia(limpiarTextoOpcional(dto.provincia()));
        p.setObservaciones(limpiarTextoOpcional(dto.observaciones()));
    }

    private String normalizarCuitOpcional(String cuitRaw) {
        if (cuitRaw == null || cuitRaw.isBlank()) {
            return null;
        }
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

    private ProveedorResponseDto toResponseDto(Proveedor p) {
        return new ProveedorResponseDto(
                p.getId(),
                p.getCodigo(),
                p.getRazonSocial(),
                p.getNombreFantasia(),
                p.getCuit(),
                p.getCondicionIva(),
                p.getCondicionIva() != null ? p.getCondicionIva().getDescripcion() : null,
                p.getTelefono(),
                p.getEmail(),
                p.getDireccion(),
                p.getLocalidad(),
                p.getProvincia(),
                p.getObservaciones(),
                p.isEsProveedorSistema(),
                p.isActivo(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

}
