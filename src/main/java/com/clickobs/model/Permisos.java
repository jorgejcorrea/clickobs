package com.clickobs.model;

/**
 * Control de acceso por rol (RF-04). Los ids de rol corresponden a los datos de prueba de rol_permiso:
 * 1 CTO, 2 SRE Lead, 3 DevOps, 4 Finanzas. Un switch por pantalla concentra TODA la regla en un solo lugar.
 */
public final class Permisos {

    public static final int CTO = 1;
    public static final int SRE_LEAD = 2;
    public static final int DEVOPS = 3;
    public static final int FINANZAS = 4;

    private Permisos() { }

    public static boolean puedeAcceder(Usuario u, Pantalla pantalla) {
        int rol = u.getIdRol();
        switch (pantalla) {
            case DASHBOARD:        return rol == CTO || rol == SRE_LEAD || rol == FINANZAS;
            case CONSUMO:          return rol == DEVOPS || rol == SRE_LEAD || rol == CTO;
            case COMPARAR:         return rol == SRE_LEAD;                 // CU-02
            case RECOMENDACIONES:  return rol == CTO;                      // CU-03
            case ALERTAS:          return rol == FINANZAS || rol == CTO;   // CU-04
            case ADMINISTRACION:   return rol == SRE_LEAD;                 // conectores
            default:               return false;
        }
    }
}
