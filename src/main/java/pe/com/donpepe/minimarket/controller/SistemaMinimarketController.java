package pe.com.donpepe.minimarket.controller;

import pe.com.donpepe.minimarket.dao.SqliteDataStore;
import pe.com.donpepe.minimarket.model.*;
import pe.com.donpepe.minimarket.security.SecurityService;
import pe.com.donpepe.minimarket.service.*;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador Principal bajo el Patron Modelo-Vista-Controlador (MVC).
 * Orquesta las peticiones de las 5 pantallas de la interfaz grafica hacia los servicios de negocio.
 *
 * Autor: Equipo de Desarrollo / DevTeam
 */
public class SistemaMinimarketController {
    private final SqliteDataStore dataStore;
    private final SecurityService securityService;
    private final InventarioPepsService inventarioService;
    private final CreditoVecinalService creditoService;
    private final VentaPosService ventaPosService;
    private final ArqueoCajaService arqueoCajaService;
    private final ExcelReportExporterService excelExporterService;

    public SistemaMinimarketController() {
        this(LocalDate.of(2026, 10, 29));
    }

    public SistemaMinimarketController(LocalDate fechaSistema) {
        this.dataStore = new SqliteDataStore(fechaSistema);
        this.securityService = new SecurityService(dataStore);
        this.inventarioService = new InventarioPepsService(dataStore, dataStore, fechaSistema);
        this.creditoService = new CreditoVecinalService(dataStore, dataStore);
        this.ventaPosService = new VentaPosService(
                inventarioService,
                creditoService,
                dataStore,
                dataStore,
                dataStore,
                new DescuentoPepsAutomaticoStrategy(fechaSistema)
        );
        this.arqueoCajaService = new ArqueoCajaService(dataStore);
        this.excelExporterService = new ExcelReportExporterService();
    }

    public Usuario iniciarSesion(String username, String password) {
        return securityService.autenticar(username, password);
    }

    public TurnoCaja abrirTurno(String cajeroUsername, double fondoInicial) {
        return arqueoCajaService.abrirTurnoCaja(cajeroUsername, fondoInicial);
    }

    public DetalleVenta escanearProductoPos(String ean13, int cantidad) {
        return ventaPosService.construirLineaVentaPeps(ean13, cantidad);
    }

    public Venta registrarVenta(List<DetalleVenta> items, double efectivo, double yapePlin,
                                double tarjeta, double fiado, String dniCliente) {
        return ventaPosService.procesarVentaTransaccional(items, efectivo, yapePlin, tarjeta, fiado, dniCliente);
    }

    public String generarTicketTexto(Venta venta) {
        return ventaPosService.formatearTicketTermico80mm(venta);
    }

    public List<LoteProducto> obtenerLotesPeps() {
        return inventarioService.listarInventarioConSemaforoPeps();
    }

    public List<ClienteVecinal> obtenerClientesVecinales() {
        return creditoService.listarPadronClientes();
    }

    public AmortizacionCredito abonarDeudaVecinal(String dni, double monto, String medioPago, String obs) {
        return creditoService.registrarAmortizacion(dni, monto, medioPago, obs);
    }

    public MermaInventario registrarMerma(String ean13, String lote, int cantidad, String motivo, String usuario) {
        return inventarioService.registrarBajaPorMerma(ean13, lote, cantidad, motivo, usuario);
    }

    public TurnoCaja cerrarCajaConArqueo(double efectivoContado, String justificacion) {
        return arqueoCajaService.ejecutarArqueoYCierreTurno(efectivoContado, justificacion);
    }

    public String generarInformeZTexto(TurnoCaja turno) {
        return arqueoCajaService.generarInformeZTermico(turno);
    }

    public File exportarExcelGeneral(String rutaDestino) {
        return excelExporterService.exportarReporteConsolidadoExcel(
                inventarioService.listarInventarioConSemaforoPeps(),
                creditoService.listarPadronClientes(),
                inventarioService.getFechaReferencia(),
                rutaDestino
        );
    }

    public SecurityService getSecurityService() { return securityService; }
    public InventarioPepsService getInventarioService() { return inventarioService; }
    public CreditoVecinalService getCreditoService() { return creditoService; }
    public VentaPosService getVentaPosService() { return ventaPosService; }
    public ArqueoCajaService getArqueoCajaService() { return arqueoCajaService; }
    public SqliteDataStore getDataStore() { return dataStore; }
}
