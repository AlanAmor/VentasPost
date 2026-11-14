const AppState = {
    sesion: null,
    sucursales: [],
    usuarios: [],
    catalogoRolesPermisos: null,
    seccionActiva: 'sucursales'
};

document.addEventListener('DOMContentLoaded', () => {
    bindEvents();
    verificarSesionInicial();
});

function bindEvents() {
    // Login & Logout
    document.getElementById('form-login').addEventListener('submit', onLoginSubmit);
    document.getElementById('btn-logout').addEventListener('click', onLogout);
    document.getElementById('btn-logout-from-selector').addEventListener('click', onLogout);

    // Cambio de contraseña propia / obligatoria
    document.getElementById('form-change-password').addEventListener('submit', onChangePasswordSubmit);
    document.getElementById('btn-cancel-cp').addEventListener('click', onCancelChangePassword);
    document.getElementById('btn-open-self-password').addEventListener('click', abrirVistaCambioPasswordVoluntario);

    // Selector de sucursal / contexto
    document.getElementById('btn-switch-branch').addEventListener('click', mostrarSelectorSucursal);

    // Navegación lateral
    document.getElementById('nav-btn-sucursales').addEventListener('click', () => cambiarSeccion('sucursales'));
    document.getElementById('nav-btn-usuarios').addEventListener('click', () => cambiarSeccion('usuarios'));

    // Modal Sucursales
    document.getElementById('btn-nueva-sucursal').addEventListener('click', () => abrirModalSucursal(null));
    document.getElementById('btn-close-modal').addEventListener('click', cerrarModalSucursal);
    document.getElementById('btn-cancel-modal').addEventListener('click', cerrarModalSucursal);
    document.getElementById('form-sucursal').addEventListener('submit', onGuardarSucursal);

    // Modal Usuarios
    document.getElementById('btn-nuevo-usuario').addEventListener('click', () => abrirModalUsuario(null));
    document.getElementById('btn-close-usuario-modal').addEventListener('click', cerrarModalUsuario);
    document.getElementById('btn-cancel-usuario-modal').addEventListener('click', cerrarModalUsuario);
    document.getElementById('form-usuario').addEventListener('submit', onGuardarUsuario);
    document.getElementById('usr-acceso-todas').addEventListener('change', onToggleAccesoTodasSucursales);

    // Modal Blanqueo Password
    document.getElementById('btn-close-reset-modal').addEventListener('click', cerrarModalResetPass);
    document.getElementById('btn-cancel-reset-modal').addEventListener('click', cerrarModalResetPass);
    document.getElementById('form-reset-pass').addEventListener('submit', onConfirmarResetPass);

    // Modal Autorización de Supervisor (Regla 1.2)
    document.getElementById('btn-test-supervisor').addEventListener('click', abrirModalSupervisor);
    document.getElementById('btn-close-sup-modal').addEventListener('click', cerrarModalSupervisor);
    document.getElementById('btn-cancel-sup-modal').addEventListener('click', cerrarModalSupervisor);
    document.getElementById('form-supervisor').addEventListener('submit', onAutorizarSupervisorSubmit);
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

    // Regla 4.2: Si requiere cambio de contraseña inicial o tras blanqueo
    if (sesion.requiereCambioPassword) {
        document.getElementById('form-change-password').reset();
        document.getElementById('cp-error').classList.add('hidden');
        mostrarVista('view-change-password');
        return;
    }

    // Reglas 2B y 2.1A: Selección de sucursal o Modo Consolidado
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

    // Regla 2.1 A: Opción de Vista Consolidada para Administrador
    if (sesion.puedeVerConsolidado) {
        const cardConsolidado = document.createElement('div');
        cardConsolidado.className = 'branch-card consolidated';
        cardConsolidado.innerHTML = `
            <strong>🌐 Todas las Sucursales (Vista Consolidada)</strong>
            <div style="margin-top:0.35rem;font-size:0.85rem;color:#1e40af;">
                Visualización unificada de sucursales, empleados y reportes multi-sucursal.
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
    if (sesion.modoConsolidado) {
        document.getElementById('ui-active-branch-name').textContent = '🌐 Todas las Sucursales (Consolidado)';
        pvEl.textContent = 'GLOBAL';
    } else if (sesion.sucursalActiva) {
        document.getElementById('ui-active-branch-name').textContent =
            `${sesion.sucursalActiva.codigo} - ${sesion.sucursalActiva.nombre}`;
        pvEl.textContent = `PV ${String(sesion.sucursalActiva.puntoVentaArca).padStart(5, '0')}`;
    }

    const btnSwitch = document.getElementById('btn-switch-branch');
    if ((sesion.sucursalesDisponibles || []).length > 1 || sesion.puedeVerConsolidado) {
        btnSwitch.classList.remove('hidden');
    } else {
        btnSwitch.classList.add('hidden');
    }

    const esAdmin = (sesion.roles || []).includes('ADMIN');
    const puedeGestionarUsuarios = esAdmin
        || (sesion.roles || []).includes('ENCARGADO_SUCURSAL')
        || (sesion.permisos || []).includes('GESTIONAR_EMPLEADOS_SUCURSAL');

    document.getElementById('btn-nueva-sucursal').classList.toggle('hidden', !esAdmin);
    document.getElementById('nav-btn-usuarios').classList.toggle('hidden', !puedeGestionarUsuarios);
}

function cambiarSeccion(seccion) {
    AppState.seccionActiva = seccion;
    document.getElementById('nav-btn-sucursales').classList.toggle('active', seccion === 'sucursales');
    document.getElementById('nav-btn-usuarios').classList.toggle('active', seccion === 'usuarios');

    document.getElementById('section-sucursales').classList.toggle('hidden', seccion !== 'sucursales');
    document.getElementById('section-usuarios').classList.toggle('hidden', seccion !== 'usuarios');

    cargarDatosSeccionActiva();
}

function cargarDatosSeccionActiva() {
    if (AppState.seccionActiva === 'usuarios') {
        cargarUsuarios();
    } else {
        cargarSucursales();
    }
}

/* =========================================================================
   SUCURSALES
   ========================================================================= */
async function cargarSucursales() {
    try {
        const lista = await ApiClient.get('/api/v1/sucursales');
        AppState.sucursales = lista;
        renderizarTablaSucursales(lista);
    } catch (err) {
        manejarErrorApi(err);
    }
}

function renderizarTablaSucursales(lista) {
    const tbody = document.getElementById('tbody-sucursales');
    const esAdmin = AppState.sesion && (AppState.sesion.roles || []).includes('ADMIN');

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
                ${esAdmin ? `
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
   USUARIOS, ROLES Y PERMISOS GRANULARES
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

    const esAdmin = AppState.sesion && (AppState.sesion.roles || []).includes('ADMIN');
    document.getElementById('wrap-usr-all-branches').classList.toggle('hidden', !esAdmin);

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

    // 1. Roles
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

    // 2. Sucursales disponibles para asignar
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

    // 3. Permisos Granulares (Regla 1.1 B)
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

function abrirModalSupervisor() {
    document.getElementById('form-supervisor').reset();
    document.getElementById('modal-sup-error').classList.add('hidden');
    document.getElementById('modal-supervisor').classList.remove('hidden');
}

function cerrarModalSupervisor() {
    document.getElementById('modal-supervisor').classList.add('hidden');
}

async function onAutorizarSupervisorSubmit(e) {
    e.preventDefault();
    const errorEl = document.getElementById('modal-sup-error');
    errorEl.classList.add('hidden');

    const payload = {
        permisoRequerido: document.getElementById('sup-permiso').value,
        usernameSupervisor: document.getElementById('sup-username').value.trim(),
        passwordSupervisor: document.getElementById('sup-password').value
    };

    try {
        const res = await ApiClient.post('/api/v1/auth/autorizar-supervisor', payload);
        cerrarModalSupervisor();
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

function showToast(msg) {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.textContent = msg;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 3500);
}

function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}
