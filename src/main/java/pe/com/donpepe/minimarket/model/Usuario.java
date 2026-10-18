package pe.com.donpepe.minimarket.model;

public class Usuario {
    private int id;
    private String username;
    private String passwordHash;
    private String salt;
    private String nombreCompleto;
    private RolUsuario rol;
    private int intentosFallidos;
    private boolean bloqueado;
    private boolean activo;

    public Usuario(int id, String username, String passwordHash, String salt,
                   String nombreCompleto, RolUsuario rol) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.intentosFallidos = 0;
        this.bloqueado = false;
        this.activo = true;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }
    public String getNombreCompleto() { return nombreCompleto; }
    public RolUsuario getRol() { return rol; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }
    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
