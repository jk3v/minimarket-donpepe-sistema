package pe.com.donpepe.minimarket.service;

import pe.com.donpepe.minimarket.model.LoteProducto;
import pe.com.donpepe.minimarket.model.Producto;

/**
 * Principio SOLID Abierto/Cerrado (OCP) y Sustitucion de Liskov (LSP):
 * Permite extender nuevas politicas de promocion o liquidacion anti-merma
 * sin modificar el codigo fuente de VentaPosService.
 */
public interface PoliticaDescuentoStrategy {
    double calcularDescuentoUnitario(Producto producto, LoteProducto loteAsignado);
}
