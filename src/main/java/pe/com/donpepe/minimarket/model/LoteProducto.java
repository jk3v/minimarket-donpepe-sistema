package pe.com.donpepe.minimarket.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LoteProducto {
    private int id;
    private String codigoBarras;
    private String nombreProducto;
    private String numeroLote;
    private LocalDate fechaIngreso;
    private LocalDate fechaVencimiento;
    private int cantidadInicial;
    private int cantidadDisponible;
    private double costoUnitario;
    private double descuentoPromocional; // porcentaje ej. 0.20 = 20%

    public LoteProducto(int id, String codigoBarras, String nombreProducto, String numeroLote,
                        LocalDate fechaIngreso, LocalDate fechaVencimiento,
                        int cantidadInicial, int cantidadDisponible, double costoUnitario) {
        this.id = id;
        this.codigoBarras = codigoBarras;
        this.nombreProducto = nombreProducto;
        this.numeroLote = numeroLote;
        this.fechaIngreso = fechaIngreso;
        this.fechaVencimiento = fechaVencimiento;
        this.cantidadInicial = cantidadInicial;
        this.cantidadDisponible = cantidadDisponible;
        this.costoUnitario = costoUnitario;
        this.descuentoPromocional = 0.0;
    }

    public long getDiasParaVencer(LocalDate fechaReferencia) {
        return ChronoUnit.DAYS.between(fechaReferencia, fechaVencimiento);
    }

    public EstadoSemaforoPeps calcularEstadoSemaforo(LocalDate fechaReferencia) {
        long dias = getDiasParaVencer(fechaReferencia);
        if (dias <= 0) return EstadoSemaforoPeps.VENCIDO;
        if (dias < 15) return EstadoSemaforoPeps.ROJO;
        if (dias <= 30) return EstadoSemaforoPeps.AMARILLO;
        return EstadoSemaforoPeps.VERDE;
    }

    public int getId() { return id; }
    public String getCodigoBarras() { return codigoBarras; }
    public String getNombreProducto() { return nombreProducto; }
    public String getNumeroLote() { return numeroLote; }
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public int getCantidadInicial() { return cantidadInicial; }
    public int getCantidadDisponible() { return cantidadDisponible; }
    public void setCantidadDisponible(int cantidadDisponible) { this.cantidadDisponible = cantidadDisponible; }
    public double getCostoUnitario() { return costoUnitario; }
    public double getDescuentoPromocional() { return descuentoPromocional; }
    public void setDescuentoPromocional(double descuentoPromocional) { this.descuentoPromocional = descuentoPromocional; }
}
