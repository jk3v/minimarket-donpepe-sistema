package pe.com.donpepe.minimarket.dao;

import pe.com.donpepe.minimarket.model.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioDao {
    Optional<Usuario> buscarPorUsername(String username);
    void guardar(Usuario usuario);
    void actualizar(Usuario usuario);
    List<Usuario> listarUsuarios();
}
