package com.ventaspost.service;

import com.ventaspost.common.exception.BusinessException;
import com.ventaspost.common.exception.ResourceNotFoundException;
import com.ventaspost.domain.model.*;
import com.ventaspost.domain.repository.*;
import com.ventaspost.web.dto.sueldo.*;
import com.ventaspost.web.dto.sueldo.ConfiguracionTarifasSueldosResponseDto.EmpleadoTarifaResponseDto;
import com.ventaspost.web.dto.sueldo.ConfiguracionTarifasSueldosResponseDto.RolTarifaResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class HorarioLiquidacionService {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal SESENTA = new BigDecimal("60");
    private static final String[] NOMBRES_DIAS = {
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    };

    private final ConfiguracionLaboralSueldosRepository configLaboralRepository;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final HorarioAsignadoEmpleadoRepository horarioRepository;
    private final AsistenciaJornadaTrabajoRepository asistenciaRepository;
    private final AdelantoSueldoRepository adelantoRepository;
    private final LiquidacionSueldoRepository liquidacionRepository;

    /* =========================================================================
       1. CONFIGURACIÓN DE TARIFAS POR HORA (POR ROL Y PERSONALIZADA) Y RECARGOS
       ========================================================================= */
    @Transactional
    public ConfiguracionTarifasSueldosResponseDto obtenerConfiguracionTarifas() {
        ConfiguracionLaboralSueldos cfg = asegurarConfiguracionLaboral();
        List<RolTarifaResponseDto> rolesDto = rolRepository.findAll().stream()
                .sorted(Comparator.comparing(Rol::getId))
                .map(r -> new RolTarifaResponseDto(
                        r.getId(),
                        r.getCodigo(),
                        r.getNombre(),
                        r.getValorHoraGeneral()
                ))
                .toList();

        // Precargar últimas liquidaciones pagadas en un único query para evitar N+1 por empleado
        Map<Long, LocalDate> ultimasFechasPorUsuario = new HashMap<>();
        for (LiquidacionSueldo l : liquidacionRepository.findAllByOrderByFechaLiquidacionDescIdDesc()) {
            if ("PAGADA".equalsIgnoreCase(l.getEstado())) {
                ultimasFechasPorUsuario.merge(
                        l.getUsuario().getId(),
                        l.getFechaHasta(),
                        (prev, curr) -> curr.isAfter(prev) ? curr : prev
                );
            }
        }

        List<EmpleadoTarifaResponseDto> empleadosDto = usuarioRepository.findAll().stream()
                .filter(Usuario::isActivo)
                .sorted(Comparator.comparing(Usuario::getApellido).thenComparing(Usuario::getNombre))
                .map(u -> {
                    LocalDate fechaUltima = ultimasFechasPorUsuario.get(u.getId());
                    LocalDate fechaSugeridaDesde = fechaUltima != null
                            ? fechaUltima.plusDays(1)
                            : (u.getFechaIngreso() != null ? u.getFechaIngreso() : LocalDate.now().withDayOfMonth(1));

                    List<String> rolesNombres = u.getRoles().stream()
                            .filter(Rol::isActivo)
                            .map(Rol::getNombre)
                            .sorted()
                            .toList();

                    return new EmpleadoTarifaResponseDto(
                            u.getId(),
                            u.getUsername(),
                            u.getApellido() + ", " + u.getNombre(),
                            u.getLegajo(),
                            rolesNombres,
                            u.getValorHoraPersonalizado(),
                            u.obtenerValorHoraEfectivo(),
                            u.obtenerOrigenValorHora(),
                            fechaUltima,
                            fechaSugeridaDesde
                    );
                })
                .toList();

        return new ConfiguracionTarifasSueldosResponseDto(
                cfg.getPorcentajeRecargoExtraSimple(),
                cfg.getPorcentajeRecargoFeriadoDomingo(),
                cfg.getPorcentajeRecargoNocturno(),
                cfg.getHoraInicioNocturno(),
                cfg.getHoraFinNocturno(),
                rolesDto,
                empleadosDto
        );
    }

    @Transactional
    public ConfiguracionTarifasSueldosResponseDto actualizarConfiguracionTarifas(
            ConfiguracionTarifasSueldosRequestDto dto) {

        ConfiguracionLaboralSueldos cfg = asegurarConfiguracionLaboral();
        cfg.setPorcentajeRecargoExtraSimple(dto.porcentajeRecargoExtraSimple().setScale(2, RoundingMode.HALF_UP));
        cfg.setPorcentajeRecargoFeriadoDomingo(dto.porcentajeRecargoFeriadoDomingo().setScale(2, RoundingMode.HALF_UP));
        cfg.setPorcentajeRecargoNocturno(dto.porcentajeRecargoNocturno().setScale(2, RoundingMode.HALF_UP));
        if (dto.horaInicioNocturno() != null) {
            cfg.setHoraInicioNocturno(dto.horaInicioNocturno());
        }
        if (dto.horaFinNocturno() != null) {
            cfg.setHoraFinNocturno(dto.horaFinNocturno());
        }
        configLaboralRepository.save(cfg);

        if (dto.tarifasPorRol() != null) {
            for (ConfiguracionTarifasSueldosRequestDto.TarifaRolDto tr : dto.tarifasPorRol()) {
                if (tr.codigoRol() != null) {
                    rolRepository.findByCodigo(tr.codigoRol()).ifPresent(rol -> {
                        rol.setValorHoraGeneral(tr.valorHoraGeneral().setScale(2, RoundingMode.HALF_UP));
                        rolRepository.save(rol);
                    });
                }
            }
        }

        if (dto.tarifasPersonalizadasEmpleados() != null) {
            for (ConfiguracionTarifasSueldosRequestDto.TarifaEmpleadoDto te : dto.tarifasPersonalizadasEmpleados()) {
                usuarioRepository.findById(te.usuarioId()).ifPresent(u -> {
                    if (te.valorHoraPersonalizado() != null && te.valorHoraPersonalizado().compareTo(BigDecimal.ZERO) > 0) {
                        u.setValorHoraPersonalizado(te.valorHoraPersonalizado().setScale(2, RoundingMode.HALF_UP));
                    } else {
                        u.setValorHoraPersonalizado(null);
                    }
                    usuarioRepository.save(u);
                });
            }
        }

        return obtenerConfiguracionTarifas();
    }

    /* =========================================================================
       2. ASIGNACIÓN DE HORARIOS SEMANALES POR EMPLEADO Y SUCURSAL (Regla 1.1 A)
       ========================================================================= */
    @Transactional(readOnly = true)
    public List<HorarioAsignadoResponseDto> listarHorariosAsignados(Long usuarioId, Long sucursalId) {
        List<HorarioAsignadoEmpleado> lista;
        if (usuarioId != null) {
            lista = horarioRepository.findByUsuarioIdAndActivoTrueOrderByDiaSemanaAscHoraEntradaAsc(usuarioId);
        } else if (sucursalId != null) {
            lista = horarioRepository.findBySucursalIdAndActivoTrueOrderByDiaSemanaAscHoraEntradaAsc(sucursalId);
        } else {
            lista = horarioRepository.findByActivoTrueOrderByUsuarioIdAscDiaSemanaAscHoraEntradaAsc();
        }
        return lista.stream().map(this::toHorarioResponseDto).toList();
    }

    @Transactional
    public HorarioAsignadoResponseDto crearHorarioAsignado(HorarioAsignadoRequestDto dto) {
        Usuario usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + dto.usuarioId()));
        Sucursal sucursal = sucursalRepository.findById(dto.sucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + dto.sucursalId()));

        BigDecimal horasTeoricas = calcularDuracionHorasEntre(dto.horaEntrada(), dto.horaSalida());
        if (horasTeoricas.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La franja horaria debe tener una duración mayor a 0 horas.");
        }

        HorarioAsignadoEmpleado h = new HorarioAsignadoEmpleado();
        h.setUsuario(usuario);
        h.setSucursal(sucursal);
        h.setDiaSemana(dto.diaSemana());
        h.setHoraEntrada(dto.horaEntrada());
        h.setHoraSalida(dto.horaSalida());
        h.setHorasTeoricas(horasTeoricas);
        h.setObservaciones(limpiarTextoOpcional(dto.observaciones()));
        h.setActivo(true);

        return toHorarioResponseDto(horarioRepository.save(h));
    }

    @Transactional
    public void eliminarHorarioAsignado(Long horarioId) {
        HorarioAsignadoEmpleado h = horarioRepository.findById(horarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Horario asignado no encontrado: " + horarioId));
        horarioRepository.delete(h);
    }

    /* =========================================================================
       3. FICHAJE EN VIVO (INGRESO / EGRESO) Y GESTIÓN DE ASISTENCIAS / JORNADAS
       ========================================================================= */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadoFichajeActual(Long usuarioId) {
        Optional<AsistenciaJornadaTrabajo> enCurso = asistenciaRepository
                .findFirstByUsuarioIdAndEstadoJornadaOrderByIdDesc(usuarioId, EstadoJornadaTrabajo.EN_CURSO);
        if (enCurso.isPresent()) {
            return Map.of(
                    "tieneJornadaEnCurso", true,
                    "jornada", toAsistenciaResponseDto(enCurso.get())
            );
        }
        return Map.of("tieneJornadaEnCurso", false);
    }

    @Transactional
    public AsistenciaJornadaResponseDto ficharIngresoReloj(Long usuarioId, Long sucursalActivaId) {
        Optional<AsistenciaJornadaTrabajo> enCurso = asistenciaRepository
                .findFirstByUsuarioIdAndEstadoJornadaOrderByIdDesc(usuarioId, EstadoJornadaTrabajo.EN_CURSO);
        if (enCurso.isPresent()) {
            throw new BusinessException("Ya posee una jornada en curso con ingreso fichado. Debe fichar su salida primero.");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));

        Long sucId = sucursalActivaId != null
                ? sucursalActivaId
                : usuario.getSucursalesHabilitadas().stream().findFirst().map(Sucursal::getId)
                .orElseGet(() -> sucursalRepository.findAll().stream().findFirst().map(Sucursal::getId).orElseThrow());

        Sucursal sucursal = sucursalRepository.findById(sucId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + sucId));

        LocalDate hoy = LocalDate.now();
        int diaSemana = hoy.getDayOfWeek().getValue();

        HorarioAsignadoEmpleado horarioHoy = horarioRepository
                .findByUsuarioIdAndActivoTrueOrderByDiaSemanaAscHoraEntradaAsc(usuarioId).stream()
                .filter(h -> h.getDiaSemana() == diaSemana)
                .findFirst()
                .orElse(null);

        AsistenciaJornadaTrabajo a = new AsistenciaJornadaTrabajo();
        a.setUsuario(usuario);
        a.setSucursal(sucursal);
        a.setFechaJornada(hoy);
        a.setOrigenRegistro(OrigenRegistroJornada.FICHAJE_RELOJ);
        a.setEstadoJornada(EstadoJornadaTrabajo.EN_CURSO);
        if (horarioHoy != null) {
            a.setHoraEntradaProgramada(horarioHoy.getHoraEntrada());
            a.setHoraSalidaProgramada(horarioHoy.getHoraSalida());
        }
        a.setFechaHoraIngresoReal(LocalDateTime.now());
        a.setEsFeriadoODomingo(hoy.getDayOfWeek() == DayOfWeek.SUNDAY);
        a.setActivo(true);

        return toAsistenciaResponseDto(asistenciaRepository.save(a));
    }

    @Transactional
    public AsistenciaJornadaResponseDto ficharEgresoReloj(Long usuarioId) {
        AsistenciaJornadaTrabajo a = asistenciaRepository
                .findFirstByUsuarioIdAndEstadoJornadaOrderByIdDesc(usuarioId, EstadoJornadaTrabajo.EN_CURSO)
                .orElseThrow(() -> new BusinessException("No posee ninguna jornada en curso para fichar salida."));

        LocalDateTime ahora = LocalDateTime.now();
        a.setFechaHoraEgresoReal(ahora);
        a.setEstadoJornada(EstadoJornadaTrabajo.TRABAJADA);

        long minutosTrabajados = Math.max(1, Duration.between(a.getFechaHoraIngresoReal(), ahora).toMinutes());
        BigDecimal totalHorasReales = BigDecimal.valueOf(minutosTrabajados)
                .divide(SESENTA, 2, RoundingMode.HALF_UP);

        BigDecimal horasTeoricasDia = (a.getHoraEntradaProgramada() != null && a.getHoraSalidaProgramada() != null)
                ? calcularDuracionHorasEntre(a.getHoraEntradaProgramada(), a.getHoraSalidaProgramada())
                : new BigDecimal("8.00");

        if (a.isEsFeriadoODomingo()) {
            a.setHorasFeriadoDomingo(totalHorasReales);
            a.setHorasNormales(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            a.setHorasExtrasSimples(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        } else if (totalHorasReales.compareTo(horasTeoricasDia) > 0) {
            a.setHorasNormales(horasTeoricasDia);
            a.setHorasExtrasSimples(totalHorasReales.subtract(horasTeoricasDia).setScale(2, RoundingMode.HALF_UP));
            a.setHorasFeriadoDomingo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        } else {
            a.setHorasNormales(totalHorasReales);
            a.setHorasExtrasSimples(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            a.setHorasFeriadoDomingo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        a.setHorasNocturnas(calcularHorasNocturnasEnRango(
                a.getFechaHoraIngresoReal().toLocalTime(),
                ahora.toLocalTime()
        ));
        a.setTotalHorasComputadas(totalHorasReales);

        return toAsistenciaResponseDto(asistenciaRepository.save(a));
    }

    /**
     * Regla 1.1 A: Genera automáticamente las jornadas de trabajo a partir de los Horarios Teóricos asignados
     * para el rango de fechas indicado, sin duplicar días que ya tengan fichaje o registro cargado.
     */
    @Transactional
    public Map<String, Object> generarJornadasDesdeHorariosTeoricos(GenerarAsistenciasDesdeHorarioRequestDto dto) {
        if (dto.fechaDesde().isAfter(dto.fechaHasta())) {
            throw new BusinessException("La fecha desde no puede ser posterior a la fecha hasta.");
        }

        List<HorarioAsignadoEmpleado> horarios = dto.usuarioId() != null
                ? horarioRepository.findByUsuarioIdAndActivoTrueOrderByDiaSemanaAscHoraEntradaAsc(dto.usuarioId())
                : horarioRepository.findByActivoTrueOrderByUsuarioIdAscDiaSemanaAscHoraEntradaAsc();

        if (horarios.isEmpty()) {
            throw new BusinessException("No hay horarios semanales asignados para generar jornadas en el período.");
        }

        int generadas = 0;
        LocalDate cursor = dto.fechaDesde();
        while (!cursor.isAfter(dto.fechaHasta())) {
            int diaSem = cursor.getDayOfWeek().getValue();
            boolean esDomingo = cursor.getDayOfWeek() == DayOfWeek.SUNDAY;

            for (HorarioAsignadoEmpleado h : horarios) {
                if (h.getDiaSemana() == diaSem && h.getUsuario().isActivo()) {
                    boolean yaExiste = asistenciaRepository.existsByUsuarioIdAndFechaJornadaAndActivoTrue(
                            h.getUsuario().getId(),
                            cursor
                    );
                    if (!yaExiste) {
                        AsistenciaJornadaTrabajo a = new AsistenciaJornadaTrabajo();
                        a.setUsuario(h.getUsuario());
                        a.setSucursal(h.getSucursal());
                        a.setFechaJornada(cursor);
                        a.setOrigenRegistro(OrigenRegistroJornada.HORARIO_TEORICO);
                        a.setEstadoJornada(EstadoJornadaTrabajo.TRABAJADA);
                        a.setHoraEntradaProgramada(h.getHoraEntrada());
                        a.setHoraSalidaProgramada(h.getHoraSalida());
                        a.setEsFeriadoODomingo(esDomingo);

                        BigDecimal hs = h.getHorasTeoricas().setScale(2, RoundingMode.HALF_UP);
                        if (esDomingo) {
                            a.setHorasFeriadoDomingo(hs);
                            a.setHorasNormales(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                        } else {
                            a.setHorasNormales(hs);
                            a.setHorasFeriadoDomingo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                        }
                        a.setHorasExtrasSimples(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                        a.setHorasNocturnas(calcularHorasNocturnasEnRango(h.getHoraEntrada(), h.getHoraSalida()));
                        a.setTotalHorasComputadas(hs);
                        a.setObservaciones("Generada desde horario asignado (" + h.getHoraEntrada() + " a " + h.getHoraSalida() + ")");
                        a.setActivo(true);
                        asistenciaRepository.save(a);
                        generadas++;
                    }
                }
            }
            cursor = cursor.plusDays(1);
        }

        return Map.of(
                "jornadasGeneradas", generadas,
                "mensaje", "Se generaron " + generadas + " jornadas de trabajo desde la grilla de horarios asignados."
        );
    }

    @Transactional(readOnly = true)
    public List<AsistenciaJornadaResponseDto> listarAsistencias(
            Long usuarioId,
            Long sucursalId,
            LocalDate desde,
            LocalDate hasta) {

        LocalDate fechaHasta = hasta != null ? hasta : LocalDate.now();
        LocalDate fechaDesde = desde != null ? desde : fechaHasta.minusDays(30);

        List<AsistenciaJornadaTrabajo> lista;
        if (usuarioId != null) {
            lista = asistenciaRepository.findByUsuarioIdAndFechaJornadaBetweenOrderByFechaJornadaAsc(
                    usuarioId, fechaDesde, fechaHasta
            );
        } else if (sucursalId != null) {
            lista = asistenciaRepository.findBySucursalIdAndFechaJornadaBetweenOrderByFechaJornadaDescIdDesc(
                    sucursalId, fechaDesde, fechaHasta
            );
        } else {
            lista = asistenciaRepository.findByFechaJornadaBetweenOrderByFechaJornadaDescIdDesc(
                    fechaDesde, fechaHasta
            );
        }

        return lista.stream().map(this::toAsistenciaResponseDto).toList();
    }

    @Transactional
    public AsistenciaJornadaResponseDto guardarOActualizarJornadaManual(
            Long idOpcional,
            AsistenciaJornadaRequestDto dto) {

        Usuario usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + dto.usuarioId()));
        Sucursal sucursal = sucursalRepository.findById(dto.sucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + dto.sucursalId()));

        AsistenciaJornadaTrabajo a;
        if (idOpcional != null) {
            a = asistenciaRepository.findById(idOpcional)
                    .orElseThrow(() -> new ResourceNotFoundException("Jornada no encontrada: " + idOpcional));
            if (a.isLiquidada()) {
                throw new BusinessException("No se puede modificar una jornada que ya fue incluida en una liquidación cerrada.");
            }
        } else {
            a = new AsistenciaJornadaTrabajo();
            a.setOrigenRegistro(OrigenRegistroJornada.CARGA_MANUAL);
            a.setActivo(true);
        }

        a.setUsuario(usuario);
        a.setSucursal(sucursal);
        a.setFechaJornada(dto.fechaJornada());
        a.setEstadoJornada(dto.estadoJornada());
        a.setHoraEntradaProgramada(dto.horaEntradaProgramada());
        a.setHoraSalidaProgramada(dto.horaSalidaProgramada());
        a.setEsFeriadoODomingo(Boolean.TRUE.equals(dto.esFeriadoODomingo()));

        if (dto.estadoJornada() == EstadoJornadaTrabajo.INASISTENCIA_INJUSTIFICADA
                || dto.estadoJornada() == EstadoJornadaTrabajo.INASISTENCIA_JUSTIFICADA) {
            a.setHorasNormales(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            a.setHorasExtrasSimples(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            a.setHorasFeriadoDomingo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            a.setHorasNocturnas(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            a.setTotalHorasComputadas(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        } else {
            BigDecimal hNorm = (dto.horasNormales() != null ? dto.horasNormales() : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal hExt = (dto.horasExtrasSimples() != null ? dto.horasExtrasSimples() : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal hFer = (dto.horasFeriadoDomingo() != null ? dto.horasFeriadoDomingo() : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal hNoc = (dto.horasNocturnas() != null ? dto.horasNocturnas() : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            a.setHorasNormales(hNorm);
            a.setHorasExtrasSimples(hExt);
            a.setHorasFeriadoDomingo(hFer);
            a.setHorasNocturnas(hNoc);
            a.setTotalHorasComputadas(hNorm.add(hExt).add(hFer).setScale(2, RoundingMode.HALF_UP));
        }

        a.setObservaciones(limpiarTextoOpcional(dto.observaciones()));
        return toAsistenciaResponseDto(asistenciaRepository.save(a));
    }

    @Transactional
    public void eliminarJornadaNoLiquidada(Long id) {
        AsistenciaJornadaTrabajo a = asistenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Jornada no encontrada: " + id));
        if (a.isLiquidada()) {
            throw new BusinessException("No se puede eliminar una jornada que ya fue liquidada.");
        }
        asistenciaRepository.delete(a);
    }

    /* =========================================================================
       4. ADELANTOS / VALES DE SUELDO (Regla 3.1 A)
       ========================================================================= */
    @Transactional(readOnly = true)
    public List<AdelantoSueldoResponseDto> listarAdelantos(Long usuarioId) {
        List<AdelantoSueldo> lista = usuarioId != null
                ? adelantoRepository.findByUsuarioIdOrderByFechaAdelantoDescIdDesc(usuarioId)
                : adelantoRepository.findAllByOrderByFechaAdelantoDescIdDesc();
        return lista.stream().map(this::toAdelantoResponseDto).toList();
    }

    @Transactional
    public AdelantoSueldoResponseDto registrarAdelanto(AdelantoSueldoRequestDto dto, String operadorUsername) {
        Usuario usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + dto.usuarioId()));
        Sucursal sucursal = sucursalRepository.findById(dto.sucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + dto.sucursalId()));

        AdelantoSueldo ad = new AdelantoSueldo();
        ad.setUsuario(usuario);
        ad.setSucursal(sucursal);
        ad.setFechaAdelanto(dto.fechaAdelanto());
        ad.setMonto(dto.monto().setScale(2, RoundingMode.HALF_UP));
        ad.setMedioPago(dto.medioPago().trim().toUpperCase());
        ad.setMotivo(dto.motivo().trim());
        ad.setDescontado(false);
        ad.setRegistradoPorUsername(operadorUsername);
        ad.setActivo(true);

        return toAdelantoResponseDto(adelantoRepository.save(ad));
    }

    @Transactional
    public void eliminarAdelantoPendiente(Long id) {
        AdelantoSueldo ad = adelantoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adelanto no encontrado: " + id));
        if (ad.isDescontado()) {
            throw new BusinessException("No se puede eliminar un adelanto que ya fue descontado en una liquidación.");
        }
        adelantoRepository.delete(ad);
    }

    /* =========================================================================
       5. PRELIQUIDACIÓN Y EMISIÓN DE LIQUIDACIÓN DE SUELDOS (Reglas 2 y 3)
       ========================================================================= */
    @Transactional(readOnly = true)
    public PreliquidacionSueldoResponseDto calcularPreliquidacion(
            Long usuarioId,
            LocalDate fechaDesdeParam,
            LocalDate fechaHastaParam) {

        Usuario empleado = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + usuarioId));

        ConfiguracionLaboralSueldos cfg = asegurarConfiguracionLaboral();

        LocalDate fechaHasta = fechaHastaParam != null ? fechaHastaParam : LocalDate.now();
        LocalDate fechaDesde = fechaDesdeParam;
        if (fechaDesde == null) {
            Optional<LiquidacionSueldo> ult = liquidacionRepository
                    .findFirstByUsuarioIdAndEstadoOrderByFechaHastaDescIdDesc(usuarioId, "PAGADA");
            fechaDesde = ult.map(l -> l.getFechaHasta().plusDays(1))
                    .orElseGet(() -> empleado.getFechaIngreso() != null ? empleado.getFechaIngreso() : fechaHasta.withDayOfMonth(1));
        }

        List<AsistenciaJornadaTrabajo> jornadasNoLiquidadas = asistenciaRepository
                .findByUsuarioIdAndFechaJornadaBetweenOrderByFechaJornadaAsc(usuarioId, fechaDesde, fechaHasta)
                .stream()
                .filter(j -> j.isActivo() && !j.isLiquidada() && j.getEstadoJornada() == EstadoJornadaTrabajo.TRABAJADA)
                .toList();

        BigDecimal valorHoraBase = empleado.obtenerValorHoraEfectivo().setScale(2, RoundingMode.HALF_UP);
        String origenValorHora = empleado.obtenerOrigenValorHora();

        BigDecimal porcExtra = cfg.getPorcentajeRecargoExtraSimple();
        BigDecimal porcFeriado = cfg.getPorcentajeRecargoFeriadoDomingo();
        BigDecimal porcNocturno = cfg.getPorcentajeRecargoNocturno();

        BigDecimal totalHsNormales = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalHsExtras = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalHsFeriado = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalHsNocturnas = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (AsistenciaJornadaTrabajo j : jornadasNoLiquidadas) {
            totalHsNormales = totalHsNormales.add(j.getHorasNormales());
            totalHsExtras = totalHsExtras.add(j.getHorasExtrasSimples());
            totalHsFeriado = totalHsFeriado.add(j.getHorasFeriadoDomingo());
            totalHsNocturnas = totalHsNocturnas.add(j.getHorasNocturnas());
        }

        BigDecimal totalHsTrabajadas = totalHsNormales.add(totalHsExtras).add(totalHsFeriado).setScale(2, RoundingMode.HALF_UP);

        BigDecimal impNormales = totalHsNormales.multiply(valorHoraBase).setScale(2, RoundingMode.HALF_UP);
        BigDecimal factorExtra = BigDecimal.ONE.add(porcExtra.divide(CIEN, 6, RoundingMode.HALF_UP));
        BigDecimal impExtras = totalHsExtras.multiply(valorHoraBase).multiply(factorExtra).setScale(2, RoundingMode.HALF_UP);
        BigDecimal factorFeriado = BigDecimal.ONE.add(porcFeriado.divide(CIEN, 6, RoundingMode.HALF_UP));
        BigDecimal impFeriado = totalHsFeriado.multiply(valorHoraBase).multiply(factorFeriado).setScale(2, RoundingMode.HALF_UP);
        BigDecimal impNocturno = totalHsNocturnas.multiply(valorHoraBase)
                .multiply(porcNocturno.divide(CIEN, 6, RoundingMode.HALF_UP)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal subtotalHaberes = impNormales.add(impExtras).add(impFeriado).add(impNocturno).setScale(2, RoundingMode.HALF_UP);

        List<AdelantoSueldo> adelantosPend = adelantoRepository
                .findByUsuarioIdAndDescontadoFalseAndActivoTrueOrderByFechaAdelantoAsc(usuarioId);
        BigDecimal totalAdelantos = adelantosPend.stream()
                .map(AdelantoSueldo::getMonto)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);

        BigDecimal netoSugerido = subtotalHaberes.subtract(totalAdelantos).setScale(2, RoundingMode.HALF_UP);

        return new PreliquidacionSueldoResponseDto(
                empleado.getId(),
                empleado.getUsername(),
                empleado.getApellido() + ", " + empleado.getNombre(),
                empleado.getDni(),
                empleado.getLegajo(),
                fechaDesde,
                fechaHasta,
                valorHoraBase,
                origenValorHora,
                porcExtra,
                porcFeriado,
                porcNocturno,
                totalHsNormales,
                totalHsExtras,
                totalHsFeriado,
                totalHsNocturnas,
                totalHsTrabajadas,
                impNormales,
                impExtras,
                impFeriado,
                impNocturno,
                subtotalHaberes,
                totalAdelantos,
                netoSugerido,
                jornadasNoLiquidadas.stream().map(this::toAsistenciaResponseDto).toList(),
                adelantosPend.stream().map(this::toAdelantoResponseDto).toList()
        );
    }

    @Transactional
    public LiquidacionSueldoResponseDto emitirLiquidacion(
            EmitirLiquidacionSueldoRequestDto dto,
            String operadorUsername) {

        Usuario empleado = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + dto.usuarioId()));
        Sucursal sucursal = sucursalRepository.findById(dto.sucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + dto.sucursalId()));

        PreliquidacionSueldoResponseDto pre = calcularPreliquidacion(
                dto.usuarioId(),
                dto.fechaDesde(),
                dto.fechaHasta()
        );

        List<AsistenciaJornadaTrabajo> jornadasALiquidar = asistenciaRepository
                .findByUsuarioIdAndFechaJornadaBetweenOrderByFechaJornadaAsc(dto.usuarioId(), dto.fechaDesde(), dto.fechaHasta())
                .stream()
                .filter(j -> j.isActivo() && !j.isLiquidada() && j.getEstadoJornada() == EstadoJornadaTrabajo.TRABAJADA)
                .toList();

        BigDecimal totalBonos = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalOtrosDesc = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (dto.conceptosManuales() != null) {
            for (EmitirLiquidacionSueldoRequestDto.ConceptoManualRequestDto cm : dto.conceptosManuales()) {
                BigDecimal m = cm.monto().setScale(2, RoundingMode.HALF_UP);
                if (cm.tipoConcepto() == TipoConceptoLiquidacion.HABER_ADICIONAL) {
                    totalBonos = totalBonos.add(m);
                } else {
                    totalOtrosDesc = totalOtrosDesc.add(m);
                }
            }
        }

        boolean descontarAdelantos = dto.descontarAdelantosPendientes() == null || dto.descontarAdelantosPendientes();
        List<AdelantoSueldo> adelantosPendientes = descontarAdelantos
                ? adelantoRepository.findByUsuarioIdAndDescontadoFalseAndActivoTrueOrderByFechaAdelantoAsc(dto.usuarioId())
                : List.of();

        BigDecimal totalAdelantos = adelantosPendientes.stream()
                .map(AdelantoSueldo::getMonto)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);

        if (jornadasALiquidar.isEmpty() && totalBonos.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("No existen horas trabajadas pendientes de liquidar ni bonos adicionales en el período seleccionado.");
        }

        BigDecimal netoFinal = pre.subtotalHaberesHoras()
                .add(totalBonos)
                .subtract(totalAdelantos)
                .subtract(totalOtrosDesc)
                .setScale(2, RoundingMode.HALF_UP);

        LiquidacionSueldo liq = new LiquidacionSueldo();
        liq.setNumeroReciboInterno(generarNumeroReciboSueldo());
        liq.setUsuario(empleado);
        liq.setSucursal(sucursal);
        liq.setFechaDesde(dto.fechaDesde());
        liq.setFechaHasta(dto.fechaHasta());
        liq.setFechaLiquidacion(dto.fechaLiquidacion() != null ? dto.fechaLiquidacion() : LocalDate.now());
        liq.setValorHoraBaseAplicado(pre.valorHoraBaseAplicado());
        liq.setOrigenValorHora(pre.origenValorHora());
        liq.setPorcentajeExtraAplicado(pre.porcentajeExtraAplicado());
        liq.setPorcentajeFeriadoAplicado(pre.porcentajeFeriadoAplicado());
        liq.setPorcentajeNocturnoAplicado(pre.porcentajeNocturnoAplicado());
        liq.setTotalHorasNormales(pre.totalHorasNormales());
        liq.setTotalHorasExtras(pre.totalHorasExtras());
        liq.setTotalHorasFeriado(pre.totalHorasFeriado());
        liq.setTotalHorasNocturnas(pre.totalHorasNocturnas());
        liq.setTotalHorasTrabajadas(pre.totalHorasTrabajadas());
        liq.setImporteHorasNormales(pre.importeHorasNormales());
        liq.setImporteHorasExtras(pre.importeHorasExtras());
        liq.setImporteHorasFeriado(pre.importeHorasFeriado());
        liq.setImporteAdicionalNocturno(pre.importeAdicionalNocturno());
        liq.setSubtotalHaberesHoras(pre.subtotalHaberesHoras());
        liq.setTotalBonosAdicionales(totalBonos);
        liq.setTotalAdelantosDescontados(totalAdelantos);
        liq.setTotalOtrosDescuentos(totalOtrosDesc);
        liq.setNetoACobrar(netoFinal);
        liq.setEstado("PAGADA");
        liq.setMedioPago(dto.medioPago().trim().toUpperCase());
        liq.setFechaPago(LocalDateTime.now());
        liq.setObservaciones(limpiarTextoOpcional(dto.observaciones()));
        liq.setLiquidadoPorUsername(operadorUsername);
        liq.setActivo(true);

        for (AdelantoSueldo ad : adelantosPendientes) {
            LiquidacionConcepto lc = new LiquidacionConcepto();
            lc.setLiquidacion(liq);
            lc.setTipoConcepto(TipoConceptoLiquidacion.DESCUENTO_ADELANTO);
            lc.setDescripcion("Adelanto del " + ad.getFechaAdelanto() + ": " + ad.getMotivo());
            lc.setMonto(ad.getMonto());
            lc.setAdelanto(ad);
            lc.setActivo(true);
            liq.getConceptos().add(lc);
        }

        if (dto.conceptosManuales() != null) {
            for (EmitirLiquidacionSueldoRequestDto.ConceptoManualRequestDto cm : dto.conceptosManuales()) {
                LiquidacionConcepto lc = new LiquidacionConcepto();
                lc.setLiquidacion(liq);
                lc.setTipoConcepto(cm.tipoConcepto());
                lc.setDescripcion(cm.descripcion().trim());
                lc.setMonto(cm.monto().setScale(2, RoundingMode.HALF_UP));
                lc.setActivo(true);
                liq.getConceptos().add(lc);
            }
        }

        LiquidacionSueldo guardada = liquidacionRepository.save(liq);

        // Regla 3.2.2: Marcar jornadas y adelantos como liquidados para impedir doble liquidación futura
        for (AsistenciaJornadaTrabajo j : jornadasALiquidar) {
            j.setLiquidada(true);
            j.setLiquidacion(guardada);
            asistenciaRepository.save(j);
        }
        for (AdelantoSueldo ad : adelantosPendientes) {
            ad.setDescontado(true);
            ad.setLiquidacion(guardada);
            adelantoRepository.save(ad);
        }

        return toLiquidacionResponseDto(guardada);
    }

    @Transactional(readOnly = true)
    public List<LiquidacionSueldoResponseDto> listarLiquidaciones(Long usuarioId, Long sucursalId) {
        List<LiquidacionSueldo> lista;
        if (usuarioId != null) {
            lista = liquidacionRepository.findByUsuarioIdOrderByFechaLiquidacionDescIdDesc(usuarioId);
        } else if (sucursalId != null) {
            lista = liquidacionRepository.findBySucursalIdOrderByFechaLiquidacionDescIdDesc(sucursalId);
        } else {
            lista = liquidacionRepository.findAllByOrderByFechaLiquidacionDescIdDesc();
        }
        return lista.stream().map(this::toLiquidacionResponseDto).toList();
    }

    @Transactional(readOnly = true)
    public LiquidacionSueldoResponseDto obtenerLiquidacionPorId(Long id) {
        LiquidacionSueldo liq = liquidacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidación no encontrada: " + id));
        return toLiquidacionResponseDto(liq);
    }

    /* =========================================================================
       HELPERS DE CÁLCULO HORARIO Y MAPEO DTO
       ========================================================================= */
    private ConfiguracionLaboralSueldos asegurarConfiguracionLaboral() {
        return configLaboralRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    ConfiguracionLaboralSueldos nueva = new ConfiguracionLaboralSueldos();
                    nueva.setActivo(true);
                    return configLaboralRepository.save(nueva);
                });
    }

    private BigDecimal calcularDuracionHorasEntre(LocalTime entrada, LocalTime salida) {
        long minEntrada = entrada.getHour() * 60L + entrada.getMinute();
        long minSalida = salida.getHour() * 60L + salida.getMinute();
        long difMinutos = minSalida >= minEntrada
                ? (minSalida - minEntrada)
                : ((24L * 60L - minEntrada) + minSalida);
        return BigDecimal.valueOf(difMinutos).divide(SESENTA, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularHorasNocturnasEnRango(LocalTime entrada, LocalTime salida) {
        if (entrada == null || salida == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        int minEntrada = entrada.getHour() * 60 + entrada.getMinute();
        int minSalida = salida.getHour() * 60 + salida.getMinute();
        if (minSalida <= minEntrada) {
            minSalida += 24 * 60;
        }
        int minutosNocturnos = 0;
        for (int m = minEntrada; m < minSalida; m++) {
            int horaReloj = (m / 60) % 24;
            if (horaReloj >= 21 || horaReloj < 6) {
                minutosNocturnos++;
            }
        }
        return BigDecimal.valueOf(minutosNocturnos).divide(SESENTA, 2, RoundingMode.HALF_UP);
    }

    private String generarNumeroReciboSueldo() {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        int rnd = ThreadLocalRandom.current().nextInt(10, 99);
        return "REC-" + fecha + "-" + rnd;
    }

    private HorarioAsignadoResponseDto toHorarioResponseDto(HorarioAsignadoEmpleado h) {
        int idxDia = Math.max(1, Math.min(7, h.getDiaSemana())) - 1;
        return new HorarioAsignadoResponseDto(
                h.getId(),
                h.getUsuario().getId(),
                h.getUsuario().getUsername(),
                h.getUsuario().getApellido() + ", " + h.getUsuario().getNombre(),
                h.getSucursal().getId(),
                h.getSucursal().getCodigo(),
                h.getSucursal().getNombre(),
                h.getDiaSemana(),
                NOMBRES_DIAS[idxDia],
                h.getHoraEntrada(),
                h.getHoraSalida(),
                h.getHorasTeoricas(),
                h.getObservaciones(),
                h.isActivo()
        );
    }

    private AsistenciaJornadaResponseDto toAsistenciaResponseDto(AsistenciaJornadaTrabajo a) {
        return new AsistenciaJornadaResponseDto(
                a.getId(),
                a.getUsuario().getId(),
                a.getUsuario().getUsername(),
                a.getUsuario().getApellido() + ", " + a.getUsuario().getNombre(),
                a.getSucursal().getId(),
                a.getSucursal().getCodigo(),
                a.getSucursal().getNombre(),
                a.getFechaJornada(),
                a.getOrigenRegistro(),
                a.getOrigenRegistro().getDescripcion(),
                a.getEstadoJornada(),
                a.getEstadoJornada().getDescripcion(),
                a.getHoraEntradaProgramada(),
                a.getHoraSalidaProgramada(),
                a.getFechaHoraIngresoReal(),
                a.getFechaHoraEgresoReal(),
                a.isEsFeriadoODomingo(),
                a.getHorasNormales(),
                a.getHorasExtrasSimples(),
                a.getHorasFeriadoDomingo(),
                a.getHorasNocturnas(),
                a.getTotalHorasComputadas(),
                a.isLiquidada(),
                a.getLiquidacion() != null ? a.getLiquidacion().getId() : null,
                a.getObservaciones()
        );
    }

    private AdelantoSueldoResponseDto toAdelantoResponseDto(AdelantoSueldo ad) {
        return new AdelantoSueldoResponseDto(
                ad.getId(),
                ad.getUsuario().getId(),
                ad.getUsuario().getUsername(),
                ad.getUsuario().getApellido() + ", " + ad.getUsuario().getNombre(),
                ad.getSucursal().getId(),
                ad.getSucursal().getNombre(),
                ad.getFechaAdelanto(),
                ad.getMonto(),
                ad.getMedioPago(),
                ad.getMotivo(),
                ad.isDescontado(),
                ad.getLiquidacion() != null ? ad.getLiquidacion().getId() : null,
                ad.getRegistradoPorUsername()
        );
    }

    private LiquidacionSueldoResponseDto toLiquidacionResponseDto(LiquidacionSueldo l) {
        List<LiquidacionSueldoResponseDto.ConceptoLiquidacionResponseDto> conceptosDto = l.getConceptos().stream()
                .map(c -> new LiquidacionSueldoResponseDto.ConceptoLiquidacionResponseDto(
                        c.getId(),
                        c.getTipoConcepto(),
                        c.getTipoConcepto().getDescripcion(),
                        c.getDescripcion(),
                        c.getMonto(),
                        c.getAdelanto() != null ? c.getAdelanto().getId() : null
                ))
                .toList();

        List<AsistenciaJornadaResponseDto> jornadasDto = asistenciaRepository
                .findByLiquidacionIdOrderByFechaJornadaAsc(l.getId()).stream()
                .map(this::toAsistenciaResponseDto)
                .toList();

        return new LiquidacionSueldoResponseDto(
                l.getId(),
                l.getNumeroReciboInterno(),
                l.getUsuario().getId(),
                l.getUsuario().getUsername(),
                l.getUsuario().getApellido() + ", " + l.getUsuario().getNombre(),
                l.getUsuario().getDni(),
                l.getUsuario().getLegajo(),
                l.getSucursal().getId(),
                l.getSucursal().getCodigo(),
                l.getSucursal().getNombre(),
                l.getSucursal().getRazonSocial(),
                l.getSucursal().getCuit(),
                l.getSucursal().getDireccion(),
                l.getFechaDesde(),
                l.getFechaHasta(),
                l.getFechaLiquidacion(),
                l.getValorHoraBaseAplicado(),
                l.getOrigenValorHora(),
                l.getPorcentajeExtraAplicado(),
                l.getPorcentajeFeriadoAplicado(),
                l.getPorcentajeNocturnoAplicado(),
                l.getTotalHorasNormales(),
                l.getTotalHorasExtras(),
                l.getTotalHorasFeriado(),
                l.getTotalHorasNocturnas(),
                l.getTotalHorasTrabajadas(),
                l.getImporteHorasNormales(),
                l.getImporteHorasExtras(),
                l.getImporteHorasFeriado(),
                l.getImporteAdicionalNocturno(),
                l.getSubtotalHaberesHoras(),
                l.getTotalBonosAdicionales(),
                l.getTotalAdelantosDescontados(),
                l.getTotalOtrosDescuentos(),
                l.getNetoACobrar(),
                l.getEstado(),
                l.getMedioPago(),
                l.getFechaPago(),
                l.getObservaciones(),
                l.getLiquidadoPorUsername(),
                conceptosDto,
                jornadasDto
        );
    }

    private String limpiarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    public HorarioLiquidacionService(ConfiguracionLaboralSueldosRepository configLaboralRepository, RolRepository rolRepository, UsuarioRepository usuarioRepository, SucursalRepository sucursalRepository, HorarioAsignadoEmpleadoRepository horarioRepository, AsistenciaJornadaTrabajoRepository asistenciaRepository, AdelantoSueldoRepository adelantoRepository, LiquidacionSueldoRepository liquidacionRepository) {
        this.configLaboralRepository = configLaboralRepository;
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.sucursalRepository = sucursalRepository;
        this.horarioRepository = horarioRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.adelantoRepository = adelantoRepository;
        this.liquidacionRepository = liquidacionRepository;
    }

}
