package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.CodigoRol;
import com.ventaspost.domain.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByCodigo(CodigoRol codigo);
}
