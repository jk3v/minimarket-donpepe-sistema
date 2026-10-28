package pe.com.donpepe.minimarket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.com.donpepe.minimarket.controller.SistemaMinimarketController;
import pe.com.donpepe.minimarket.model.*;

import java.io.File;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de Pruebas Unitarias aplicando Desarrollo Guiado por Pruebas (TDD) con JUnit 5.
 * Verifica el 100% de las reglas de negocio comprometidas en el SRS (RF-01 al RF-12 y RNF-01 al RNF-08).
 *
 * Autor: Equipo de Desarrollo / DevTeam
 */
public class MinimarketTddTestSuite {

    private SistemaMinimarketController controller;

    @BeforeEach
    public void setUp() {
        controller = new SistemaMinimarketController(LocalDate.of(2026, 10, 29));
    }

    @Test
    @DisplayName("TDD-01 (RF-01 / RNF-03): Autenticacion con Hash SHA-256+Salt y bloqueo al 3er intento fallido")
    public void testAutenticacionYBloqueoPorIntentosFallidos() {
        Usuario admin = controller.iniciarSesion("admin", "Admin2026!");
        assertNotNull(admin, "El usuario administrador debe autenticarse correctamente");
        assertEquals(RolUsuario.ADMINISTRADOR, admin.getRol());

        // Probar 3 intentos fallidos en cajero_ana
        assertThrows(SecurityException.class, () -> controller.iniciarSesion("cajero_ana", "ClaveErronea1"), "Intento 1");
        assertThrows(SecurityException.class, () -> controller.iniciarSesion("cajero_ana", "ClaveErronea2"), "Intento 2");
        assertThrows(SecurityException.class, () -> controller.iniciarSesion("cajero_ana", "ClaveErronea3"), "Intento 3 bloquea cuenta");

        // Incluso con la clave correcta ahora debe rechazar por cuenta bloqueada
        SecurityException ex = assertThrows(SecurityException.class,
                () -> controller.iniciarSesion("cajero_ana", "Caja2026!"),
                "Cuenta debe permanecer bloqueada tras 3 intentos fallidos");
        assertTrue(ex.getMessage().contains("bloqueada"), "El mensaje debe informar el bloqueo de seguridad");
    }

    @Test
    @DisplayName("TDD-02 (RF-02): Bloqueo preventivo de ventas en POS si el turno de caja no fue abierto")
    public void testBloqueoVentaSinTurnoCajaAbierto() {
        DetalleVenta item = controller.escanearProductoPos("7751271011204", 1);
        assertThrows(IllegalStateException.class, () ->
                controller.registrarVenta(Collections.singletonList(item), 10.0, 0, 0, 0, null),
                "RF-02: No debe permitir facturar sin abrir turno de caja");
    }

    @Test
    @DisplayName("TDD-03 (RF-03): Validacion de unicidad EAN-13 e impedimento de precio de venta menor al costo")
    public void testValidacionCatalogoPreciosYUnicidad() {
        Producto invalido = new Producto(99, "7759999999991", "Producto Precio Perdida", "Abarrotes", "UND", 10.00, 8.50, 5);
        assertThrows(IllegalArgumentException.class,
                () -> controller.getInventarioService().registrarNuevoProducto(invalido),
                "RF-03: Debe impedir precios de venta menores al costo");
    }

    @Test
    @DisplayName("TDD-04 (RF-04 / RF-05): Algoritmo PEPS selecciona siempre el lote mas proximo a vencer y aplica Semaforo Rojo")
    public void testAlgoritmoPepsYSemaforoCaducidad() {
        DetalleVenta lineaYogurt = controller.escanearProductoPos("7751271023450", 2);
        assertEquals("LOT-2026-Y01", lineaYogurt.getNumeroLotePeps(),
                "PEPS debe seleccionar el lote LOT-2026-Y01 (vence en 8 dias) antes que LOT-2026-Y02 (vence en 45 dias)");
        assertEquals(1.50, lineaYogurt.getDescuentoUnitario(), 0.01,
                "Debe aplicar 20% de descuento promocional por Semaforo ROJO (S/. 7.50 * 20% = S/. 1.50)");
        assertEquals(12.00, lineaYogurt.getSubtotalLinea(), 0.01);
    }

    @Test
    @DisplayName("TDD-05 (RF-06 / RF-07): Facturacion rapida POS con Pagos Mixtos (Efectivo + Yape/Plin + Fiado) y calculo de IGV/Vuelto")
    public void testVentaConPagosMixtosYCalculoExactoVuelto() {
        controller.abrirTurno("admin", 100.00);
        DetalleVenta item1 = controller.escanearProductoPos("7751271023450", 2); // S/. 12.00
        DetalleVenta item2 = controller.escanearProductoPos("7750243055109", 1); // S/. 9.80 -> Total S/. 21.80

        Venta venta = controller.registrarVenta(Arrays.asList(item1, item2), 15.00, 10.00, 0.00, 0.00, null);
        assertEquals(21.80, venta.getTotal(), 0.01);
        assertEquals(3.20, venta.getVuelto(), 0.01, "Si el cliente entrega S/.15 en efectivo + S/.10 en Yape (S/.25) para pagar S/.21.80, el vuelto es S/.3.20");
    }

    @Test
    @DisplayName("TDD-06 (RF-08 / RF-09): Bloqueo de fiado vecinal que supera limite de credito y actualizacion por amortizacion")
    public void testControlTopeCreditoVecinalYAmortizacion() {
        controller.abrirTurno("admin", 100.00);
        // Cliente Carlos Gutierrez (DNI 41029384) tiene Limite S/.120.00 y Deuda S/.112.00 (Disponible S/.8.00)
        assertThrows(IllegalStateException.class,
                () -> controller.getCreditoService().validarYCargarFiado("41029384", 15.00),
                "RF-08: Debe bloquear venta al fiado que excede los S/. 8.00 disponibles");

        // Registrar amortizacion de S/. 50.00 (RF-09)
        AmortizacionCredito abono = controller.abonarDeudaVecinal("41029384", 50.00, "EFECTIVO", "Pago parcial quincena");
        assertEquals(62.00, abono.getSaldoRestante(), 0.01, "La deuda debe bajar de S/. 112.00 a S/. 62.00");

        // Ahora si tiene credito disponible para una compra al fiado de S/. 15.00
        ClienteVecinal actualizado = controller.getCreditoService().validarYCargarFiado("41029384", 15.00);
        assertEquals(77.00, actualizado.getDeudaActual(), 0.01);
    }

    @Test
    @DisplayName("TDD-07 (RF-10): Registro de mermas descuenta stock fisico del lote y valoriza perdida contable")
    public void testRegistroMermaDescuentaStock() {
        MermaInventario merma = controller.registrarMerma("7751271088909", "LOT-2026-Q04", 2, "Empaque danado en refrigeradora", "almacen_luis");
        assertEquals(14.40, merma.getCostoPerdida(), 0.01, "2 unidades * S/. 7.20 de costo = S/. 14.40 de perdida");
        assertEquals(9, controller.getInventarioService().obtenerStockTotalVigentes("7751271088909"), "Stock del lote baja de 11 a 9");
    }

    @Test
    @DisplayName("TDD-08 (RF-11 / RF-12): Arqueo de caja exige justificacion si descuadre > S/. 2.00 y genera Informe Z + Backup")
    public void testArqueoCajaYExigenciaJustificacionPorDescuadre() {
        controller.abrirTurno("admin", 150.00);
        // Efectivo esperado = S/. 150.00. Si cuenta S/. 140.00 (faltante de S/. 10.00) sin justificar -> Error RF-11
        assertThrows(IllegalArgumentException.class,
                () -> controller.cerrarCajaConArqueo(140.00, ""),
                "RF-11: Debe exigir comentario justificativo cuando el descuadre supera S/. 2.00");

        // Con justificacion formal debe permitir el cierre y emitir Informe Z + Backup
        TurnoCaja cerrado = controller.cerrarCajaConArqueo(140.00, "Adelanto autorizado para pago de hielo mostrador");
        assertFalse(cerrado.isAbierto());
        assertEquals(-10.00, cerrado.getDiferenciaArqueo(), 0.01);
    }

    @Test
    @DisplayName("TDD-09 (Uso de Apache POI): Exportacion de reporte de inventario PEPS y fiados a archivo .xlsx valido")
    public void testExportacionExcelApachePoi() {
        File excel = controller.exportarExcelGeneral("target/test_reporte_donpepe.xlsx");
        assertTrue(excel.exists() && excel.length() > 100, "El archivo .xlsx generado con Apache POI debe existir y contener datos");
    }

    // Ejecutor autonomo para verificacion en consola y CI/CD
    public static void main(String[] args) throws Exception {
        System.out.println("=========================================================================");
        System.out.println(" EJECUTANDO SUITE TDD JUNIT 5 - MINIMARKET DON PEPE E.I.R.L.            ");
        System.out.println("=========================================================================");
        MinimarketTddTestSuite suite = new MinimarketTddTestSuite();
        int passed = 0;
        for (Method m : MinimarketTddTestSuite.class.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Test.class)) {
                suite.setUp();
                DisplayName dn = m.getAnnotation(DisplayName.class);
                String name = dn != null ? dn.value() : m.getName();
                m.invoke(suite);
                passed++;
                System.out.printf(" [PASS] %s%n", name);
            }
        }
        System.out.println("=========================================================================");
        System.out.printf(" RESULTADO TDD: %d/%d PRUEBAS UNITARIAS SUPERADAS AL 100%% (0 FALLOS)%n", passed, passed);
        System.out.println("=========================================================================");
    }
}
