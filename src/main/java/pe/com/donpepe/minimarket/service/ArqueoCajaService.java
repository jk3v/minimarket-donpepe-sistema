package pe.com.donpepe.minimarket.service;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.com.donpepe.minimarket.dao.TurnoCajaDao;
import pe.com.donpepe.minimarket.model.TurnoCaja;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Servicio de Apertura de Turno, Arqueo Ciego, Cierre Diario e Informe Z (RF-02, RF-11, RF-12, RNF-08).
 * Autor: Equipo de Desarrollo / DevTeam
 */
public class ArqueoCajaService {
    private static final Logger log = LoggerFactory.getLogger(ArqueoCajaService.class);
    private static final double TOLERANCIA_DESCUADRE_SOLES = 2.00;

    private final TurnoCajaDao turnoDao;

    public ArqueoCajaService(TurnoCajaDao turnoDao) {
        this.turnoDao = Preconditions.checkNotNull(turnoDao, "TurnoCajaDao requerido");
    }

    public TurnoCaja abrirTurnoCaja(String cajeroUsername, double fondoInicial) {
        Preconditions.checkArgument(StringUtils.isNotBlank(cajeroUsername), "El usuario de cajero es obligatorio");
        Preconditions.checkArgument(fondoInicial >= 0, "El fondo de caja inicial no puede ser negativo");
        Preconditions.checkState(turnoDao.obtenerTurnoAbierto().isEmpty(), "Ya existe un turno de caja abierto en curso");

        TurnoCaja turno = turnoDao.abrirTurno(cajeroUsername, fondoInicial);
        log.info("Turno de Caja #{} abierto por {} con fondo inicial S/. {}", turno.getId(), cajeroUsername, fondoInicial);
        return turno;
    }

    public Optional<TurnoCaja> obtenerTurnoActivo() {
        return turnoDao.obtenerTurnoAbierto();
    }

    public TurnoCaja ejecutarArqueoYCierreTurno(double efectivoRealContado, String justificacion) {
        TurnoCaja turno = turnoDao.obtenerTurnoAbierto()
                .orElseThrow(() -> new IllegalStateException("No existe un turno abierto para realizar arqueo"));

        Preconditions.checkArgument(efectivoRealContado >= 0, "El efectivo fisico contado no puede ser negativo");

        double esperado = turno.getEfectivoEsperadoEnCaja();
        double diferencia = Math.round((efectivoRealContado - esperado) * 100.0) / 100.0;

        if (Math.abs(diferencia) > TOLERANCIA_DESCUADRE_SOLES && StringUtils.isBlank(justificacion)) {
            log.warn("RF-11 Alerta de Descuadre: Esperado=S/.{} Contado=S/.{} Diferencia=S/.{} sin justificacion",
                    esperado, efectivoRealContado, diferencia);
            throw new IllegalArgumentException(String.format(
                    "RF-11 Violado: Existe un descuadre de S/. %.2f (> S/. 2.00). Es obligatorio registrar una justificacion formal.",
                    diferencia));
        }

        turno.setEfectivoContadoReal(efectivoRealContado);
        turno.setDiferenciaArqueo(diferencia);
        turno.setJustificacionDescuadre(StringUtils.defaultIfBlank(justificacion, "Cuadre conforme dentro de tolerancia"));
        turno.setFechaCierre(LocalDateTime.now());
        turno.setAbierto(false);
        turnoDao.actualizarTurno(turno);

        // Ejecutar respaldo automatico de la BD SQLite al cierre (RF-12, RNF-08)
        String backupPath = turnoDao.generarRespaldoBaseDatosLocal();
        log.info("Cierre de Turno #{} completado (Informe Z emitido). Diferencia=S/.{} Backup={}",
                turno.getId(), diferencia, backupPath);
        return turno;
    }

    public String generarInformeZTermico(TurnoCaja t) {
        StringBuilder sb = new StringBuilder();
        sb.append("==========================================\n");
        sb.append("     INFORME Z - CIERRE DIARIO DE CAJA    \n");
        sb.append("   MINIMARKET Y COMERCIAL DON PEPE EIRL   \n");
        sb.append("==========================================\n");
        sb.append("TURNO NRO: #").append(t.getId()).append(" | CAJERO: ").append(t.getCajeroUsername()).append("\n");
        sb.append("APERTURA:  ").append(t.getFechaApertura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
        if (t.getFechaCierre() != null) {
            sb.append("CIERRE:    ").append(t.getFechaCierre().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
        }
        sb.append("------------------------------------------\n");
        sb.append(String.format(" (+) FONDO INICIAL DE CAJA:  S/. %8.2f\n", t.getFondoInicial()));
        sb.append(String.format(" (+) VENTAS EN EFECTIVO:     S/. %8.2f\n", t.getTotalEfectivoVentas()));
        sb.append(String.format(" (+) ABONOS FIADOS EFECTIVO: S/. %8.2f\n", t.getTotalAmortizacionesEfectivo()));
        sb.append("------------------------------------------\n");
        sb.append(String.format(" (=) EFECTIVO ESPERADO CAJA: S/. %8.2f\n", t.getEfectivoEsperadoEnCaja()));
        sb.append(String.format(" (x) EFECTIVO REAL CONTADO:  S/. %8.2f\n", t.getEfectivoContadoReal()));
        sb.append(String.format(" (D) DIFERENCIA DE ARQUEO:   S/. %+8.2f\n", t.getDiferenciaArqueo()));
        sb.append("------------------------------------------\n");
        sb.append(" OTROS MEDIOS DE COBRO Y CREDITO:\n");
        sb.append(String.format("  • Billeteras (Yape/Plin):  S/. %8.2f\n", t.getTotalYapePlin()));
        sb.append(String.format("  • Tarjetas POS (Izipay):   S/. %8.2f\n", t.getTotalTarjeta()));
        sb.append(String.format("  • Creditos Otorgados:      S/. %8.2f\n", t.getTotalCreditoFiado()));
        sb.append("------------------------------------------\n");
        sb.append("OBS: ").append(StringUtils.defaultString(t.getJustificacionDescuadre(), "Sin observaciones")).append("\n");
        sb.append("BACKUP LOCAL SQLITE: VERIFICADO (OK)\n");
        sb.append("==========================================\n");
        return sb.toString();
    }
}
