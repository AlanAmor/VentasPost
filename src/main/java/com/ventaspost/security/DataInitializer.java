package com.ventaspost.security;

import com.ventaspost.domain.model.CodigoRol;
import com.ventaspost.domain.model.PermisoGranular;
import com.ventaspost.domain.model.Rol;
import com.ventaspost.domain.model.Sucursal;
import com.ventaspost.domain.model.Usuario;
import com.ventaspost.domain.repository.RolRepository;
import com.ventaspost.domain.repository.SucursalRepository;
import com.ventaspost.domain.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final SucursalRepository sucursalRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            Rol rolAdmin = rolRepository.findByCodigo(CodigoRol.ADMIN)
                    .orElseThrow(() -> new IllegalStateException("El rol ADMIN no fue inicializado en la base de datos."));

            List<Sucursal> sucursales = sucursalRepository.findAll();

            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setNombre("Administrador");
            admin.setApellido("General");
            admin.setDni("00000000");
            admin.setLegajo("ADM-001");
            admin.setEmail("admin@ventaspost.local");
            admin.setFechaIngreso(LocalDate.now());
            admin.setAccesoTodasSucursales(true);
            admin.setRequiereCambioPassword(false);
            admin.setIntentosFallidos(0);
            admin.setCuentaBloqueada(false);
            admin.setActivo(true);
            admin.setRoles(Set.of(rolAdmin));
            admin.setSucursalesHabilitadas(new HashSet<>(sucursales));
            admin.setPermisosGranulares(EnumSet.allOf(PermisoGranular.class));

            usuarioRepository.save(admin);
            log.info("Usuario inicial 'admin' creado exitosamente con rol ADMIN y todos los permisos granulares.");
        }
    }

    public DataInitializer(UsuarioRepository usuarioRepository, RolRepository rolRepository, SucursalRepository sucursalRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.sucursalRepository = sucursalRepository;
        this.passwordEncoder = passwordEncoder;
    }

}
