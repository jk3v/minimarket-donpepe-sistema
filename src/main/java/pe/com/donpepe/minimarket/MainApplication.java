package pe.com.donpepe.minimarket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.com.donpepe.minimarket.controller.SistemaMinimarketController;
import pe.com.donpepe.minimarket.model.*;
import pe.com.donpepe.minimarket.view.MinimarketMainFrame;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;

/**
 * Clase Principal de Arranque del Sistema Minimarket Don Pepe E.I.R.L.
 * Permite ejecucion interactiva en escritorio o modo demostracion/exportacion de reportes.
 *
 * Autor: Equipo de Desarrollo / DevTeam
 */
public class MainApplication {
    private static final Logger log = LoggerFactory.getLogger(MainApplication.class);

    public static void main(String[] args) {
        log.info("Iniciando Sistema de Gestion Comercial Minimarket Don Pepe E.I.R.L. (Java 17 MVC + DAO + SOLID)");
        SistemaMinimarketController controller = new SistemaMinimarketController();

        // Demostracion de flujo completo de negocio y exportacion Excel con Apache POI
        Usuario admin = controller.iniciarSesion("admin", "Admin2026!");
        TurnoCaja turno = controller.abrirTurno(admin.getUsername(), 150.00);

        DetalleVenta item1 = controller.escanearProductoPos("7751271023450", 2); // Yogurt Gloria (Lote ROJO con 20% off)
        DetalleVenta item2 = controller.escanearProductoPos("7751271011204", 3); // Leche Gloria
        Venta ventaDemo = controller.registrarVenta(Arrays.asList(item1, item2), 20.00, 5.00, 0.00, 4.60, "43891024");
        System.out.println(controller.generarTicketTexto(ventaDemo));

        File reporteExcel = controller.exportarExcelGeneral("reportes_excel/Reporte_Consolidado_DonPepe_2026.xlsx");
        log.info("Reporte Excel Apache POI disponible en: {}", reporteExcel.getAbsolutePath());

        if (args.length > 0 && "--headless-screenshots".equals(args[0])) {
            try {
                System.setProperty("java.awt.headless", "false");
                MinimarketMainFrame frame = new MinimarketMainFrame(controller);
                frame.pack();
                frame.setSize(1220, 760);
                frame.exportarCapturasPantallas(args.length > 1 ? args[1] : "capturas_ui");
                frame.dispose();
                log.info("Capturas de las 5 interfaces graficas exportadas correctamente.");
                return;
            } catch ( Throwable t ) {
                log.warn("Ejecucion en entorno sin servidor X11 para Swing directo: {}", t.getMessage());
                return;
            }
        }

        if (!GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(() -> {
                MinimarketMainFrame frame = new MinimarketMainFrame(controller);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            });
        }
    }
}
