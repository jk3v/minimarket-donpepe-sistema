package pe.com.donpepe.minimarket.model;

public class ClienteVecinal {
    private int id;
    private String dni;
    private String nombresApellidos;
    private String direccion;
    private String telefono;
    private double limiteCredito;
    private double deudaActual;
    private boolean bloqueadoCredito;

    public ClienteVecinal(int id, String dni, String nombresApellidos, String direccion,
                          String telefono, double limiteCredito, double deudaActual) {
        this.id = id;
        this.dni = dni;
        this.nombresApellidos = nombresApellidos;
        this.direccion = direccion;
        this.telefono = telefono;
        this.limiteCredito = limiteCredito;
        this.deudaActual = deudaActual;
        this.bloqueadoCredito = false;
    }

    public double getCreditoDisponible() {
        return Math.max(0.0, Math.round((limiteCredito - deudaActual) * 100.0) / 100.0);
    }

    public int getId() { return id; }
    public String getDni() { return dni; }
    public String getNombresApellidos() { return nombresApellidos; }
    public String getDireccion() { return direccion; }
    public String getTelefono() { return telefono; }
    public double getLimiteCredito() { return limiteCredito; }
    public double getDeudaActual() { return deudaActual; }
    public void setDeudaActual(double deudaActual) { this.deudaActual = Math.round(deudaActual * 100.0) / 100.0; }
    public boolean isBloqueadoCredito() { return bloqueadoCredito; }
    public void setBloqueadoCredito(boolean bloqueadoCredito) { this.bloqueadoCredito = bloqueadoCredito; }
}
