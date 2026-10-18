package pe.com.donpepe.minimarket.model;

import java.time.LocalDateTime;

public class TurnoCaja {
    private int id;
    private String cajeroUsername;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private double fondoInicial;
    private double totalEfectivoVentas;
    private double totalYapePlin;
    private double totalTarjeta;
    private double totalCreditoFiado;
    private double totalAmortizacionesEfectivo;
    private double efectivoContadoReal;
    private double diferenciaArqueo;
    private String justificacionDescuadre;
    private boolean abierto;

    public TurnoCaja(int id, String cajeroUsername, LocalDateTime fechaApertura, double fondoInicial) {
        this.id = id;
        this.cajeroUsername = cajeroUsername;
        this.fechaApertura = fechaApertura;
        this.fondoInicial = fondoInicial;
        this.abierto = true;
    }

    public double getEfectivoEsperadoEnCaja() {
        return Math.round((fondoInicial + totalEfectivoVentas + totalAmortizacionesEfectivo) * 100.0) / 100.0;
    }

    public double getTotalRecaudadoGeneral() {
        return Math.round((totalEfectivoVentas + totalYapePlin + totalTarjeta + totalAmortizacionesEfectivo) * 100.0) / 100.0;
    }

    public int getId() { return id; }
    public String getCajeroUsername() { return cajeroUsername; }
    public LocalDateTime getFechaApertura() { return fechaApertura; }
    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }
    public double getFondoInicial() { return fondoInicial; }
    public double getTotalEfectivoVentas() { return totalEfectivoVentas; }
    public void addEfectivoVentas(double monto) { this.totalEfectivoVentas = Math.round((this.totalEfectivoVentas + monto) * 100.0) / 100.0; }
    public double getTotalYapePlin() { return totalYapePlin; }
    public void addYapePlin(double monto) { this.totalYapePlin = Math.round((this.totalYapePlin + monto) * 100.0) / 100.0; }
    public double getTotalTarjeta() { return totalTarjeta; }
    public void addTarjeta(double monto) { this.totalTarjeta = Math.round((this.totalTarjeta + monto) * 100.0) / 100.0; }
    public double getTotalCreditoFiado() { return totalCreditoFiado; }
    public void addCreditoFiado(double monto) { this.totalCreditoFiado = Math.round((this.totalCreditoFiado + monto) * 100.0) / 100.0; }
    public double getTotalAmortizacionesEfectivo() { return totalAmortizacionesEfectivo; }
    public void addAmortizacionEfectivo(double monto) { this.totalAmortizacionesEfectivo = Math.round((this.totalAmortizacionesEfectivo + monto) * 100.0) / 100.0; }
    public double getEfectivoContadoReal() { return efectivoContadoReal; }
    public void setEfectivoContadoReal(double efectivoContadoReal) { this.efectivoContadoReal = efectivoContadoReal; }
    public double getDiferenciaArqueo() { return diferenciaArqueo; }
    public void setDiferenciaArqueo(double diferenciaArqueo) { this.diferenciaArqueo = diferenciaArqueo; }
    public String getJustificacionDescuadre() { return justificacionDescuadre; }
    public void setJustificacionDescuadre(String justificacionDescuadre) { this.justificacionDescuadre = justificacionDescuadre; }
    public boolean isAbierto() { return abierto; }
    public void setAbierto(boolean abierto) { this.abierto = abierto; }
}
