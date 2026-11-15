package com.ventaspost.web.controller;

import com.ventaspost.service.CatalogoMaestroService;
import com.ventaspost.web.dto.producto.MaestroSimpleRequestDto;
import com.ventaspost.web.dto.producto.MaestroSimpleResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogo")
public class CatalogoMaestroController {

    private final CatalogoMaestroService catalogoMaestroService;

    @GetMapping("/categorias")
    public ResponseEntity<List<MaestroSimpleResponseDto>> listarCategorias(
            @RequestParam(defaultValue = "true") boolean soloActivas) {
        return ResponseEntity.ok(catalogoMaestroService.listarCategorias(soloActivas));
    }

    @PostMapping("/categorias")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_CAMBIAR_PRECIOS_GLOBAL') or hasAuthority('PERM_GESTIONAR_STOCK_SUCURSAL')")
    public ResponseEntity<MaestroSimpleResponseDto> crearCategoria(
            @Valid @RequestBody MaestroSimpleRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoMaestroService.crearCategoria(dto));
    }

    @GetMapping("/marcas")
    public ResponseEntity<List<MaestroSimpleResponseDto>> listarMarcas(
            @RequestParam(defaultValue = "true") boolean soloActivas) {
        return ResponseEntity.ok(catalogoMaestroService.listarMarcas(soloActivas));
    }

    @PostMapping("/marcas")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_CAMBIAR_PRECIOS_GLOBAL') or hasAuthority('PERM_GESTIONAR_STOCK_SUCURSAL')")
    public ResponseEntity<MaestroSimpleResponseDto> crearMarca(
            @Valid @RequestBody MaestroSimpleRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoMaestroService.crearMarca(dto));
    }

    public CatalogoMaestroController(CatalogoMaestroService catalogoMaestroService) {
        this.catalogoMaestroService = catalogoMaestroService;
    }

}
