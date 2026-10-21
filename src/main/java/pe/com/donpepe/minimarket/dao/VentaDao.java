package pe.com.donpepe.minimarket.dao;

import pe.com.donpepe.minimarket.model.Venta;
import java.util.List;

public interface VentaDao {
    void registrarVentaTransaccional(Venta venta);
    List<Venta> listarPorTurno(int turnoId);
    List<Venta> listarTodas();
}
