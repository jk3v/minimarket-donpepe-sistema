package pe.com.donpepe.minimarket.dao;

import pe.com.donpepe.minimarket.model.AmortizacionCredito;
import pe.com.donpepe.minimarket.model.ClienteVecinal;
import java.util.List;
import java.util.Optional;

public interface ClienteVecinalDao {
    Optional<ClienteVecinal> buscarPorDni(String dni);
    List<ClienteVecinal> listarClientes();
    void guardarCliente(ClienteVecinal cliente);
    void actualizarCliente(ClienteVecinal cliente);
    void registrarAmortizacion(AmortizacionCredito amortizacion);
    List<AmortizacionCredito> listarAmortizacionesPorDni(String dni);
}
