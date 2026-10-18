package pe.com.donpepe.minimarket.model;

/**
 * Roles de seguridad del sistema segun RF-01.
 * Autor: Equipo de Desarrollo / DevTeam
 */
public enum RolUsuario {
    ADMINISTRADOR("Acceso total: POS, Inventario PEPS, Creditos, Arqueo y Configuracion"),
    CAJERO("Acceso operativo: Apertura de Turno, Terminal POS, Fiados y Arqueo Ciego"),
    ALMACENERO("Acceso logistico: Catalogo de Productos, Recepcion de Lotes PEPS y Mermas");

    private final String descripcionPermisos;

    RolUsuario(String descripcionPermisos) {
        this.descripcionPermisos = descripcionPermisos;
    }

    public String getDescripcionPermisos() {
        return descripcionPermisos;
    }
}
