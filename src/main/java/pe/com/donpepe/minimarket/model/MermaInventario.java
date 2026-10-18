package pe.com.donpepe.minimarket.model;

import java.time.LocalDateTime;

public class MermaInventario {
    private int id;
    private String codigoBarras;
    private String nombreProducto;
    private String numeroLote;
    private int cantidadBaja;
    private String motivo;
    private double costoPerdida;
    private String usuarioRegistro;
    private LocalDateTime fechaHora;

    public MermaInventario(int id, String codigoBarras, String nombreProducto, String numeroLote,
                           int cantidadBaja, String motivo, double costoPerdida,
                           String usuarioRegistro, LocalDateTime fechaHora) {
        this.id = id;
        this.codigoBarras = codigoBarras;
        this.nombreProducto = nombreProducto;
        this.numeroLote = numeroLote;
        this.cantidadBaja = cantidadBaja;
        this.motivo = motivo;
        this.costoPerdida = costoPerdida;
        this.usuarioRegistro = usuarioRegistro;
        this.fechaHora = fechaHora;
    }

    public int getId() { return id; }
    public String getCodigoBarras() { return codigoBarras; }
    public String getNombreProducto() { return nombreProducto; }
    public String getNumeroLote() { return numeroLote; }
    public int getCantidadBaja() { return cantidadBaja; }
    public String getMotivo() { return motivo; }
    public double getCostoPerdida() { return costoPerdida; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public LocalDateTime getFechaHora() { return fechaHora; }
}
