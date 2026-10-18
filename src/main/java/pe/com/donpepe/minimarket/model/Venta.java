package pe.com.donpepe.minimarket.model;

import java.time.LocalDateTime;
import java.util.List;

public class Venta {
    private int id;
    private String numeroComprobante;
    private int turnoId;
    private String cajeroUsername;
    private String dniCliente;
    private LocalDateTime fechaHora;
    private double subtotalBase;
    private double igv;
    private double descuentoTotal;
    private double total;
    private double montoEfectivo;
    private double montoYapePlin;
    private double montoTarjeta;
    private double montoFiado;
    private double vuelto;
    private List<DetalleVenta> items;

    public Venta(int id, String numeroComprobante, int turnoId, String cajeroUsername, String dniCliente,
                 LocalDateTime fechaHora, double subtotalBase, double igv, double descuentoTotal, double total,
                 double montoEfectivo, double montoYapePlin, double montoTarjeta, double montoFiado,
                 double vuelto, List<DetalleVenta> items) {
        this.id = id;
        this.numeroComprobante = numeroComprobante;
        this.turnoId = turnoId;
        this.cajeroUsername = cajeroUsername;
        this.dniCliente = dniCliente;
        this.fechaHora = fechaHora;
        this.subtotalBase = subtotalBase;
        this.igv = igv;
        this.descuentoTotal = descuentoTotal;
        this.total = total;
        this.montoEfectivo = montoEfectivo;
        this.montoYapePlin = montoYapePlin;
        this.montoTarjeta = montoTarjeta;
        this.montoFiado = montoFiado;
        this.vuelto = vuelto;
        this.items = items;
    }

    public int getId() { return id; }
    public String getNumeroComprobante() { return numeroComprobante; }
    public int getTurnoId() { return turnoId; }
    public String getCajeroUsername() { return cajeroUsername; }
    public String getDniCliente() { return dniCliente; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public double getSubtotalBase() { return subtotalBase; }
    public double getIgv() { return igv; }
    public double getDescuentoTotal() { return descuentoTotal; }
    public double getTotal() { return total; }
    public double getMontoEfectivo() { return montoEfectivo; }
    public double getMontoYapePlin() { return montoYapePlin; }
    public double getMontoTarjeta() { return montoTarjeta; }
    public double getMontoFiado() { return montoFiado; }
    public double getVuelto() { return vuelto; }
    public List<DetalleVenta> getItems() { return items; }
}
