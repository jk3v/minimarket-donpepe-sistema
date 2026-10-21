package pe.com.donpepe.minimarket.dao;

import pe.com.donpepe.minimarket.model.Producto;
import java.util.List;
import java.util.Optional;

public interface ProductoDao {
    Optional<Producto> buscarPorCodigoBarras(String codigoBarras);
    List<Producto> buscarPorTextoPredictivo(String filtro);
    List<Producto> listarProductos();
    void guardar(Producto producto);
    void actualizar(Producto producto);
}
