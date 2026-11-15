const AppState = {
    sesion: null,
    sucursales: [],
    usuarios: [],
    proveedores: [],
    categorias: [],
    marcas: [],
    productos: [],
    mediosPago: [],
    turnoCajaActual: null,
    ventasHistorial: [],
    turnosHistorial: [],
    posCarrito: [],
    posPagos: [],
    posSupervisorAuth: null,
    productoEtiquetaActual: null,
    ventaTicketActual: null,
    configuracionesArca: [],
    configArcaSucursalActiva: null,
    ventaAFacturarArca: null,
    comprobanteArcaActual: null,
    ventaComprobanteArcaActual: null,
    formatoImpresionArca: 'A4',
    dashboardBi: null,
    biModoEvolucion: 'DIA',
    biModoRanking: 'FACTURACION',
    biModoFlujo: 'HORA',
    biModoRubro: 'CATEGORIA',
    sueldosConfigTarifas: null,
    sueldosPreliquidacionActual: null,
    sueldosConceptosManuales: [],
    sueldosAsistenciasCache: [],
    sueldosLiquidacionReciboActual: null,
    sueldosSubTabActiva: 'liquidacion',
    fichajeJornadaEnCurso: null,
    catalogoRolesPermisos: null,
    seccionActiva: 'pos'
};

document.addEventListener('DOMContentLoaded', () => {
    bindEvents();
    verificarSesionInicial();
});

function bindEvents() {
    // Atajos de teclado globales y POS (F2 = foco en escáner, F4 = confirmar venta, ESC = cerrar modal / cancelar)
    document.addEventListener('keydown', (e) => {
        if (!AppState.sesion) return;

        if (e.key === 'Escape') {
            // 1. Si hay un modal abierto, cerrarlo inmediatamente
            const openModal = Array.from(document.querySelectorAll('.modal-backdrop'))
                .find(m => !m.classList.contains('hidden'));
            if (openModal) {
                e.preventDefault();
                openModal.classList.add('hidden');
                return;
            }

            // 2. Si estamos en POS, cerrar predictivo, limpiar buscador o vaciar carrito
            if (AppState.seccionActiva === 'pos') {
                const dropdown = document.getElementById('pos-predictive-dropdown');
                const scanEl = document.getElementById('pos-scan-input');
                if (dropdown && !dropdown.classList.contains('hidden')) {
                    e.preventDefault();
                    dropdown.classList.add('hidden');
                    return;
                }
                if (scanEl && scanEl.value.trim() !== '') {
                    e.preventDefault();
                    scanEl.value = '';
                    scanEl.focus();
                    return;
                }
                if (AppState.posCarrito.length > 0) {
                    e.preventDefault();
                    vaciarCarritoPos();
                    return;
                }
            }
            return;
        }

        if (AppState.seccionActiva !== 'pos') return;
        if (e.key === 'F2') {
            e.preventDefault();
            const scanEl = document.getElementById('pos-scan-input');
            if (scanEl) {
                scanEl.focus();
                scanEl.select();
            }
        } else if (e.key === 'F4') {
            e.preventDefault();
            onConfirmarVentaPos();
        }
    });

    // Login & Logout
    document.getElementById('form-login').addEventListener('submit', onLoginSubmit);
    document.getElementById('btn-logout').addEventListener('click', onLogout);
    document.getElementById('btn-logout-from-selector').addEventListener('click', onLogout);

    // Cambio de contraseña
    document.getElementById('form-change-password').addEventListener('submit', onChangePasswordSubmit);
    document.getElementById('btn-cancel-cp').addEventListener('click', onCancelChangePassword);
    document.getElementById('btn-open-self-password').addEventListener('click', abrirVistaCambioPasswordVoluntario);

    // Selector de sucursal / contexto
    document.getElementById('btn-switch-branch').addEventListener('click', mostrarSelectorSucursal);

    // Navegación lateral
    ['pos', 'caja', 'estadisticas', 'productos', 'proveedores', 'sucursales', 'arca', 'sueldos', 'usuarios'].forEach(sec => {
        const btn = document.getElementById(`nav-btn-${sec}`);
        if (btn) btn.addEventListener('click', () => cambiarSeccion(sec));
    });

    // POS: Escáner de Barras / QR y Buscador Predictivo
    const scanInput = document.getElementById('pos-scan-input');
    scanInput.addEventListener('input', onPosScanInputChange);
    scanInput.addEventListener('keydown', onPosScanKeyDown);
    document.addEventListener('click', (e) => {
        if (!e.target.closest('.scanner-input-wrap')) {
            document.getElementById('pos-predictive-dropdown').classList.add('hidden');
        }
    });

    document.getElementById('btn-pos-vaciar').addEventListener('click', vaciarCarritoPos);
    document.getElementById('pos-desc-global').addEventListener('input', recalcularTotalesPos);
    document.getElementById('btn-pos-unlock-discount').addEventListener('click', () => {
        abrirModalSupervisorConPermiso('APLICAR_DESCUENTOS_ESPECIALES');
    });
    document.getElementById('btn-pos-add-pago').addEventListener('click', agregarLineaPagoPos);
    document.getElementById('btn-pos-confirmar-venta').addEventListener('click', onConfirmarVentaPos);
    document.getElementById('btn-config-medios-pago').addEventListener('click', abrirModalConfigMediosPago);
    document.getElementById('btn-close-medios-modal').addEventListener('click', cerrarModalConfigMediosPago);
    document.getElementById('btn-cancel-medios-modal').addEventListener('click', cerrarModalConfigMediosPago);

    // Caja y Turnos
    document.getElementById('btn-caja-abrir').addEventListener('click', abrirModalAbrirCaja);
    document.getElementById('btn-close-abrir-caja').addEventListener('click', cerrarModalAbrirCaja);
    document.getElementById('btn-cancel-abrir-caja').addEventListener('click', cerrarModalAbrirCaja);
    document.getElementById('form-abrir-caja').addEventListener('submit', onConfirmarAbrirCaja);

    document.getElementById('btn-caja-movimiento').addEventListener('click', abrirModalMovimientoCaja);
    document.getElementById('btn-close-mov-caja').addEventListener('click', cerrarModalMovimientoCaja);
    document.getElementById('btn-cancel-mov-caja').addEventListener('click', cerrarModalMovimientoCaja);
    document.getElementById('form-mov-caja').addEventListener('submit', onConfirmarMovimientoCaja);

    document.getElementById('btn-caja-cerrar').addEventListener('click', abrirModalCerrarCaja);
    document.getElementById('btn-close-cerrar-caja').addEventListener('click', cerrarModalCerrarCaja);
    document.getElementById('btn-cancel-cerrar-caja').addEventListener('click', cerrarModalCerrarCaja);
    document.getElementById('caja-efectivo-declarado').addEventListener('input', actualizarPreviewDiferenciaArqueo);
    document.getElementById('form-cerrar-caja').addEventListener('submit', onConfirmarCerrarCaja);

    document.getElementById('flt-ventas-facturacion').addEventListener('change', cargarSeccionCajaEHistorial);

    // Modal Anular Venta
    document.getElementById('btn-close-anular-venta').addEventListener('click', cerrarModalAnularVenta);
    document.getElementById('btn-cancel-anular-venta').addEventListener('click', cerrarModalAnularVenta);
    document.getElementById('form-anular-venta').addEventListener('submit', onConfirmarAnularVenta);

    // Modal Ticket Venta
    document.getElementById('btn-close-ticket-modal').addEventListener('click', cerrarModalTicketVenta);
    document.getElementById('btn-cancel-ticket-modal').addEventListener('click', cerrarModalTicketVenta);
    document.getElementById('btn-imprimir-ticket').addEventListener('click', imprimirTicketVentaActual);
    document.getElementById('btn-facturar-desde-ticket').addEventListener('click', () => {
        if (AppState.ventaTicketActual) {
            const ventaId = AppState.ventaTicketActual.id;
            cerrarModalTicketVenta();
            abrirModalFacturarVentaArca(ventaId);
        }
    });

    // Fase 4: Panel de Configuración ARCA y Facturación Electrónica
    document.getElementById('arca-cfg-sucursal-select').addEventListener('change', onCambioSucursalConfigArca);
    document.getElementById('arca-entorno').addEventListener('change', onCambioEntornoConfigArca);
    document.getElementById('arca-file-crt').addEventListener('change', (e) => leerArchivoPemEnTextarea(e, 'arca-pem-crt'));
    document.getElementById('arca-file-key').addEventListener('change', (e) => leerArchivoPemEnTextarea(e, 'arca-pem-key'));
    document.getElementById('form-config-arca').addEventListener('submit', onGuardarConfiguracionArca);
    document.getElementById('btn-arca-probar-wsaa').addEventListener('click', onProbarConexionArcaClick);

    document.getElementById('btn-close-facturar-arca').addEventListener('click', cerrarModalFacturarArca);
    document.getElementById('btn-cancel-facturar-arca').addEventListener('click', cerrarModalFacturarArca);
    document.getElementById('facturar-arca-condicion-iva').addEventListener('change', actualizarPreviewModalFacturarArca);
    document.getElementById('facturar-arca-doc').addEventListener('input', actualizarPreviewModalFacturarArca);
    document.getElementById('form-facturar-arca').addEventListener('submit', onConfirmarFacturarVentaArca);

    document.getElementById('btn-vista-arca-ticket').addEventListener('click', () => cambiarFormatoVistaComprobanteArca('TICKET'));
    document.getElementById('btn-vista-arca-a4').addEventListener('click', () => cambiarFormatoVistaComprobanteArca('A4'));
    document.getElementById('btn-close-comp-arca').addEventListener('click', cerrarModalComprobanteArca);
    document.getElementById('btn-cancel-comp-arca').addEventListener('click', cerrarModalComprobanteArca);
    document.getElementById('btn-imprimir-comp-arca').addEventListener('click', imprimirComprobanteArcaActual);

    document.getElementById('btn-close-intentos-arca').addEventListener('click', cerrarModalIntentosArca);
    document.getElementById('btn-cancel-intentos-arca').addEventListener('click', cerrarModalIntentosArca);

    // Fase 5: Panel Estadístico e Inteligencia de Negocio (BI)
    document.getElementById('btn-bi-actualizar').addEventListener('click', cargarDashboardEstadistico);
    document.getElementById('bi-flt-sucursal').addEventListener('change', cargarDashboardEstadistico);
    document.getElementById('bi-flt-facturacion').addEventListener('change', cargarDashboardEstadistico);
    document.getElementById('btn-bi-export-csv').addEventListener('click', exportarBiCsvExcel);
    document.getElementById('btn-bi-export-pdf').addEventListener('click', exportarBiInformePdf);

    document.getElementById('btn-bi-evo-dia').addEventListener('click', () => cambiarModoEvolucionBi('DIA'));
    document.getElementById('btn-bi-evo-mes').addEventListener('click', () => cambiarModoEvolucionBi('MES'));
    document.getElementById('btn-bi-rank-fact').addEventListener('click', () => cambiarModoRankingBi('FACTURACION'));
    document.getElementById('btn-bi-rank-unid').addEventListener('click', () => cambiarModoRankingBi('UNIDADES'));
    document.getElementById('btn-bi-flujo-hora').addEventListener('click', () => cambiarModoFlujoBi('HORA'));
    document.getElementById('btn-bi-flujo-dia').addEventListener('click', () => cambiarModoFlujoBi('DIA_SEMANA'));
    document.getElementById('btn-bi-rubro-cat').addEventListener('click', () => cambiarModoRubroBi('CATEGORIA'));
    document.getElementById('btn-bi-rubro-prov').addEventListener('click', () => cambiarModoRubroBi('PROVEEDOR'));
    document.getElementById('btn-bi-rubro-suc').addEventListener('click', () => cambiarModoRubroBi('SUCURSAL'));

    // Filtros instantáneos de Productos
    document.getElementById('flt-prod-q').addEventListener('input', aplicarFiltrosProductosLocal);
    document.getElementById('flt-prod-categoria').addEventListener('change', aplicarFiltrosProductosLocal);
    document.getElementById('flt-prod-marca').addEventListener('change', aplicarFiltrosProductosLocal);
    document.getElementById('flt-prod-proveedor').addEventListener('change', aplicarFiltrosProductosLocal);
    document.getElementById('flt-prod-bajo-stock').addEventListener('change', aplicarFiltrosProductosLocal);
    document.getElementById('btn-limpiar-filtros-prod').addEventListener('click', limpiarFiltrosProductos);

    // Modal Productos
    document.getElementById('btn-nuevo-producto').addEventListener('click', () => abrirModalProducto(null));
    document.getElementById('btn-close-producto-modal').addEventListener('click', cerrarModalProducto);
    document.getElementById('btn-cancel-producto-modal').addEventListener('click', cerrarModalProducto);
    document.getElementById('form-producto').addEventListener('submit', onGuardarProducto);
    document.getElementById('btn-generar-ean').addEventListener('click', onGenerarCodigoInternoClick);
    document.getElementById('btn-quick-categoria').addEventListener('click', onCrearCategoriaRapida);
    document.getElementById('btn-quick-marca').addEventListener('click', onCrearMarcaRapida);

    document.getElementById('prod-precio-compra').addEventListener('input', () => recalcularPreciosModal('COSTO_O_GANANCIA'));
    document.getElementById('prod-iva-incluido').addEventListener('change', () => recalcularPreciosModal('COSTO_O_GANANCIA'));
    document.getElementById('prod-alicuota-iva').addEventListener('change', () => recalcularPreciosModal('COSTO_O_GANANCIA'));
    document.getElementById('prod-ganancia').addEventListener('input', () => recalcularPreciosModal('COSTO_O_GANANCIA'));
    document.getElementById('prod-precio-venta').addEventListener('input', () => recalcularPreciosModal('PRECIO_VENTA'));
    document.getElementById('prod-stock-min-defecto').addEventListener('input', onStockMinimoDefectoChange);

    // Modal Actualización Masiva
    document.getElementById('btn-aumento-masivo').addEventListener('click', abrirModalAumentoMasivo);
    document.getElementById('btn-close-masivo-modal').addEventListener('click', cerrarModalAumentoMasivo);
    document.getElementById('btn-cancel-masivo-modal').addEventListener('click', cerrarModalAumentoMasivo);
    document.getElementById('mas-modo-redondeo').addEventListener('change', onModoRedondeoMasivoChange);
    document.getElementById('form-aumento-masivo').addEventListener('submit', onEjecutarAumentoMasivo);

    // Modal Etiqueta / QR
    document.getElementById('btn-close-etiqueta-modal').addEventListener('click', cerrarModalEtiqueta);
    document.getElementById('btn-cancel-etiqueta-modal').addEventListener('click', cerrarModalEtiqueta);
    document.getElementById('btn-imprimir-etiqueta').addEventListener('click', imprimirEtiquetaActual);

    // Proveedores
    document.getElementById('flt-prov-q').addEventListener('input', aplicarFiltroProveedoresLocal);
    document.getElementById('btn-nuevo-proveedor').addEventListener('click', () => abrirModalProveedor(null));
    document.getElementById('btn-close-proveedor-modal').addEventListener('click', cerrarModalProveedor);
    document.getElementById('btn-cancel-proveedor-modal').addEventListener('click', cerrarModalProveedor);
    document.getElementById('form-proveedor').addEventListener('submit', onGuardarProveedor);

    // Sucursales
    document.getElementById('btn-nueva-sucursal').addEventListener('click', () => abrirModalSucursal(null));
    document.getElementById('btn-close-modal').addEventListener('click', cerrarModalSucursal);
    document.getElementById('btn-cancel-modal').addEventListener('click', cerrarModalSucursal);
    document.getElementById('form-sucursal').addEventListener('submit', onGuardarSucursal);

    // Usuarios
    document.getElementById('btn-nuevo-usuario').addEventListener('click', () => abrirModalUsuario(null));
    document.getElementById('btn-close-usuario-modal').addEventListener('click', cerrarModalUsuario);
    document.getElementById('btn-cancel-usuario-modal').addEventListener('click', cerrarModalUsuario);
    document.getElementById('form-usuario').addEventListener('submit', onGuardarUsuario);
    document.getElementById('usr-acceso-todas').addEventListener('change', onToggleAccesoTodasSucursales);

    // Blanqueo Password
    document.getElementById('btn-close-reset-modal').addEventListener('click', cerrarModalResetPass);
    document.getElementById('btn-cancel-reset-modal').addEventListener('click', cerrarModalResetPass);
    document.getElementById('form-reset-pass').addEventListener('submit', onConfirmarResetPass);

    // Autorización de Supervisor
    document.getElementById('btn-test-supervisor').addEventListener('click', () => abrirModalSupervisorConPermiso('APLICAR_DESCUENTOS_ESPECIALES'));
    document.getElementById('btn-close-sup-modal').addEventListener('click', cerrarModalSupervisor);
    document.getElementById('btn-cancel-sup-modal').addEventListener('click', cerrarModalSupervisor);
    document.getElementById('form-supervisor').addEventListener('submit', onAutorizarSupervisorSubmit);

    // Fase 6: Fichaje en Topbar, Horarios, Asistencias, Adelantos y Liquidación de Sueldos
    document.getElementById('btn-fichaje-topbar').addEventListener('click', onToggleFichajeRelojTopbar);

    document.querySelectorAll('.sueldos-tab-btn').forEach(btn => {
        btn.addEventListener('click', () => cambiarSubTabSueldos(btn.dataset.tab));
    });

    document.getElementById('liq-usuario-select').addEventListener('change', onCambioEmpleadoLiquidacion);
    document.getElementById('btn-liq-calcular').addEventListener('click', calcularPreliquidacionActual);
    document.getElementById('liq-chk-descontar-adelantos').addEventListener('change', recalcularNetoFinalVistaPreliquidacion);
    document.getElementById('btn-liq-add-concepto').addEventListener('click', agregarConceptoManualPreliquidacion);
    document.getElementById('btn-liq-emitir-cerrar').addEventListener('click', emitirYCerrarLiquidacionSueldo);
    document.getElementById('flt-liq-hist-usuario').addEventListener('change', cargarHistorialLiquidaciones);
    document.getElementById('btn-refresh-liq-hist').addEventListener('click', cargarHistorialLiquidaciones);

    document.getElementById('btn-filtrar-asistencias').addEventListener('click', cargarListadoAsistencias);
    document.getElementById('btn-generar-jornadas-teoricas').addEventListener('click', generarJornadasDesdeHorariosTeoricosClick);
    document.getElementById('btn-nueva-asistencia-manual').addEventListener('click', () => abrirModalAsistenciaManual(null));
    document.getElementById('btn-close-asist-modal').addEventListener('click', cerrarModalAsistenciaManual);
    document.getElementById('btn-cancel-asist-modal').addEventListener('click', cerrarModalAsistenciaManual);
    document.getElementById('form-asistencia-manual').addEventListener('submit', onGuardarAsistenciaManual);

    document.getElementById('form-nuevo-horario').addEventListener('submit', onCrearHorarioAsignado);
    document.getElementById('form-nuevo-adelanto').addEventListener('submit', onRegistrarAdelantoSueldo);
    document.getElementById('form-tarifas-sueldos').addEventListener('submit', onGuardarConfiguracionTarifasSueldos);

    document.getElementById('btn-close-recibo-sueldo').addEventListener('click', cerrarModalReciboSueldo);
    document.getElementById('btn-cerrar-recibo-sueldo-footer').addEventListener('click', cerrarModalReciboSueldo);
    document.getElementById('btn-imprimir-recibo-sueldo').addEventListener('click', imprimirReciboSueldoActual);
}

function esAdmin() {
    return AppState.sesion && (AppState.sesion.roles || []).includes('ADMIN');
}

function tienePermiso(perm) {
    if (!AppState.sesion) return false;
    return esAdmin() || (AppState.sesion.permisos || []).includes(perm);
}

function puedeVerCostos() {
    return esAdmin() || tienePermiso('CAMBIAR_PRECIOS_GLOBAL') || tienePermiso('CAMBIAR_PRECIOS_SUCURSAL');
}

function puedeEditarCatalogo() {
    return esAdmin() || tienePermiso('CAMBIAR_PRECIOS_GLOBAL') || tienePermiso('GESTIONAR_STOCK_SUCURSAL');
}

function puedeAplicarDescuentoDirecto() {
    return esAdmin() || tienePermiso('APLICAR_DESCUENTOS_ESPECIALES') || Boolean(AppState.posSupervisorAuth);
}

async function verificarSesionInicial() {
    try {
        const sesion = await ApiClient.get('/api/v1/auth/me');
        procesarEstadoSesion(sesion);
    } catch (err) {
        mostrarVista('view-login');
    }
}

async function onLoginSubmit(e) {
    e.preventDefault();
    const errorEl = document.getElementById('login-error');
    errorEl.classList.add('hidden');

    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;

    try {
        const sesion = await ApiClient.post('/api/v1/auth/login', { username, password });
        document.getElementById('login-password').value = '';
        procesarEstadoSesion(sesion);
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
}

function procesarEstadoSesion(sesion) {
    AppState.sesion = sesion;

    if (sesion.requiereCambioPassword) {
        document.getElementById('form-change-password').reset();
        document.getElementById('cp-error').classList.add('hidden');
        mostrarVista('view-change-password');
        return;
    }

    if (sesion.requiereSeleccionSucursal || (!sesion.sucursalActiva && !sesion.modoConsolidado)) {
        mostrarSelectorSucursal();
        return;
    }

    renderizarCabeceraUsuario(sesion);
    mostrarVista('view-app');
    cargarDatosSeccionActiva();
}

function abrirVistaCambioPasswordVoluntario() {
    document.getElementById('form-change-password').reset();
    document.getElementById('cp-error').classList.add('hidden');
    mostrarVista('view-change-password');
}

function onCancelChangePassword() {
    if (AppState.sesion && AppState.sesion.requiereCambioPassword) {
        onLogout();
    } else {
        mostrarVista('view-app');
    }
}

async function onChangePasswordSubmit(e) {
    e.preventDefault();
    const errorEl = document.getElementById('cp-error');
    errorEl.classList.add('hidden');

    const passwordActual = document.getElementById('cp-actual').value;
    const passwordNueva = document.getElementById('cp-nueva').value;
    const confirmacionPassword = document.getElementById('cp-confirmar').value;

    try {
        const sesionActualizada = await ApiClient.post('/api/v1/auth/cambiar-password', {
            passwordActual,
            passwordNueva,
            confirmacionPassword
        });
        showToast('Contraseña actualizada exitosamente');
        procesarEstadoSesion(sesionActualizada);
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
}

function mostrarSelectorSucursal() {
    const contenedor = document.getElementById('sucursales-selector-list');
    contenedor.innerHTML = '';

    const sesion = AppState.sesion;
    if (!sesion) return;

    if (sesion.puedeVerConsolidado) {
        const cardConsolidado = document.createElement('div');
        cardConsolidado.className = 'branch-card consolidated';
        cardConsolidado.innerHTML = `
            <strong>🌐 Todas las Sucursales (Vista Consolidada)</strong>
            <div style="margin-top:0.35rem;font-size:0.85rem;color:#1e40af;">
                Visualización unificada de sucursales, stock total, empleados y ventas. (Para cobrar en POS deberá elegir una sucursal).
            </div>
        `;
        cardConsolidado.addEventListener('click', () => seleccionarContextoSucursal({ modoConsolidado: true }));
        contenedor.appendChild(cardConsolidado);
    }

    (sesion.sucursalesDisponibles || []).forEach(suc => {
        const card = document.createElement('div');
        card.className = 'branch-card';
        card.innerHTML = `
            <strong>${escapeHtml(suc.codigo)} — ${escapeHtml(suc.nombre)}</strong>
            <div style="margin-top:0.35rem;font-size:0.85rem;color:#475569;">
                Punto de Venta ARCA: <b>${String(suc.puntoVentaArca).padStart(5, '0')}</b>
                ${suc.esCasaCentral ? ' • <span style="color:#2563eb;font-weight:600;">Casa Central</span>' : ''}
            </div>
        `;
        card.addEventListener('click', () => seleccionarContextoSucursal({ sucursalId: suc.id, modoConsolidado: false }));
        contenedor.appendChild(card);
    });

    mostrarVista('view-select-sucursal');
}

async function seleccionarContextoSucursal(payload) {
    try {
        const sesionActualizada = await ApiClient.post('/api/v1/auth/seleccionar-sucursal', payload);
        AppState.posCarrito = [];
        AppState.posPagos = [];
        showToast(sesionActualizada.modoConsolidado
            ? 'Vista Consolidada Multi-Sucursal activada'
            : 'Sucursal de operación seleccionada');
        procesarEstadoSesion(sesionActualizada);
    } catch (err) {
        showToast(err.message);
    }
}

function renderizarCabeceraUsuario(sesion) {
    document.getElementById('ui-user-fullname').textContent = sesion.nombreCompleto;
    const rolesContainer = document.getElementById('ui-user-roles');
    rolesContainer.innerHTML = (sesion.roles || [])
        .map(r => `<span class="role-badge">${escapeHtml(r)}</span>`)
        .join('');

    const pvEl = document.getElementById('ui-active-branch-pv');
    const thStockLabel = document.getElementById('th-stock-context-label');

    if (sesion.modoConsolidado) {
        document.getElementById('ui-active-branch-name').textContent = '🌐 Todas las Sucursales (Consolidado)';
        pvEl.textContent = 'GLOBAL';
        if (thStockLabel) thStockLabel.textContent = 'Total Consolidado';
    } else if (sesion.sucursalActiva) {
        document.getElementById('ui-active-branch-name').textContent =
            `${sesion.sucursalActiva.codigo} - ${sesion.sucursalActiva.nombre}`;
        pvEl.textContent = `PV ${String(sesion.sucursalActiva.puntoVentaArca).padStart(5, '0')}`;
        if (thStockLabel) thStockLabel.textContent = sesion.sucursalActiva.codigo;
    }

    const btnSwitch = document.getElementById('btn-switch-branch');
    if ((sesion.sucursalesDisponibles || []).length > 1 || sesion.puedeVerConsolidado) {
        btnSwitch.classList.remove('hidden');
    } else {
        btnSwitch.classList.add('hidden');
    }

    const admin = esAdmin();
    const verCostos = puedeVerCostos();
    const editarCat = puedeEditarCatalogo();
    const puedeGestionarUsuarios = admin
        || (sesion.roles || []).includes('ENCARGADO_SUCURSAL')
        || tienePermiso('GESTIONAR_EMPLEADOS_SUCURSAL');

    const puedeVerEstadisticas = admin
        || (sesion.roles || []).includes('ENCARGADO_SUCURSAL')
        || tienePermiso('VER_REPORTES_CONSOLIDADOS')
        || tienePermiso('VER_REPORTES_SUCURSAL');

    const puedeGestionarSueldos = admin
        || (sesion.roles || []).includes('ENCARGADO_SUCURSAL')
        || tienePermiso('SUELDOS_LIQUIDAR')
        || tienePermiso('HORARIOS_ASISTENCIAS_GESTIONAR');

    document.getElementById('th-prod-costo').classList.toggle('hidden', !verCostos);
    document.getElementById('wrap-flt-proveedor').classList.toggle('hidden', !verCostos && !editarCat);
    document.getElementById('nav-btn-proveedores').classList.toggle('hidden', !verCostos && !editarCat);
    document.getElementById('nav-btn-estadisticas').classList.toggle('hidden', !puedeVerEstadisticas);
    document.getElementById('btn-nuevo-producto').classList.toggle('hidden', !editarCat);
    document.getElementById('btn-aumento-masivo').classList.toggle('hidden', !admin && !tienePermiso('CAMBIAR_PRECIOS_GLOBAL'));
    document.getElementById('btn-nueva-sucursal').classList.toggle('hidden', !admin);
    document.getElementById('btn-config-medios-pago').classList.toggle('hidden', !admin);
    document.getElementById('nav-btn-arca').classList.toggle('hidden', !admin);
    document.getElementById('nav-btn-sueldos').classList.toggle('hidden', !puedeGestionarSueldos);
    document.getElementById('nav-btn-usuarios').classList.toggle('hidden', !puedeGestionarUsuarios);

    refrescarEstadoFichajeTopbar();
}

function cambiarSeccion(seccion) {
    AppState.seccionActiva = seccion;
    ['pos', 'caja', 'estadisticas', 'productos', 'proveedores', 'sucursales', 'arca', 'sueldos', 'usuarios'].forEach(sec => {
        const btn = document.getElementById(`nav-btn-${sec}`);
        const panel = document.getElementById(`section-${sec}`);
        if (btn) btn.classList.toggle('active', sec === seccion);
        if (panel) panel.classList.toggle('hidden', sec !== seccion);
    });

    cargarDatosSeccionActiva();
}

function cargarDatosSeccionActiva() {
    switch (AppState.seccionActiva) {
        case 'pos':
            cargarVistaPos();
            break;
        case 'caja':
            cargarSeccionCajaEHistorial();
            break;
        case 'estadisticas':
            inicializarYCargarSeccionEstadisticas();
            break;
        case 'productos':
            cargarVistaProductosCompleta();
            break;
        case 'proveedores':
            cargarProveedores();
            break;
        case 'arca':
            cargarPanelConfiguracionArca();
            break;
        case 'sueldos':
            inicializarYCargarSeccionSueldos();
            break;
        case 'usuarios':
            cargarUsuarios();
            break;
        case 'sucursales':
        default:
            cargarSucursales();
            break;
    }
}

/* =========================================================================
   FASE 3: PUNTO DE VENTA (POS), CARRITO Y PAGOS COMBINADOS
   ========================================================================= */
async function cargarVistaPos() {
    try {
        const [turnoInfo, mediosPago, productos] = await Promise.all([
            ApiClient.get('/api/v1/cajas/turno-actual'),
            ApiClient.get('/api/v1/ventas/medios-pago?soloActivos=true'),
            ApiClient.get('/api/v1/productos?soloActivos=true')
        ]);

        AppState.turnoCajaActual = turnoInfo.tieneTurnoAbierto ? turnoInfo.turno : null;
        AppState.mediosPago = mediosPago;
        AppState.productos = productos;

        renderizarEstadoCajaTopbarYBanner();
        actualizarEstadoDescuentoInputPos();

        if (AppState.posPagos.length === 0 && mediosPago.length > 0) {
            AppState.posPagos.push({
                medioPago: mediosPago[0].codigo,
                montoBase: 0,
                montoRecibidoEfectivo: '',
                cuotas: 1,
                referenciaOperacion: ''
            });
        }

        renderizarCarritoPos();
        const scanInput = document.getElementById('pos-scan-input');
        if (scanInput) scanInput.focus();
    } catch (err) {
        manejarErrorApi(err);
    }
}

function renderizarEstadoCajaTopbarYBanner() {
    const pillTop = document.getElementById('ui-caja-status-pill');
    const infoEl = document.getElementById('pos-caja-banner-info');
    const actionsEl = document.getElementById('pos-caja-banner-actions');

    if (AppState.sesion && AppState.sesion.modoConsolidado) {
        pillTop.className = 'status-pill status-locked';
        pillTop.textContent = 'Modo Consolidado';
        infoEl.innerHTML = `<strong style="color:#92400e;">⚠️ Está en Vista Consolidada.</strong> Para abrir caja y registrar ventas en el POS debe seleccionar una sucursal específica.`;
        actionsEl.innerHTML = `<button class="btn btn-primary btn-xs" onclick="mostrarSelectorSucursal()">Elegir Sucursal</button>`;
        return;
    }

    const t = AppState.turnoCajaActual;
    if (t) {
        pillTop.className = 'status-pill status-active';
        pillTop.textContent = `Caja Abierta ($${formatearMoneda(t.efectivoEsperado)})`;
        infoEl.innerHTML = `
            <span class="status-pill status-active">TURNO ABIERTO #${t.id}</span>
            <span style="margin-left:0.5rem;font-size:0.86rem;">
                Efectivo en Caja: <b>$ ${formatearMoneda(t.efectivoEsperado)}</b>
                • Ventas del Turno: <b>$ ${formatearMoneda(t.totalGeneralVentas)}</b>
            </span>
        `;
        actionsEl.innerHTML = `
            <button class="btn btn-outline btn-xs" onclick="abrirModalMovimientoCaja()">➕/➖ Ingreso / Retiro</button>
            <button class="btn btn-outline btn-xs" onclick="abrirModalCerrarCaja()">🔒 Cerrar Caja</button>
        `;
    } else {
        pillTop.className = 'status-pill status-inactive';
        pillTop.textContent = 'Caja Cerrada';
        infoEl.innerHTML = `
            <span class="status-pill status-inactive">CAJA CERRADA</span>
            <span style="margin-left:0.5rem;font-size:0.86rem;color:#b91c1c;">
                Debe abrir su Turno de Caja (indicando el fondo fijo inicial) antes de registrar ventas.
            </span>
        `;
        actionsEl.innerHTML = `
            <button class="btn btn-primary btn-sm" onclick="abrirModalAbrirCaja()">🔓 Abrir Turno de Caja Ahora</button>
        `;
    }
}

function actualizarEstadoDescuentoInputPos() {
    const descInput = document.getElementById('pos-desc-global');
    const btnUnlock = document.getElementById('btn-pos-unlock-discount');
    const supBadge = document.getElementById('pos-sup-badge');

    const habilitado = puedeAplicarDescuentoDirecto();
    descInput.disabled = !habilitado;
    btnUnlock.classList.toggle('hidden', habilitado);

    if (AppState.posSupervisorAuth) {
        supBadge.textContent = `✓ Autorizado por supervisor: ${AppState.posSupervisorAuth.username}`;
        supBadge.classList.remove('hidden');
    } else {
        supBadge.classList.add('hidden');
    }
}

function onPosScanInputChange(e) {
    const raw = e.target.value.trim();
    const dropdown = document.getElementById('pos-predictive-dropdown');
    if (raw.length < 2) {
        dropdown.classList.add('hidden');
        return;
    }

    const term = raw.toLowerCase();
    const matches = AppState.productos
        .filter(p => p.activo && (
            p.nombre.toLowerCase().includes(term)
            || p.codigoBarras.toLowerCase().includes(term)
            || (p.marcaNombre || '').toLowerCase().includes(term)
        ))
        .slice(0, 8);

    if (!matches.length) {
        dropdown.classList.add('hidden');
        return;
    }

    dropdown.innerHTML = matches.map(p => `
        <div class="predictive-item" onclick="seleccionarProductoPos(${p.id})">
            <div>
                <strong>${escapeHtml(p.nombre)}</strong>
                <div style="font-size:0.75rem;color:#64748b;">
                    Cód: ${escapeHtml(p.codigoBarras)} • Marca: ${escapeHtml(p.marcaNombre)}
                    • Stock: <b>${formatearCantidadSegunUnidad(p.stockContextoActual, p.permiteDecimales)} ${escapeHtml(p.unidadMedidaAbreviatura)}</b>
                </div>
            </div>
            <strong style="color:#2563eb;">$ ${formatearMoneda(p.precioVentaContextoEfectivo)}</strong>
        </div>
    `).join('');
    dropdown.classList.remove('hidden');
}

function onPosScanKeyDown(e) {
    if (e.key !== 'Enter') return;
    e.preventDefault();

    let raw = e.target.value.trim();
    if (!raw) return;

    // Soporte para lectura de Código QR personalizado (formato: codigoBarras|nombre|precio)
    if (raw.includes('|')) {
        raw = raw.split('|')[0].trim();
    }

    const exactMatch = AppState.productos.find(
        p => p.activo && p.codigoBarras.toLowerCase() === raw.toLowerCase()
    );

    if (exactMatch) {
        seleccionarProductoPos(exactMatch.id);
        return;
    }

    const partial = AppState.productos.filter(p =>
        p.activo && (
            p.nombre.toLowerCase().includes(raw.toLowerCase())
            || p.codigoBarras.toLowerCase().includes(raw.toLowerCase())
        )
    );

    if (partial.length === 1) {
        seleccionarProductoPos(partial[0].id);
    } else if (partial.length === 0) {
        showToast(`No se encontró ningún producto con "${raw}"`);
    }
}

window.seleccionarProductoPos = function (productoId) {
    const prod = AppState.productos.find(p => p.id === productoId);
    if (!prod) return;

    const qtyInput = document.getElementById('pos-scan-qty');
    let cantAgregar = parseFloat(qtyInput.value) || 1;
    if (!prod.permiteDecimales) {
        cantAgregar = Math.max(1, Math.round(cantAgregar));
    }

    const existente = AppState.posCarrito.find(item => item.producto.id === prod.id);
    if (existente) {
        existente.cantidad = Number((existente.cantidad + cantAgregar).toFixed(3));
    } else {
        AppState.posCarrito.push({
            producto: prod,
            cantidad: cantAgregar,
            descuentoPorcentaje: 0
        });
    }

    document.getElementById('pos-scan-input').value = '';
    qtyInput.value = '1';
    document.getElementById('pos-predictive-dropdown').classList.add('hidden');
    renderizarCarritoPos();
    document.getElementById('pos-scan-input').focus();
};

function renderizarCarritoPos() {
    const tbody = document.getElementById('tbody-pos-cart');
    document.getElementById('pos-cart-count').textContent = AppState.posCarrito.length;
    const puedeDesc = puedeAplicarDescuentoDirecto();

    if (!AppState.posCarrito.length) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center" style="padding:2rem;color:#64748b;">
                    El carrito está vacío. Escanee un código de barras/QR o busque un artículo arriba.
                </td>
            </tr>
        `;
        recalcularTotalesPos();
        return;
    }

    tbody.innerHTML = AppState.posCarrito.map((item, idx) => {
        const p = item.producto;
        const precioUnit = Number(p.precioVentaContextoEfectivo || 0);
        const bruto = precioUnit * item.cantidad;
        const subtotalItem = bruto * (1 - (item.descuentoPorcentaje || 0) / 100);
        const stockDisp = Number(p.stockContextoActual || 0);
        const sinStockSuficiente = item.cantidad > stockDisp;

        return `
            <tr class="${sinStockSuficiente ? 'row-low-stock' : ''}">
                <td>
                    <strong>${escapeHtml(p.nombre)}</strong>
                    <div style="font-size:0.74rem;color:#64748b;">${escapeHtml(p.codigoBarras)} • ${escapeHtml(p.marcaNombre)}</div>
                </td>
                <td>
                    <div>${formatearCantidadSegunUnidad(stockDisp, p.permiteDecimales)} ${escapeHtml(p.unidadMedidaAbreviatura)}</div>
                    ${sinStockSuficiente
                        ? '<span class="status-pill status-low-stock" title="Regla 3.1A: Se permite vender quedando stock en negativo">⚠️ Quedará negativo</span>'
                        : ''}
                </td>
                <td>$ ${formatearMoneda(precioUnit)}</td>
                <td>
                    <input type="number" min="${p.permiteDecimales ? '0.001' : '1'}"
                           step="${p.permiteDecimales ? '0.001' : '1'}"
                           value="${item.cantidad}"
                           onchange="actualizarCantidadItemPos(${idx}, this.value)">
                </td>
                <td>
                    <input type="number" min="0" max="100" step="0.5"
                           value="${item.descuentoPorcentaje}"
                           ${!puedeDesc ? 'disabled' : ''}
                           onchange="actualizarDescuentoItemPos(${idx}, this.value)">
                </td>
                <td><strong>$ ${formatearMoneda(subtotalItem)}</strong></td>
                <td>
                    <button type="button" class="btn-icon" onclick="quitarItemPos(${idx})" title="Quitar ítem">✕</button>
                </td>
            </tr>
        `;
    }).join('');

    recalcularTotalesPos();
}

window.actualizarCantidadItemPos = function (idx, val) {
    const item = AppState.posCarrito[idx];
    if (!item) return;
    let num = parseFloat(val) || 0;
    if (!item.producto.permiteDecimales) {
        num = Math.round(num);
    }
    if (num <= 0) {
        AppState.posCarrito.splice(idx, 1);
    } else {
        item.cantidad = num;
    }
    renderizarCarritoPos();
};

window.actualizarDescuentoItemPos = function (idx, val) {
    const item = AppState.posCarrito[idx];
    if (!item) return;
    item.descuentoPorcentaje = Math.min(100, Math.max(0, parseFloat(val) || 0));
    renderizarCarritoPos();
};

window.quitarItemPos = function (idx) {
    AppState.posCarrito.splice(idx, 1);
    renderizarCarritoPos();
};

function vaciarCarritoPos() {
    AppState.posCarrito = [];
    AppState.posSupervisorAuth = null;
    document.getElementById('pos-desc-global').value = '0';
    actualizarEstadoDescuentoInputPos();
    if (AppState.mediosPago.length > 0) {
        AppState.posPagos = [{
            medioPago: AppState.mediosPago[0].codigo,
            montoBase: 0,
            montoRecibidoEfectivo: '',
            cuotas: 1,
            referenciaOperacion: ''
        }];
    }
    renderizarCarritoPos();
}

function agregarLineaPagoPos() {
    if (!AppState.mediosPago.length) return;
    const netoBase = calcularNetoBaseCarrito();
    const cubiertoActual = AppState.posPagos.reduce((acc, p) => acc + (Number(p.montoBase) || 0), 0);
    const restante = Math.max(0, Number((netoBase - cubiertoActual).toFixed(2)));

    AppState.posPagos.push({
        medioPago: AppState.mediosPago[0].codigo,
        montoBase: restante,
        montoRecibidoEfectivo: '',
        cuotas: 1,
        referenciaOperacion: ''
    });
    renderizarLineasPagoPos();
    recalcularTotalesPos(false);
}

window.quitarLineaPagoPos = function (idx) {
    if (AppState.posPagos.length <= 1) return;
    AppState.posPagos.splice(idx, 1);
    if (AppState.posPagos.length === 1) {
        AppState.posPagos[0].montoBase = calcularNetoBaseCarrito();
    }
    renderizarLineasPagoPos();
    recalcularTotalesPos(false);
};

function calcularSubtotalItemsCarrito() {
    return Number(AppState.posCarrito.reduce((acc, item) => {
        const pUnit = Number(item.producto.precioVentaContextoEfectivo || 0);
        const bruto = pUnit * item.cantidad;
        const neto = bruto * (1 - (item.descuentoPorcentaje || 0) / 100);
        return acc + neto;
    }, 0).toFixed(2));
}

function calcularNetoBaseCarrito() {
    const subtotal = calcularSubtotalItemsCarrito();
    const descGlobalPorc = Math.min(100, Math.max(0, parseFloat(document.getElementById('pos-desc-global').value) || 0));
    const descMonto = Number((subtotal * (descGlobalPorc / 100)).toFixed(2));
    return Number((subtotal - descMonto).toFixed(2));
}

function recalcularTotalesPos(autoAjustarPagoUnico = true) {
    const subtotal = calcularSubtotalItemsCarrito();
    const descGlobalPorc = Math.min(100, Math.max(0, parseFloat(document.getElementById('pos-desc-global').value) || 0));
    const descMonto = Number((subtotal * (descGlobalPorc / 100)).toFixed(2));
    const netoBase = Number((subtotal - descMonto).toFixed(2));

    // Si solo hay 1 línea de pago, auto-asignarle el 100% del netoBase automáticamente
    if (autoAjustarPagoUnico && AppState.posPagos.length === 1) {
        AppState.posPagos[0].montoBase = netoBase;
    }

    renderizarLineasPagoPos();

    let sumaBasePagos = 0;
    let sumaAjustesMedios = 0;
    let totalFinalCobrar = 0;
    let vueltoTotal = 0;

    AppState.posPagos.forEach(p => {
        const base = Number(p.montoBase) || 0;
        const cfg = AppState.mediosPago.find(m => m.codigo === p.medioPago);
        const porcAjuste = cfg ? Number(cfg.porcentajeAjuste || 0) : 0;
        const ajuste = Number((base * (porcAjuste / 100)).toFixed(2));
        const finalLinea = Number((base + ajuste).toFixed(2));

        sumaBasePagos += base;
        sumaAjustesMedios += ajuste;
        totalFinalCobrar += finalLinea;

        if (p.medioPago === 'EFECTIVO' && p.montoRecibidoEfectivo !== '' && Number(p.montoRecibidoEfectivo) > finalLinea) {
            vueltoTotal += (Number(p.montoRecibidoEfectivo) - finalLinea);
        }
    });

    const faltaCubrir = Number((netoBase - sumaBasePagos).toFixed(2));

    document.getElementById('pos-lbl-subtotal').textContent = `$ ${formatearMoneda(subtotal)}`;
    document.getElementById('pos-lbl-descuento').textContent = `- $ ${formatearMoneda(descMonto)}`;
    document.getElementById('pos-lbl-neto-base').textContent = `$ ${formatearMoneda(netoBase)}`;
    document.getElementById('pos-lbl-ajuste-medios').textContent =
        `${sumaAjustesMedios >= 0 ? '+' : '-'} $ ${formatearMoneda(Math.abs(sumaAjustesMedios))}`;

    const rowFalta = document.getElementById('pos-row-falta-cubrir');
    document.getElementById('pos-lbl-falta-cubrir').textContent = `$ ${formatearMoneda(faltaCubrir)}`;
    rowFalta.style.color = Math.abs(faltaCubrir) > 0.05 ? '#fca5a5' : '#86efac';

    document.getElementById('pos-lbl-total-final').textContent = `$ ${formatearMoneda(totalFinalCobrar)}`;

    const rowVuelto = document.getElementById('pos-row-vuelto');
    if (vueltoTotal > 0) {
        document.getElementById('pos-lbl-vuelto').textContent = `$ ${formatearMoneda(vueltoTotal)}`;
        rowVuelto.classList.remove('hidden');
    } else {
        rowVuelto.classList.add('hidden');
    }
}

function renderizarLineasPagoPos() {
    const container = document.getElementById('pos-pagos-container');
    container.innerHTML = AppState.posPagos.map((p, idx) => {
        const cfg = AppState.mediosPago.find(m => m.codigo === p.medioPago);
        const porc = cfg ? Number(cfg.porcentajeAjuste || 0) : 0;
        const base = Number(p.montoBase || 0);
        const finalLinea = Number((base * (1 + porc / 100)).toFixed(2));

        const opcionesMedios = AppState.mediosPago.map(m => {
            const tagPorc = Number(m.porcentajeAjuste) !== 0
                ? ` (${m.porcentajeAjuste > 0 ? '+' : ''}${m.porcentajeAjuste}%)`
                : '';
            return `<option value="${m.codigo}" ${m.codigo === p.medioPago ? 'selected' : ''}>${escapeHtml(m.nombre)}${tagPorc}</option>`;
        }).join('');

        return `
            <div class="pago-line-card">
                <div class="pago-line-row">
                    <select onchange="onChangeMedioPagoPos(${idx}, this.value)">${opcionesMedios}</select>
                    <input type="number" step="0.01" min="0" value="${base}"
                           title="Monto base que cubre este medio"
                           onchange="onChangeMontoBasePagoPos(${idx}, this.value)">
                    ${AppState.posPagos.length > 1
                        ? `<button type="button" class="btn-icon" onclick="quitarLineaPagoPos(${idx})">✕</button>`
                        : ''}
                </div>
                <div class="pago-line-extra">
                    ${p.medioPago === 'EFECTIVO' ? `
                        <div>
                            <label>Paga con ($):</label>
                            <input type="number" step="0.01" min="0" value="${p.montoRecibidoEfectivo}"
                                   placeholder="Ej: ${finalLinea}"
                                    oninput="onChangeRecibidoEfectivoPos(${idx}, this.value)">
                        </div>
                        <div style="display:flex;align-items:end;font-weight:700;color:#1e40af;">
                            A cobrar: $ ${formatearMoneda(finalLinea)}
                        </div>
                    ` : `
                        <div>
                            <label>Ref. / Cupón ${cfg && cfg.requiereReferencia ? '*' : '(Opc.)'}:</label>
                            <input type="text" maxlength="100" value="${escapeHtml(p.referenciaOperacion)}"
                                   placeholder="Nro comprobante"
                                   oninput="onChangeReferenciaPagoPos(${idx}, this.value)">
                        </div>
                        <div>
                            <label>${p.medioPago === 'TARJETA_CREDITO' ? 'Cuotas:' : 'A cobrar c/ajuste:'}</label>
                            ${p.medioPago === 'TARJETA_CREDITO' ? `
                                <input type="number" min="1" max="24" value="${p.cuotas || 1}"
                                       onchange="onChangeCuotasPagoPos(${idx}, this.value)">
                            ` : `<div style="padding-top:0.35rem;font-weight:700;color:#1e40af;">$ ${formatearMoneda(finalLinea)}</div>`}
                        </div>
                    `}
                </div>
            </div>
        `;
    }).join('');
}

window.onChangeMedioPagoPos = function (idx, nuevoMedio) {
    AppState.posPagos[idx].medioPago = nuevoMedio;
    recalcularTotalesPos(false);
};

window.onChangeMontoBasePagoPos = function (idx, val) {
    AppState.posPagos[idx].montoBase = Math.max(0, parseFloat(val) || 0);
    // Si hay exactamente 2 medios de pago y edita el primero, autocompletar el segundo con el saldo restante
    if (AppState.posPagos.length === 2 && idx === 0) {
        const netoBase = calcularNetoBaseCarrito();
        AppState.posPagos[1].montoBase = Math.max(0, Number((netoBase - AppState.posPagos[0].montoBase).toFixed(2)));
    }
    recalcularTotalesPos(false);
};

window.onChangeRecibidoEfectivoPos = function (idx, val) {
    AppState.posPagos[idx].montoRecibidoEfectivo = val;
    recalcularTotalesPos(false);
};

window.onChangeReferenciaPagoPos = function (idx, val) {
    AppState.posPagos[idx].referenciaOperacion = val;
};

window.onChangeCuotasPagoPos = function (idx, val) {
    AppState.posPagos[idx].cuotas = Math.max(1, parseInt(val, 10) || 1);
};

async function onConfirmarVentaPos() {
    const errorEl = document.getElementById('pos-checkout-error');
    errorEl.classList.add('hidden');

    if (!AppState.turnoCajaActual) {
        errorEl.textContent = 'Debe abrir su Turno de Caja antes de poder registrar una venta.';
        errorEl.classList.remove('hidden');
        return;
    }

    if (!AppState.posCarrito.length) {
        errorEl.textContent = 'Agregue al menos un artículo al carrito para registrar la venta.';
        errorEl.classList.remove('hidden');
        return;
    }

    const payload = {
        clienteNombre: document.getElementById('pos-cli-nombre').value.trim() || 'Consumidor Final',
        clienteDocumento: document.getElementById('pos-cli-doc').value.trim() || null,
        clienteCondicionIva: document.getElementById('pos-cli-iva').value,
        descuentoManualPorcentaje: parseFloat(document.getElementById('pos-desc-global').value) || 0,
        supervisorUsername: AppState.posSupervisorAuth ? AppState.posSupervisorAuth.username : null,
        supervisorPassword: AppState.posSupervisorAuth ? AppState.posSupervisorAuth.password : null,
        observaciones: null,
        items: AppState.posCarrito.map(i => ({
            productoId: i.producto.id,
            cantidad: i.cantidad,
            descuentoItemPorcentaje: i.descuentoPorcentaje || 0
        })),
        pagos: AppState.posPagos.map(p => ({
            medioPago: p.medioPago,
            montoBase: Number(p.montoBase),
            montoRecibidoEfectivo: p.medioPago === 'EFECTIVO' && p.montoRecibidoEfectivo !== ''
                ? Number(p.montoRecibidoEfectivo)
                : null,
            cuotas: p.cuotas || 1,
            referenciaOperacion: p.referenciaOperacion ? p.referenciaOperacion.trim() : null
        }))
    };

    try {
        const ventaCreada = await ApiClient.post('/api/v1/ventas', payload);
        showToast(`Venta ${ventaCreada.numeroComprobanteInterno} registrada exitosamente`);
        vaciarCarritoPos();
        document.getElementById('pos-cli-nombre').value = 'Consumidor Final';
        document.getElementById('pos-cli-doc').value = '';
        document.getElementById('pos-cli-iva').value = 'CONSUMIDOR_FINAL';
        await cargarVistaPos();
        mostrarModalTicketVenta(ventaCreada);
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
}

/* =========================================================================
   CONFIGURACIÓN DE RECARGOS / DESCUENTOS POR MEDIO DE PAGO (Regla 2.1 A)
   ========================================================================= */
async function abrirModalConfigMediosPago() {
    try {
        const todosMedios = await ApiClient.get('/api/v1/ventas/medios-pago?soloActivos=false');
        const tbody = document.getElementById('tbody-config-medios');
        tbody.innerHTML = todosMedios.map(m => `
            <tr data-medio-id="${m.id}">
                <td><strong>${escapeHtml(m.nombre)}</strong></td>
                <td><input type="number" class="inp-medio-porc" step="0.01" value="${m.porcentajeAjuste}"></td>
                <td class="text-center"><input type="checkbox" class="inp-medio-ref" ${m.requiereReferencia ? 'checked' : ''}></td>
                <td class="text-center"><input type="checkbox" class="inp-medio-act" ${m.activo ? 'checked' : ''}></td>
                <td><button type="button" class="btn btn-primary btn-xs" onclick="guardarConfigMedioPago(${m.id}, this)">Guardar</button></td>
            </tr>
        `).join('');
        document.getElementById('modal-config-medios').classList.remove('hidden');
    } catch (err) {
        showToast(err.message);
    }
}

window.guardarConfigMedioPago = async function (id, btnEl) {
    const tr = btnEl.closest('tr');
    const payload = {
        porcentajeAjuste: parseFloat(tr.querySelector('.inp-medio-porc').value) || 0,
        requiereReferencia: tr.querySelector('.inp-medio-ref').checked,
        activo: tr.querySelector('.inp-medio-act').checked
    };
    try {
        await ApiClient.put(`/api/v1/ventas/medios-pago/${id}`, payload);
        showToast('Medio de pago actualizado');
        AppState.mediosPago = await ApiClient.get('/api/v1/ventas/medios-pago?soloActivos=true');
        recalcularTotalesPos(false);
    } catch (err) {
        showToast(err.message);
    }
};

function cerrarModalConfigMediosPago() {
    document.getElementById('modal-config-medios').classList.add('hidden');
}

/* =========================================================================
   SECCIÓN CAJA, ARQUEO E HISTORIAL DE VENTAS / ANULACIONES
   ========================================================================= */
async function cargarSeccionCajaEHistorial() {
    try {
        const fltFact = document.getElementById('flt-ventas-facturacion').value;
        const queryFact = fltFact ? `?estadoFacturacion=${fltFact}` : '';

        const [turnoInfo, ventas, turnos] = await Promise.all([
            ApiClient.get('/api/v1/cajas/turno-actual'),
            ApiClient.get(`/api/v1/ventas${queryFact}`),
            ApiClient.get('/api/v1/cajas/turnos')
        ]);

        AppState.turnoCajaActual = turnoInfo.tieneTurnoAbierto ? turnoInfo.turno : null;
        AppState.ventasHistorial = ventas;
        AppState.turnosHistorial = turnos;

        renderizarEstadoCajaTopbarYBanner();
        renderizarPanelTurnoActivo();
        renderizarTablaVentasHistorial(ventas);
        renderizarTablaTurnosHistorial(turnos);
    } catch (err) {
        renderizarTablaVentasHistorial([]);
        renderizarTablaTurnosHistorial([]);
        manejarErrorApi(err);
    }
}

function renderizarPanelTurnoActivo() {
    const card = document.getElementById('caja-turno-activo-card');
    const btnAbrir = document.getElementById('btn-caja-abrir');
    const btnMov = document.getElementById('btn-caja-movimiento');
    const btnCerrar = document.getElementById('btn-caja-cerrar');

    const t = AppState.turnoCajaActual;
    btnAbrir.classList.toggle('hidden', Boolean(t));
    btnMov.classList.toggle('hidden', !t);
    btnCerrar.classList.toggle('hidden', !t);

    if (!t) {
        card.innerHTML = `
            <div style="display:flex;justify-content:space-between;align-items:center;">
                <div>
                    <h3 style="color:#991b1b;">No posee un Turno de Caja abierto en la sucursal actual</h3>
                    <p class="subtitle">Abra su caja indicando el fondo fijo de efectivo inicial para comenzar a operar.</p>
                </div>
                <button class="btn btn-primary" onclick="abrirModalAbrirCaja()">🔓 Abrir Turno de Caja</button>
            </div>
        `;
        return;
    }

    const movsHtml = (t.movimientos || []).length
        ? `<div style="margin-top:0.85rem;">
               <strong>Movimientos Manuales del Turno:</strong>
               <ul style="margin-top:0.35rem;padding-left:1.2rem;font-size:0.83rem;">
                   ${t.movimientos.map(m => `
                       <li>
                           <b>[${m.tipo}] $ ${formatearMoneda(m.monto)}</b> — ${escapeHtml(m.motivo)}
                           <small style="color:#64748b;">(${formatearFechaHora(m.fechaMovimiento)})</small>
                       </li>
                   `).join('')}
               </ul>
           </div>`
        : '';

    card.innerHTML = `
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:0.85rem;">
            <div>
                <h3>Turno Activo #${t.id} — ${escapeHtml(t.sucursalCodigo)} (${escapeHtml(t.usuarioNombreCompleto)})</h3>
                <small style="color:#64748b;">Apertura: ${formatearFechaHora(t.fechaApertura)}</small>
            </div>
            <span class="status-pill status-active">ABIERTO</span>
        </div>
        <div class="form-grid" style="grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));">
            <div class="pricing-box">
                <small>Fondo Inicial Efectivo</small>
                <div style="font-size:1.1rem;font-weight:700;">$ ${formatearMoneda(t.montoInicialEfectivo)}</div>
            </div>
            <div class="pricing-box">
                <small>Ventas en Efectivo</small>
                <div style="font-size:1.1rem;font-weight:700;color:#16a34a;">+ $ ${formatearMoneda(t.totalVentasEfectivo)}</div>
            </div>
            <div class="pricing-box">
                <small>Ingresos / Retiros Manuales</small>
                <div style="font-size:0.95rem;font-weight:600;">
                    +${formatearMoneda(t.totalIngresosManuales)} / -${formatearMoneda(t.totalEgresosManuales)}
                </div>
            </div>
            <div class="pricing-box" style="border-color:#2563eb;background:#eff6ff;">
                <small>Efectivo Esperado en Caja</small>
                <div style="font-size:1.2rem;font-weight:800;color:#1e40af;">$ ${formatearMoneda(t.efectivoEsperado)}</div>
            </div>
            <div class="pricing-box">
                <small>Transf. / QR / Tarjetas</small>
                <div style="font-size:0.82rem;">
                    Transf: $${formatearMoneda(t.totalVentasTransferencia)} | QR: $${formatearMoneda(t.totalVentasQr)}<br>
                    Déb: $${formatearMoneda(t.totalVentasDebito)} | Créd: $${formatearMoneda(t.totalVentasCredito)}
                </div>
            </div>
        </div>
        ${movsHtml}
    `;
}

function renderizarTablaVentasHistorial(lista) {
    const tbody = document.getElementById('tbody-ventas-historial');
    if (!lista.length) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center">No hay ventas registradas para este filtro.</td></tr>`;
        return;
    }

    tbody.innerHTML = lista.map(v => {
        const mediosStr = (v.pagos || [])
            .map(p => `${escapeHtml(p.medioPagoDescripcion)}: $${formatearMoneda(p.montoFinalCobrado)}`)
            .join(' + ');

        const esRealizada = v.estadoVenta === 'REALIZADA';
        const comprobantes = v.comprobantesArca || [];
        const facturaEmitida = comprobantes.find(c => c.naturalezaComprobante === 'FACTURA');
        const ncEmitida = comprobantes.find(c => c.naturalezaComprobante === 'NOTA_CREDITO');

        let badgeFacturacionHtml = '';
        if (v.estadoFacturacion === 'FACTURADA') {
            badgeFacturacionHtml = `<span class="status-pill status-active">✓ Facturada ARCA</span>`;
        } else if (v.estadoFacturacion === 'ERROR_ARCA') {
            badgeFacturacionHtml = `<span class="status-pill status-inactive">⚠️ Error ARCA</span>`;
        } else if (v.estadoFacturacion === 'ANULADA_NOTA_CREDITO') {
            badgeFacturacionHtml = `<span class="status-pill status-inactive">Anulada c/NC ARCA</span>`;
        } else {
            badgeFacturacionHtml = `<span class="status-pill status-locked">Pendiente Facturación</span>`;
        }

        if (facturaEmitida) {
            badgeFacturacionHtml += `
                <div style="font-size:0.74rem;font-weight:700;color:#1e40af;margin-top:0.2rem;">
                    ${escapeHtml(facturaEmitida.tipoComprobanteDescripcion)} ${escapeHtml(facturaEmitida.numeroFormateado)}
                </div>
                <div style="font-size:0.7rem;color:#475569;">CAE: ${escapeHtml(facturaEmitida.cae)}</div>
            `;
        }
        if (ncEmitida) {
            badgeFacturacionHtml += `
                <div style="font-size:0.73rem;font-weight:700;color:#dc2626;margin-top:0.15rem;">
                    ${escapeHtml(ncEmitida.tipoComprobanteDescripcion)} ${escapeHtml(ncEmitida.numeroFormateado)}
                </div>
            `;
        }
        if (v.ultimoErrorArca) {
            badgeFacturacionHtml += `
                <div style="font-size:0.71rem;color:#b91c1c;margin-top:0.2rem;" title="${escapeHtml(v.ultimoErrorArca)}">
                    ${escapeHtml(v.ultimoErrorArca.slice(0, 75))}${v.ultimoErrorArca.length > 75 ? '...' : ''}
                </div>
            `;
        }

        const puedeFacturarOReintentar = esRealizada && (v.estadoFacturacion === 'NO_FACTURADA' || v.estadoFacturacion === 'ERROR_ARCA');

        return `
            <tr>
                <td><strong>${escapeHtml(v.numeroComprobanteInterno)}</strong></td>
                <td>
                    <div>${formatearFechaHora(v.fechaVenta)}</div>
                    <small style="color:#64748b;">Cajero: ${escapeHtml(v.usuarioUsername)}</small>
                </td>
                <td>
                    <div>${escapeHtml(v.clienteNombre)}</div>
                    <small style="color:#64748b;">${escapeHtml(v.clienteCondicionIvaDescripcion)}${v.clienteDocumento ? ` • Doc: ${escapeHtml(v.clienteDocumento)}` : ''}</small>
                </td>
                <td><small>${mediosStr}</small></td>
                <td><strong>$ ${formatearMoneda(v.totalFinal)}</strong></td>
                <td>
                    <span class="status-pill ${esRealizada ? 'status-active' : 'status-inactive'}">
                        ${escapeHtml(v.estadoVentaDescripcion)}
                    </span>
                    ${v.motivoAnulacion ? `<div style="font-size:0.72rem;color:#991b1b;">${escapeHtml(v.motivoAnulacion)}</div>` : ''}
                </td>
                <td>${badgeFacturacionHtml}</td>
                <td>
                    <div class="action-group">
                        <button class="btn btn-outline btn-xs" onclick="verTicketVentaPorId(${v.id})">🧾 Ticket X</button>
                        ${puedeFacturarOReintentar ? `
                            <button class="btn btn-primary btn-xs" onclick="abrirModalFacturarVentaArca(${v.id})">
                                🏛️ ${v.estadoFacturacion === 'ERROR_ARCA' ? 'Reintentar ARCA' : 'Facturar ARCA'}
                            </button>
                        ` : ''}
                        ${facturaEmitida ? `
                            <button class="btn btn-outline btn-xs" style="border-color:#2563eb;color:#1d4ed8;"
                                    onclick="abrirModalComprobanteFiscalArca(${facturaEmitida.id}, ${v.id})">
                                🖨️ ${escapeHtml(facturaEmitida.tipoComprobanteDescripcion)}
                            </button>
                        ` : ''}
                        ${ncEmitida ? `
                            <button class="btn btn-outline btn-xs" style="border-color:#dc2626;color:#dc2626;"
                                    onclick="abrirModalComprobanteFiscalArca(${ncEmitida.id}, ${v.id})">
                                🖨️ ${escapeHtml(ncEmitida.tipoComprobanteDescripcion)}
                            </button>
                        ` : ''}
                        ${(v.cantidadIntentosArca || 0) > 0 ? `
                            <button class="btn btn-outline btn-xs" onclick="abrirModalIntentosArca(${v.id})" title="Ver auditoría de comunicación con ARCA">
                                📡 (${v.cantidadIntentosArca})
                            </button>
                        ` : ''}
                        ${esRealizada ? `
                            <button class="btn btn-outline btn-xs" style="color:#dc2626;"
                                    onclick="abrirModalAnularVenta(${v.id}, '${escapeHtml(v.numeroComprobanteInterno)}')">
                                Anular
                            </button>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function renderizarTablaTurnosHistorial(lista) {
    const tbody = document.getElementById('tbody-turnos-historial');
    if (!lista.length) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center">No hay turnos registrados.</td></tr>`;
        return;
    }

    tbody.innerHTML = lista.map(t => {
        let difHtml = '-';
        if (t.diferenciaEfectivo !== null && t.diferenciaEfectivo !== undefined) {
            const dif = Number(t.diferenciaEfectivo);
            const color = dif === 0 ? '#16a34a' : (dif > 0 ? '#2563eb' : '#dc2626');
            const label = dif === 0 ? 'Exacto' : (dif > 0 ? 'Sobrante' : 'Faltante');
            difHtml = `<strong style="color:${color};">${dif >= 0 ? '+' : ''}$ ${formatearMoneda(dif)} (${label})</strong>`;
        }

        return `
            <tr>
                <td><strong>#${t.id}</strong> — ${escapeHtml(t.sucursalCodigo)}</td>
                <td>${escapeHtml(t.usuarioNombreCompleto)}</td>
                <td>
                    <div>Ap: ${formatearFechaHora(t.fechaApertura)}</div>
                    ${t.fechaCierre ? `<small style="color:#64748b;">Ci: ${formatearFechaHora(t.fechaCierre)}</small>` : ''}
                </td>
                <td>$ ${formatearMoneda(t.montoInicialEfectivo)}</td>
                <td><strong>$ ${formatearMoneda(t.totalGeneralVentas)}</strong></td>
                <td>$ ${formatearMoneda(t.efectivoEsperado)}</td>
                <td>${t.efectivoDeclaradoCierre !== null ? '$ ' + formatearMoneda(t.efectivoDeclaradoCierre) : '-'}</td>
                <td>${difHtml}</td>
                <td>
                    <span class="status-pill ${t.estado === 'ABIERTO' ? 'status-active' : 'status-locked'}">
                        ${t.estado}
                    </span>
                </td>
            </tr>
        `;
    }).join('');
}

window.abrirModalAbrirCaja = function () {
    document.getElementById('form-abrir-caja').reset();
    document.getElementById('modal-abrir-caja-error').classList.add('hidden');
    document.getElementById('modal-abrir-caja').classList.remove('hidden');
};

function cerrarModalAbrirCaja() {
    document.getElementById('modal-abrir-caja').classList.add('hidden');
}

async function onConfirmarAbrirCaja(e) {
    e.preventDefault();
    const errorEl = document.getElementById('modal-abrir-caja-error');
    errorEl.classList.add('hidden');

    const payload = {
        montoInicialEfectivo: parseFloat(document.getElementById('caja-monto-inicial').value) || 0,
        observacionesApertura: document.getElementById('caja-obs-apertura').value.trim() || null
    };

    try {
        await ApiClient.post('/api/v1/cajas/abrir', payload);
        cerrarModalAbrirCaja();
        showToast('Turno de caja abierto exitosamente');
        cargarDatosSeccionActiva();
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
}

window.abrirModalMovimientoCaja = function () {
    document.getElementById('form-mov-caja').reset();
    document.getElementById('modal-mov-caja-error').classList.add('hidden');
    document.getElementById('modal-mov-caja').classList.remove('hidden');
};

function cerrarModalMovimientoCaja() {
    document.getElementById('modal-mov-caja').classList.add('hidden');
}

async function onConfirmarMovimientoCaja(e) {
    e.preventDefault();
    const errorEl = document.getElementById('modal-mov-caja-error');
    errorEl.classList.add('hidden');

    const payload = {
        tipo: document.getElementById('mov-caja-tipo').value,
        monto: parseFloat(document.getElementById('mov-caja-monto').value) || 0,
        motivo: document.getElementById('mov-caja-motivo').value.trim()
    };

    try {
        await ApiClient.post('/api/v1/cajas/movimientos', payload);
        cerrarModalMovimientoCaja();
        showToast('Movimiento de caja registrado');
        cargarDatosSeccionActiva();
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
}

window.abrirModalCerrarCaja = function () {
    const t = AppState.turnoCajaActual;
    if (!t) return;

    document.getElementById('form-cerrar-caja').reset();
    document.getElementById('modal-cerrar-caja-error').classList.add('hidden');
    document.getElementById('caja-efectivo-declarado').value = Number(t.efectivoEsperado || 0).toFixed(2);

    document.getElementById('arqueo-resumen-esperado').innerHTML = `
        <h4>Resumen Esperado del Turno #${t.id}</h4>
        <div style="font-size:0.85rem;margin-top:0.4rem;">
            <div>Fondo Inicial: <b>$ ${formatearMoneda(t.montoInicialEfectivo)}</b></div>
            <div>+ Ventas en Efectivo: <b>$ ${formatearMoneda(t.totalVentasEfectivo)}</b></div>
            <div>+ Ingresos Manuales: <b>$ ${formatearMoneda(t.totalIngresosManuales)}</b></div>
            <div>- Egresos / Retiros: <b>$ ${formatearMoneda(t.totalEgresosManuales)}</b></div>
            <div style="margin-top:0.35rem;font-size:1rem;color:#1e40af;">
                = <b>Efectivo Esperado en Caja: $ ${formatearMoneda(t.efectivoEsperado)}</b>
            </div>
            <hr class="divider">
            <div>Otros medios cobrados: Transf: $${formatearMoneda(t.totalVentasTransferencia)} | QR: $${formatearMoneda(t.totalVentasQr)} | Débito: $${formatearMoneda(t.totalVentasDebito)} | Crédito: $${formatearMoneda(t.totalVentasCredito)}</div>
        </div>
    `;

    actualizarPreviewDiferenciaArqueo();
    document.getElementById('modal-cerrar-caja').classList.remove('hidden');
};

function actualizarPreviewDiferenciaArqueo() {
    const t = AppState.turnoCajaActual;
    if (!t) return;
    const esperado = Number(t.efectivoEsperado || 0);
    const contado = parseFloat(document.getElementById('caja-efectivo-declarado').value) || 0;
    const dif = Number((contado - esperado).toFixed(2));
    const el = document.getElementById('arqueo-diferencia-preview');

    if (dif === 0) {
        el.style.color = '#16a34a';
        el.textContent = '✓ Arqueo Exacto (Diferencia: $ 0,00)';
    } else if (dif > 0) {
        el.style.color = '#2563eb';
        el.textContent = `ℹ️ Sobrante de Caja: + $ ${formatearMoneda(dif)}`;
    } else {
        el.style.color = '#dc2626';
        el.textContent = `⚠️ Faltante de Caja: - $ ${formatearMoneda(Math.abs(dif))}`;
    }
}

function cerrarModalCerrarCaja() {
    document.getElementById('modal-cerrar-caja').classList.add('hidden');
}

async function onConfirmarCerrarCaja(e) {
    e.preventDefault();
    const errorEl = document.getElementById('modal-cerrar-caja-error');
    errorEl.classList.add('hidden');

    const payload = {
        efectivoDeclaradoCierre: parseFloat(document.getElementById('caja-efectivo-declarado').value) || 0,
        observacionesCierre: document.getElementById('caja-obs-cierre').value.trim() || null
    };

    try {
        await ApiClient.post('/api/v1/cajas/cerrar', payload);
        cerrarModalCerrarCaja();
        showToast('Turno de caja cerrado y arqueado correctamente');
        cargarDatosSeccionActiva();
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
}

window.abrirModalAnularVenta = function (ventaId, nroComprobante) {
    document.getElementById('form-anular-venta').reset();
    document.getElementById('modal-anular-venta-error').classList.add('hidden');
    document.getElementById('anular-venta-id').value = ventaId;
    document.getElementById('anular-venta-nro').textContent = nroComprobante;

    const tienePermisoAnular = esAdmin() || tienePermiso('ANULAR_VENTAS');
    document.getElementById('wrap-anular-supervisor').classList.toggle('hidden', tienePermisoAnular);

    document.getElementById('modal-anular-venta').classList.remove('hidden');
};

function cerrarModalAnularVenta() {
    document.getElementById('modal-anular-venta').classList.add('hidden');
}

async function onConfirmarAnularVenta(e) {
    e.preventDefault();
    const errorEl = document.getElementById('modal-anular-venta-error');
    errorEl.classList.add('hidden');

    const id = document.getElementById('anular-venta-id').value;
    const payload = {
        motivoAnulacion: document.getElementById('anular-venta-motivo').value.trim(),
        supervisorUsername: document.getElementById('anular-sup-user').value.trim() || null,
        supervisorPassword: document.getElementById('anular-sup-pass').value || null
    };

    try {
        await ApiClient.post(`/api/v1/ventas/${id}/anular`, payload);
        cerrarModalAnularVenta();
        showToast('Venta anulada y stock reintegrado a la sucursal');
        await cargarSeccionCajaEHistorial();
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
}

/* =========================================================================
   TICKET / COMPROBANTE INTERNO IMPRIMIBLE (Regla 5.2)
   ========================================================================= */
window.verTicketVentaPorId = async function (ventaId) {
    try {
        const v = await ApiClient.get(`/api/v1/ventas/${ventaId}`);
        mostrarModalTicketVenta(v);
    } catch (err) {
        showToast(err.message);
    }
};

function mostrarModalTicketVenta(v) {
    AppState.ventaTicketActual = v;
    const itemsHtml = (v.items || []).map(i => `
        <div style="margin-bottom:4px;">
            <div>${escapeHtml(i.nombreProducto)}</div>
            <div style="display:flex;justify-content:space-between;">
                <span>${formatearCantidadSegunUnidad(i.cantidad, true)} ${escapeHtml(i.unidadMedidaAbreviatura)} x $${formatearMoneda(i.precioUnitario)}</span>
                <b>$${formatearMoneda(i.subtotalNetoItem)}</b>
            </div>
        </div>
    `).join('');

    const pagosHtml = (v.pagos || []).map(p => `
        <div style="display:flex;justify-content:space-between;">
            <span>${escapeHtml(p.medioPagoDescripcion)}${p.cuotas > 1 ? ` (${p.cuotas} c.)` : ''}:</span>
            <span>$${formatearMoneda(p.montoFinalCobrado)}</span>
        </div>
        ${p.vueltoEfectivo && Number(p.vueltoEfectivo) > 0 ? `
            <div style="display:flex;justify-content:space-between;font-size:0.76rem;">
                <span>Recibido: $${formatearMoneda(p.montoRecibidoEfectivo)}</span>
                <span>Vuelto: $${formatearMoneda(p.vueltoEfectivo)}</span>
            </div>
        ` : ''}
    `).join('');

    document.getElementById('ticket-print-area').innerHTML = `
        <div style="text-align:center;border-bottom:1px dashed #000;padding-bottom:6px;margin-bottom:6px;">
            <strong style="font-size:1rem;">${escapeHtml(v.sucursalNombre)}</strong>
            <div>${escapeHtml(v.sucursalDireccion || '')}</div>
            <div>CUIT: ${escapeHtml(v.sucursalCuit)} • PV: ${String(v.sucursalPuntoVentaArca).padStart(5, '0')}</div>
            <div style="margin-top:4px;font-weight:700;">COMPROBANTE X: ${escapeHtml(v.numeroComprobanteInterno)}</div>
            <div style="font-size:0.72rem;">(Documento No Válido como Factura — ${escapeHtml(v.estadoFacturacionDescripcion)})</div>
        </div>
        <div style="margin-bottom:6px;border-bottom:1px dashed #000;padding-bottom:6px;">
            <div>Fecha: ${formatearFechaHora(v.fechaVenta)}</div>
            <div>Cliente: ${escapeHtml(v.clienteNombre)} ${v.clienteDocumento ? `(${escapeHtml(v.clienteDocumento)})` : ''}</div>
            <div>Cond. IVA: ${escapeHtml(v.clienteCondicionIvaDescripcion)}</div>
            <div>Cajero: ${escapeHtml(v.usuarioUsername)}</div>
        </div>
        <div style="margin-bottom:6px;border-bottom:1px dashed #000;padding-bottom:6px;">
            ${itemsHtml}
        </div>
        <div style="margin-bottom:6px;border-bottom:1px dashed #000;padding-bottom:6px;">
            <div style="display:flex;justify-content:space-between;">
                <span>Subtotal:</span><span>$${formatearMoneda(v.subtotalItems)}</span>
            </div>
            ${Number(v.descuentoManualMonto) > 0 ? `
                <div style="display:flex;justify-content:space-between;">
                    <span>Descuento (${v.descuentoManualPorcentaje}%):</span><span>-$${formatearMoneda(v.descuentoManualMonto)}</span>
                </div>
            ` : ''}
            ${Number(v.ajusteMediosPagoMonto) !== 0 ? `
                <div style="display:flex;justify-content:space-between;">
                    <span>Ajuste Medios Pago:</span><span>$${formatearMoneda(v.ajusteMediosPagoMonto)}</span>
                </div>
            ` : ''}
            <div style="display:flex;justify-content:space-between;font-size:1rem;font-weight:800;margin-top:4px;">
                <span>TOTAL:</span><span>$ ${formatearMoneda(v.totalFinal)}</span>
            </div>
        </div>
        <div>
            <strong>Pagos:</strong>
            ${pagosHtml}
        </div>
    `;

    const btnFacturarTicket = document.getElementById('btn-facturar-desde-ticket');
    if (btnFacturarTicket) {
        const puedeFacturar = v.estadoVenta === 'REALIZADA' && (v.estadoFacturacion === 'NO_FACTURADA' || v.estadoFacturacion === 'ERROR_ARCA');
        btnFacturarTicket.classList.toggle('hidden', !puedeFacturar);
    }

    document.getElementById('modal-ticket-venta').classList.remove('hidden');
}

function cerrarModalTicketVenta() {
    document.getElementById('modal-ticket-venta').classList.add('hidden');
}

function imprimirTicketVentaActual() {
    const html = document.getElementById('ticket-print-area').innerHTML;
    const win = window.open('', '_blank', 'width=360,height=560');
    if (!win) {
        showToast('Permita las ventanas emergentes para imprimir el ticket.');
        return;
    }
    win.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>Ticket de Venta</title>
            <style>
                body { font-family: 'Courier New', monospace; width: 76mm; margin: 0 auto; padding: 6px; font-size: 12px; color: #000; }
            </style>
        </head>
        <body onload="window.print(); window.close();">${html}</body>
        </html>
    `);
    win.document.close();
}

/* =========================================================================
   FASE 2: CATÁLOGO DE PRODUCTOS, PRECIOS Y STOCK POR SUCURSAL
   ========================================================================= */
async function cargarVistaProductosCompleta() {
    try {
        const [categorias, marcas, proveedores, sucursales, productos] = await Promise.all([
            ApiClient.get('/api/v1/catalogo/categorias?soloActivas=true'),
            ApiClient.get('/api/v1/catalogo/marcas?soloActivas=true'),
            ApiClient.get('/api/v1/proveedores?soloActivos=true'),
            ApiClient.get('/api/v1/sucursales'),
            ApiClient.get('/api/v1/productos')
        ]);

        AppState.categorias = categorias;
        AppState.marcas = marcas;
        AppState.proveedores = proveedores;
        AppState.sucursales = sucursales;
        AppState.productos = productos;

        poblarSelectoresFiltroProductos();
        aplicarFiltrosProductosLocal();
    } catch (err) {
        renderizarTablaProductos([]);
        manejarErrorApi(err);
    }
}

function poblarSelectoresFiltroProductos() {
    const selCat = document.getElementById('flt-prod-categoria');
    const valCat = selCat.value;
    selCat.innerHTML = `<option value="">Todas las categorías</option>` +
        AppState.categorias.map(c => `<option value="${c.id}">${escapeHtml(c.nombre)}</option>`).join('');
    selCat.value = valCat;

    const selMarca = document.getElementById('flt-prod-marca');
    const valMarca = selMarca.value;
    selMarca.innerHTML = `<option value="">Todas las marcas</option>` +
        AppState.marcas.map(m => `<option value="${m.id}">${escapeHtml(m.nombre)}</option>`).join('');
    selMarca.value = valMarca;

    const selProv = document.getElementById('flt-prod-proveedor');
    const valProv = selProv.value;
    selProv.innerHTML = `<option value="">Todos los proveedores</option>` +
        AppState.proveedores.map(p => `<option value="${p.id}">${escapeHtml(p.razonSocial)}</option>`).join('');
    selProv.value = valProv;
}

function aplicarFiltrosProductosLocal() {
    const q = document.getElementById('flt-prod-q').value.trim().toLowerCase();
    const catId = document.getElementById('flt-prod-categoria').value;
    const marcaId = document.getElementById('flt-prod-marca').value;
    const provId = document.getElementById('flt-prod-proveedor').value;
    const soloBajoStock = document.getElementById('flt-prod-bajo-stock').checked;

    const totalBajoStock = AppState.productos.filter(p => p.activo && p.alertaBajoStock).length;
    document.getElementById('badge-count-bajo-stock').textContent = totalBajoStock;

    const filtrados = AppState.productos.filter(p => {
        if (catId && String(p.categoriaId) !== catId) return false;
        if (marcaId && String(p.marcaId) !== marcaId) return false;
        if (provId && String(p.proveedorId) !== provId) return false;
        if (soloBajoStock && !p.alertaBajoStock) return false;
        if (q) {
            const matchNombre = (p.nombre || '').toLowerCase().includes(q);
            const matchBarras = (p.codigoBarras || '').toLowerCase().includes(q);
            const matchSku = (p.codigoProveedorSku || '').toLowerCase().includes(q);
            const matchMarca = (p.marcaNombre || '').toLowerCase().includes(q);
            if (!matchNombre && !matchBarras && !matchSku && !matchMarca) return false;
        }
        return true;
    });

    renderizarTablaProductos(filtrados);
}

function limpiarFiltrosProductos() {
    document.getElementById('flt-prod-q').value = '';
    document.getElementById('flt-prod-categoria').value = '';
    document.getElementById('flt-prod-marca').value = '';
    document.getElementById('flt-prod-proveedor').value = '';
    document.getElementById('flt-prod-bajo-stock').checked = false;
    aplicarFiltrosProductosLocal();
}

function renderizarTablaProductos(lista) {
    const tbody = document.getElementById('tbody-productos');
    const verCostos = puedeVerCostos();
    const editarCat = puedeEditarCatalogo();
    const colSpan = verCostos ? 8 : 7;

    if (!lista.length) {
        tbody.innerHTML = `<tr><td colspan="${colSpan}" class="text-center">No se encontraron productos con los filtros seleccionados.</td></tr>`;
        return;
    }

    tbody.innerHTML = lista.map(p => {
        const stockStr = formatearCantidadSegunUnidad(p.stockContextoActual, p.permiteDecimales) + ' ' + escapeHtml(p.unidadMedidaAbreviatura);
        const minStr = formatearCantidadSegunUnidad(p.stockContextoMinimo, p.permiteDecimales) + ' ' + escapeHtml(p.unidadMedidaAbreviatura);

        const costoTd = verCostos ? `
            <td>
                <div>Costo c/IVA: <b>$ ${formatearMoneda(p.costoFinalConIva ?? p.precioCompra)}</b></div>
                <small style="color:#16a34a;font-weight:600;">Margen: ${formatearMoneda(p.porcentajeGanancia)}%</small>
            </td>
        ` : '';

        return `
            <tr class="${p.alertaBajoStock ? 'row-low-stock' : ''}">
                <td>
                    <strong>${escapeHtml(p.codigoBarras)}</strong>
                    ${p.esCodigoInterno ? '<span class="pv-pill" style="font-size:0.65rem;">INTERNO</span>' : ''}
                    ${p.codigoProveedorSku ? `<div style="font-size:0.75rem;color:#64748b;">SKU: ${escapeHtml(p.codigoProveedorSku)}</div>` : ''}
                </td>
                <td>
                    <strong>${escapeHtml(p.nombre)}</strong>
                    <div style="font-size:0.78rem;color:#475569;">Marca: ${escapeHtml(p.marcaNombre)}</div>
                </td>
                <td>
                    <div>${escapeHtml(p.categoriaNombre)}</div>
                    ${verCostos ? `<small style="color:#64748b;">Prov: ${escapeHtml(p.proveedorRazonSocial)}</small>` : ''}
                </td>
                ${costoTd}
                <td>
                    <strong style="font-size:0.98rem;color:#0f172a;">$ ${formatearMoneda(p.precioVentaContextoEfectivo)}</strong>
                    ${p.tienePrecioPersonalizadoEnContexto
                        ? `<div style="font-size:0.72rem;color:#2563eb;font-weight:600;">Precio Local (Gral: $${formatearMoneda(p.precioVentaGeneral)})</div>`
                        : `<div style="font-size:0.72rem;color:#64748b;">IVA ${formatearMoneda(p.alicuotaIva)}% incl.</div>`}
                </td>
                <td>
                    <div><strong>${stockStr}</strong> <small style="color:#64748b;">(Mín: ${minStr})</small></div>
                    ${p.alertaBajoStock ? '<span class="status-pill status-low-stock">⚠️ Bajo Stock</span>' : ''}
                </td>
                <td>
                    <span class="status-pill ${p.activo ? 'status-active' : 'status-inactive'}">
                        ${p.activo ? 'Activo' : 'Inactivo'}
                    </span>
                </td>
                <td>
                    <div class="action-group">
                        <button class="btn btn-outline btn-xs" onclick="abrirModalEtiqueta(${p.id})" title="Ver e imprimir código de barras o QR">🏷️ Etiqueta/QR</button>
                        ${editarCat ? `<button class="btn btn-outline btn-xs" onclick="editarProducto(${p.id})">Editar</button>` : ''}
                        ${editarCat ? `
                            <button class="btn btn-outline btn-xs" onclick="toggleEstadoProducto(${p.id}, ${!p.activo})">
                                ${p.activo ? 'Desactivar' : 'Activar'}
                            </button>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

window.editarProducto = function (id) {
    const prod = AppState.productos.find(item => item.id === id);
    if (prod) abrirModalProducto(prod);
};

window.toggleEstadoProducto = async function (id, nuevoEstado) {
    try {
        await ApiClient.patch(`/api/v1/productos/${id}/estado`, { activo: nuevoEstado });
        showToast(nuevoEstado ? 'Producto activado' : 'Producto desactivado');
        await cargarVistaProductosCompleta();
    } catch (err) {
        manejarErrorApi(err);
    }
};

function poblarCombosModalProducto() {
    document.getElementById('prod-categoria').innerHTML = AppState.categorias
        .map(c => `<option value="${c.id}">${escapeHtml(c.nombre)}</option>`).join('');
    document.getElementById('prod-marca').innerHTML = AppState.marcas
        .map(m => `<option value="${m.id}">${escapeHtml(m.nombre)}</option>`).join('');
    document.getElementById('prod-proveedor').innerHTML = AppState.proveedores
        .map(p => `<option value="${p.id}">${escapeHtml(p.razonSocial)} (${escapeHtml(p.codigo)})</option>`).join('');
}

function abrirModalProducto(prod) {
    const errorEl = document.getElementById('modal-producto-error');
    errorEl.classList.add('hidden');
    document.getElementById('form-producto').reset();
    poblarCombosModalProducto();

    const wrapUpdBranch = document.getElementById('wrap-prod-update-branch-prices');

    if (prod) {
        document.getElementById('modal-producto-title').textContent = `Editar Producto: ${prod.nombre}`;
        document.getElementById('prod-id').value = prod.id;
        document.getElementById('prod-codigo-barras').value = prod.codigoBarras;
        document.getElementById('prod-sku').value = prod.codigoProveedorSku || '';
        document.getElementById('prod-nombre').value = prod.nombre;
        document.getElementById('prod-descripcion').value = prod.descripcion || '';
        document.getElementById('prod-categoria').value = prod.categoriaId;
        document.getElementById('prod-marca').value = prod.marcaId;
        document.getElementById('prod-proveedor').value = prod.proveedorId;
        document.getElementById('prod-unidad').value = prod.unidadMedida;
        document.getElementById('prod-precio-compra').value = prod.precioCompra ?? 0;
        document.getElementById('prod-iva-incluido').value = String(prod.ivaIncluidoCompra !== false);
        document.getElementById('prod-alicuota-iva').value = Number(prod.alicuotaIva ?? 21).toFixed(2);
        document.getElementById('prod-ganancia').value = Number(prod.porcentajeGanancia ?? 0).toFixed(2);
        document.getElementById('prod-precio-venta').value = Number(prod.precioVentaGeneral ?? 0).toFixed(2);
        document.getElementById('prod-stock-min-defecto').value = prod.stockMinimoDefecto ?? 0;
        wrapUpdBranch.classList.remove('hidden');
        recalcularPreciosModal('PRECIO_VENTA');
        renderizarTablaSucursalesEnModalProducto(prod);
    } else {
        document.getElementById('modal-producto-title').textContent = 'Nuevo Producto';
        document.getElementById('prod-id').value = '';
        document.getElementById('prod-precio-compra').value = '0';
        document.getElementById('prod-iva-incluido').value = 'true';
        document.getElementById('prod-alicuota-iva').value = '21.00';
        document.getElementById('prod-ganancia').value = '30.00';
        document.getElementById('prod-precio-venta').value = '0.00';
        document.getElementById('prod-stock-min-defecto').value = '5';
        wrapUpdBranch.classList.add('hidden');
        recalcularPreciosModal('COSTO_O_GANANCIA');
        renderizarTablaSucursalesEnModalProducto(null);
    }

    document.getElementById('modal-producto').classList.remove('hidden');
}

function renderizarTablaSucursalesEnModalProducto(prod) {
    const tbody = document.getElementById('tbody-prod-sucursales-config');
    const stockMinDef = parseFloat(document.getElementById('prod-stock-min-defecto').value) || 0;
    const mapExistente = new Map();
    if (prod && prod.sucursales) {
        prod.sucursales.forEach(s => mapExistente.set(s.sucursalId, s));
    }

    tbody.innerHTML = AppState.sucursales.map(s => {
        const cfg = mapExistente.get(s.id);
        const stockAct = cfg ? cfg.stockActual : 0;
        const stockMin = cfg ? cfg.stockMinimo : stockMinDef;
        const precioSuc = (cfg && cfg.precioVentaSucursal !== null && cfg.precioVentaSucursal !== undefined)
            ? cfg.precioVentaSucursal
            : '';
        const ubicacion = (cfg && cfg.ubicacionGondola) ? cfg.ubicacionGondola : '';

        return `
            <tr data-sucursal-id="${s.id}">
                <td><strong>${escapeHtml(s.codigo)}</strong> — ${escapeHtml(s.nombre)}</td>
                <td><input type="number" class="inp-suc-stock-actual" step="0.001" value="${stockAct}"></td>
                <td><input type="number" class="inp-suc-stock-minimo" min="0" step="0.001" value="${stockMin}"></td>
                <td><input type="number" class="inp-suc-precio" min="0.01" step="0.01" value="${precioSuc}" placeholder="Usa Precio General"></td>
                <td><input type="text" class="inp-suc-ubicacion" maxlength="80" value="${escapeHtml(ubicacion)}" placeholder="Ej: Pasillo 2"></td>
            </tr>
        `;
    }).join('');
}

function onStockMinimoDefectoChange() {
    const id = document.getElementById('prod-id').value;
    if (!id) {
        const nuevoMin = document.getElementById('prod-stock-min-defecto').value || '0';
        document.querySelectorAll('#tbody-prod-sucursales-config .inp-suc-stock-minimo').forEach(inp => {
            inp.value = nuevoMin;
        });
    }
}

function recalcularPreciosModal(origen) {
    const compra = parseFloat(document.getElementById('prod-precio-compra').value) || 0;
    const ivaIncl = document.getElementById('prod-iva-incluido').value === 'true';
    const alicuota = parseFloat(document.getElementById('prod-alicuota-iva').value) || 0;

    const costoFinalConIva = ivaIncl ? compra : compra * (1 + alicuota / 100);
    document.getElementById('prod-costo-final').value = costoFinalConIva.toFixed(2);

    const gananciaEl = document.getElementById('prod-ganancia');
    const ventaEl = document.getElementById('prod-precio-venta');

    if (origen === 'COSTO_O_GANANCIA') {
        const ganancia = parseFloat(gananciaEl.value) || 0;
        if (costoFinalConIva > 0) {
            const sugerido = costoFinalConIva * (1 + ganancia / 100);
            ventaEl.value = sugerido.toFixed(2);
        }
    } else if (origen === 'PRECIO_VENTA') {
        const venta = parseFloat(ventaEl.value) || 0;
        if (costoFinalConIva > 0) {
            const margenReal = ((venta - costoFinalConIva) / costoFinalConIva) * 100;
            gananciaEl.value = margenReal.toFixed(2);
        }
    }
}

async function onGenerarCodigoInternoClick() {
    try {
        const res = await ApiClient.get('/api/v1/productos/generar-codigo-interno');
        document.getElementById('prod-codigo-barras').value = res.codigoBarras;
        showToast(`Código EAN-13 interno generado: ${res.codigoBarras}`);
    } catch (err) {
        showToast(err.message);
    }
}

async function onCrearCategoriaRapida() {
    const nombre = prompt('Ingrese el nombre de la nueva Categoría:');
    if (!nombre || !nombre.trim()) return;
    try {
        const creada = await ApiClient.post('/api/v1/catalogo/categorias', { nombre: nombre.trim() });
        AppState.categorias.push(creada);
        poblarCombosModalProducto();
        poblarSelectoresFiltroProductos();
        document.getElementById('prod-categoria').value = creada.id;
        showToast(`Categoría "${creada.nombre}" creada`);
    } catch (err) {
        showToast(err.message);
    }
}

async function onCrearMarcaRapida() {
    const nombre = prompt('Ingrese el nombre de la nueva Marca:');
    if (!nombre || !nombre.trim()) return;
    try {
        const creada = await ApiClient.post('/api/v1/catalogo/marcas', { nombre: nombre.trim() });
        AppState.marcas.push(creada);
        poblarCombosModalProducto();
        poblarSelectoresFiltroProductos();
        document.getElementById('prod-marca').value = creada.id;
        showToast(`Marca "${creada.nombre}" creada`);
    } catch (err) {
        showToast(err.message);
    }
}

function cerrarModalProducto() {
    document.getElementById('modal-producto').classList.add('hidden');
}

async function onGuardarProducto(e) {
    e.preventDefault();
    const errorEl = document.getElementById('modal-producto-error');
    errorEl.classList.add('hidden');

    const id = document.getElementById('prod-id').value;
    const sucursalesConfig = Array.from(document.querySelectorAll('#tbody-prod-sucursales-config tr')).map(tr => {
        const precioRaw = tr.querySelector('.inp-suc-precio').value.trim();
        return {
            sucursalId: Number(tr.dataset.sucursalId),
            stockActual: parseFloat(tr.querySelector('.inp-suc-stock-actual').value) || 0,
            stockMinimo: parseFloat(tr.querySelector('.inp-suc-stock-minimo').value) || 0,
            precioVentaSucursal: precioRaw !== '' ? parseFloat(precioRaw) : null,
            ubicacionGondola: tr.querySelector('.inp-suc-ubicacion').value.trim() || null
        };
    });

    const payload = {
        codigoBarras: document.getElementById('prod-codigo-barras').value.trim(),
        generarCodigoInterno: false,
        codigoProveedorSku: document.getElementById('prod-sku').value.trim() || null,
        nombre: document.getElementById('prod-nombre').value.trim(),
        descripcion: document.getElementById('prod-descripcion').value.trim() || null,
        categoriaId: Number(document.getElementById('prod-categoria').value),
        marcaId: Number(document.getElementById('prod-marca').value),
        proveedorId: Number(document.getElementById('prod-proveedor').value),
        unidadMedida: document.getElementById('prod-unidad').value,
        precioCompra: parseFloat(document.getElementById('prod-precio-compra').value) || 0,
        ivaIncluidoCompra: document.getElementById('prod-iva-incluido').value === 'true',
        alicuotaIva: parseFloat(document.getElementById('prod-alicuota-iva').value) || 0,
        porcentajeGanancia: parseFloat(document.getElementById('prod-ganancia').value) || 0,
        precioVentaGeneral: parseFloat(document.getElementById('prod-precio-venta').value) || 0,
        stockMinimoDefecto: parseFloat(document.getElementById('prod-stock-min-defecto').value) || 0,
        actualizarPreciosSucursalProporcional: document.getElementById('prod-upd-proporcional').checked,
        restablecerPreciosSucursalAlGeneral: document.getElementById('prod-reset-precios-suc').checked,
        sucursalesConfig
    };

    try {
        if (id) {
            await ApiClient.put(`/api/v1/productos/${id}`, payload);
            showToast('Producto actualizado correctamente');
        } else {
            await ApiClient.post('/api/v1/productos', payload);
            showToast('Producto registrado correctamente');
        }
        cerrarModalProducto();
        await cargarVistaProductosCompleta();
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
}

/* =========================================================================
   ACTUALIZACIÓN MASIVA DE PRECIOS
   ========================================================================= */
function abrirModalAumentoMasivo() {
    document.getElementById('form-aumento-masivo').reset();
    document.getElementById('modal-masivo-error').classList.add('hidden');

    document.getElementById('mas-proveedor').innerHTML = `<option value="">Todos los proveedores</option>` +
        AppState.proveedores.map(p => `<option value="${p.id}">${escapeHtml(p.razonSocial)}</option>`).join('');
    document.getElementById('mas-categoria').innerHTML = `<option value="">Todas las categorías</option>` +
        AppState.categorias.map(c => `<option value="${c.id}">${escapeHtml(c.nombre)}</option>`).join('');
    document.getElementById('mas-marca').innerHTML = `<option value="">Todas las marcas</option>` +
        AppState.marcas.map(m => `<option value="${m.id}">${escapeHtml(m.nombre)}</option>`).join('');

    onModoRedondeoMasivoChange();
    document.getElementById('modal-aumento-masivo').classList.remove('hidden');
}

function cerrarModalAumentoMasivo() {
    document.getElementById('modal-aumento-masivo').classList.add('hidden');
}

function onModoRedondeoMasivoChange() {
    const modo = document.getElementById('mas-modo-redondeo').value;
    document.getElementById('wrap-mas-multiplo').classList.toggle('hidden', modo === 'SIN_REDONDEO');
}

window.setMultiploMasivo = function (valor) {
    document.getElementById('mas-multiplo').value = valor;
};

async function onEjecutarAumentoMasivo(e) {
    e.preventDefault();
    const errorEl = document.getElementById('modal-masivo-error');
    errorEl.classList.add('hidden');

    const provVal = document.getElementById('mas-proveedor').value;
    const catVal = document.getElementById('mas-categoria').value;
    const marcaVal = document.getElementById('mas-marca').value;
    const modoRedondeo = document.getElementById('mas-modo-redondeo').value;

    const payload = {
        proveedorId: provVal ? Number(provVal) : null,
        categoriaId: catVal ? Number(catVal) : null,
        marcaId: marcaVal ? Number(marcaVal) : null,
        porcentajeVariacion: parseFloat(document.getElementById('mas-porcentaje').value),
        actualizarTambienPrecioCompra: document.getElementById('mas-upd-compra').checked,
        modoRedondeo,
        multiploRedondeo: modoRedondeo === 'SIN_REDONDEO'
            ? null
            : (parseFloat(document.getElementById('mas-multiplo').value) || 1),
        aplicarPorcentajeAPreciosPersonalizadosSucursal: document.getElementById('mas-upd-sucursales').checked,
        restablecerPreciosPersonalizadosSucursal: document.getElementById('mas-reset-sucursales').checked
    };

    try {
        const res = await ApiClient.post('/api/v1/productos/actualizacion-masiva', payload);
        cerrarModalAumentoMasivo();
        showToast(`Se actualizaron ${res.productosActualizados} productos (${res.porcentajeAplicado}%)`);
        await cargarVistaProductosCompleta();
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
}

/* =========================================================================
   ETIQUETAS DE CÓDIGO DE BARRAS Y CÓDIGO QR PERSONALIZADO
   ========================================================================= */
window.abrirModalEtiqueta = function (productoId) {
    const prod = AppState.productos.find(p => p.id === productoId);
    if (!prod) return;
    AppState.productoEtiquetaActual = prod;
    renderizarVistaPreviaEtiqueta();
    document.getElementById('modal-etiqueta').classList.remove('hidden');
};

function cerrarModalEtiqueta() {
    document.getElementById('modal-etiqueta').classList.add('hidden');
}

window.renderizarVistaPreviaEtiqueta = function () {
    const prod = AppState.productoEtiquetaActual;
    if (!prod) return;

    const tipo = document.querySelector('input[name="etiq-tipo"]:checked').value;
    const mostrarPrecio = document.getElementById('etiq-mostrar-precio').checked;

    document.getElementById('etiq-nombre').textContent = prod.nombre;
    document.getElementById('etiq-marca').textContent = `${prod.marcaNombre} • ${prod.categoriaNombre}`;
    document.getElementById('etiq-codigo').textContent = prod.codigoBarras;

    const precioEl = document.getElementById('etiq-precio');
    precioEl.textContent = `$ ${formatearMoneda(prod.precioVentaContextoEfectivo)}`;
    precioEl.classList.toggle('hidden', !mostrarPrecio);

    const graphicContainer = document.getElementById('etiq-graphic-container');
    if (tipo === 'QR') {
        const qrTexto = `${prod.codigoBarras}|${prod.nombre}|$${formatearMoneda(prod.precioVentaContextoEfectivo)}`;
        graphicContainer.innerHTML = generarMatrizQrSvg(qrTexto, 128);
    } else {
        graphicContainer.innerHTML = generarCodigoBarrasSvg(prod.codigoBarras, 240, 64);
    }
};

function generarCodigoBarrasSvg(codigo, width, height) {
    const str = String(codigo || '0000000000000');
    let bits = '101';
    for (let i = 0; i < str.length; i++) {
        const charCode = str.charCodeAt(i);
        const pattern = ((charCode * 37 + i * 13) & 0x7F).toString(2).padStart(7, '0');
        bits += pattern.replace(/000/g, '010');
    }
    bits += '101';

    const barWidth = width / bits.length;
    let rects = '';
    for (let i = 0; i < bits.length; i++) {
        if (bits[i] === '1') {
            rects += `<rect x="${(i * barWidth).toFixed(2)}" y="0" width="${Math.max(barWidth, 1.1).toFixed(2)}" height="${height}" fill="#000"/>`;
        }
    }
    return `<svg width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" xmlns="http://www.w3.org/2000/svg">${rects}</svg>`;
}

function generarMatrizQrSvg(payload, size) {
    const cells = 21;
    const matrix = Array.from({ length: cells }, () => Array(cells).fill(false));

    function drawFinder(r0, c0) {
        for (let r = 0; r < 7; r++) {
            for (let c = 0; c < 7; c++) {
                if (r === 0 || r === 6 || c === 0 || c === 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4)) {
                    matrix[r0 + r][c0 + c] = true;
                }
            }
        }
    }
    drawFinder(0, 0);
    drawFinder(0, cells - 7);
    drawFinder(cells - 7, 0);

    let hash = 2166136261;
    for (let i = 0; i < payload.length; i++) {
        hash ^= payload.charCodeAt(i);
        hash = Math.imul(hash, 16777619);
    }

    for (let r = 0; r < cells; r++) {
        for (let c = 0; c < cells; c++) {
            const inFinder = (r < 8 && c < 8) || (r < 8 && c >= cells - 8) || (r >= cells - 8 && c < 8);
            if (!inFinder) {
                const charIdx = (r * cells + c) % payload.length;
                const bit = ((payload.charCodeAt(charIdx) + r * 17 + c * 31 + hash) & 3) !== 0;
                matrix[r][c] = bit;
            }
        }
    }

    const cellSize = size / cells;
    let rects = '';
    for (let r = 0; r < cells; r++) {
        for (let c = 0; c < cells; c++) {
            if (matrix[r][c]) {
                rects += `<rect x="${(c * cellSize).toFixed(2)}" y="${(r * cellSize).toFixed(2)}" width="${cellSize.toFixed(2)}" height="${cellSize.toFixed(2)}" fill="#000"/>`;
            }
        }
    }
    return `<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" xmlns="http://www.w3.org/2000/svg">${rects}</svg>`;
}

function imprimirEtiquetaActual() {
    const area = document.getElementById('etiqueta-print-area').innerHTML;
    const win = window.open('', '_blank', 'width=420,height=420');
    if (!win) {
        showToast('Permita las ventanas emergentes para imprimir la etiqueta.');
        return;
    }
    win.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>Imprimir Etiqueta</title>
            <style>
                body { font-family: sans-serif; text-align: center; padding: 15px; }
                .label-title { font-weight: 700; font-size: 15px; }
                .label-brand { font-size: 11px; color: #555; margin-bottom: 6px; }
                .label-code { font-family: monospace; font-size: 13px; letter-spacing: 2px; font-weight: 700; }
                .label-price { margin-top: 6px; font-size: 20px; font-weight: 800; }
                .hidden { display: none; }
            </style>
        </head>
        <body onload="window.print(); window.close();">
            ${area}
        </body>
        </html>
    `);
    win.document.close();
}

/* =========================================================================
   FASE 2: PANEL DE PROVEEDORES
   ========================================================================= */
async function cargarProveedores() {
    try {
        const lista = await ApiClient.get('/api/v1/proveedores?soloActivos=false');
        AppState.proveedores = lista;
        aplicarFiltroProveedoresLocal();
    } catch (err) {
        renderizarTablaProveedores([]);
        manejarErrorApi(err);
    }
}

function aplicarFiltroProveedoresLocal() {
    const q = (document.getElementById('flt-prov-q').value || '').trim().toLowerCase();
    const filtrados = AppState.proveedores.filter(p => {
        if (!q) return true;
        return (p.razonSocial || '').toLowerCase().includes(q)
            || (p.nombreFantasia || '').toLowerCase().includes(q)
            || (p.codigo || '').toLowerCase().includes(q)
            || (p.cuit || '').toLowerCase().includes(q);
    });

    document.getElementById('prov-count-label').textContent = `${filtrados.length} proveedores`;
    renderizarTablaProveedores(filtrados);
}

function renderizarTablaProveedores(lista) {
    const tbody = document.getElementById('tbody-proveedores');
    if (!lista.length) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center">No hay proveedores registrados.</td></tr>`;
        return;
    }

    tbody.innerHTML = lista.map(p => `
        <tr>
            <td>
                <strong>${escapeHtml(p.codigo)}</strong>
                ${p.esProveedorSistema ? '<div style="font-size:0.7rem;color:#2563eb;font-weight:600;">Por Defecto</div>' : ''}
            </td>
            <td>
                <strong>${escapeHtml(p.razonSocial)}</strong>
                ${p.nombreFantasia ? `<div style="font-size:0.78rem;color:#64748b;">${escapeHtml(p.nombreFantasia)}</div>` : ''}
            </td>
            <td>
                <div>${p.cuit ? escapeHtml(p.cuit) : '<span style="color:#94a3b8;">Sin CUIT</span>'}</div>
                <small style="color:#64748b;">${escapeHtml(p.condicionIvaDescripcion || p.condicionIva)}</small>
            </td>
            <td>
                <div>${escapeHtml(p.telefono || '-')}</div>
                <small style="color:#64748b;">${escapeHtml(p.email || '')}</small>
            </td>
            <td>
                <div>${escapeHtml(p.direccion || '-')}</div>
                <small style="color:#64748b;">${escapeHtml(p.localidad || '')} ${p.provincia ? `(${escapeHtml(p.provincia)})` : ''}</small>
            </td>
            <td><small>${escapeHtml(p.observaciones || '-')}</small></td>
            <td>
                <span class="status-pill ${p.activo ? 'status-active' : 'status-inactive'}">
                    ${p.activo ? 'Activo' : 'Inactivo'}
                </span>
            </td>
            <td>
                <div class="action-group">
                    <button class="btn btn-outline btn-xs" onclick="editarProveedor(${p.id})">Editar</button>
                    ${!p.esProveedorSistema ? `
                        <button class="btn btn-outline btn-xs" onclick="toggleEstadoProveedor(${p.id}, ${!p.activo})">
                            ${p.activo ? 'Desactivar' : 'Activar'}
                        </button>
                    ` : ''}
                </div>
            </td>
        </tr>
    `).join('');
}

window.editarProveedor = function (id) {
    const prov = AppState.proveedores.find(item => item.id === id);
    if (prov) abrirModalProveedor(prov);
};

window.toggleEstadoProveedor = async function (id, nuevoEstado) {
    try {
        await ApiClient.patch(`/api/v1/proveedores/${id}/estado`, { activo: nuevoEstado });
        showToast(nuevoEstado ? 'Proveedor activado' : 'Proveedor desactivado');
        await cargarProveedores();
    } catch (err) {
        manejarErrorApi(err);
    }
};

function abrirModalProveedor(prov) {
    const errorEl = document.getElementById('modal-proveedor-error');
    errorEl.classList.add('hidden');
    document.getElementById('form-proveedor').reset();

    if (prov) {
        document.getElementById('modal-proveedor-title').textContent = `Editar Proveedor: ${prov.codigo}`;
        document.getElementById('prov-id').value = prov.id;
        document.getElementById('prov-codigo').value = prov.codigo;
        document.getElementById('prov-codigo').readOnly = prov.esProveedorSistema;
        document.getElementById('prov-cuit').value = prov.cuit || '';
        document.getElementById('prov-razon-social').value = prov.razonSocial;
        document.getElementById('prov-fantasia').value = prov.nombreFantasia || '';
        document.getElementById('prov-condicion-iva').value = prov.condicionIva;
        document.getElementById('prov-telefono').value = prov.telefono || '';
        document.getElementById('prov-email').value = prov.email || '';
        document.getElementById('prov-direccion').value = prov.direccion || '';
        document.getElementById('prov-localidad').value = prov.localidad || '';
        document.getElementById('prov-provincia').value = prov.provincia || '';
        document.getElementById('prov-observaciones').value = prov.observaciones || '';
    } else {
        document.getElementById('modal-proveedor-title').textContent = 'Nuevo Proveedor';
        document.getElementById('prov-id').value = '';
        document.getElementById('prov-codigo').readOnly = false;
    }

    document.getElementById('modal-proveedor').classList.remove('hidden');
}

function cerrarModalProveedor() {
    document.getElementById('modal-proveedor').classList.add('hidden');
}

async function onGuardarProveedor(e) {
    e.preventDefault();
    const errorEl = document.getElementById('modal-proveedor-error');
    errorEl.classList.add('hidden');

    const id = document.getElementById('prov-id').value;
    const payload = {
        codigo: document.getElementById('prov-codigo').value.trim(),
        cuit: document.getElementById('prov-cuit').value.trim() || null,
        razonSocial: document.getElementById('prov-razon-social').value.trim(),
        nombreFantasia: document.getElementById('prov-fantasia').value.trim() || null,
        condicionIva: document.getElementById('prov-condicion-iva').value,
        telefono: document.getElementById('prov-telefono').value.trim() || null,
        email: document.getElementById('prov-email').value.trim() || null,
        direccion: document.getElementById('prov-direccion').value.trim() || null,
        localidad: document.getElementById('prov-localidad').value.trim() || null,
        provincia: document.getElementById('prov-provincia').value.trim() || null,
        observaciones: document.getElementById('prov-observaciones').value.trim() || null
    };

    try {
        if (id) {
            await ApiClient.put(`/api/v1/proveedores/${id}`, payload);
            showToast('Proveedor actualizado correctamente');
        } else {
            await ApiClient.post('/api/v1/proveedores', payload);
            showToast('Proveedor creado correctamente');
        }
        cerrarModalProveedor();
        await cargarProveedores();
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
}

/* =========================================================================
   SUCURSALES (FASE 1)
   ========================================================================= */
async function cargarSucursales() {
    try {
        const lista = await ApiClient.get('/api/v1/sucursales');
        AppState.sucursales = lista;
        renderizarTablaSucursales(lista);
    } catch (err) {
        renderizarTablaSucursales([]);
        manejarErrorApi(err);
    }
}

function renderizarTablaSucursales(lista) {
    const tbody = document.getElementById('tbody-sucursales');
    const admin = esAdmin();

    if (!lista.length) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center">No hay sucursales registradas.</td></tr>`;
        return;
    }

    tbody.innerHTML = lista.map(s => `
        <tr>
            <td><strong>${escapeHtml(s.codigo)}</strong></td>
            <td>
                ${escapeHtml(s.nombre)}
                ${s.esCasaCentral ? '<div style="font-size:0.75rem;color:#2563eb;font-weight:600;">Casa Central</div>' : ''}
            </td>
            <td>
                <div>${escapeHtml(s.razonSocial)}</div>
                <small style="color:#64748b;">CUIT: ${escapeHtml(s.cuit)}</small>
            </td>
            <td>${escapeHtml(s.condicionIvaDescripcion || s.condicionIva)}</td>
            <td><span class="pv-pill">${String(s.puntoVentaArca).padStart(5, '0')}</span></td>
            <td>
                <small>
                    ${s.vendeAlPublico ? '🛒 Venta Público' : ''}
                    ${s.tieneDeposito ? ' 📦 Depósito' : ''}
                </small>
            </td>
            <td>
                <span class="status-pill ${s.activo ? 'status-active' : 'status-inactive'}">
                    ${s.activo ? 'Activa' : 'Inactiva'}
                </span>
            </td>
            <td>
                ${admin ? `
                    <div class="action-group">
                        <button class="btn btn-outline btn-xs" onclick="editarSucursal(${s.id})">Editar</button>
                        <button class="btn btn-outline btn-xs" onclick="toggleEstadoSucursal(${s.id}, ${!s.activo})">
                            ${s.activo ? 'Desactivar' : 'Activar'}
                        </button>
                    </div>
                ` : '-'}
            </td>
        </tr>
    `).join('');
}

window.editarSucursal = function (id) {
    const suc = AppState.sucursales.find(item => item.id === id);
    if (suc) abrirModalSucursal(suc);
};

window.toggleEstadoSucursal = async function (id, nuevoEstado) {
    try {
        await ApiClient.patch(`/api/v1/sucursales/${id}/estado`, { activo: nuevoEstado });
        showToast(nuevoEstado ? 'Sucursal activada' : 'Sucursal desactivada');
        const sesionActualizada = await ApiClient.get('/api/v1/auth/me');
        procesarEstadoSesion(sesionActualizada);
    } catch (err) {
        manejarErrorApi(err);
    }
};

function abrirModalSucursal(suc) {
    const errorEl = document.getElementById('modal-sucursal-error');
    errorEl.classList.add('hidden');
    document.getElementById('form-sucursal').reset();

    if (suc) {
        document.getElementById('modal-sucursal-title').textContent = `Editar Sucursal: ${suc.codigo}`;
        document.getElementById('suc-id').value = suc.id;
        document.getElementById('suc-codigo').value = suc.codigo;
        document.getElementById('suc-nombre').value = suc.nombre;
        document.getElementById('suc-razon-social').value = suc.razonSocial;
        document.getElementById('suc-cuit').value = suc.cuit;
        document.getElementById('suc-condicion-iva').value = suc.condicionIva;
        document.getElementById('suc-pv-arca').value = suc.puntoVentaArca;
        document.getElementById('suc-iibb').value = suc.ingresosBrutos || '';
        document.getElementById('suc-inicio-act').value = suc.fechaInicioActividades || '';
        document.getElementById('suc-direccion').value = suc.direccion;
        document.getElementById('suc-localidad').value = suc.localidad || '';
        document.getElementById('suc-provincia').value = suc.provincia || '';
        document.getElementById('suc-telefono').value = suc.telefono || '';
        document.getElementById('suc-email').value = suc.email || '';
        document.getElementById('suc-vende-publico').checked = suc.vendeAlPublico;
        document.getElementById('suc-tiene-deposito').checked = suc.tieneDeposito;
        document.getElementById('suc-casa-central').checked = suc.esCasaCentral;
    } else {
        document.getElementById('modal-sucursal-title').textContent = 'Nueva Sucursal';
        document.getElementById('suc-id').value = '';
    }

    document.getElementById('modal-sucursal').classList.remove('hidden');
}

function cerrarModalSucursal() {
    document.getElementById('modal-sucursal').classList.add('hidden');
}

async function onGuardarSucursal(e) {
    e.preventDefault();
    const errorEl = document.getElementById('modal-sucursal-error');
    errorEl.classList.add('hidden');

    const id = document.getElementById('suc-id').value;
    const payload = {
        codigo: document.getElementById('suc-codigo').value.trim(),
        nombre: document.getElementById('suc-nombre').value.trim(),
        razonSocial: document.getElementById('suc-razon-social').value.trim(),
        cuit: document.getElementById('suc-cuit').value.trim(),
        condicionIva: document.getElementById('suc-condicion-iva').value,
        puntoVentaArca: parseInt(document.getElementById('suc-pv-arca').value, 10),
        ingresosBrutos: document.getElementById('suc-iibb').value.trim() || null,
        fechaInicioActividades: document.getElementById('suc-inicio-act').value || null,
        direccion: document.getElementById('suc-direccion').value.trim(),
        localidad: document.getElementById('suc-localidad').value.trim() || null,
        provincia: document.getElementById('suc-provincia').value.trim() || null,
        telefono: document.getElementById('suc-telefono').value.trim() || null,
        email: document.getElementById('suc-email').value.trim() || null,
        vendeAlPublico: document.getElementById('suc-vende-publico').checked,
        tieneDeposito: document.getElementById('suc-tiene-deposito').checked,
        esCasaCentral: document.getElementById('suc-casa-central').checked
    };

    try {
        if (id) {
            await ApiClient.put(`/api/v1/sucursales/${id}`, payload);
            showToast('Sucursal actualizada correctamente');
        } else {
            await ApiClient.post('/api/v1/sucursales', payload);
            showToast('Sucursal creada correctamente');
        }
        cerrarModalSucursal();
        const sesionActualizada = await ApiClient.get('/api/v1/auth/me');
        AppState.sesion = sesionActualizada;
        renderizarCabeceraUsuario(sesionActualizada);
        await cargarSucursales();
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
}

/* =========================================================================
   USUARIOS, ROLES Y PERMISOS GRANULARES (FASE 1)
   ========================================================================= */
async function cargarUsuarios() {
    try {
        if (!AppState.catalogoRolesPermisos) {
            AppState.catalogoRolesPermisos = await ApiClient.get('/api/v1/usuarios/catalogo-permisos');
        }
        if (!AppState.sucursales.length) {
            AppState.sucursales = await ApiClient.get('/api/v1/sucursales');
        }

        const sucursalFiltro = (!AppState.sesion.modoConsolidado && AppState.sesion.sucursalActiva)
            ? `?sucursalId=${AppState.sesion.sucursalActiva.id}`
            : '';

        const lista = await ApiClient.get(`/api/v1/usuarios${sucursalFiltro}`);
        AppState.usuarios = lista;
        renderizarTablaUsuarios(lista);
    } catch (err) {
        renderizarTablaUsuarios([]);
        manejarErrorApi(err);
    }
}

function renderizarTablaUsuarios(lista) {
    const tbody = document.getElementById('tbody-usuarios');
    if (!lista.length) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center">No hay empleados registrados para este alcance.</td></tr>`;
        return;
    }

    tbody.innerHTML = lista.map(u => {
        const rolesHtml = (u.roles || []).map(r => `<span class="pv-pill" style="margin-right:3px;">${escapeHtml(r)}</span>`).join('');
        const sucursalesHtml = u.accesoTodasSucursales
            ? '<span style="color:#2563eb;font-weight:600;">Todas las sucursales</span>'
            : (u.sucursalesHabilitadas || []).map(s => escapeHtml(s.codigo)).join(', ');

        let estadoSeguridadHtml = `
            <span class="status-pill ${u.activo ? 'status-active' : 'status-inactive'}">
                ${u.activo ? 'Activo' : 'Inactivo'}
            </span>
        `;
        if (u.cuentaBloqueada) {
            estadoSeguridadHtml += ` <span class="status-pill status-locked">🔒 Bloqueado (${u.intentosFallidos} int.)</span>`;
        } else if (u.requiereCambioPassword) {
            estadoSeguridadHtml += ` <span class="status-pill status-locked">Clave Temporal</span>`;
        }

        return `
            <tr>
                <td>
                    <strong>${escapeHtml(u.username)}</strong>
                    ${u.legajo ? `<div style="font-size:0.75rem;color:#64748b;">Leg: ${escapeHtml(u.legajo)}</div>` : ''}
                </td>
                <td>${escapeHtml(u.apellido)}, ${escapeHtml(u.nombre)}</td>
                <td>
                    <div>DNI: ${escapeHtml(u.dni)}</div>
                    ${u.telefono ? `<small style="color:#64748b;">Tel: ${escapeHtml(u.telefono)}</small>` : ''}
                </td>
                <td>
                    <div>${rolesHtml}</div>
                    <small style="color:#64748b;">${(u.permisosGranulares || []).length} permisos activos</small>
                </td>
                <td><small>${sucursalesHtml}</small></td>
                <td>${estadoSeguridadHtml}</td>
                <td>
                    <div class="action-group">
                        <button class="btn btn-outline btn-xs" onclick="editarUsuario(${u.id})">Editar</button>
                        <button class="btn btn-outline btn-xs" onclick="abrirModalResetPass(${u.id}, '${escapeHtml(u.username)}')">Blanquear Clave</button>
                        ${u.cuentaBloqueada ? `<button class="btn btn-primary btn-xs" onclick="desbloquearUsuario(${u.id})">Desbloquear</button>` : ''}
                        ${u.id !== AppState.sesion.usuarioId ? `
                            <button class="btn btn-outline btn-xs" onclick="toggleEstadoUsuario(${u.id}, ${!u.activo})">
                                ${u.activo ? 'Desactivar' : 'Activar'}
                            </button>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

window.editarUsuario = function (id) {
    const u = AppState.usuarios.find(item => item.id === id);
    if (u) abrirModalUsuario(u);
};

window.desbloquearUsuario = async function (id) {
    try {
        await ApiClient.post(`/api/v1/usuarios/${id}/desbloquear`);
        showToast('Cuenta de usuario desbloqueada');
        await cargarUsuarios();
    } catch (err) {
        manejarErrorApi(err);
    }
};

window.toggleEstadoUsuario = async function (id, nuevoEstado) {
    try {
        await ApiClient.patch(`/api/v1/usuarios/${id}/estado`, { activo: nuevoEstado });
        showToast(nuevoEstado ? 'Usuario activado' : 'Usuario desactivado');
        await cargarUsuarios();
    } catch (err) {
        manejarErrorApi(err);
    }
};

function abrirModalUsuario(usuario) {
    const errorEl = document.getElementById('modal-usuario-error');
    errorEl.classList.add('hidden');
    document.getElementById('form-usuario').reset();

    document.getElementById('wrap-usr-all-branches').classList.toggle('hidden', !esAdmin());
    renderizarCheckboxesCatalogoUsuario(usuario);

    const passGroup = document.getElementById('group-usr-password');
    const passInput = document.getElementById('usr-password');

    if (usuario) {
        document.getElementById('modal-usuario-title').textContent = `Editar Empleado: ${usuario.username}`;
        document.getElementById('usr-id').value = usuario.id;
        document.getElementById('usr-username').value = usuario.username;
        passGroup.classList.add('hidden');
        passInput.required = false;

        document.getElementById('usr-nombre').value = usuario.nombre;
        document.getElementById('usr-apellido').value = usuario.apellido;
        document.getElementById('usr-dni').value = usuario.dni;
        document.getElementById('usr-legajo').value = usuario.legajo || '';
        document.getElementById('usr-telefono').value = usuario.telefono || '';
        document.getElementById('usr-email').value = usuario.email || '';
        document.getElementById('usr-direccion').value = usuario.direccion || '';
        document.getElementById('usr-fecha-ingreso').value = usuario.fechaIngreso || '';
        document.getElementById('usr-acceso-todas').checked = usuario.accesoTodasSucursales;
        onToggleAccesoTodasSucursales();
    } else {
        document.getElementById('modal-usuario-title').textContent = 'Nuevo Empleado';
        document.getElementById('usr-id').value = '';
        passGroup.classList.remove('hidden');
        passInput.required = true;
        document.getElementById('usr-acceso-todas').checked = false;
        onToggleAccesoTodasSucursales();
    }

    document.getElementById('modal-usuario').classList.remove('hidden');
}

function renderizarCheckboxesCatalogoUsuario(usuario) {
    const cat = AppState.catalogoRolesPermisos;
    if (!cat) return;

    const rolesContainer = document.getElementById('usr-roles-container');
    const rolesUsuario = new Set((usuario && usuario.roles) || []);
    rolesContainer.innerHTML = cat.rolesDisponibles.map(r => `
        <label class="check-item">
            <input type="checkbox" name="usr-rol" value="${escapeHtml(r.codigo)}"
                ${rolesUsuario.has(r.codigo) ? 'checked' : ''}
                onchange="onRolCheckboxChange()">
            <span><b>${escapeHtml(r.nombre)}</b></span>
        </label>
    `).join('');

    const sucursalesContainer = document.getElementById('usr-sucursales-container');
    const sucursalesUsuario = new Set(((usuario && usuario.sucursalesHabilitadas) || []).map(s => s.id));
    const sucursalesAsignables = (AppState.sesion && AppState.sesion.sucursalesDisponibles) || [];
    sucursalesContainer.innerHTML = sucursalesAsignables.map(s => `
        <label class="check-item">
            <input type="checkbox" name="usr-sucursal" value="${s.id}"
                ${sucursalesUsuario.has(s.id) ? 'checked' : ''}>
            <span>${escapeHtml(s.codigo)} — ${escapeHtml(s.nombre)}</span>
        </label>
    `).join('');

    const permisosContainer = document.getElementById('usr-permisos-container');
    const permisosUsuario = new Set((usuario && usuario.permisosGranulares) || []);
    permisosContainer.innerHTML = cat.permisosDisponibles.map(p => `
        <label class="check-item">
            <input type="checkbox" name="usr-permiso" value="${escapeHtml(p.codigo)}"
                ${permisosUsuario.has(p.codigo) ? 'checked' : ''}>
            <span>${escapeHtml(p.descripcion)}</span>
        </label>
    `).join('');
}

window.onRolCheckboxChange = function () {
    const cat = AppState.catalogoRolesPermisos;
    if (!cat || !cat.permisosPorDefectoPorRol) return;

    const rolesSeleccionados = Array.from(document.querySelectorAll('input[name="usr-rol"]:checked'))
        .map(el => el.value);

    const permisosSugeridos = new Set();
    rolesSeleccionados.forEach(rol => {
        (cat.permisosPorDefectoPorRol[rol] || []).forEach(p => permisosSugeridos.add(p));
    });

    document.querySelectorAll('input[name="usr-permiso"]').forEach(chk => {
        if (permisosSugeridos.has(chk.value)) {
            chk.checked = true;
        }
    });
};

function onToggleAccesoTodasSucursales() {
    const todas = document.getElementById('usr-acceso-todas').checked;
    document.querySelectorAll('input[name="usr-sucursal"]').forEach(chk => {
        chk.disabled = todas;
        if (todas) chk.checked = true;
    });
}

function cerrarModalUsuario() {
    document.getElementById('modal-usuario').classList.add('hidden');
}

async function onGuardarUsuario(e) {
    e.preventDefault();
    const errorEl = document.getElementById('modal-usuario-error');
    errorEl.classList.add('hidden');

    const id = document.getElementById('usr-id').value;
    const roles = Array.from(document.querySelectorAll('input[name="usr-rol"]:checked')).map(el => el.value);
    const sucursalIds = Array.from(document.querySelectorAll('input[name="usr-sucursal"]:checked')).map(el => Number(el.value));
    const permisosGranulares = Array.from(document.querySelectorAll('input[name="usr-permiso"]:checked')).map(el => el.value);

    const basePayload = {
        username: document.getElementById('usr-username').value.trim(),
        nombre: document.getElementById('usr-nombre').value.trim(),
        apellido: document.getElementById('usr-apellido').value.trim(),
        dni: document.getElementById('usr-dni').value.trim(),
        legajo: document.getElementById('usr-legajo').value.trim() || null,
        telefono: document.getElementById('usr-telefono').value.trim() || null,
        email: document.getElementById('usr-email').value.trim() || null,
        direccion: document.getElementById('usr-direccion').value.trim() || null,
        fechaIngreso: document.getElementById('usr-fecha-ingreso').value || null,
        accesoTodasSucursales: document.getElementById('usr-acceso-todas').checked,
        roles,
        sucursalIds,
        permisosGranulares
    };

    try {
        if (id) {
            await ApiClient.put(`/api/v1/usuarios/${id}`, basePayload);
            showToast('Empleado actualizado correctamente');
        } else {
            const createPayload = {
                ...basePayload,
                passwordInicial: document.getElementById('usr-password').value
            };
            await ApiClient.post('/api/v1/usuarios', createPayload);
            showToast('Empleado creado (deberá elegir su clave al ingresar)');
        }
        cerrarModalUsuario();
        await cargarUsuarios();
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
}

/* =========================================================================
   BLANQUEO DE CONTRASEÑA Y AUTORIZACIÓN DE SUPERVISOR
   ========================================================================= */
window.abrirModalResetPass = function (id, username) {
    document.getElementById('form-reset-pass').reset();
    document.getElementById('modal-reset-error').classList.add('hidden');
    document.getElementById('reset-user-id').value = id;
    document.getElementById('reset-target-username').textContent = username;
    document.getElementById('modal-reset-pass').classList.remove('hidden');
};

function cerrarModalResetPass() {
    document.getElementById('modal-reset-pass').classList.add('hidden');
}

async function onConfirmarResetPass(e) {
    e.preventDefault();
    const errorEl = document.getElementById('modal-reset-error');
    errorEl.classList.add('hidden');

    const id = document.getElementById('reset-user-id').value;
    const nuevaPasswordTemporal = document.getElementById('reset-temp-pass').value;

    try {
        await ApiClient.post(`/api/v1/usuarios/${id}/blanquear-password`, { nuevaPasswordTemporal });
        showToast('Contraseña blanqueada y cuenta desbloqueada');
        cerrarModalResetPass();
        await cargarUsuarios();
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
}

function abrirModalSupervisorConPermiso(permiso) {
    document.getElementById('form-supervisor').reset();
    document.getElementById('modal-sup-error').classList.add('hidden');
    if (permiso) {
        document.getElementById('sup-permiso').value = permiso;
    }
    document.getElementById('modal-supervisor').classList.remove('hidden');
}

function cerrarModalSupervisor() {
    document.getElementById('modal-supervisor').classList.add('hidden');
}

async function onAutorizarSupervisorSubmit(e) {
    e.preventDefault();
    const errorEl = document.getElementById('modal-sup-error');
    errorEl.classList.add('hidden');

    const permisoRequerido = document.getElementById('sup-permiso').value;
    const usernameSupervisor = document.getElementById('sup-username').value.trim();
    const passwordSupervisor = document.getElementById('sup-password').value;

    try {
        const res = await ApiClient.post('/api/v1/auth/autorizar-supervisor', {
            permisoRequerido,
            usernameSupervisor,
            passwordSupervisor
        });
        AppState.posSupervisorAuth = {
            username: usernameSupervisor,
            password: passwordSupervisor,
            permiso: permisoRequerido
        };
        cerrarModalSupervisor();
        actualizarEstadoDescuentoInputPos();
        renderizarCarritoPos();
        showToast(`Autorizado por ${res.supervisorNombreCompleto} (${res.permisoAutorizado})`);
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
}

async function onLogout() {
    try {
        await ApiClient.post('/api/v1/auth/logout');
    } finally {
        AppState.sesion = null;
        mostrarVista('view-login');
    }
}

function manejarErrorApi(err) {
    if (err.status === 401) {
        showToast(err.message || 'Sesión expirada o invalidada');
        AppState.sesion = null;
        mostrarVista('view-login');
        return;
    }
    showToast(err.message);
}

function mostrarVista(viewId) {
    ['view-login', 'view-change-password', 'view-select-sucursal', 'view-app'].forEach(id => {
        const el = document.getElementById(id);
        if (id === viewId) {
            el.classList.remove('hidden');
        } else {
            el.classList.add('hidden');
        }
    });
}

function formatearMoneda(valor) {
    const num = Number(valor || 0);
    return num.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatearCantidadSegunUnidad(valor, permiteDecimales) {
    const num = Number(valor || 0);
    if (!permiteDecimales) {
        return Math.round(num).toLocaleString('es-AR');
    }
    return num.toLocaleString('es-AR', { minimumFractionDigits: 0, maximumFractionDigits: 3 });
}

function formatearFechaHora(isoStr) {
    if (!isoStr) return '-';
    const d = new Date(isoStr);
    if (isNaN(d.getTime())) return isoStr;
    return d.toLocaleString('es-AR', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
}

function showToast(msg, tipo = null) {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const texto = String(msg || '');
    const lower = texto.toLowerCase();
    let categoria = tipo;
    if (!categoria) {
        if (lower.includes('error') || lower.includes('no se pudo') || lower.includes('inválid') || lower.includes('denegad') || lower.includes('insuficiente') || lower.includes('bloquead')) {
            categoria = 'error';
        } else if (lower.includes('seleccione') || lower.includes('ingrese') || lower.includes('debe ') || lower.includes('verifique') || lower.includes('atención')) {
            categoria = 'warning';
        } else if (lower.includes('exitosa') || lower.includes('correctamente') || lower.includes('guardad') || lower.includes('cread') || lower.includes('registrad') || lower.includes('emitid') || lower.includes('actualizad') || lower.includes('autorizad')) {
            categoria = 'success';
        } else {
            categoria = 'info';
        }
    }

    const icono = categoria === 'success' ? '✅'
        : categoria === 'error' ? '❌'
        : categoria === 'warning' ? '⚠️'
        : 'ℹ️';

    const toast = document.createElement('div');
    toast.className = `toast toast-${categoria}`;
    toast.innerHTML = `
        <span>${icono} ${escapeHtml(texto)}</span>
        <button type="button" class="toast-close" aria-label="Cerrar">✕</button>
    `;
    toast.querySelector('.toast-close').addEventListener('click', () => toast.remove());
    container.appendChild(toast);
    setTimeout(() => {
        if (toast.parentNode) toast.remove();
    }, 4000);
}

function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

/* =========================================================================
   FASE 4: CONFIGURACIÓN ARCA, EMISIÓN DE FACTURAS A/B/C, NOTAS DE CRÉDITO
           Y FORMATOS IMPRIMIBLES (TICKET 80MM / HOJA A4 CON QR OFICIAL)
   ========================================================================= */
async function cargarPanelConfiguracionArca() {
    try {
        const lista = await ApiClient.get('/api/v1/arca/configuraciones');
        AppState.configuracionesArca = lista;

        const sel = document.getElementById('arca-cfg-sucursal-select');
        const prevVal = sel.value;
        sel.innerHTML = lista.map(c =>
            `<option value="${c.sucursalId}">${escapeHtml(c.sucursalCodigo)} — ${escapeHtml(c.sucursalNombre)} (CUIT: ${escapeHtml(c.cuitEmisor)} • PV: ${String(c.puntoVentaArca).padStart(5, '0')})</option>`
        ).join('');

        if (prevVal && lista.some(c => String(c.sucursalId) === String(prevVal))) {
            sel.value = prevVal;
        } else if (AppState.sesion && AppState.sesion.sucursalActiva) {
            sel.value = AppState.sesion.sucursalActiva.id;
        }

        onCambioSucursalConfigArca();
    } catch (err) {
        manejarErrorApi(err);
    }
}

function onCambioSucursalConfigArca() {
    const sucursalId = Number(document.getElementById('arca-cfg-sucursal-select').value);
    const cfg = AppState.configuracionesArca.find(c => c.sucursalId === sucursalId);
    if (!cfg) return;

    AppState.configArcaSucursalActiva = cfg;
    document.getElementById('arca-config-error').classList.add('hidden');
    document.getElementById('arca-file-crt').value = '';
    document.getElementById('arca-file-key').value = '';
    document.getElementById('arca-pem-crt').value = '';
    document.getElementById('arca-pem-key').value = '';

    document.getElementById('arca-razon-social').value = cfg.razonSocialEmisor || '';
    document.getElementById('arca-cuit').value = cfg.cuitEmisor || '';
    document.getElementById('arca-pv').value = cfg.puntoVentaArca || 1;
    document.getElementById('arca-condicion-iva').value = cfg.condicionIvaEmisor || 'RESPONSABLE_INSCRIPTO';
    document.getElementById('arca-iibb').value = cfg.ingresosBrutos || '';
    document.getElementById('arca-inicio-act').value = cfg.fechaInicioActividades || '';
    document.getElementById('arca-domicilio').value = cfg.domicilioComercial || '';
    document.getElementById('arca-entorno').value = cfg.entornoArca || 'HOMOLOGACION';
    document.getElementById('arca-simulador-activo').checked = Boolean(cfg.simuladorHomologacionActivo);
    document.getElementById('arca-tope-cf').value = Number(cfg.montoTopeConsumidorFinalSinDni || 344488).toFixed(2);
    document.getElementById('arca-plazo-dias').value = cfg.plazoMaximoDiasFacturacion ?? 30;

    onCambioEntornoConfigArca();

    const badgesEl = document.getElementById('arca-cfg-status-badges');
    badgesEl.innerHTML = `
        <span class="status-pill ${cfg.entornoArca === 'PRODUCCION' ? 'status-inactive' : 'status-active'}">
            ${escapeHtml(cfg.entornoArcaDescripcion)}
        </span>
        <span class="status-pill ${cfg.poseeCertificadoCargado && cfg.poseeClavePrivadaCargada ? 'status-active' : 'status-locked'}">
            ${cfg.poseeCertificadoCargado && cfg.poseeClavePrivadaCargada ? '🔐 Certificado .crt y .key Cargados' : '⚠️ Sin Certificado .crt/.key'}
        </span>
        ${cfg.poseeTicketAccesoWsaaVigente ? `<span class="status-pill status-active">⚡ TA WSAA en Caché Vigente</span>` : ''}
    `;

    const summaryEl = document.getElementById('arca-cert-summary-box');
    if (cfg.poseeCertificadoCargado) {
        summaryEl.innerHTML = `
            <div><b>Archivo .crt actual:</b> ${escapeHtml(cfg.nombreArchivoCrt || 'certificado.crt')} • <b>Archivo .key:</b> ${escapeHtml(cfg.nombreArchivoKey || 'clave.key')}</div>
            <div><b>Titular X.509 (Subject):</b> <code>${escapeHtml(cfg.certificadoSubjectResumen)}</code> • <b>Vencimiento Certificado:</b> <b>${escapeHtml(cfg.certificadoVencimientoResumen)}</b></div>
            <small style="color:#64748b;">Deje los campos de archivo en blanco si desea conservar el certificado y clave privada actuales.</small>
        `;
    } else {
        summaryEl.innerHTML = `
            <div>No se ha subido aún un Certificado X.509 (.crt) ni Clave Privada (.key) para esta sucursal.</div>
            <small style="color:#16a34a;font-weight:600;">En modo HOMOLOGACIÓN puede operar con el Simulador Local Asistido sin certificado, o subir su certificado de WSAA Homologación/Producción cuando lo desee.</small>
        `;
    }
}

function onCambioEntornoConfigArca() {
    const entorno = document.getElementById('arca-entorno').value;
    const wrapSim = document.getElementById('wrap-arca-simulador');
    if (entorno === 'PRODUCCION') {
        document.getElementById('arca-simulador-activo').checked = false;
        wrapSim.classList.add('hidden');
    } else {
        wrapSim.classList.remove('hidden');
    }
}

function leerArchivoPemEnTextarea(event, textareaId) {
    const file = event.target.files && event.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
        document.getElementById(textareaId).value = ev.target.result || '';
    };
    reader.readAsText(file);
}

async function onGuardarConfiguracionArca(e) {
    e.preventDefault();
    const errorEl = document.getElementById('arca-config-error');
    errorEl.classList.add('hidden');

    const sucursalId = Number(document.getElementById('arca-cfg-sucursal-select').value);
    const fileCrt = document.getElementById('arca-file-crt').files[0];
    const fileKey = document.getElementById('arca-file-key').files[0];

    const payload = {
        razonSocialEmisor: document.getElementById('arca-razon-social').value.trim(),
        cuitEmisor: document.getElementById('arca-cuit').value.trim(),
        puntoVentaArca: parseInt(document.getElementById('arca-pv').value, 10),
        condicionIvaEmisor: document.getElementById('arca-condicion-iva').value,
        ingresosBrutos: document.getElementById('arca-iibb').value.trim() || null,
        fechaInicioActividades: document.getElementById('arca-inicio-act').value || null,
        domicilioComercial: document.getElementById('arca-domicilio').value.trim(),
        entornoArca: document.getElementById('arca-entorno').value,
        simuladorHomologacionActivo: document.getElementById('arca-simulador-activo').checked,
        certificadoCrtPem: document.getElementById('arca-pem-crt').value.trim() || null,
        clavePrivadaKeyPem: document.getElementById('arca-pem-key').value.trim() || null,
        nombreArchivoCrt: fileCrt ? fileCrt.name : null,
        nombreArchivoKey: fileKey ? fileKey.name : null,
        montoTopeConsumidorFinalSinDni: parseFloat(document.getElementById('arca-tope-cf').value) || 0,
        plazoMaximoDiasFacturacion: parseInt(document.getElementById('arca-plazo-dias').value, 10) || 0,
        copiarCertificadosASucursalesMismoCuit: document.getElementById('arca-copiar-mismo-cuit').checked
    };

    try {
        await ApiClient.put(`/api/v1/arca/configuraciones/sucursal/${sucursalId}`, payload);
        showToast('Configuración Fiscal ARCA guardada correctamente');
        await cargarPanelConfiguracionArca();
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
}

async function onProbarConexionArcaClick() {
    const sucursalId = Number(document.getElementById('arca-cfg-sucursal-select').value);
    if (!sucursalId) return;
    try {
        const res = await ApiClient.post(`/api/v1/arca/configuraciones/sucursal/${sucursalId}/probar-conexion`);
        showToast(res.mensaje);
        await cargarPanelConfiguracionArca();
    } catch (err) {
        showToast(err.message);
    }
}

/* =========================================================================
   MODAL DE FACTURACIÓN POSTERIOR DE VENTA MEDIANTE ARCA (Reglas 1, 2 y 3)
   ========================================================================= */
window.abrirModalFacturarVentaArca = async function (ventaId) {
    try {
        const venta = await ApiClient.get(`/api/v1/ventas/${ventaId}`);
        const cfgArca = await ApiClient.get(`/api/v1/arca/configuraciones/sucursal/${venta.sucursalId}`);

        AppState.ventaAFacturarArca = venta;
        AppState.configArcaSucursalActiva = cfgArca;

        document.getElementById('form-facturar-arca').reset();
        document.getElementById('modal-facturar-arca-error').classList.add('hidden');
        document.getElementById('facturar-arca-venta-id').value = venta.id;

        // Regla 2.1 A: Precargar datos del cliente o Consumidor Final por defecto
        document.getElementById('facturar-arca-condicion-iva').value = venta.clienteCondicionIva || 'CONSUMIDOR_FINAL';
        document.getElementById('facturar-arca-doc').value = venta.clienteDocumento || '';
        document.getElementById('facturar-arca-nombre').value = venta.clienteNombre || 'Consumidor Final';
        document.getElementById('facturar-arca-domicilio').value = '';

        document.getElementById('facturar-arca-venta-resumen').innerHTML = `
            <div style="display:flex;justify-content:space-between;align-items:center;">
                <div>
                    <strong>Venta ${escapeHtml(venta.numeroComprobanteInterno)}</strong> — ${escapeHtml(venta.sucursalNombre)}
                    <div style="color:#475569;">Emisor: ${escapeHtml(cfgArca.razonSocialEmisor)} (${escapeHtml(cfgArca.condicionIvaEmisorDescripcion)}) • PV ${String(cfgArca.puntoVentaArca).padStart(5, '0')}</div>
                </div>
                <div style="font-size:1.15rem;font-weight:800;color:#0f172a;">$ ${formatearMoneda(venta.totalFinal)}</div>
            </div>
        `;

        actualizarPreviewModalFacturarArca();
        document.getElementById('modal-facturar-arca').classList.remove('hidden');
    } catch (err) {
        showToast(err.message);
    }
};

function actualizarPreviewModalFacturarArca() {
    const venta = AppState.ventaAFacturarArca;
    const cfg = AppState.configArcaSucursalActiva;
    if (!venta || !cfg) return;

    const condReceptor = document.getElementById('facturar-arca-condicion-iva').value;
    const docRaw = (document.getElementById('facturar-arca-doc').value || '').replace(/[^0-9]/g, '');

    let tipoCompDesc = 'Factura B (Cód. 006)';
    if (cfg.condicionIvaEmisor === 'MONOTRIBUTO') {
        tipoCompDesc = 'Factura C (Cód. 011 — Monotributo)';
    } else if (condReceptor === 'RESPONSABLE_INSCRIPTO' || condReceptor === 'MONOTRIBUTO') {
        tipoCompDesc = 'Factura A (Cód. 001 — Discrimina IVA)';
    } else {
        tipoCompDesc = 'Factura B (Cód. 006 — Transparencia Fiscal Ley 27.743)';
    }

    const modoTxt = (cfg.entornoArca === 'HOMOLOGACION' && cfg.simuladorHomologacionActivo)
        ? 'Homologación Asistida'
        : `WSFEv1 (${cfg.entornoArca})`;

    document.getElementById('facturar-arca-tipo-preview').innerHTML = `
        Comprobante a emitir en ARCA: <b>${tipoCompDesc}</b> • Entorno: <b>${modoTxt}</b>
    `;

    const lblDoc = document.getElementById('lbl-facturar-arca-doc');
    const warnEl = document.getElementById('facturar-arca-tope-warning');
    const topeCf = Number(cfg.montoTopeConsumidorFinalSinDni || 0);

    if (condReceptor === 'RESPONSABLE_INSCRIPTO' || condReceptor === 'MONOTRIBUTO' || condReceptor === 'EXENTO') {
        lblDoc.textContent = 'CUIT del Receptor (11 dígitos obligatorio) *';
        warnEl.classList.add('hidden');
    } else {
        lblDoc.textContent = 'DNI o CUIT del Receptor (Opcional salvo tope ARCA)';
        if (topeCf > 0 && Number(venta.totalFinal) >= topeCf && !docRaw) {
            warnEl.textContent = `⚠️ Atención (Regla 2.2): El total de la venta ($${formatearMoneda(venta.totalFinal)}) supera el tope de Consumidor Final sin identificar ($${formatearMoneda(topeCf)}). Es obligatorio ingresar el DNI o CUIT y Nombre del cliente.`;
            warnEl.classList.remove('hidden');
        } else {
            warnEl.classList.add('hidden');
        }
    }
}

function cerrarModalFacturarArca() {
    document.getElementById('modal-facturar-arca').classList.add('hidden');
}

async function onConfirmarFacturarVentaArca(e) {
    e.preventDefault();
    const errorEl = document.getElementById('modal-facturar-arca-error');
    errorEl.classList.add('hidden');

    const ventaId = document.getElementById('facturar-arca-venta-id').value;
    const btnSubmit = document.getElementById('btn-submit-facturar-arca');
    btnSubmit.disabled = true;

    const payload = {
        receptorCondicionIva: document.getElementById('facturar-arca-condicion-iva').value,
        receptorDocumento: document.getElementById('facturar-arca-doc').value.trim() || null,
        receptorNombreRazonSocial: document.getElementById('facturar-arca-nombre').value.trim() || 'Consumidor Final',
        receptorDomicilio: document.getElementById('facturar-arca-domicilio').value.trim() || null
    };

    try {
        const compEmitido = await ApiClient.post(`/api/v1/arca/ventas/${ventaId}/facturar`, payload);
        cerrarModalFacturarArca();
        showToast(`${compEmitido.tipoComprobanteDescripcion} ${compEmitido.numeroFormateado} autorizada (CAE: ${compEmitido.cae})`);
        if (AppState.seccionActiva === 'caja') {
            await cargarSeccionCajaEHistorial();
        }
        await abrirModalComprobanteFiscalArca(compEmitido.id, Number(ventaId));
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
        if (AppState.seccionActiva === 'caja') {
            await cargarSeccionCajaEHistorial();
        }
    } finally {
        btnSubmit.disabled = false;
    }
}

/* =========================================================================
   VISTA IMPRIMIBLE FISCAL ARCA (TICKET 80MM Y HOJA A4 CON QR OFICIAL RG 4892)
   ========================================================================= */
window.abrirModalComprobanteFiscalArca = async function (comprobanteId, ventaId) {
    try {
        const [comp, venta] = await Promise.all([
            ApiClient.get(`/api/v1/arca/comprobantes/${comprobanteId}`),
            ApiClient.get(`/api/v1/ventas/${ventaId}`)
        ]);
        AppState.comprobanteArcaActual = comp;
        AppState.ventaComprobanteArcaActual = venta;
        document.getElementById('modal-comp-arca-title').textContent =
            `🧾 ${comp.tipoComprobanteDescripcion} N° ${comp.numeroFormateado} (CAE: ${comp.cae})`;

        cambiarFormatoVistaComprobanteArca(AppState.formatoImpresionArca || 'A4');
        document.getElementById('modal-comprobante-arca').classList.remove('hidden');
    } catch (err) {
        showToast(err.message);
    }
};

function cambiarFormatoVistaComprobanteArca(formato) {
    AppState.formatoImpresionArca = formato;
    const btnTicket = document.getElementById('btn-vista-arca-ticket');
    const btnA4 = document.getElementById('btn-vista-arca-a4');

    btnTicket.className = formato === 'TICKET' ? 'btn btn-primary btn-xs' : 'btn btn-outline btn-xs';
    btnA4.className = formato === 'A4' ? 'btn btn-primary btn-xs' : 'btn btn-outline btn-xs';

    renderizarComprobanteArcaEnContenedor();
}

function renderizarComprobanteArcaEnContenedor() {
    const c = AppState.comprobanteArcaActual;
    const v = AppState.ventaComprobanteArcaActual;
    if (!c || !v) return;

    const container = document.getElementById('arca-print-container');
    const qrSvg = generarMatrizQrSvg(c.qrUrlOficialArca, 118);
    const codAfipPadded = String(c.codigoAfipComprobante).padStart(3, '0');
    const discriminaIva = c.letraComprobante === 'A';
    const esFacturaB = c.letraComprobante === 'B';

    if (AppState.formatoImpresionArca === 'TICKET') {
        const itemsTicket = (v.items || []).map(i => `
            <div style="margin-bottom:4px;">
                <div>${escapeHtml(i.nombreProducto)} ${discriminaIva ? `(${i.alicuotaIva}%)` : ''}</div>
                <div style="display:flex;justify-content:space-between;">
                    <span>${formatearCantidadSegunUnidad(i.cantidad, true)} ${escapeHtml(i.unidadMedidaAbreviatura)} x $${formatearMoneda(i.precioUnitario)}</span>
                    <b>$${formatearMoneda(i.subtotalNetoItem)}</b>
                </div>
            </div>
        `).join('');

        container.innerHTML = `
            <div class="arca-ticket-80mm" id="arca-printable-content">
                <div style="text-align:center;border-bottom:1px dashed #000;padding-bottom:6px;margin-bottom:6px;">
                    <strong style="font-size:0.95rem;">${escapeHtml(c.razonSocialEmisor)}</strong>
                    <div>${escapeHtml(c.domicilioComercialEmisor)}</div>
                    <div>CUIT: ${escapeHtml(c.cuitEmisor)}</div>
                    ${c.ingresosBrutosEmisor ? `<div>IIBB: ${escapeHtml(c.ingresosBrutosEmisor)}</div>` : ''}
                    <div>${escapeHtml(c.condicionIvaEmisorDescripcion)}</div>
                </div>
                <div style="text-align:center;border-bottom:1px dashed #000;padding-bottom:6px;margin-bottom:6px;">
                    <div style="font-size:1.05rem;font-weight:800;">${escapeHtml(c.tipoComprobanteDescripcion.toUpperCase())} (CÓD. ${codAfipPadded})</div>
                    <div>N°: <b>${escapeHtml(c.numeroFormateado)}</b></div>
                    <div>Fecha: ${escapeHtml(c.fechaEmision)}</div>
                    ${c.comprobanteAsociadoNumeroFormateado ? `<div>Comp. Asoc.: ${escapeHtml(c.comprobanteAsociadoNumeroFormateado)}</div>` : ''}
                </div>
                <div style="border-bottom:1px dashed #000;padding-bottom:6px;margin-bottom:6px;">
                    <div>Cliente: <b>${escapeHtml(c.receptorNombreRazonSocial)}</b></div>
                    <div>${escapeHtml(c.receptorTipoDocDescripcion)}: ${c.receptorNumeroDoc !== '0' ? escapeHtml(c.receptorNumeroDoc) : 'S/I'}</div>
                    <div>IVA: ${escapeHtml(c.receptorCondicionIvaDescripcion)}</div>
                </div>
                <div style="border-bottom:1px dashed #000;padding-bottom:6px;margin-bottom:6px;">
                    ${itemsTicket}
                </div>
                <div style="border-bottom:1px dashed #000;padding-bottom:6px;margin-bottom:6px;">
                    ${discriminaIva ? `
                        <div style="display:flex;justify-content:space-between;"><span>Neto Gravado:</span><span>$${formatearMoneda(c.importeNetoGravado)}</span></div>
                        ${Number(c.importeExento) > 0 ? `<div style="display:flex;justify-content:space-between;"><span>Exento:</span><span>$${formatearMoneda(c.importeExento)}</span></div>` : ''}
                        ${Number(c.importeIva21) > 0 ? `<div style="display:flex;justify-content:space-between;"><span>IVA 21%:</span><span>$${formatearMoneda(c.importeIva21)}</span></div>` : ''}
                        ${Number(c.importeIva105) > 0 ? `<div style="display:flex;justify-content:space-between;"><span>IVA 10.5%:</span><span>$${formatearMoneda(c.importeIva105)}</span></div>` : ''}
                        ${Number(c.importeIva27) > 0 ? `<div style="display:flex;justify-content:space-between;"><span>IVA 27%:</span><span>$${formatearMoneda(c.importeIva27)}</span></div>` : ''}
                    ` : ''}
                    <div style="display:flex;justify-content:space-between;font-size:1.05rem;font-weight:800;margin-top:4px;">
                        <span>TOTAL:</span><span>$ ${formatearMoneda(c.importeTotal)}</span>
                    </div>
                    ${esFacturaB ? `
                        <div style="margin-top:6px;font-size:0.72rem;border-top:1px dotted #000;padding-top:4px;">
                            Régimen de Transparencia Fiscal al Consumidor (Ley 27.743)<br>
                            <b>IVA Contenido: $ ${formatearMoneda(c.importeIvaContenidoTransparencia)}</b>
                        </div>
                    ` : ''}
                </div>
                <div style="text-align:center;">
                    <div><b>CAE N°: ${escapeHtml(c.cae)}</b></div>
                    <div><b>Vto. CAE: ${escapeHtml(c.caeFechaVencimiento)}</b></div>
                    <div style="margin:8px auto;display:flex;justify-content:center;">${qrSvg}</div>
                    <div style="font-size:0.7rem;font-weight:700;">Comprobante Autorizado — ARCA</div>
                </div>
            </div>
        `;
        return;
    }

    // Formato Hoja A4 Estándar Fiscal ARCA
    const filasItemsA4 = (v.items || []).map(i => `
        <tr>
            <td>${escapeHtml(i.codigoBarras)}</td>
            <td>${escapeHtml(i.nombreProducto)}</td>
            <td style="text-align:right;">${formatearCantidadSegunUnidad(i.cantidad, true)} ${escapeHtml(i.unidadMedidaAbreviatura)}</td>
            <td style="text-align:right;">$ ${formatearMoneda(i.precioUnitario)}</td>
            <td style="text-align:right;">${formatearMoneda(i.descuentoItemPorcentaje)}%</td>
            ${discriminaIva ? `<td style="text-align:right;">${formatearMoneda(i.alicuotaIva)}%</td>` : ''}
            <td style="text-align:right;"><b>$ ${formatearMoneda(i.subtotalNetoItem)}</b></td>
        </tr>
    `).join('');

    container.innerHTML = `
        <div class="arca-a4-sheet" id="arca-printable-content">
            <div class="arca-a4-header">
                <div>
                    <div style="font-size:1.15rem;font-weight:800;">${escapeHtml(c.razonSocialEmisor)}</div>
                    <div style="margin-top:0.25rem;"><b>Domicilio Comercial:</b> ${escapeHtml(c.domicilioComercialEmisor)}</div>
                    <div><b>Condición frente al IVA:</b> ${escapeHtml(c.condicionIvaEmisorDescripcion)}</div>
                </div>
                <div class="arca-letra-box">
                    <div class="letra">${escapeHtml(c.letraComprobante)}</div>
                    <div class="cod">CÓD. ${codAfipPadded}</div>
                </div>
                <div style="text-align:right;">
                    <div style="font-size:1.1rem;font-weight:800;">${escapeHtml(c.tipoComprobanteDescripcion.toUpperCase())}</div>
                    <div style="font-size:1rem;font-weight:700;color:#1e40af;">N° ${escapeHtml(c.numeroFormateado)}</div>
                    <div><b>Fecha de Emisión:</b> ${escapeHtml(c.fechaEmision)}</div>
                    <div><b>CUIT:</b> ${escapeHtml(c.cuitEmisor)}</div>
                    ${c.ingresosBrutosEmisor ? `<div><b>Ingresos Brutos:</b> ${escapeHtml(c.ingresosBrutosEmisor)}</div>` : ''}
                    ${c.fechaInicioActividadesEmisor ? `<div><b>Inicio de Actividades:</b> ${escapeHtml(c.fechaInicioActividadesEmisor)}</div>` : ''}
                </div>
            </div>

            <div class="arca-receptor-box">
                <div><b>Cliente / Razón Social:</b> ${escapeHtml(c.receptorNombreRazonSocial)}</div>
                <div><b>${escapeHtml(c.receptorTipoDocDescripcion)}:</b> ${c.receptorNumeroDoc !== '0' ? escapeHtml(c.receptorNumeroDoc) : 'Sin identificar'}</div>
                <div><b>Condición frente al IVA:</b> ${escapeHtml(c.receptorCondicionIvaDescripcion)}</div>
                <div><b>Domicilio:</b> ${escapeHtml(c.receptorDomicilio || '-')}</div>
                ${c.comprobanteAsociadoNumeroFormateado ? `<div style="grid-column:1/-1;color:#dc2626;font-weight:700;">Comprobante Original Asociado: ${escapeHtml(c.comprobanteAsociadoNumeroFormateado)}</div>` : ''}
            </div>

            <table class="data-table compact-table" style="border:1px solid #cbd5e1;margin-bottom:0.85rem;">
                <thead>
                    <tr>
                        <th>Código</th>
                        <th> Producto / Descripción</th>
                        <th style="text-align:right;">Cant.</th>
                        <th style="text-align:right;">P. Unitario</th>
                        <th style="text-align:right;">% Bonif.</th>
                        ${discriminaIva ? `<th style="text-align:right;">Alíc. IVA</th>` : ''}
                        <th style="text-align:right;">Subtotal</th>
                    </tr>
                </thead>
                <tbody>${filasItemsA4}</tbody>
            </table>

            <div style="display:flex;justify-content:space-between;align-items:flex-end;gap:1rem;">
                <div style="font-size:0.78rem;color:#334155;max-width:55%;">
                    ${esFacturaB ? `
                        <div style="padding:0.5rem;border:1px solid #94a3b8;border-radius:4px;background:#f8fafc;">
                            <b>Régimen de Transparencia Fiscal al Consumidor (Ley 27.743):</b><br>
                            IVA Contenido en esta operación: <b>$ ${formatearMoneda(c.importeIvaContenidoTransparencia)}</b>
                        </div>
                    ` : ''}
                    <div style="margin-top:0.4rem;">Ref. Interna POS: ${escapeHtml(c.numeroComprobanteInternoVenta)} • Entorno: ${escapeHtml(c.entornoArcaDescripcion)}</div>
                </div>
                <div style="min-width:250px;border:1px solid #0f172a;padding:0.65rem;background:#f8fafc;">
                    ${discriminaIva ? `
                        <div style="display:flex;justify-content:space-between;"><span>Importe Neto Gravado:</span><b>$ ${formatearMoneda(c.importeNetoGravado)}</b></div>
                        ${Number(c.importeExento) > 0 ? `<div style="display:flex;justify-content:space-between;"><span>Importe Exento:</span><b>$ ${formatearMoneda(c.importeExento)}</b></div>` : ''}
                        ${Number(c.importeIva21) > 0 ? `<div style="display:flex;justify-content:space-between;"><span>IVA 21%:</span><b>$ ${formatearMoneda(c.importeIva21)}</b></div>` : ''}
                        ${Number(c.importeIva105) > 0 ? `<div style="display:flex;justify-content:space-between;"><span>IVA 10.5%:</span><b>$ ${formatearMoneda(c.importeIva105)}</b></div>` : ''}
                        ${Number(c.importeIva27) > 0 ? `<div style="display:flex;justify-content:space-between;"><span>IVA 27%:</span><b>$ ${formatearMoneda(c.importeIva27)}</b></div>` : ''}
                    ` : ''}
                    <div style="display:flex;justify-content:space-between;font-size:1.1rem;font-weight:900;border-top:1px solid #cbd5e1;margin-top:0.35rem;padding-top:0.35rem;">
                        <span>IMPORTE TOTAL:</span><span>$ ${formatearMoneda(c.importeTotal)}</span>
                    </div>
                </div>
            </div>

            <div class="arca-footer-grid">
                <div style="display:flex;align-items:center;gap:0.85rem;">
                    <div>${qrSvg}</div>
                    <div>
                        <div style="font-weight:900;font-size:0.95rem;">ARCA — Comprobante Autorizado</div>
                        <div style="font-size:0.74rem;color:#475569;max-width:310px;">
                            Código QR Oficial RG 4892 verificable en:<br>
                            <code style="font-size:0.66rem;word-break:break-all;">${escapeHtml(c.qrUrlOficialArca.slice(0, 72))}...</code>
                        </div>
                    </div>
                </div>
                <div style="text-align:right;">
                    <div style="font-size:0.95rem;"><b>CAE N°:</b> ${escapeHtml(c.cae)}</div>
                    <div style="font-size:0.9rem;"><b>Fecha de Vto. de CAE:</b> ${escapeHtml(c.caeFechaVencimiento)}</div>
                </div>
            </div>
        </div>
    `;
}

function cerrarModalComprobanteArca() {
    document.getElementById('modal-comprobante-arca').classList.add('hidden');
}

function imprimirComprobanteArcaActual() {
    const contentEl = document.getElementById('arca-printable-content');
    if (!contentEl) return;
    const esTicket = AppState.formatoImpresionArca === 'TICKET';
    const win = window.open('', '_blank', esTicket ? 'width=380,height=640' : 'width=860,height=720');
    if (!win) {
        showToast('Permita las ventanas emergentes para imprimir el comprobante fiscal.');
        return;
    }
    win.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>Comprobante Fiscal ARCA</title>
            <link rel="stylesheet" href="/css/styles.css">
            <style>
                body { background: #fff; padding: ${esTicket ? '6px' : '16px'}; }
            </style>
        </head>
        <body onload="setTimeout(() => { window.print(); window.close(); }, 180);">
            ${contentEl.outerHTML}
        </body>
        </html>
    `);
    win.document.close();
}

/* =========================================================================
   HISTORIAL DE INTENTOS DE FACTURACIÓN ARCA (Regla 3.2)
   ========================================================================= */
window.abrirModalIntentosArca = async function (ventaId) {
    try {
        const intentos = await ApiClient.get(`/api/v1/arca/ventas/${ventaId}/intentos`);
        const tbody = document.getElementById('tbody-intentos-arca');
        if (!intentos.length) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center">Sin intentos registrados.</td></tr>`;
        } else {
            tbody.innerHTML = intentos.map(i => `
                <tr>
                    <td>${formatearFechaHora(i.fechaIntento)}</td>
                    <td><strong>${escapeHtml(i.naturalezaOperacion)}</strong></td>
                    <td><small>${escapeHtml(i.modoEmision)} (${escapeHtml(i.entornoArca)})</small></td>
                    <td>
                        <span class="status-pill ${i.exitoso ? 'status-active' : 'status-inactive'}">
                            ${i.exitoso ? 'APROBADO' : 'ERROR / RECHAZO'}
                        </span>
                    </td>
                    <td><small>${escapeHtml(i.mensajeDetalle)}</small></td>
                </tr>
            `).join('');
        }
        document.getElementById('modal-intentos-arca').classList.remove('hidden');
    } catch (err) {
        showToast(err.message);
    }
};

function cerrarModalIntentosArca() {
    document.getElementById('modal-intentos-arca').classList.add('hidden');
}

/* =========================================================================
   FASE 5: PANEL ESTADÍSTICO E INTELIGENCIA DE NEGOCIO (BI)
   ========================================================================= */
async function inicializarYCargarSeccionEstadisticas() {
    const selSuc = document.getElementById('bi-flt-sucursal');
    const sesion = AppState.sesion;
    if (sesion && !selSuc.options.length) {
        let htmlOpts = '';
        if (esAdmin() || tienePermiso('VER_REPORTES_CONSOLIDADOS')) {
            htmlOpts += `<option value="0">🌐 Todas las Sucursales (Consolidado)</option>`;
        }
        (sesion.sucursalesDisponibles || []).forEach(s => {
            htmlOpts += `<option value="${s.id}">${escapeHtml(s.codigo)} — ${escapeHtml(s.nombre)}</option>`;
        });
        selSuc.innerHTML = htmlOpts;

        if (!sesion.modoConsolidado && sesion.sucursalActiva) {
            selSuc.value = String(sesion.sucursalActiva.id);
        } else {
            selSuc.value = '0';
        }
    }

    const inpDesde = document.getElementById('bi-flt-desde');
    const inpHasta = document.getElementById('bi-flt-hasta');
    if (!inpDesde.value || !inpHasta.value) {
        aplicarRangoRapidoBi('30D', false);
    }

    await cargarDashboardEstadistico();
}

window.aplicarRangoRapidoBi = function (rango, autoConsultar = true) {
    const hoy = new Date();
    const hastaStr = formatearFechaIsoLocal(hoy);
    let desdeDate = new Date(hoy);

    if (rango === 'HOY') {
        desdeDate = new Date(hoy);
    } else if (rango === '7D') {
        desdeDate.setDate(hoy.getDate() - 6);
    } else if (rango === '30D') {
        desdeDate.setDate(hoy.getDate() - 29);
    } else if (rango === 'MES') {
        desdeDate = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
    }

    document.getElementById('bi-flt-desde').value = formatearFechaIsoLocal(desdeDate);
    document.getElementById('bi-flt-hasta').value = hastaStr;

    if (autoConsultar) {
        cargarDashboardEstadistico();
    }
};

function formatearFechaIsoLocal(dateObj) {
    const y = dateObj.getFullYear();
    const m = String(dateObj.getMonth() + 1).padStart(2, '0');
    const d = String(dateObj.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

async function cargarDashboardEstadistico() {
    try {
        const sucursalId = document.getElementById('bi-flt-sucursal').value || '0';
        const fechaDesde = document.getElementById('bi-flt-desde').value;
        const fechaHasta = document.getElementById('bi-flt-hasta').value;
        const filtroFacturacion = document.getElementById('bi-flt-facturacion').value || 'TODAS_REALIZADAS';

        const params = new URLSearchParams({
            sucursalId,
            fechaDesde,
            fechaHasta,
            filtroFacturacion
        });

        const data = await ApiClient.get(`/api/v1/estadisticas/dashboard?${params.toString()}`);
        AppState.dashboardBi = data;
        renderizarDashboardBi();
    } catch (err) {
        const emptyMsg = `<td colspan="6" class="text-center">No hay datos registrados.</td>`;
        ['tbody-bi-ranking-productos', 'tbody-bi-rubros', 'tbody-bi-vendedores', 'tbody-bi-sin-rotacion'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.innerHTML = `<tr>${emptyMsg}</tr>`;
        });
        manejarErrorApi(err);
    }
}

function renderizarDashboardBi() {
    const d = AppState.dashboardBi;
    if (!d) return;

    renderizarKpisBi(d);
    renderizarGraficoEvolucionBi(d);
    renderizarDistribucionMediosPagoBi(d);
    renderizarRankingProductosBi(d);
    renderizarGraficoFlujoHorarioBi(d);
    renderizarTablaRubrosBi(d);
    renderizarTablaVendedoresBi(d);
    renderizarTablaSinRotacionBi(d);
}

function renderizarKpisBi(d) {
    const grid = document.getElementById('bi-kpis-grid');
    const verCostos = Boolean(d.puedeVerCostosYGanancias);

    const cardGanancia = verCostos ? `
        <div class="bi-kpi-card profit">
            <span class="bi-kpi-label">💰 Ganancia Bruta Real (Período)</span>
            <div class="bi-kpi-value" style="color:#15803d;">$ ${formatearMoneda(d.gananciaBrutaRealMonto)}</div>
            <div class="bi-kpi-sub">
                Margen Real: <b>${formatearMoneda(d.margenGananciaBrutaPorcentaje)}%</b> • Costo Mercadería: $${formatearMoneda(d.costoTotalMercaderiaVendida)}
            </div>
        </div>
    ` : '';

    const cardInventario = `
        <div class="bi-kpi-card">
            <span class="bi-kpi-label">📦 Valuación de Stock Actual</span>
            <div class="bi-kpi-value">$ ${formatearMoneda(d.valorTotalStockVenta)}</div>
            <div class="bi-kpi-sub">
                ${verCostos ? `Capital a Costo: <b>$${formatearMoneda(d.capitalTotalStockCosto)}</b> • ` : ''}
                Sin rotación: <b>${d.cantidadProductosSinRotacion} art.</b>
            </div>
        </div>
    `;

    grid.innerHTML = `
        <div class="bi-kpi-card">
            <span class="bi-kpi-label">📅 Ventas del Día (Hoy)</span>
            <div class="bi-kpi-value">$ ${formatearMoneda(d.ventasHoyMonto)}</div>
            <div class="bi-kpi-sub"><b>${d.ventasHoyCantidad}</b> operaciones registradas hoy</div>
        </div>
        <div class="bi-kpi-card">
            <span class="bi-kpi-label">📆 Ventas del Mes Actual</span>
            <div class="bi-kpi-value">$ ${formatearMoneda(d.ventasMesActualMonto)}</div>
            <div class="bi-kpi-sub"><b>${d.ventasMesActualCantidad}</b> operaciones en el mes en curso</div>
        </div>
        <div class="bi-kpi-card">
            <span class="bi-kpi-label">📊 Ventas del Año Actual</span>
            <div class="bi-kpi-value">$ ${formatearMoneda(d.ventasAnioActualMonto)}</div>
            <div class="bi-kpi-sub"><b>${d.ventasAnioActualCantidad}</b> operaciones acumuladas en el año</div>
        </div>
        <div class="bi-kpi-card highlight">
            <span class="bi-kpi-label">🎯 Total Período Filtrado & Ticket Prom.</span>
            <div class="bi-kpi-value" style="color:#1e40af;">$ ${formatearMoneda(d.ventasPeriodoMonto)}</div>
            <div class="bi-kpi-sub">
                <b>${d.ventasPeriodoCantidad}</b> ventas • Ticket Prom.: <b>$${formatearMoneda(d.ticketPromedioPeriodo)}</b> (${formatearMoneda(d.unidadesPromedioPorTicket)} u/ticket)
            </div>
        </div>
        ${cardGanancia}
        ${cardInventario}
        <div class="bi-kpi-card">
            <span class="bi-kpi-label">🏛️ Facturación ARCA vs. Pendientes</span>
            <div class="bi-kpi-value" style="font-size:1.15rem;">
                ARCA: $ ${formatearMoneda(d.ventasFacturadasArcaMonto)} <small style="font-weight:600;color:#64748b;">(${d.ventasFacturadasArcaCantidad})</small>
            </div>
            <div class="bi-kpi-sub">
                No Facturadas: <b>$${formatearMoneda(d.ventasNoFacturadasMonto)}</b> (${d.ventasNoFacturadasCantidad})
                • Anuladas excluidas: <b style="color:#dc2626;">${d.ventasAnuladasCantidad} ($${formatearMoneda(d.ventasAnuladasMonto)})</b>
            </div>
        </div>
    `;
}

function cambiarModoEvolucionBi(modo) {
    AppState.biModoEvolucion = modo;
    document.getElementById('btn-bi-evo-dia').className = modo === 'DIA' ? 'btn btn-primary btn-xs' : 'btn btn-outline btn-xs';
    document.getElementById('btn-bi-evo-mes').className = modo === 'MES' ? 'btn btn-primary btn-xs' : 'btn btn-outline btn-xs';
    if (AppState.dashboardBi) {
        renderizarGraficoEvolucionBi(AppState.dashboardBi);
    }
}

function renderizarGraficoEvolucionBi(d) {
    const box = document.getElementById('bi-chart-evolucion');
    const serie = AppState.biModoEvolucion === 'MES' ? (d.evolucionPorMes || []) : (d.evolucionPorDia || []);
    const verCostos = Boolean(d.puedeVerCostosYGanancias);

    if (!serie.length) {
        box.innerHTML = `<p class="subtitle">Sin datos para el período seleccionado.</p>`;
        return;
    }

    const maxVal = Math.max(1, ...serie.map(item => Number(item.totalFacturado || 0)));
    const barras = serie.slice(-31).map(p => {
        const pct = Math.min(100, Math.round((Number(p.totalFacturado || 0) / maxVal) * 100));
        const pctGan = verCostos && p.gananciaBruta !== null
            ? Math.min(100, Math.max(0, Math.round((Number(p.gananciaBruta || 0) / maxVal) * 100)))
            : 0;
        return `
            <div class="bi-bar-row">
                <div class="bi-bar-header">
                    <span><b>${escapeHtml(p.etiqueta)}</b> <small style="color:#64748b;">(${p.cantidadVentas} op.)</small></span>
                    <span>
                        <b>$ ${formatearMoneda(p.totalFacturado)}</b>
                        ${verCostos && p.gananciaBruta !== null ? `<small style="color:#16a34a;margin-left:6px;">(Gan: $${formatearMoneda(p.gananciaBruta)})</small>` : ''}
                    </span>
                </div>
                <div class="bi-bar-track" style="position:relative;">
                    <div class="bi-bar-fill" style="width:${pct}%;"></div>
                    ${verCostos ? `<div class="bi-bar-fill green" style="width:${pctGan}%;position:absolute;top:0;left:0;opacity:0.85;" title="Ganancia Bruta"></div>` : ''}
                </div>
            </div>
        `;
    }).join('');

    box.innerHTML = `<div style="max-height:310px;overflow-y:auto;padding-right:4px;">${barras}</div>`;
}

function renderizarDistribucionMediosPagoBi(d) {
    const box = document.getElementById('bi-chart-medios-pago');
    const lista = d.distribucionMediosPago || [];

    const filas = lista.map(m => `
        <div class="bi-bar-row">
            <div class="bi-bar-header">
                <span><b>${escapeHtml(m.nombre)}</b> <small style="color:#64748b;">(${m.cantidadOperaciones} cobros)</small></span>
                <span><b>$ ${formatearMoneda(m.montoFinalCobrado)}</b> (${formatearMoneda(m.porcentajeParticipacion)}%)</span>
            </div>
            <div class="bi-bar-track">
                <div class="bi-bar-fill" style="width:${Math.min(100, Number(m.porcentajeParticipacion || 0))}%;"></div>
            </div>
            ${Number(m.montoAjusteTotal) !== 0 ? `
                <div style="font-size:0.72rem;color:#64748b;margin-top:2px;">
                    Base: $${formatearMoneda(m.montoBaseTotal)} • Ajuste recargo/desc.: ${Number(m.montoAjusteTotal) >= 0 ? '+' : ''}$${formatearMoneda(m.montoAjusteTotal)}
                </div>
            ` : ''}
        </div>
    `).join('');

    const compArcaHtml = (d.desgloseComprobantesArca || []).length
        ? `<hr class="divider">
           <div style="font-size:0.8rem;">
               <strong>Desglose de Comprobantes ARCA en el Período:</strong>
               <div style="display:flex;flex-wrap:wrap;gap:0.45rem;margin-top:0.35rem;">
                   ${d.desgloseComprobantesArca.map(c => `
                       <span class="pv-pill">${escapeHtml(c.tipoComprobante)}: ${c.cantidad} ($${formatearMoneda(c.importeTotal)})</span>
                   `).join('')}
               </div>
           </div>`
        : '';

    box.innerHTML = filas + compArcaHtml;
}

function cambiarModoRankingBi(modo) {
    AppState.biModoRanking = modo;
    document.getElementById('btn-bi-rank-fact').className = modo === 'FACTURACION' ? 'btn btn-primary btn-xs' : 'btn btn-outline btn-xs';
    document.getElementById('btn-bi-rank-unid').className = modo === 'UNIDADES' ? 'btn btn-primary btn-xs' : 'btn btn-outline btn-xs';
    if (AppState.dashboardBi) {
        renderizarRankingProductosBi(AppState.dashboardBi);
    }
}

function renderizarRankingProductosBi(d) {
    const thead = document.getElementById('thead-bi-ranking-productos');
    const tbody = document.getElementById('tbody-bi-ranking-productos');
    const verCostos = Boolean(d.puedeVerCostosYGanancias);
    const lista = AppState.biModoRanking === 'UNIDADES'
        ? (d.rankingPorUnidades || [])
        : (d.rankingPorFacturacion || []);

    thead.innerHTML = `
        <th># / Producto</th>
        <th>Categoría</th>
        <th style="text-align:right;">Unidades</th>
        <th style="text-align:right;">Facturado</th>
        ${verCostos ? `<th style="text-align:right;">Ganancia Real</th><th style="text-align:right;">Margen</th>` : ''}
    `;

    if (!lista.length) {
        tbody.innerHTML = `<tr><td colspan="${verCostos ? 6 : 4}" class="text-center">No se registraron productos vendidos en este período.</td></tr>`;
        return;
    }

    tbody.innerHTML = lista.map((p, idx) => `
        <tr>
            <td>
                <strong>#${idx + 1} ${escapeHtml(p.nombre)}</strong>
                <div style="font-size:0.73rem;color:#64748b;">${escapeHtml(p.codigoBarras)} • ${escapeHtml(p.marca)}</div>
            </td>
            <td><small>${escapeHtml(p.categoria)}</small></td>
            <td style="text-align:right;"><b>${formatearCantidadSegunUnidad(p.unidadesVendidas, true)} ${escapeHtml(p.unidadMedida)}</b></td>
            <td style="text-align:right;"><strong>$ ${formatearMoneda(p.totalFacturado)}</strong></td>
            ${verCostos ? `
                <td style="text-align:right;color:#15803d;font-weight:700;">$ ${formatearMoneda(p.gananciaBruta)}</td>
                <td style="text-align:right;"><span class="pv-pill">${formatearMoneda(p.margenPorcentaje)}%</span></td>
            ` : ''}
        </tr>
    `).join('');
}

function cambiarModoFlujoBi(modo) {
    AppState.biModoFlujo = modo;
    document.getElementById('btn-bi-flujo-hora').className = modo === 'HORA' ? 'btn btn-primary btn-xs' : 'btn btn-outline btn-xs';
    document.getElementById('btn-bi-flujo-dia').className = modo === 'DIA_SEMANA' ? 'btn btn-primary btn-xs' : 'btn btn-outline btn-xs';
    if (AppState.dashboardBi) {
        renderizarGraficoFlujoHorarioBi(AppState.dashboardBi);
    }
}

function renderizarGraficoFlujoHorarioBi(d) {
    const box = document.getElementById('bi-chart-horas-pico');
    if (AppState.biModoFlujo === 'DIA_SEMANA') {
        const dias = d.ventasPorDiaSemana || [];
        const maxMonto = Math.max(1, ...dias.map(x => Number(x.totalFacturado || 0)));
        box.innerHTML = dias.map(dia => {
            const pct = Math.min(100, Math.round((Number(dia.totalFacturado || 0) / maxMonto) * 100));
            return `
                <div class="bi-bar-row">
                    <div class="bi-bar-header">
                        <span><b>${escapeHtml(dia.nombreDia)}</b> <small style="color:#64748b;">(${dia.cantidadVentas} ventas)</small></span>
                        <b>$ ${formatearMoneda(dia.totalFacturado)}</b>
                    </div>
                    <div class="bi-bar-track">
                        <div class="bi-bar-fill amber" style="width:${pct}%;"></div>
                    </div>
                </div>
            `;
        }).join('');
        return;
    }

    const horas = (d.ventasPorHora || []).filter(h => h.cantidadVentas > 0 || (h.hora >= 8 && h.hora <= 22));
    const maxOps = Math.max(1, ...horas.map(h => Number(h.cantidadVentas || 0)));
    box.innerHTML = `<div style="max-height:310px;overflow-y:auto;padding-right:4px;">` +
        horas.map(h => {
            const pct = Math.min(100, Math.round((Number(h.cantidadVentas || 0) / maxOps) * 100));
            return `
                <div class="bi-bar-row">
                    <div class="bi-bar-header">
                        <span><b>${escapeHtml(h.etiquetaHora)} hs</b></span>
                        <span><b>${h.cantidadVentas} tickets</b> ($ ${formatearMoneda(h.totalFacturado)})</span>
                    </div>
                    <div class="bi-bar-track">
                        <div class="bi-bar-fill amber" style="width:${pct}%;"></div>
                    </div>
                </div>
            `;
        }).join('') + `</div>`;
}

function cambiarModoRubroBi(modo) {
    AppState.biModoRubro = modo;
    document.getElementById('btn-bi-rubro-cat').className = modo === 'CATEGORIA' ? 'btn btn-primary btn-xs' : 'btn btn-outline btn-xs';
    document.getElementById('btn-bi-rubro-prov').className = modo === 'PROVEEDOR' ? 'btn btn-primary btn-xs' : 'btn btn-outline btn-xs';
    document.getElementById('btn-bi-rubro-suc').className = modo === 'SUCURSAL' ? 'btn btn-primary btn-xs' : 'btn btn-outline btn-xs';
    if (AppState.dashboardBi) {
        renderizarTablaRubrosBi(AppState.dashboardBi);
    }
}

function renderizarTablaRubrosBi(d) {
    const thead = document.getElementById('thead-bi-rubros');
    const tbody = document.getElementById('tbody-bi-rubros');
    const verCostos = Boolean(d.puedeVerCostosYGanancias);

    if (AppState.biModoRubro === 'SUCURSAL') {
        const sucs = d.comparativaSucursales || [];
        thead.innerHTML = `
            <th>Sucursal</th>
            <th style="text-align:right;">Ventas</th>
            <th style="text-align:right;">Facturado</th>
            ${verCostos ? `<th style="text-align:right;">Ganancia Bruta</th>` : ''}
            <th style="text-align:right;">% Part.</th>
        `;
        if (!sucs.length) {
            tbody.innerHTML = `<tr><td colspan="${verCostos ? 5 : 4}" class="text-center">Sin ventas registradas.</td></tr>`;
            return;
        }
        tbody.innerHTML = sucs.map(s => `
            <tr>
                <td><strong>${escapeHtml(s.codigo)} — ${escapeHtml(s.nombre)}</strong></td>
                <td style="text-align:right;">${s.cantidadVentas}</td>
                <td style="text-align:right;"><b>$ ${formatearMoneda(s.totalFacturado)}</b></td>
                ${verCostos ? `<td style="text-align:right;color:#15803d;font-weight:700;">$ ${formatearMoneda(s.gananciaBruta)}</td>` : ''}
                <td style="text-align:right;">${formatearMoneda(s.porcentajeParticipacion)}%</td>
            </tr>
        `).join('');
        return;
    }

    const lista = AppState.biModoRubro === 'PROVEEDOR'
        ? (d.rendimientoPorProveedor || [])
        : (d.rendimientoPorCategoria || []);

    thead.innerHTML = `
        <th>${AppState.biModoRubro === 'PROVEEDOR' ? 'Proveedor' : 'Categoría'}</th>
        <th style="text-align:right;">Unid.</th>
        <th style="text-align:right;">Facturado</th>
        ${verCostos ? `<th style="text-align:right;">Ganancia Real</th>` : ''}
        <th style="text-align:right;">% Part.</th>
    `;

    if (!lista.length) {
        tbody.innerHTML = `<tr><td colspan="${verCostos ? 5 : 4}" class="text-center">Sin movimientos en el período.</td></tr>`;
        return;
    }

    tbody.innerHTML = lista.map(r => `
        <tr>
            <td><strong>${escapeHtml(r.nombre)}</strong></td>
            <td style="text-align:right;">${formatearCantidadSegunUnidad(r.unidadesVendidas, true)}</td>
            <td style="text-align:right;"><b>$ ${formatearMoneda(r.totalFacturado)}</b></td>
            ${verCostos ? `<td style="text-align:right;color:#15803d;font-weight:700;">$ ${formatearMoneda(r.gananciaBruta)}</td>` : ''}
            <td style="text-align:right;">${formatearMoneda(r.porcentajeParticipacion)}%</td>
        </tr>
    `).join('');
}

function renderizarTablaVendedoresBi(d) {
    const tbody = document.getElementById('tbody-bi-vendedores');
    const lista = d.rendimientoPorVendedor || [];
    if (!lista.length) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center">Sin operaciones en el período.</td></tr>`;
        return;
    }
    tbody.innerHTML = lista.map(v => `
        <tr>
            <td>
                <strong>${escapeHtml(v.nombreCompleto)}</strong>
                <div style="font-size:0.73rem;color:#64748b;">@${escapeHtml(v.username)}</div>
            </td>
            <td style="text-align:right;"><b>${v.cantidadVentasRealizadas}</b></td>
            <td style="text-align:right;"><strong>$ ${formatearMoneda(v.totalFacturado)}</strong></td>
            <td style="text-align:right;">$ ${formatearMoneda(v.ticketPromedio)}</td>
            <td style="text-align:right;color:${v.cantidadAnulaciones > 0 ? '#dc2626' : '#64748b'};">
                ${v.cantidadAnulaciones} ($${formatearMoneda(v.montoAnulaciones)})
            </td>
        </tr>
    `).join('');
}

function renderizarTablaSinRotacionBi(d) {
    const badge = document.getElementById('bi-badge-sin-rotacion');
    const thead = document.getElementById('thead-bi-sin-rotacion');
    const tbody = document.getElementById('tbody-bi-sin-rotacion');
    const verCostos = Boolean(d.puedeVerCostosYGanancias);
    const lista = d.productosSinRotacion || [];

    badge.textContent = `${d.cantidadProductosSinRotacion} artículos inmovilizados`;

    thead.innerHTML = `
        <th>Código / Producto</th>
        <th>Categoría / Marca</th>
        <th style="text-align:right;">Stock Inmovilizado</th>
        <th style="text-align:right;">Precio Venta</th>
        ${verCostos ? `<th style="text-align:right;">Capital Inmovilizado (a Costo)</th>` : ''}
        <th style="text-align:right;">Valorizado a Venta</th>
    `;

    if (!lista.length) {
        tbody.innerHTML = `<tr><td colspan="${verCostos ? 6 : 5}" class="text-center">¡Excelente! Todos los productos con stock tuvieron rotación en el período.</td></tr>`;
        return;
    }

    tbody.innerHTML = lista.map(p => `
        <tr>
            <td>
                <strong>${escapeHtml(p.nombre)}</strong>
                <div style="font-size:0.73rem;color:#64748b;">${escapeHtml(p.codigoBarras)}</div>
            </td>
            <td><small>${escapeHtml(p.categoria)} • ${escapeHtml(p.marca)}</small></td>
            <td style="text-align:right;"><b>${formatearCantidadSegunUnidad(p.stockActual, true)} ${escapeHtml(p.unidadMedida)}</b></td>
            <td style="text-align:right;">$ ${formatearMoneda(p.precioVenta)}</td>
            ${verCostos ? `<td style="text-align:right;font-weight:700;color:#b45309;">$ ${formatearMoneda(p.capitalInmovilizadoCosto)}</td>` : ''}
            <td style="text-align:right;"><strong>$ ${formatearMoneda(p.valorInmovilizadoVenta)}</strong></td>
        </tr>
    `).join('');
}

function exportarBiCsvExcel() {
    const tipoReporte = document.getElementById('bi-export-tipo').value || 'VENTAS_DETALLE';
    const sucursalId = document.getElementById('bi-flt-sucursal').value || '0';
    const fechaDesde = document.getElementById('bi-flt-desde').value;
    const fechaHasta = document.getElementById('bi-flt-hasta').value;
    const filtroFacturacion = document.getElementById('bi-flt-facturacion').value || 'TODAS_REALIZADAS';

    const params = new URLSearchParams({
        tipoReporte,
        sucursalId,
        fechaDesde,
        fechaHasta,
        filtroFacturacion
    });

    window.location.href = `/api/v1/estadisticas/exportar-csv?${params.toString()}`;
}

function exportarBiInformePdf() {
    const d = AppState.dashboardBi;
    if (!d) return;
    const content = document.getElementById('bi-printable-area').innerHTML;
    const win = window.open('', '_blank', 'width=1024,height=768');
    if (!win) {
        showToast('Permita las ventanas emergentes para imprimir o guardar en PDF.');
        return;
    }
    win.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>Informe Estadístico de Negocio — VentasPost</title>
            <link rel="stylesheet" href="/css/styles.css">
            <style>
                body { background: #fff; padding: 20px; font-size: 12px; }
                .action-group { display: none !important; }
            </style>
        </head>
        <body onload="setTimeout(() => { window.print(); window.close(); }, 250);">
            <div style="border-bottom:2px solid #0f172a;padding-bottom:10px;margin-bottom:14px;display:flex;justify-content:space-between;align-items:center;">
                <div>
                    <h2 style="margin:0;">VentasPost — Informe Ejecutivo e Inteligencia de Negocio</h2>
                    <div>Alcance: <b>${escapeHtml(d.sucursalFiltroNombre)}</b> • Período: <b>${escapeHtml(d.fechaDesde)} al ${escapeHtml(d.fechaHasta)}</b></div>
                </div>
                <div style="text-align:right;font-size:11px;">
                    Filtro Fiscal: <b>${escapeHtml(d.filtroFacturacion)}</b>
                </div>
            </div>
            ${content}
        </body>
        </html>
    `);
    win.document.close();
}



/* =========================================================================
   FASE 6: FICHAJE DE ENTRADA/SALIDA, HORARIOS, ASISTENCIAS Y LIQUIDACIÓN DE SUELDOS
   ========================================================================= */

async function refrescarEstadoFichajeTopbar() {
    const btn = document.getElementById('btn-fichaje-topbar');
    if (!btn || !AppState.sesion) return;

    try {
        const res = await ApiClient.get('/api/v1/sueldos/fichaje/estado-actual');
        if (res.tieneJornadaEnCurso && res.jornada) {
            AppState.fichajeJornadaEnCurso = res.jornada;
            const horaIng = (res.jornada.fechaHoraIngresoReal || '').substring(11, 16);
            btn.innerHTML = `🔴 Fichar Salida (Ingreso ${escapeHtml(horaIng)})`;
            btn.className = 'btn btn-danger-outline btn-xs';
        } else {
            AppState.fichajeJornadaEnCurso = null;
            btn.innerHTML = '🕒 Fichar Entrada';
            btn.className = 'btn btn-outline btn-xs';
        }
    } catch (err) {
        // Ignorar silenciosamente si aún no hay sesión
    }
}

async function onToggleFichajeRelojTopbar() {
    try {
        if (AppState.fichajeJornadaEnCurso) {
            if (!confirm('¿Confirmar fichaje de SALIDA y cierre de su jornada laboral actual?')) return;
            const jornadaCerrada = await ApiClient.post('/api/v1/sueldos/fichaje/egreso', {});
            showToast(`Salida fichada. Total computado hoy: ${jornadaCerrada.totalHorasComputadas} hs.`);
        } else {
            const jornadaAbierta = await ApiClient.post('/api/v1/sueldos/fichaje/ingreso', {});
            showToast(`Entrada fichada correctamente en ${jornadaAbierta.sucursalNombre}.`);
        }
        await refrescarEstadoFichajeTopbar();
        if (AppState.seccionActiva === 'sueldos') {
            cargarListadoAsistencias();
        }
    } catch (err) {
        showToast(err.message);
    }
}

function cambiarSubTabSueldos(tab) {
    AppState.sueldosSubTabActiva = tab;
    const tabs = ['liquidacion', 'asistencias', 'horarios', 'tarifas'];
    tabs.forEach(t => {
        const panel = document.getElementById(`sueldos-panel-${t}`);
        if (panel) panel.classList.toggle('hidden', t !== tab);
    });

    document.querySelectorAll('.sueldos-tab-btn').forEach(btn => {
        const activo = btn.dataset.tab === tab;
        btn.className = activo ? 'btn btn-primary btn-sm sueldos-tab-btn' : 'btn btn-outline btn-sm sueldos-tab-btn';
    });

    if (tab === 'liquidacion') {
        cargarHistorialLiquidaciones();
    } else if (tab === 'asistencias') {
        cargarListadoAsistencias();
    } else if (tab === 'horarios') {
        cargarHorariosYAdelantos();
    } else if (tab === 'tarifas') {
        renderizarPanelTarifasSueldos();
    }
}

async function inicializarYCargarSeccionSueldos() {
    try {
        const [configTarifas, sucursales] = await Promise.all([
            ApiClient.get('/api/v1/sueldos/configuracion-tarifas'),
            ApiClient.get('/api/v1/sucursales')
        ]);

        AppState.sueldosConfigTarifas = configTarifas;
        AppState.sucursales = sucursales;

        poblarSelectoresEmpleadosYSucursalesSueldos();

        const hoyStr = new Date().toISOString().substring(0, 10);
        const hace30 = new Date();
        hace30.setDate(hace30.getDate() - 30);
        const hace30Str = hace30.toISOString().substring(0, 10);

        if (!document.getElementById('liq-fecha-hasta').value) {
            document.getElementById('liq-fecha-hasta').value = hoyStr;
        }
        if (!document.getElementById('flt-asist-desde').value) {
            document.getElementById('flt-asist-desde').value = hace30Str;
            document.getElementById('flt-asist-hasta').value = hoyStr;
        }
        if (!document.getElementById('adel-fecha').value) {
            document.getElementById('adel-fecha').value = hoyStr;
        }

        onCambioEmpleadoLiquidacion();
        cambiarSubTabSueldos(AppState.sueldosSubTabActiva || 'liquidacion');
    } catch (err) {
        showToast(err.message);
    }
}

function poblarSelectoresEmpleadosYSucursalesSueldos() {
    const cfg = AppState.sueldosConfigTarifas;
    if (!cfg) return;

    const empleados = cfg.empleados || [];
    const opcionesEmp = empleados.map(e =>
        `<option value="${e.usuarioId}">${escapeHtml(e.nombreCompleto)} (${escapeHtml(e.username)}) — $${formatearMoneda(e.valorHoraEfectivo)}/h</option>`
    ).join('');

    ['liq-usuario-select', 'hor-usuario', 'adel-usuario', 'asist-usuario'].forEach(id => {
        const sel = document.getElementById(id);
        if (sel) sel.innerHTML = opcionesEmp;
    });

    const opcionesFiltroEmp = `<option value="">Todos los empleados</option>` + empleados.map(e =>
        `<option value="${e.usuarioId}">${escapeHtml(e.nombreCompleto)} (${escapeHtml(e.username)})</option>`
    ).join('');

    ['flt-liq-hist-usuario', 'flt-asist-usuario'].forEach(id => {
        const sel = document.getElementById(id);
        if (sel) {
            const prev = sel.value;
            sel.innerHTML = opcionesFiltroEmp;
            if (prev) sel.value = prev;
        }
    });

    const sucursalesActivas = (AppState.sucursales || []).filter(s => s.activa);
    const opcionesSuc = sucursalesActivas.map(s =>
        `<option value="${s.id}">${escapeHtml(s.codigo)} — ${escapeHtml(s.nombre)}</option>`
    ).join('');

    ['liq-sucursal-select', 'hor-sucursal', 'adel-sucursal', 'asist-sucursal'].forEach(id => {
        const sel = document.getElementById(id);
        if (sel) {
            sel.innerHTML = opcionesSuc;
            if (AppState.sesion && AppState.sesion.sucursalActiva) {
                sel.value = String(AppState.sesion.sucursalActiva.id);
            }
        }
    });
}

function onCambioEmpleadoLiquidacion() {
    const cfg = AppState.sueldosConfigTarifas;
    if (!cfg) return;
    const empId = Number(document.getElementById('liq-usuario-select').value);
    const emp = (cfg.empleados || []).find(e => e.usuarioId === empId);
    if (emp && emp.fechaSugeridaDesde) {
        document.getElementById('liq-fecha-desde').value = emp.fechaSugeridaDesde;
    }
    document.getElementById('liq-preview-container').classList.add('hidden');
}

async function calcularPreliquidacionActual() {
    const usuarioId = document.getElementById('liq-usuario-select').value;
    const fechaDesde = document.getElementById('liq-fecha-desde').value;
    const fechaHasta = document.getElementById('liq-fecha-hasta').value;

    if (!usuarioId) {
        showToast('Seleccione un empleado para calcular su liquidación.');
        return;
    }

    try {
        const params = new URLSearchParams({ usuarioId });
        if (fechaDesde) params.set('fechaDesde', fechaDesde);
        if (fechaHasta) params.set('fechaHasta', fechaHasta);

        const pre = await ApiClient.get(`/api/v1/sueldos/preliquidacion?${params.toString()}`);
        AppState.sueldosPreliquidacionActual = pre;
        AppState.sueldosConceptosManuales = [];

        document.getElementById('liq-fecha-desde').value = pre.fechaDesdeSugerida;
        document.getElementById('liq-fecha-hasta').value = pre.fechaHasta;

        renderizarVistaPreliquidacion(pre);
    } catch (err) {
        showToast(err.message);
    }
}

function renderizarVistaPreliquidacion(pre) {
    document.getElementById('liq-preview-container').classList.remove('hidden');

    const kpiContainer = document.getElementById('liq-kpi-summary');
    kpiContainer.innerHTML = `
        <div class="bi-kpi-card">
            <div class="bi-kpi-title">Tarifa Base Aplicada</div>
            <div class="bi-kpi-value">$ ${formatearMoneda(pre.valorHoraBaseAplicado)} / h</div>
            <div class="bi-kpi-sub">${escapeHtml(pre.origenValorHora)}</div>
        </div>
        <div class="bi-kpi-card">
            <div class="bi-kpi-title">Total Horas del Período</div>
            <div class="bi-kpi-value">${pre.totalHorasTrabajadas} hs</div>
            <div class="bi-kpi-sub">Norm: ${pre.totalHorasNormales}h • Ext: ${pre.totalHorasExtras}h • Fer: ${pre.totalHorasFeriado}h</div>
        </div>
        <div class="bi-kpi-card">
            <div class="bi-kpi-title">Subtotal Haberes por Horas</div>
            <div class="bi-kpi-value" style="color:#16a34a;">$ ${formatearMoneda(pre.subtotalHaberesHoras)}</div>
            <div class="bi-kpi-sub">Incluye extras (+${pre.porcentajeExtraAplicado}%), feriados (+${pre.porcentajeFeriadoAplicado}%) y nocturnidad (+${pre.porcentajeNocturnoAplicado}%)</div>
        </div>
        <div class="bi-kpi-card">
            <div class="bi-kpi-title">Adelantos Pendientes</div>
            <div class="bi-kpi-value" style="color:#dc2626;">- $ ${formatearMoneda(pre.totalAdelantosPendientes)}</div>
            <div class="bi-kpi-sub">${(pre.adelantosPendientes || []).length} vale(s) sin descontar</div>
        </div>
    `;

    const jornadas = pre.jornadasIncluidas || [];
    document.getElementById('liq-jornadas-count').textContent = jornadas.length;
    const tbodyJ = document.getElementById('tbody-liq-pre-jornadas');
    if (!jornadas.length) {
        tbodyJ.innerHTML = `<tr><td colspan="7" class="text-center">No hay jornadas trabajadas pendientes de liquidar en este rango. Puede generarlas desde "Asistencias y Jornadas".</td></tr>`;
    } else {
        tbodyJ.innerHTML = jornadas.map(j => `
            <tr>
                <td><b>${escapeHtml(j.fechaJornada)}</b></td>
                <td><small>${escapeHtml(j.sucursalCodigo)}</small></td>
                <td>${j.horasNormales} h</td>
                <td>${j.horasExtrasSimples > 0 ? `<b style="color:#2563eb;">${j.horasExtrasSimples} h</b>` : '0 h'}</td>
                <td>${j.horasFeriadoDomingo > 0 ? `<b style="color:#9333ea;">${j.horasFeriadoDomingo} h</b>` : '0 h'}</td>
                <td>${j.horasNocturnas > 0 ? `<b style="color:#0891b2;">${j.horasNocturnas} h</b>` : '0 h'}</td>
                <td><strong>${j.totalHorasComputadas} h</strong></td>
            </tr>
        `).join('');
    }

    document.getElementById('liq-adelantos-pend-label').textContent = `$ ${formatearMoneda(pre.totalAdelantosPendientes)}`;
    renderizarConceptosManualesPreliquidacion();
    recalcularNetoFinalVistaPreliquidacion();
}

function agregarConceptoManualPreliquidacion() {
    const tipoConcepto = document.getElementById('liq-cm-tipo').value;
    const descripcion = document.getElementById('liq-cm-desc').value.trim();
    const monto = Number(document.getElementById('liq-cm-monto').value);

    if (!descripcion || !(monto > 0)) {
        showToast('Ingrese descripción y un monto mayor a $0 para el concepto manual.');
        return;
    }

    AppState.sueldosConceptosManuales.push({ tipoConcepto, descripcion, monto });
    document.getElementById('liq-cm-desc').value = '';
    document.getElementById('liq-cm-monto').value = '';
    renderizarConceptosManualesPreliquidacion();
    recalcularNetoFinalVistaPreliquidacion();
}

function quitarConceptoManualPreliquidacion(idx) {
    AppState.sueldosConceptosManuales.splice(idx, 1);
    renderizarConceptosManualesPreliquidacion();
    recalcularNetoFinalVistaPreliquidacion();
}

function renderizarConceptosManualesPreliquidacion() {
    const cont = document.getElementById('liq-conceptos-manuales-list');
    const lista = AppState.sueldosConceptosManuales || [];
    if (!lista.length) {
        cont.innerHTML = `<div style="font-size:0.8rem;color:#64748b;">Sin bonos ni descuentos manuales adicionales cargados.</div>`;
        return;
    }

    cont.innerHTML = lista.map((c, idx) => {
        const esHaber = c.tipoConcepto === 'HABER_ADICIONAL';
        return `
            <div style="display:flex;justify-content:space-between;align-items:center;padding:0.35rem 0.6rem;background:#fff;border:1px solid var(--border);border-radius:6px;margin-bottom:0.3rem;font-size:0.84rem;">
                <span>
                    <b style="color:${esHaber ? '#16a34a' : '#dc2626'};">${esHaber ? '+ Bono/Haber' : '- Descuento'}:</b>
                    ${escapeHtml(c.descripcion)}
                </span>
                <span style="display:flex;align-items:center;gap:0.5rem;">
                    <b>${esHaber ? '+' : '-'} $ ${formatearMoneda(c.monto)}</b>
                    <button type="button" class="btn btn-outline btn-xs" onclick="quitarConceptoManualPreliquidacion(${idx})">✕</button>
                </span>
            </div>
        `;
    }).join('');
}

function recalcularNetoFinalVistaPreliquidacion() {
    const pre = AppState.sueldosPreliquidacionActual;
    if (!pre) return;

    const descontarAdelantos = document.getElementById('liq-chk-descontar-adelantos').checked;
    let neto = Number(pre.subtotalHaberesHoras || 0);

    if (descontarAdelantos) {
        neto -= Number(pre.totalAdelantosPendientes || 0);
    }

    (AppState.sueldosConceptosManuales || []).forEach(c => {
        if (c.tipoConcepto === 'HABER_ADICIONAL') {
            neto += Number(c.monto);
        } else {
            neto -= Number(c.monto);
        }
    });

    document.getElementById('liq-neto-final-big').textContent = `$ ${formatearMoneda(neto)}`;
}

async function emitirYCerrarLiquidacionSueldo() {
    const pre = AppState.sueldosPreliquidacionActual;
    if (!pre) {
        showToast('Primero presione "Calcular Horas" para verificar la preliquidación.');
        return;
    }

    const sucursalId = Number(document.getElementById('liq-sucursal-select').value);
    const fechaDesde = document.getElementById('liq-fecha-desde').value;
    const fechaHasta = document.getElementById('liq-fecha-hasta').value;
    const medioPago = document.getElementById('liq-medio-pago').value;
    const observaciones = document.getElementById('liq-observaciones').value.trim();
    const descontarAdelantosPendientes = document.getElementById('liq-chk-descontar-adelantos').checked;

    if (!confirm(`¿Confirmar el cierre y pago de la liquidación de ${pre.nombreCompleto} del ${fechaDesde} al ${fechaHasta}? Las jornadas y adelantos incluidos quedarán marcados como liquidados.`)) {
        return;
    }

    try {
        const recibo = await ApiClient.post('/api/v1/sueldos/liquidaciones', {
            usuarioId: pre.usuarioId,
            sucursalId,
            fechaDesde,
            fechaHasta,
            fechaLiquidacion: fechaHasta,
            medioPago,
            descontarAdelantosPendientes,
            observaciones,
            conceptosManuales: AppState.sueldosConceptosManuales
        });

        showToast(`Liquidación ${recibo.numeroReciboInterno} emitida y pagada exitosamente.`);
        document.getElementById('liq-preview-container').classList.add('hidden');
        await inicializarYCargarSeccionSueldos();
        abrirModalReciboSueldo(recibo);
    } catch (err) {
        showToast(err.message);
    }
}

async function cargarHistorialLiquidaciones() {
    const tbody = document.getElementById('tbody-liquidaciones-hist');
    const usuarioId = document.getElementById('flt-liq-hist-usuario').value;
    try {
        const params = new URLSearchParams();
        if (usuarioId) params.set('usuarioId', usuarioId);
        const lista = await ApiClient.get(`/api/v1/sueldos/liquidaciones?${params.toString()}`);

        if (!lista.length) {
            tbody.innerHTML = `<tr><td colspan="10" class="text-center">No hay liquidaciones registradas aún.</td></tr>`;
            return;
        }

        tbody.innerHTML = lista.map(l => {
            const haberesMasBonos = Number(l.subtotalHaberesHoras || 0) + Number(l.totalBonosAdicionales || 0);
            const descTotales = Number(l.totalAdelantosDescontados || 0) + Number(l.totalOtrosDescuentos || 0);
            return `
                <tr>
                    <td><strong>${escapeHtml(l.numeroReciboInterno)}</strong></td>
                    <td>${escapeHtml(l.fechaLiquidacion)}</td>
                    <td>
                        <strong>${escapeHtml(l.nombreCompleto)}</strong>
                        <div style="font-size:0.74rem;color:#64748b;">Legajo: ${escapeHtml(l.legajo || '-')} • ${escapeHtml(l.sucursalCodigo)}</div>
                    </td>
                    <td>${escapeHtml(l.fechaDesde)} al ${escapeHtml(l.fechaHasta)}</td>
                    <td><b>${l.totalHorasTrabajadas} hs</b></td>
                    <td>$ ${formatearMoneda(l.valorHoraBaseAplicado)}/h</td>
                    <td style="color:#16a34a;font-weight:600;">$ ${formatearMoneda(haberesMasBonos)}</td>
                    <td style="color:#dc2626;">- $ ${formatearMoneda(descTotales)}</td>
                    <td><strong style="font-size:0.95rem;">$ ${formatearMoneda(l.netoACobrar)}</strong> <span class="pv-pill">${escapeHtml(l.medioPago)}</span></td>
                    <td>
                        <button type="button" class="btn btn-outline btn-xs" onclick="verReciboLiquidacionPorId(${l.id})">🖨️ Recibo Duplicado</button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="10" class="text-center">No hay liquidaciones registradas.</td></tr>`;
    }
}

async function verReciboLiquidacionPorId(id) {
    try {
        const recibo = await ApiClient.get(`/api/v1/sueldos/liquidaciones/${id}`);
        abrirModalReciboSueldo(recibo);
    } catch (err) {
        showToast(err.message);
    }
}

function abrirModalReciboSueldo(recibo) {
    AppState.sueldosLiquidacionReciboActual = recibo;
    const area = document.getElementById('recibo-sueldo-print-area');

    const renderizarEjemplar = (tituloEjemplar) => {
        const conceptosRows = (recibo.conceptos || []).map(c => {
            const esHaber = c.tipoConcepto === 'HABER_ADICIONAL';
            return `
                <tr>
                    <td>${escapeHtml(c.descripcion)} <small style="color:#64748b;">(${escapeHtml(c.tipoConceptoDescripcion)})</small></td>
                    <td style="text-align:center;">-</td>
                    <td style="text-align:right;color:#16a34a;">${esHaber ? '$ ' + formatearMoneda(c.monto) : ''}</td>
                    <td style="text-align:right;color:#dc2626;">${!esHaber ? '- $ ' + formatearMoneda(c.monto) : ''}</td>
                </tr>
            `;
        }).join('');

        return `
            <div style="border:2px solid #0f172a;border-radius:8px;padding:14px;margin-bottom:16px;background:#fff;font-size:12px;">
                <div style="display:flex;justify-content:space-between;border-bottom:1px solid #cbd5e1;padding-bottom:8px;margin-bottom:8px;">
                    <div>
                        <strong style="font-size:15px;">${escapeHtml(recibo.sucursalRazonSocial)}</strong>
                        <div>Sucursal: ${escapeHtml(recibo.sucursalCodigo)} - ${escapeHtml(recibo.sucursalNombre)} • CUIT: ${escapeHtml(recibo.sucursalCuit)}</div>
                        <div>Domicilio: ${escapeHtml(recibo.sucursalDireccion)}</div>
                    </div>
                    <div style="text-align:right;">
                        <span style="background:#0f172a;color:#fff;padding:3px 8px;border-radius:4px;font-weight:700;font-size:11px;">${tituloEjemplar}</span>
                        <div style="margin-top:4px;font-weight:700;">Recibo N°: ${escapeHtml(recibo.numeroReciboInterno)}</div>
                        <div>Fecha Liquidación: ${escapeHtml(recibo.fechaLiquidacion)}</div>
                    </div>
                </div>

                <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:8px;background:#f8fafc;padding:8px;border-radius:6px;margin-bottom:10px;">
                    <div><small style="color:#64748b;">Empleado:</small><br><b>${escapeHtml(recibo.nombreCompleto)}</b></div>
                    <div><small style="color:#64748b;">DNI / Legajo:</small><br><b>${escapeHtml(recibo.dni || '-')} / ${escapeHtml(recibo.legajo || '-')}</b></div>
                    <div><small style="color:#64748b;">Período Liquidado:</small><br><b>${escapeHtml(recibo.fechaDesde)} al ${escapeHtml(recibo.fechaHasta)}</b></div>
                    <div><small style="color:#64748b;">Valor Hora Base:</small><br><b>$ ${formatearMoneda(recibo.valorHoraBaseAplicado)}/h</b></div>
                </div>

                <table class="data-table compact-table" style="width:100%;font-size:11.5px;">
                    <thead>
                        <tr>
                            <th>Concepto Liquidado</th>
                            <th style="text-align:center;">Cantidad / Horas</th>
                            <th style="text-align:right;">Haberes ($)</th>
                            <th style="text-align:right;">Deducciones / Adelantos ($)</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>Horas Normales Trabajadas ($${formatearMoneda(recibo.valorHoraBaseAplicado)}/h)</td>
                            <td style="text-align:center;">${recibo.totalHorasNormales} hs</td>
                            <td style="text-align:right;">$ ${formatearMoneda(recibo.importeHorasNormales)}</td>
                            <td style="text-align:right;"></td>
                        </tr>
                        ${recibo.totalHorasExtras > 0 ? `
                        <tr>
                            <td>Horas Extras Simples (+${recibo.porcentajeExtraAplicado}%)</td>
                            <td style="text-align:center;">${recibo.totalHorasExtras} hs</td>
                            <td style="text-align:right;">$ ${formatearMoneda(recibo.importeHorasExtras)}</td>
                            <td style="text-align:right;"></td>
                        </tr>` : ''}
                        ${recibo.totalHorasFeriado > 0 ? `
                        <tr>
                            <td>Horas Feriado / Domingo (+${recibo.porcentajeFeriadoAplicado}%)</td>
                            <td style="text-align:center;">${recibo.totalHorasFeriado} hs</td>
                            <td style="text-align:right;">$ ${formatearMoneda(recibo.importeHorasFeriado)}</td>
                            <td style="text-align:right;"></td>
                        </tr>` : ''}
                        ${recibo.totalHorasNocturnas > 0 ? `
                        <tr>
                            <td>Adicional Hora Nocturna 21:00 a 06:00 (+${recibo.porcentajeNocturnoAplicado}%)</td>
                            <td style="text-align:center;">${recibo.totalHorasNocturnas} hs</td>
                            <td style="text-align:right;">$ ${formatearMoneda(recibo.importeAdicionalNocturno)}</td>
                            <td style="text-align:right;"></td>
                        </tr>` : ''}
                        ${conceptosRows}
                    </tbody>
                </table>

                <div style="display:flex;justify-content:space-between;align-items:end;margin-top:12px;padding-top:10px;border-top:1px solid #cbd5e1;">
                    <div style="font-size:11px;color:#475569;">
                        Medio de Pago: <b>${escapeHtml(recibo.medioPago)}</b> • Total Horas Computadas: <b>${recibo.totalHorasTrabajadas} hs</b>
                        ${recibo.observaciones ? `<br>Obs: ${escapeHtml(recibo.observaciones)}` : ''}
                    </div>
                    <div style="text-align:center;min-width:190px;border-top:1px dotted #334155;padding-top:4px;font-size:11px;">
                        Firma de Conformidad (${tituloEjemplar.includes('EMPLEADO') ? 'Empleador' : 'Empleado'})
                    </div>
                    <div style="background:#0f172a;color:#fff;padding:6px 12px;border-radius:6px;text-align:right;">
                        <div style="font-size:10px;color:#cbd5e1;">NETO COBRADO</div>
                        <div style="font-size:15px;font-weight:800;">$ ${formatearMoneda(recibo.netoACobrar)}</div>
                    </div>
                </div>
            </div>
        `;
    };

    area.innerHTML = renderizarEjemplar('ORIGINAL — ARCHIVO COMERCIO') + renderizarEjemplar('DUPLICADO — RECIBO DEL EMPLEADO');
    document.getElementById('modal-recibo-sueldo').classList.remove('hidden');
}

function cerrarModalReciboSueldo() {
    document.getElementById('modal-recibo-sueldo').classList.add('hidden');
}

function imprimirReciboSueldoActual() {
    const content = document.getElementById('recibo-sueldo-print-area').innerHTML;
    const win = window.open('', '_blank', 'width=900,height=740');
    if (!win) {
        showToast('Habilite las ventanas emergentes para imprimir el recibo.');
        return;
    }
    win.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>Recibo de Liquidación de Sueldo — VentasPost</title>
            <link rel="stylesheet" href="/css/styles.css">
            <style>body { background:#fff; padding:15px; }</style>
        </head>
        <body onload="setTimeout(() => { window.print(); window.close(); }, 200);">
            ${content}
        </body>
        </html>
    `);
    win.document.close();
}

/* =========================================================================
   SUB-TAB 2: ASISTENCIAS Y JORNADAS TRABAJADAS
   ========================================================================= */
async function cargarListadoAsistencias() {
    const tbody = document.getElementById('tbody-asistencias');
    const usuarioId = document.getElementById('flt-asist-usuario').value;
    const desde = document.getElementById('flt-asist-desde').value;
    const hasta = document.getElementById('flt-asist-hasta').value;

    try {
        const params = new URLSearchParams();
        if (usuarioId) params.set('usuarioId', usuarioId);
        if (desde) params.set('desde', desde);
        if (hasta) params.set('hasta', hasta);

        const lista = await ApiClient.get(`/api/v1/sueldos/asistencias?${params.toString()}`);
        AppState.sueldosAsistenciasCache = lista;

        if (!lista.length) {
            tbody.innerHTML = `<tr><td colspan="12" class="text-center">No hay jornadas registradas en el período. Puede presionar "Generar Jornadas desde Horarios Asignados".</td></tr>`;
            return;
        }

        tbody.innerHTML = lista.map(a => {
            const horarioStr = a.horaEntradaProgramada && a.horaSalidaProgramada
                ? `${String(a.horaEntradaProgramada).substring(0,5)} a ${String(a.horaSalidaProgramada).substring(0,5)}`
                : '-';
            return `
                <tr>
                    <td><strong>${escapeHtml(a.fechaJornada)}</strong> ${a.esFeriadoODomingo ? '<span class="pv-pill" style="background:#f3e8ff;color:#7e22ce;">Dom/Fer</span>' : ''}</td>
                    <td><strong>${escapeHtml(a.nombreCompleto)}</strong></td>
                    <td><small>${escapeHtml(a.sucursalCodigo)}</small></td>
                    <td>
                        <span class="status-pill ${a.estadoJornada === 'TRABAJADA' ? 'status-active' : 'status-inactive'}">${escapeHtml(a.estadoJornadaDescripcion)}</span>
                        <div style="font-size:0.7rem;color:#64748b;margin-top:2px;">${escapeHtml(a.origenRegistroDescripcion)}</div>
                    </td>
                    <td><small>${escapeHtml(horarioStr)}</small></td>
                    <td>${a.horasNormales} h</td>
                    <td>${a.horasExtrasSimples > 0 ? `<b style="color:#2563eb;">${a.horasExtrasSimples} h</b>` : '0 h'}</td>
                    <td>${a.horasFeriadoDomingo > 0 ? `<b style="color:#9333ea;">${a.horasFeriadoDomingo} h</b>` : '0 h'}</td>
                    <td>${a.horasNocturnas > 0 ? `<b style="color:#0891b2;">${a.horasNocturnas} h</b>` : '0 h'}</td>
                    <td><strong>${a.totalHorasComputadas} h</strong></td>
                    <td>
                        ${a.liquidada
                            ? '<span class="status-pill status-active">Liquidada</span>'
                            : '<span class="status-pill status-inactive">Pendiente</span>'}
                    </td>
                    <td>
                        ${!a.liquidada ? `
                            <div class="action-group">
                                <button type="button" class="btn btn-outline btn-xs" onclick="editarAsistenciaPorId(${a.id})">✏️</button>
                                <button type="button" class="btn btn-danger-outline btn-xs" onclick="eliminarAsistenciaPorId(${a.id})">🗑️</button>
                            </div>
                        ` : '<small style="color:#94a3b8;">Cerrada</small>'}
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="12" class="text-center">No hay jornadas registradas.</td></tr>`;
    }
}

async function generarJornadasDesdeHorariosTeoricosClick() {
    const usuarioId = document.getElementById('flt-asist-usuario').value || null;
    const fechaDesde = document.getElementById('flt-asist-desde').value;
    const fechaHasta = document.getElementById('flt-asist-hasta').value;

    if (!fechaDesde || !fechaHasta) {
        showToast('Seleccione el rango de fechas (Desde / Hasta) para generar las jornadas.');
        return;
    }

    try {
        const res = await ApiClient.post('/api/v1/sueldos/asistencias/generar-desde-horarios', {
            usuarioId: usuarioId ? Number(usuarioId) : null,
            fechaDesde,
            fechaHasta
        });
        showToast(res.mensaje);
        cargarListadoAsistencias();
    } catch (err) {
        showToast(err.message);
    }
}

function abrirModalAsistenciaManual(jornada) {
    document.getElementById('modal-asist-error').classList.add('hidden');
    if (jornada) {
        document.getElementById('modal-asist-title').textContent = '✏️ Editar / Validar Jornada de Trabajo';
        document.getElementById('asist-id').value = jornada.id;
        document.getElementById('asist-usuario').value = String(jornada.usuarioId);
        document.getElementById('asist-sucursal').value = String(jornada.sucursalId);
        document.getElementById('asist-fecha').value = jornada.fechaJornada;
        document.getElementById('asist-estado').value = jornada.estadoJornada;
        document.getElementById('asist-hs-normales').value = jornada.horasNormales;
        document.getElementById('asist-hs-extras').value = jornada.horasExtrasSimples;
        document.getElementById('asist-hs-feriado').value = jornada.horasFeriadoDomingo;
        document.getElementById('asist-hs-nocturnas').value = jornada.horasNocturnas;
        document.getElementById('asist-observaciones').value = jornada.observaciones || '';
    } else {
        document.getElementById('modal-asist-title').textContent = '📅 Registrar Jornada / Inasistencia Manual';
        document.getElementById('asist-id').value = '';
        document.getElementById('asist-fecha').value = new Date().toISOString().substring(0, 10);
        document.getElementById('asist-estado').value = 'TRABAJADA';
        document.getElementById('asist-hs-normales').value = '8.00';
        document.getElementById('asist-hs-extras').value = '0.00';
        document.getElementById('asist-hs-feriado').value = '0.00';
        document.getElementById('asist-hs-nocturnas').value = '0.00';
        document.getElementById('asist-observaciones').value = '';
    }
    document.getElementById('modal-asistencia-manual').classList.remove('hidden');
}

function editarAsistenciaPorId(id) {
    const j = (AppState.sueldosAsistenciasCache || []).find(x => x.id === id);
    if (j) abrirModalAsistenciaManual(j);
}

function cerrarModalAsistenciaManual() {
    document.getElementById('modal-asistencia-manual').classList.add('hidden');
}

async function onGuardarAsistenciaManual(e) {
    e.preventDefault();
    const errEl = document.getElementById('modal-asist-error');
    errEl.classList.add('hidden');

    const id = document.getElementById('asist-id').value;
    const payload = {
        usuarioId: Number(document.getElementById('asist-usuario').value),
        sucursalId: Number(document.getElementById('asist-sucursal').value),
        fechaJornada: document.getElementById('asist-fecha').value,
        estadoJornada: document.getElementById('asist-estado').value,
        esFeriadoODomingo: Number(document.getElementById('asist-hs-feriado').value) > 0,
        horasNormales: Number(document.getElementById('asist-hs-normales').value || 0),
        horasExtrasSimples: Number(document.getElementById('asist-hs-extras').value || 0),
        horasFeriadoDomingo: Number(document.getElementById('asist-hs-feriado').value || 0),
        horasNocturnas: Number(document.getElementById('asist-hs-nocturnas').value || 0),
        observaciones: document.getElementById('asist-observaciones').value.trim()
    };

    try {
        if (id) {
            await ApiClient.put(`/api/v1/sueldos/asistencias/${id}`, payload);
            showToast('Jornada actualizada correctamente');
        } else {
            await ApiClient.post('/api/v1/sueldos/asistencias', payload);
            showToast('Jornada registrada correctamente');
        }
        cerrarModalAsistenciaManual();
        cargarListadoAsistencias();
    } catch (err) {
        errEl.textContent = err.message;
        errEl.classList.remove('hidden');
    }
}

async function eliminarAsistenciaPorId(id) {
    if (!confirm('¿Eliminar este registro de jornada?')) return;
    try {
        await ApiClient.delete(`/api/v1/sueldos/asistencias/${id}`);
        showToast('Jornada eliminada');
        cargarListadoAsistencias();
    } catch (err) {
        showToast(err.message);
    }
}

/* =========================================================================
   SUB-TAB 3: HORARIOS SEMANALES ASIGNADOS Y ADELANTOS DE SUELDO
   ========================================================================= */
async function cargarHorariosYAdelantos() {
    try {
        const [horarios, adelantos] = await Promise.all([
            ApiClient.get('/api/v1/sueldos/horarios'),
            ApiClient.get('/api/v1/sueldos/adelantos')
        ]);

        const tbodyH = document.getElementById('tbody-horarios-asignados');
        if (!horarios.length) {
            tbodyH.innerHTML = `<tr><td colspan="6" class="text-center">Sin horarios asignados.</td></tr>`;
        } else {
            tbodyH.innerHTML = horarios.map(h => `
                <tr>
                    <td><strong>${escapeHtml(h.nombreCompleto)}</strong></td>
                    <td><span class="pv-pill">${escapeHtml(h.diaSemanaNombre)}</span></td>
                    <td><small>${escapeHtml(h.sucursalCodigo)}</small></td>
                    <td><b>${String(h.horaEntrada).substring(0,5)} a ${String(h.horaSalida).substring(0,5)}</b></td>
                    <td>${h.horasTeoricas} hs</td>
                    <td>
                        <button type="button" class="btn btn-danger-outline btn-xs" onclick="eliminarHorarioAsignadoPorId(${h.id})">🗑️</button>
                    </td>
                </tr>
            `).join('');
        }

        const tbodyA = document.getElementById('tbody-adelantos-sueldo');
        if (!adelantos.length) {
            tbodyA.innerHTML = `<tr><td colspan="6" class="text-center">Sin adelantos registrados.</td></tr>`;
        } else {
            tbodyA.innerHTML = adelantos.map(ad => `
                <tr>
                    <td>${escapeHtml(ad.fechaAdelanto)}</td>
                    <td><strong>${escapeHtml(ad.nombreCompleto)}</strong></td>
                    <td><small>${escapeHtml(ad.motivo)} (${escapeHtml(ad.medioPago)})</small></td>
                    <td><strong style="color:#dc2626;">$ ${formatearMoneda(ad.monto)}</strong></td>
                    <td>
                        ${ad.descontado
                            ? '<span class="status-pill status-active">Descontado</span>'
                            : '<span class="status-pill status-inactive">Pendiente</span>'}
                    </td>
                    <td>
                        ${!ad.descontado ? `<button type="button" class="btn btn-danger-outline btn-xs" onclick="eliminarAdelantoPorId(${ad.id})">🗑️</button>` : ''}
                    </td>
                </tr>
            `).join('');
        }
    } catch (err) {
        const tbodyH = document.getElementById('tbody-horarios-asignados');
        const tbodyA = document.getElementById('tbody-adelantos-sueldo');
        if (tbodyH) tbodyH.innerHTML = `<tr><td colspan="6" class="text-center">Sin horarios asignados.</td></tr>`;
        if (tbodyA) tbodyA.innerHTML = `<tr><td colspan="6" class="text-center">Sin adelantos registrados.</td></tr>`;
        showToast(err.message);
    }
}

async function onCrearHorarioAsignado(e) {
    e.preventDefault();
    try {
        await ApiClient.post('/api/v1/sueldos/horarios', {
            usuarioId: Number(document.getElementById('hor-usuario').value),
            sucursalId: Number(document.getElementById('hor-sucursal').value),
            diaSemana: Number(document.getElementById('hor-dia').value),
            horaEntrada: document.getElementById('hor-entrada').value,
            horaSalida: document.getElementById('hor-salida').value,
            observaciones: ''
        });
        showToast('Turno semanal asignado al empleado');
        cargarHorariosYAdelantos();
    } catch (err) {
        showToast(err.message);
    }
}

async function eliminarHorarioAsignadoPorId(id) {
    try {
        await ApiClient.delete(`/api/v1/sueldos/horarios/${id}`);
        showToast('Horario eliminado');
        cargarHorariosYAdelantos();
    } catch (err) {
        showToast(err.message);
    }
}

async function onRegistrarAdelantoSueldo(e) {
    e.preventDefault();
    try {
        await ApiClient.post('/api/v1/sueldos/adelantos', {
            usuarioId: Number(document.getElementById('adel-usuario').value),
            sucursalId: Number(document.getElementById('adel-sucursal').value),
            fechaAdelanto: document.getElementById('adel-fecha').value,
            monto: Number(document.getElementById('adel-monto').value),
            medioPago: document.getElementById('adel-medio').value,
            motivo: document.getElementById('adel-motivo').value.trim()
        });
        document.getElementById('adel-monto').value = '';
        document.getElementById('adel-motivo').value = '';
        showToast('Adelanto de sueldo registrado. Se descontará automáticamente al liquidar.');
        cargarHorariosYAdelantos();
    } catch (err) {
        showToast(err.message);
    }
}

async function eliminarAdelantoPorId(id) {
    if (!confirm('¿Eliminar este adelanto pendiente?')) return;
    try {
        await ApiClient.delete(`/api/v1/sueldos/adelantos/${id}`);
        showToast('Adelanto eliminado');
        cargarHorariosYAdelantos();
    } catch (err) {
        showToast(err.message);
    }
}

/* =========================================================================
   SUB-TAB 4: TARIFAS POR HORA (POR ROL Y PERSONALIZADA) Y % RECARGOS
   ========================================================================= */
function renderizarPanelTarifasSueldos() {
    const cfg = AppState.sueldosConfigTarifas;
    if (!cfg) return;

    document.getElementById('cfg-recargo-extra').value = cfg.porcentajeRecargoExtraSimple;
    document.getElementById('cfg-recargo-feriado').value = cfg.porcentajeRecargoFeriadoDomingo;
    document.getElementById('cfg-recargo-nocturno').value = cfg.porcentajeRecargoNocturno;

    const rolesCont = document.getElementById('tarifas-roles-container');
    rolesCont.innerHTML = (cfg.roles || []).map(r => `
        <div class="form-group" style="margin-bottom:0.65rem;">
            <label>${escapeHtml(r.nombre)} (<code>${escapeHtml(r.codigo)}</code>) — Valor Hora General ($)</label>
            <input type="number" class="input-tarifa-rol" data-rol-codigo="${escapeHtml(r.codigo)}"
                   min="0" step="0.01" value="${r.valorHoraGeneral}" required>
        </div>
    `).join('');

    const tbodyEmp = document.getElementById('tbody-tarifas-empleados');
    tbodyEmp.innerHTML = (cfg.empleados || []).map(e => `
        <tr>
            <td>
                <strong>${escapeHtml(e.nombreCompleto)}</strong>
                <div style="font-size:0.74rem;color:#64748b;">@${escapeHtml(e.username)} • Leg: ${escapeHtml(e.legajo || '-')}</div>
            </td>
            <td><small>${escapeHtml((e.roles || []).join(', '))}</small></td>
            <td>
                <input type="number" class="input-tarifa-empleado" data-usuario-id="${e.usuarioId}"
                       min="0" step="0.01" placeholder="Usar valor del rol"
                       value="${e.valorHoraPersonalizado != null ? e.valorHoraPersonalizado : ''}"
                       style="width:140px;padding:0.35rem;border:1px solid var(--border);border-radius:6px;">
            </td>
            <td>
                <strong>$ ${formatearMoneda(e.valorHoraEfectivo)} / h</strong>
                <div style="font-size:0.72rem;color:#64748b;">${escapeHtml(e.origenValorHora)}</div>
            </td>
        </tr>
    `).join('');
}

async function onGuardarConfiguracionTarifasSueldos(e) {
    e.preventDefault();

    const tarifasPorRol = Array.from(document.querySelectorAll('.input-tarifa-rol')).map(inp => ({
        codigoRol: inp.dataset.rolCodigo,
        valorHoraGeneral: Number(inp.value || 0)
    }));

    const tarifasPersonalizadasEmpleados = Array.from(document.querySelectorAll('.input-tarifa-empleado')).map(inp => ({
        usuarioId: Number(inp.dataset.usuarioId),
        valorHoraPersonalizado: inp.value.trim() !== '' && Number(inp.value) > 0 ? Number(inp.value) : null
    }));

    try {
        const actualizada = await ApiClient.put('/api/v1/sueldos/configuracion-tarifas', {
            porcentajeRecargoExtraSimple: Number(document.getElementById('cfg-recargo-extra').value),
            porcentajeRecargoFeriadoDomingo: Number(document.getElementById('cfg-recargo-feriado').value),
            porcentajeRecargoNocturno: Number(document.getElementById('cfg-recargo-nocturno').value),
            horaInicioNocturno: '21:00',
            horaFinNocturno: '06:00',
            tarifasPorRol,
            tarifasPersonalizadasEmpleados
        });

        AppState.sueldosConfigTarifas = actualizada;
        poblarSelectoresEmpleadosYSucursalesSueldos();
        renderizarPanelTarifasSueldos();
        showToast('Tarifas por hora y porcentajes de recargo actualizados correctamente');
    } catch (err) {
        showToast(err.message);
    }
}
