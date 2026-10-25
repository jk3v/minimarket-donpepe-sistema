package pe.com.donpepe.minimarket.service;

import com.google.common.base.Preconditions;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.com.donpepe.minimarket.dao.SqliteDataStore;
import pe.com.donpepe.minimarket.dao.TurnoCajaDao;
import pe.com.donpepe.minimarket.dao.VentaDao;
import pe.com.donpepe.minimarket.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio Transaccional de Punto de Venta (POS), Facturacion Rapida y Pagos Mixtos (RF-06, RF-07, RF-08).
 * Autor: Equipo de Desarrollo / DevTeam
 */
public class VentaPosService {
    private static final Logger log = LoggerFactory.getLogger(VentaPosService.class);
    private static final double TASA_IGV = 0.18;

    private final InventarioPepsService inventarioService;
    private final CreditoVecinalService creditoService;
    private final TurnoCajaDao turnoDao;
    private final VentaDao ventaDao;
    private final SqliteDataStore dataStore;
    private final PoliticaDescuentoStrategy politicaDescuento;

    public VentaPosService(InventarioPepsService inventarioService,
                           CreditoVecinalService creditoService,
                           TurnoCajaDao turnoDao,
                           VentaDao ventaDao,
                           SqliteDataStore dataStore,
                           PoliticaDescuentoStrategy politicaDescuento) {
        this.inventarioService = Preconditions.checkNotNull(inventarioService);
        this.creditoService = Preconditions.checkNotNull(creditoService);
        this.turnoDao = Preconditions.checkNotNull(turnoDao);
        this.ventaDao = Preconditions.checkNotNull(ventaDao);
        this.dataStore = Preconditions.checkNotNull(dataStore);
        this.politicaDescuento = Preconditions.checkNotNull(politicaDescuento);
    }

    public DetalleVenta construirLineaVentaPeps(String codigoBarras, int cantidad) {
        Producto prod = inventarioService.buscarPorEan13ConCache(codigoBarras);
        LoteProducto lotePeps = inventarioService.obtenerLotePrioritarioPepsParaVenta(codigoBarras, cantidad);
        double descUnitario = politicaDescuento.calcularDescuentoUnitario(prod, lotePeps);
        return new DetalleVenta(
                prod.getCodigoBarras(),
                prod.getNombre(),
                lotePeps.getNumeroLote(),
                cantidad,
                prod.getPrecioVenta(),
                descUnitario
        );
    }

    public Venta procesarVentaTransaccional(List<DetalleVenta> items,
                                            double montoEfectivoEntregado,
                                            double montoYapePlin,
                                            double montoTarjeta,
                                            double montoFiado,
                                            String dniClienteFiado) {
        TurnoCaja turno = turnoDao.obtenerTurnoAbierto()
                .orElseThrow(() -> new IllegalStateException("RF-02 Violado: No se puede realizar ventas si la caja no ha sido formalmente abierta."));

        Preconditions.checkArgument(CollectionUtils.isNotEmpty(items), "El carrito de venta no puede estar vacio");
        Preconditions.checkArgument(montoEfectivoEntregado >= 0 && montoYapePlin >= 0 && montoTarjeta >= 0 && montoFiado >= 0,
                "Los montos de pago no pueden ser negativos");

        double totalVenta = Math.round(items.stream().mapToDouble(DetalleVenta::getSubtotalLinea).sum() * 100.0) / 100.0;
        double descuentoTotal = Math.round(items.stream().mapToDouble(i -> i.getDescuentoUnitario() * i.getCantidad()).sum() * 100.0) / 100.0;
        double subtotalBase = Math.round((totalVenta / (1.0 + TASA_IGV)) * 100.0) / 100.0;
        double igv = Math.round((totalVenta - subtotalBase) * 100.0) / 100.0;

        double pagosNoEfectivo = Math.round((montoYapePlin + montoTarjeta + montoFiado) * 100.0) / 100.0;
        Preconditions.checkArgument(pagosNoEfectivo <= totalVenta + 0.001,
                "La suma de pagos digitales/tarjeta/fiado no puede exceder el total de la venta");

        double totalAbonado = Math.round((montoEfectivoEntregado + pagosNoEfectivo) * 100.0) / 100.0;
        Preconditions.checkArgument(totalAbonado >= totalVenta - 0.001,
                String.format("RF-07 Violado: Monto cobrado insuficiente (Pagado: S/. %.2f, Total: S/. %.2f)", totalAbonado, totalVenta));

        double efectivoNetoCaja = Math.round((totalVenta - pagosNoEfectivo) * 100.0) / 100.0;
        double vuelto = Math.round((montoEfectivoEntregado - efectivoNetoCaja) * 100.0) / 100.0;

        if (montoFiado > 0) {
            Preconditions.checkArgument(StringUtils.isNotBlank(dniClienteFiado),
                    "RF-08: Para ventas al fiado es obligatorio ingresar el DNI del cliente vecinal");
            creditoService.validarYCargarFiado(dniClienteFiado, montoFiado);
        }

        // Descontar inventario siguiendo PEPS estricto (RF-04)
        for (DetalleVenta item : items) {
            inventarioService.descontarStockPeps(item.getCodigoBarras(), item.getCantidad());
        }

        int numId = dataStore.nextVentaId();
        String nroComprobante = String.format("B001-%06d", numId);
        Venta venta = new Venta(
                numId,
                nroComprobante,
                turno.getId(),
                turno.getCajeroUsername(),
                dniClienteFiado,
                LocalDateTime.now(),
                subtotalBase,
                igv,
                descuentoTotal,
                totalVenta,
                efectivoNetoCaja,
                montoYapePlin,
                montoTarjeta,
                montoFiado,
                vuelto,
                items
        );

        ventaDao.registrarVentaTransaccional(venta);
        turno.addEfectivoVentas(efectivoNetoCaja);
        turno.addYapePlin(montoYapePlin);
        turno.addTarjeta(montoTarjeta);
        turno.addCreditoFiado(montoFiado);
        turnoDao.actualizarTurno(turno);

        log.info("Venta Transaccional Exitosa: Comprobante={} Total=S/.{} Efectivo=S/.{} YapePlin=S/.{} Fiado=S/.{} Vuelto=S/.{}",
                nroComprobante, totalVenta, efectivoNetoCaja, montoYapePlin, montoFiado, vuelto);
        return venta;
    }

    public String formatearTicketTermico80mm(Venta v) {
        StringBuilder sb = new StringBuilder();
        sb.append("==========================================\n");
        sb.append("   MINIMARKET Y COMERCIAL DON PEPE EIRL   \n");
        sb.append("         RUC: 20608912345                 \n");
        sb.append("  Urb. Covima / Santa Patricia - La Molina\n");
        sb.append("==========================================\n");
        sb.append("BOLETA DE VENTA ELECTRONICA: ").append(v.getNumeroComprobante()).append("\n");
        sb.append("FECHA: ").append(v.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
        sb.append("CAJERO: ").append(v.getCajeroUsername()).append("\n");
        if (StringUtils.isNotBlank(v.getDniCliente())) {
            sb.append("CLIENTE DNI: ").append(v.getDniCliente()).append("\n");
        }
        sb.append("------------------------------------------\n");
        sb.append(String.format("%-18s %4s %7s %8s\n", "PRODUCTO (LOTE)", "CANT", "P.UNIT", "SUBTOT"));
        sb.append("------------------------------------------\n");
        for (DetalleVenta d : v.getItems()) {
            String nombreCorto = d.getNombreProducto().length() > 18 ? d.getNombreProducto().substring(0, 18) : d.getNombreProducto();
            sb.append(String.format("%-18s %4d %7.2f %8.2f\n",
                    nombreCorto, d.getCantidad(), d.getPrecioUnitario() - d.getDescuentoUnitario(), d.getSubtotalLinea()));
            sb.append("  Lote PEPS: ").append(d.getNumeroLotePeps());
            if (d.getDescuentoUnitario() > 0) {
                sb.append(String.format(" (Promo -S/.%.2f)", d.getDescuentoUnitario() * d.getCantidad()));
            }
            sb.append("\n");
        }
        sb.append("------------------------------------------\n");
        sb.append(String.format("OP. GRAVADA (SUBTOTAL):     S/. %8.2f\n", v.getSubtotalBase()));
        sb.append(String.format("IGV (18%%):                  S/. %8.2f\n", v.getIgv()));
        if (v.getDescuentoTotal() > 0) {
            sb.append(String.format("AHORRO PROMO PEPS:          S/. %8.2f\n", v.getDescuentoTotal()));
        }
        sb.append(String.format("IMPORTE TOTAL:              S/. %8.2f\n", v.getTotal()));
        sb.append("------------------------------------------\n");
        sb.append(String.format("EFECTIVO: S/. %.2f | YAPE/PLIN: S/. %.2f\n", v.getMontoEfectivo() + v.getVuelto(), v.getMontoYapePlin()));
        sb.append(String.format("TARJETA:  S/. %.2f | FIADO:     S/. %.2f\n", v.getMontoTarjeta(), v.getMontoFiado()));
        sb.append(String.format("VUELTO ENTREGADO:           S/. %8.2f\n", v.getVuelto()));
        sb.append("==========================================\n");
        sb.append("   ¡Gracias por comprar en Don Pepe!      \n");
        return sb.toString();
    }
}
