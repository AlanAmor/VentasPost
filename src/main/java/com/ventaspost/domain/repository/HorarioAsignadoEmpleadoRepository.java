package com.ventaspost.domain.repository;

import com.ventaspost.domain.model.HorarioAsignadoEmpleado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioAsignadoEmpleadoRepository extends JpaRepository<HorarioAsignadoEmpleado, Long> {

    @EntityGraph(attributePaths = {"usuario", "sucursal"})
    List<HorarioAsignadoEmpleado> findByUsuarioIdAndActivoTrueOrderByDiaSemanaAscHoraEntradaAsc(Long usuarioId);

    @EntityGraph(attributePaths = {"usuario", "sucursal"})
    List<HorarioAsignadoEmpleado> findBySucursalIdAndActivoTrueOrderByDiaSemanaAscHoraEntradaAsc(Long sucursalId);

    @EntityGraph(attributePaths = {"usuario", "sucursal"})
    List<HorarioAsignadoEmpleado> findByActivoTrueOrderByUsuarioIdAscDiaSemanaAscHoraEntradaAsc();
}
