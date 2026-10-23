package pe.com.donpepe.minimarket.service;

import pe.com.donpepe.minimarket.model.EstadoSemaforoPeps;
import pe.com.donpepe.minimarket.model.LoteProducto;
import pe.com.donpepe.minimarket.model.Producto;

import java.time.LocalDate;

public class DescuentoPepsAutomaticoStrategy implements PoliticaDescuentoStrategy {
    private final LocalDate fechaReferencia;

    public DescuentoPepsAutomaticoStrategy(LocalDate fechaReferencia) {
        this.fechaReferencia = fechaReferencia;
    }

    @Override
    public double calcularDescuentoUnitario(Producto producto, LoteProducto loteAsignado) {
        if (loteAsignado.getDescuentoPromocional() > 0) {
            return Math.round(producto.getPrecioVenta() * loteAsignado.getDescuentoPromocional() * 100.0) / 100.0;
        }
        EstadoSemaforoPeps estado = loteAsignado.calcularEstadoSemaforo(fechaReferencia);
        if (estado == EstadoSemaforoPeps.ROJO) {
            return Math.round(producto.getPrecioVenta() * 0.20 * 100.0) / 100.0;
        }
        return 0.0;
    }
}
