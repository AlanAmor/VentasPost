package com.ventaspost.service;

import com.ventaspost.common.exception.BusinessException;
import com.ventaspost.common.exception.ResourceNotFoundException;
import com.ventaspost.domain.model.Categoria;
import com.ventaspost.domain.model.Marca;
import com.ventaspost.domain.repository.CategoriaRepository;
import com.ventaspost.domain.repository.MarcaRepository;
import com.ventaspost.web.dto.producto.MaestroSimpleRequestDto;
import com.ventaspost.web.dto.producto.MaestroSimpleResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogoMaestroService {

    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;

    @Transactional(readOnly = true)
    public List<MaestroSimpleResponseDto> listarCategorias(boolean soloActivas) {
        List<Categoria> lista = soloActivas
                ? categoriaRepository.findByActivoTrueOrderByNombreAsc()
                : categoriaRepository.findAllByOrderByNombreAsc();
        return lista.stream()
                .map(c -> new MaestroSimpleResponseDto(c.getId(), c.getNombre(), c.getDescripcion(), c.isActivo()))
                .toList();
    }

    @Transactional
    public MaestroSimpleResponseDto crearCategoria(MaestroSimpleRequestDto dto) {
        String nombreNorm = dto.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombreNorm)) {
            throw new BusinessException("Ya existe una categoría con el nombre: " + nombreNorm, HttpStatus.CONFLICT);
        }
        Categoria c = new Categoria();
        c.setNombre(nombreNorm);
        c.setDescripcion(dto.descripcion() != null && !dto.descripcion().isBlank() ? dto.descripcion().trim() : null);
        c.setActivo(true);
        Categoria guardada = categoriaRepository.save(c);
        return new MaestroSimpleResponseDto(guardada.getId(), guardada.getNombre(), guardada.getDescripcion(), guardada.isActivo());
    }

    @Transactional(readOnly = true)
    public List<MaestroSimpleResponseDto> listarMarcas(boolean soloActivas) {
        List<Marca> lista = soloActivas
                ? marcaRepository.findByActivoTrueOrderByNombreAsc()
                : marcaRepository.findAllByOrderByNombreAsc();
        return lista.stream()
                .map(m -> new MaestroSimpleResponseDto(m.getId(), m.getNombre(), m.getDescripcion(), m.isActivo()))
                .toList();
    }

    @Transactional
    public MaestroSimpleResponseDto crearMarca(MaestroSimpleRequestDto dto) {
        String nombreNorm = dto.nombre().trim();
        if (marcaRepository.existsByNombreIgnoreCase(nombreNorm)) {
            throw new BusinessException("Ya existe una marca con el nombre: " + nombreNorm, HttpStatus.CONFLICT);
        }
        Marca m = new Marca();
        m.setNombre(nombreNorm);
        m.setDescripcion(dto.descripcion() != null && !dto.descripcion().isBlank() ? dto.descripcion().trim() : null);
        m.setActivo(true);
        Marca guardada = marcaRepository.save(m);
        return new MaestroSimpleResponseDto(guardada.getId(), guardada.getNombre(), guardada.getDescripcion(), guardada.isActivo());
    }

    public Categoria buscarCategoriaPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
    }

    public Marca buscarMarcaPorId(Long id) {
        return marcaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada con ID: " + id));
    }

    public CatalogoMaestroService(CategoriaRepository categoriaRepository, MarcaRepository marcaRepository) {
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
    }

}
