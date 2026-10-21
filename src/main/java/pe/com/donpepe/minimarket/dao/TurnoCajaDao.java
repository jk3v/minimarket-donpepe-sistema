package pe.com.donpepe.minimarket.dao;

import pe.com.donpepe.minimarket.model.TurnoCaja;
import java.util.Optional;

public interface TurnoCajaDao {
    TurnoCaja abrirTurno(String cajeroUsername, double fondoInicial);
    Optional<TurnoCaja> obtenerTurnoAbierto();
    void actualizarTurno(TurnoCaja turno);
    String generarRespaldoBaseDatosLocal();
}
