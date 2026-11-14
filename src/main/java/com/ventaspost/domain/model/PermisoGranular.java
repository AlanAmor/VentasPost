package com.ventaspost.domain.model;

import java.util.EnumSet;
import java.util.Set;

public enum PermisoGranular {
    CAMBIAR_PRECIOS_GLOBAL("Modificar precios generales de catálogo", true),
    CAMBIAR_PRECIOS_SUCURSAL("Modificar precios locales de sucursal", false),
    ANULAR_VENTAS("Anular ventas y autorizar anulaciones en caja", false),
    APLICAR_DESCUENTOS_ESPECIALES("Aplicar o autorizar descuentos especiales en caja", false),
    VER_REPORTES_SUCURSAL("Ver reportes operativos de su sucursal", false),
    VER_REPORTES_CONSOLIDADOS("Ver reportes consolidados de todas las sucursales", true),
    AUDITAR_CAJAS_SUCURSAL("Auditar y supervisar cajas de la sucursal", false),
    OPERAR_CAJA_PROPIA("Operar punto de venta y arqueo de caja propia", false),
    GESTIONAR_STOCK_SUCURSAL("Gestionar recepción, ajustes y transferencias de stock", false),
    GESTIONAR_EMPLEADOS_SUCURSAL("Gestionar empleados de su sucursal (roles inferiores)", false);

    private final String descripcion;
    private final boolean exclusivoAdmin;

    PermisoGranular(String descripcion, boolean exclusivoAdmin) {
        this.descripcion = descripcion;
        this.exclusivoAdmin = exclusivoAdmin;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isExclusivoAdmin() {
        return exclusivoAdmin;
    }

    public static Set<PermisoGranular> permisosPorDefectoParaRol(CodigoRol rol) {
        return switch (rol) {
            case ADMIN -> EnumSet.allOf(PermisoGranular.class);
            case ENCARGADO_SUCURSAL -> EnumSet.of(
                    CAMBIAR_PRECIOS_SUCURSAL,
                    ANULAR_VENTAS,
                    APLICAR_DESCUENTOS_ESPECIALES,
                    VER_REPORTES_SUCURSAL,
                    AUDITAR_CAJAS_SUCURSAL,
                    OPERAR_CAJA_PROPIA,
                    GESTIONAR_STOCK_SUCURSAL,
                    GESTIONAR_EMPLEADOS_SUCURSAL
            );
            case CAJERO -> EnumSet.of(
                    OPERAR_CAJA_PROPIA
            );
            case DEPOSITO -> EnumSet.of(
                    GESTIONAR_STOCK_SUCURSAL
            );
            case LIMPIEZA -> EnumSet.noneOf(PermisoGranular.class);
        };
    }

    public static Set<PermisoGranular> permisosPorDefectoParaRoles(Set<CodigoRol> roles) {
        EnumSet<PermisoGranular> resultado = EnumSet.noneOf(PermisoGranular.class);
        if (roles != null) {
            for (CodigoRol rol : roles) {
                resultado.addAll(permisosPorDefectoParaRol(rol));
            }
        }
        return resultado;
    }
}
