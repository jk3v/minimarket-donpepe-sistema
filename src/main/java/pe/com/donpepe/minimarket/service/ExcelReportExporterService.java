package pe.com.donpepe.minimarket.service;

import com.google.common.base.Preconditions;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.com.donpepe.minimarket.model.ClienteVecinal;
import pe.com.donpepe.minimarket.model.LoteProducto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de Exportacion de Reportes Gerenciales en Microsoft Excel (.xlsx)
 * utilizando la libreria Apache POI (XSSFWorkbook).
 *
 * Autor: Equipo de Desarrollo / DevTeam
 */
public class ExcelReportExporterService {
    private static final Logger log = LoggerFactory.getLogger(ExcelReportExporterService.class);

    public File exportarReporteConsolidadoExcel(List<LoteProducto> lotesPeps,
                                                List<ClienteVecinal> clientesFiados,
                                                LocalDate fechaRef,
                                                String rutaArchivoDestino) {
        Preconditions.checkNotNull(lotesPeps, "La lista de lotes no puede ser nula");
        Preconditions.checkNotNull(clientesFiados, "La lista de clientes no puede ser nula");

        File outFile = new File(rutaArchivoDestino);
        if (outFile.getParentFile() != null && !outFile.getParentFile().exists()) {
            outFile.getParentFile().mkdirs();
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Sheet sheetPeps = workbook.createSheet("Inventario_PEPS_y_Fiados");
            Row header = sheetPeps.createRow(0);
            String[] cols = {"CODIGO EAN-13", "PRODUCTO", "NRO LOTE", "FECHA VENCIMIENTO", "DIAS RESTANTES", "SEMAFORO PEPS", "STOCK DISPONIBLE", "COSTO UNIT (S/.)"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (LoteProducto l : lotesPeps) {
                Row r = sheetPeps.createRow(rowIdx++);
                r.createCell(0).setCellValue(l.getCodigoBarras());
                r.createCell(1).setCellValue(l.getNombreProducto());
                r.createCell(2).setCellValue(l.getNumeroLote());
                r.createCell(3).setCellValue(l.getFechaVencimiento().toString());
                r.createCell(4).setCellValue(l.getDiasParaVencer(fechaRef));
                r.createCell(5).setCellValue(l.calcularEstadoSemaforo(fechaRef).getEtiqueta());
                r.createCell(6).setCellValue(l.getCantidadDisponible());
                r.createCell(7).setCellValue(l.getCostoUnitario());
            }

            // Seccion de Padron de Creditos Vecinales
            rowIdx += 2;
            Row headerFiados = sheetPeps.createRow(rowIdx++);
            String[] colsFiados = {"DNI VECINO", "NOMBRES Y APELLIDOS", "DIRECCION", "TELEFONO", "LIMITE CREDITO (S/.)", "DEUDA ACTUAL (S/.)", "DISPONIBLE (S/.)"};
            for (int i = 0; i < colsFiados.length; i++) {
                Cell cell = headerFiados.createCell(i);
                cell.setCellValue(colsFiados[i]);
                cell.setCellStyle(headerStyle);
            }

            for (ClienteVecinal c : clientesFiados) {
                Row r = sheetPeps.createRow(rowIdx++);
                r.createCell(0).setCellValue(c.getDni());
                r.createCell(1).setCellValue(c.getNombresApellidos());
                r.createCell(2).setCellValue(c.getDireccion());
                r.createCell(3).setCellValue(c.getTelefono());
                r.createCell(4).setCellValue(c.getLimiteCredito());
                r.createCell(5).setCellValue(c.getDeudaActual());
                r.createCell(6).setCellValue(c.getCreditoDisponible());
            }

            for (int i = 0; i < cols.length; i++) {
                sheetPeps.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                workbook.write(fos);
            }
            log.info("Reporte Excel (.xlsx) generado con Apache POI en: {}", outFile.getAbsolutePath());
            return outFile;
        } catch (IOException e) {
            log.error("Error exportando archivo Excel con Apache POI", e);
            throw new IllegalStateException("No se pudo exportar el reporte Excel", e);
        }
    }
}
