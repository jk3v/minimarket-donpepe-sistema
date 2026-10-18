package pe.com.donpepe.minimarket.model;

import java.time.LocalDateTime;

public class AmortizacionCredito {
    private int id;
    private String dniCliente;
    private int turnoId;
    private LocalDateTime fechaHora;
    private double montoAbonado;
    private String medioPago;
    private double saldoAnterior;
    private double saldoRestante;
    private String observacion;

    public AmortizacionCredito(int id, String dniCliente, int turnoId, LocalDateTime fechaHora,
                               double montoAbonado, String medioPago, double saldoAnterior,
                               double saldoRestante, String observacion) {
        this.id = id;
        this.dniCliente = dniCliente;
        this.turnoId = turnoId;
        this.fechaHora = fechaHora;
        this.montoAbonado = montoAbonado;
        this.medioPago = medioPago;
        this.saldoAnterior = saldoAnterior;
        this.saldoRestante = saldoRestante;
        this.observacion = observacion;
    }

    public int getId() { return id; }
    public String getDniCliente() { return dniCliente; }
    public int getTurnoId() { return turnoId; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public double getMontoAbonado() { return montoAbonado; }
    public String getMedioPago() { return medioPago; }
    public double getSaldoAnterior() { return saldoAnterior; }
    public double getSaldoRestante() { return saldoRestante; }
    public String getObservacion() { return observacion; }
}
