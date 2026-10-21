package pe.com.donpepe.minimarket.dao;

import pe.com.donpepe.minimarket.model.LoteProducto;
import pe.com.donpepe.minimarket.model.MermaInventario;
import java.util.List;

public interface LoteProductoDao {
    List<LoteProducto> listarLotesActivosOrdenadosPeps(String codigoBarras);
    List<LoteProducto> listarTodosLosLotesOrdenadosPeps();
    void guardarLote(LoteProducto lote);
    void actualizarLote(LoteProducto lote);
    void registrarMerma(MermaInventario merma);
    List<MermaInventario> listarMermas();
}
