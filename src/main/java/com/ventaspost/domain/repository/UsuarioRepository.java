package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    List<Usuario> findAllByOrderByApellidoAscNombreAsc();
    Optional<Usuario> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
    boolean existsByDni(String dni);
    boolean existsByDniAndIdNot(String dni, Long id);
    boolean existsByLegajoIgnoreCase(String legajo);
    boolean existsByLegajoIgnoreCaseAndIdNot(String legajo, Long id);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
