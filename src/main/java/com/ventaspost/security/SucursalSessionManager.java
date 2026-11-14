package com.ventaspost.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SucursalSessionManager {

    public static final String ATTR_SUCURSAL_ACTIVA_ID = "SUCURSAL_ACTIVA_ID";
    public static final String ATTR_MODO_CONSOLIDADO = "MODO_CONSOLIDADO_ACTIVO";

    public void fijarSucursalActiva(HttpSession session, Long sucursalId) {
        if (session != null) {
            session.setAttribute(ATTR_SUCURSAL_ACTIVA_ID, sucursalId);
            session.setAttribute(ATTR_MODO_CONSOLIDADO, Boolean.FALSE);
        }
    }

    public void activarModoConsolidado(HttpSession session) {
        if (session != null) {
            session.removeAttribute(ATTR_SUCURSAL_ACTIVA_ID);
            session.setAttribute(ATTR_MODO_CONSOLIDADO, Boolean.TRUE);
        }
    }

    public Optional<Long> obtenerSucursalActivaId(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }
        Object val = session.getAttribute(ATTR_SUCURSAL_ACTIVA_ID);
        if (val instanceof Long id) {
            return Optional.of(id);
        }
        return Optional.empty();
    }

    public boolean esModoConsolidado(HttpSession session) {
        if (session == null) {
            return false;
        }
        return Boolean.TRUE.equals(session.getAttribute(ATTR_MODO_CONSOLIDADO));
    }

    public void limpiarContextoSucursal(HttpSession session) {
        if (session != null) {
            session.removeAttribute(ATTR_SUCURSAL_ACTIVA_ID);
            session.removeAttribute(ATTR_MODO_CONSOLIDADO);
        }
    }
}
