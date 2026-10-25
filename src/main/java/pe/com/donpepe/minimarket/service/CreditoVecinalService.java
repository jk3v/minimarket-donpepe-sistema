package pe.com.donpepe.minimarket.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.com.donpepe.minimarket.dao.ClienteVecinalDao;
import pe.com.donpepe.minimarket.dao.TurnoCajaDao;
import pe.com.donpepe.minimarket.model.AmortizacionCredito;
import pe.com.donpepe.minimarket.model.ClienteVecinal;
import pe.com.donpepe.minimarket.model.TurnoCaja;
import pe.com.donpepe.minimarket.security.SecurityService;

import java.time.LocalDateTime;

/**
 * Servicio de Gestion de Creditos Vecinales ("Cuaderno Digital de Fiados") y Amortizaciones (RF-08, RF-09).
 * Autor: Equipo de Desarrollo / DevTeam
 */
public class CreditoVecinalService {
    private static final Logger log = LoggerFactory.getLogger(CreditoVecinalService.class);

    private final ClienteVecinalDao clienteDao;
    private final TurnoCajaDao turnoDao;

    public CreditoVecinalService(ClienteVecinalDao clienteDao, TurnoCajaDao turnoDao) {
        this.clienteDao = Preconditions.checkNotNull(clienteDao, "ClienteVecinalDao requerido");
        this.turnoDao = Preconditions.checkNotNull(turnoDao, "TurnoCajaDao requerido");
    }

    public ClienteVecinal validarYCargarFiado(String dni, double montoFiado) {
        SecurityService.validarDniPeruano(dni);
        Preconditions.checkArgument(montoFiado > 0, "El monto a fiar debe ser mayor a S/. 0.00");

        ClienteVecinal cliente = clienteDao.buscarPorDni(dni)
                .orElseThrow(() -> new IllegalArgumentException("Vecino no registrado en padron de creditos: DNI " + dni));

        Preconditions.checkState(!cliente.isBloqueadoCredito(),
                "RF-08: El cliente " + cliente.getNombresApellidos() + " tiene su linea de credito bloqueada por morosidad.");

        double nuevoSaldo = Math.round((cliente.getDeudaActual() + montoFiado) * 100.0) / 100.0;
        if (nuevoSaldo > cliente.getLimiteCredito()) {
            log.warn("RF-08 Bloqueo de Tope de Credito: DNI={} DeudaActual=S/.{} IntentoFiado=S/.{} Limite=S/.{}",
                    SecurityService.maskDni(dni), cliente.getDeudaActual(), montoFiado, cliente.getLimiteCredito());
            throw new IllegalStateException(String.format(
                    "RF-08 Violado: El monto solicitado (S/. %.2f) excede el credito disponible (S/. %.2f) de %s.",
                    montoFiado, cliente.getCreditoDisponible(), cliente.getNombresApellidos()));
        }

        cliente.setDeudaActual(nuevoSaldo);
        clienteDao.actualizarCliente(cliente);
        log.info("Cargo de fiado vecinal autorizado: DNI={} Monto=S/.{} NuevoSaldoDeudor=S/.{}",
                SecurityService.maskDni(dni), montoFiado, nuevoSaldo);
        return cliente;
    }

    public AmortizacionCredito registrarAmortizacion(String dni, double montoAbono, String medioPago, String observacion) {
        SecurityService.validarDniPeruano(dni);
        Preconditions.checkArgument(montoAbono > 0, "El monto de amortizacion debe ser mayor a S/. 0.00");
        Preconditions.checkArgument(StringUtils.isNotBlank(medioPago), "Debe especificar el medio de pago del abono");

        TurnoCaja turnoAbierto = turnoDao.obtenerTurnoAbierto()
                .orElseThrow(() -> new IllegalStateException("RF-02: Debe tener un turno de caja abierto para recibir abonos de clientes."));

        ClienteVecinal cliente = clienteDao.buscarPorDni(dni)
                .orElseThrow(() -> new IllegalArgumentException("Vecino no encontrado con DNI: " + dni));

        Preconditions.checkArgument(cliente.getDeudaActual() > 0, "El cliente no registra deuda pendiente.");
        Preconditions.checkArgument(montoAbono <= cliente.getDeudaActual(),
                "El abono (S/. " + montoAbono + ") no puede superar la deuda actual (S/. " + cliente.getDeudaActual() + ")");

        double saldoAnterior = cliente.getDeudaActual();
        double saldoRestante = Math.round((saldoAnterior - montoAbono) * 100.0) / 100.0;
        cliente.setDeudaActual(saldoRestante);
        clienteDao.actualizarCliente(cliente);

        if ("EFECTIVO".equalsIgnoreCase(medioPago)) {
            turnoAbierto.addAmortizacionEfectivo(montoAbono);
        } else if ("YAPE/PLIN".equalsIgnoreCase(medioPago)) {
            turnoAbierto.addYapePlin(montoAbono);
        }
        turnoDao.actualizarTurno(turnoAbierto);

        AmortizacionCredito amortizacion = new AmortizacionCredito(
                clienteDao.listarAmortizacionesPorDni(dni).size() + 1,
                dni,
                turnoAbierto.getId(),
                LocalDateTime.now(),
                montoAbono,
                medioPago.toUpperCase(),
                saldoAnterior,
                saldoRestante,
                StringUtils.defaultIfBlank(observacion, "Amortizacion en mostrador")
        );
        clienteDao.registrarAmortizacion(amortizacion);
        log.info("Amortizacion registrada (RF-09): DNI={} Abono=S/.{} SaldoAnterior=S/.{} SaldoRestante=S/.{}",
                SecurityService.maskDni(dni), montoAbono, saldoAnterior, saldoRestante);
        return amortizacion;
    }

    public ImmutableList<ClienteVecinal> listarPadronClientes() {
        return ImmutableList.copyOf(clienteDao.listarClientes());
    }
}
