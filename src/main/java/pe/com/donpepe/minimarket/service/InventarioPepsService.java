package pe.com.donpepe.minimarket.service;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.com.donpepe.minimarket.dao.LoteProductoDao;
import pe.com.donpepe.minimarket.dao.ProductoDao;
import pe.com.donpepe.minimarket.model.EstadoSemaforoPeps;
import pe.com.donpepe.minimarket.model.LoteProducto;
import pe.com.donpepe.minimarket.model.MermaInventario;
import pe.com.donpepe.minimarket.model.Producto;
import pe.com.donpepe.minimarket.security.SecurityService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Servicio de Gestion de Catalogo, Lotes PEPS, Semaforo de Caducidad y Mermas (RF-03, RF-04, RF-05, RF-10).
 * Incorpora:
 * - Google Guava: Cache en memoria de alta velocidad (< 5ms RNF-01), Preconditions e ImmutableList.
 * - Apache Commons: Validaciones con Validate, StringUtils y CollectionUtils.
 * - Logback: Trazabilidad de movimientos de almacen y bajas de merma.
 *
 * Autor: Equipo de Desarrollo / DevTeam
 */
public class InventarioPepsService {
    private static final Logger log = LoggerFactory.getLogger(InventarioPepsService.class);

    private final ProductoDao productoDao;
    private final LoteProductoDao loteDao;
    private final Cache<String, Producto> productoCacheEan13;
    private final LocalDate fechaReferencia;

    public InventarioPepsService(ProductoDao productoDao, LoteProductoDao loteDao, LocalDate fechaReferencia) {
        this.productoDao = Preconditions.checkNotNull(productoDao, "ProductoDao requerido");
        this.loteDao = Preconditions.checkNotNull(loteDao, "LoteProductoDao requerido");
        this.fechaReferencia = Preconditions.checkNotNull(fechaReferencia, "Fecha de referencia requerida");
        this.productoCacheEan13 = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }

    public Producto buscarPorEan13ConCache(String codigoBarras) {
        SecurityService.validarCodigoEan13(codigoBarras);
        Producto cached = productoCacheEan13.getIfPresent(codigoBarras);
        if (cached != null) {
            return cached;
        }
        Producto prod = productoDao.buscarPorCodigoBarras(codigoBarras)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado para EAN-13: " + codigoBarras));
        productoCacheEan13.put(codigoBarras, prod);
        return prod;
    }

    public void registrarNuevoProducto(Producto producto) {
        Preconditions.checkNotNull(producto, "El producto no puede ser nulo");
        SecurityService.validarCodigoEan13(producto.getCodigoBarras());
        Validate.notBlank(producto.getNombre(), "El nombre comercial del producto es obligatorio");
        Preconditions.checkArgument(producto.getPrecioCosto() > 0, "El precio de costo debe ser mayor a S/. 0.00");
        Preconditions.checkArgument(producto.getPrecioVenta() >= producto.getPrecioCosto(),
                "RF-03 Violado: El precio de venta (S/. " + producto.getPrecioVenta()
                        + ") no puede ser menor al costo (S/. " + producto.getPrecioCosto() + ")");
        Preconditions.checkArgument(productoDao.buscarPorCodigoBarras(producto.getCodigoBarras()).isEmpty(),
                "RF-03 Violado: Ya existe un producto con el codigo de barras " + producto.getCodigoBarras());

        productoDao.guardar(producto);
        productoCacheEan13.put(producto.getCodigoBarras(), producto);
        log.info("Producto registrado en catalogo: EAN={} Nombre='{}' P.Venta=S/.{}",
                producto.getCodigoBarras(), producto.getNombre(), producto.getPrecioVenta());
    }

    public void registrarEntradaLotePeps(LoteProducto nuevoLote) {
        Preconditions.checkNotNull(nuevoLote, "El lote no puede ser nulo");
        SecurityService.validarCodigoEan13(nuevoLote.getCodigoBarras());
        Validate.notBlank(nuevoLote.getNumeroLote(), "El codigo de lote es obligatorio");
        Preconditions.checkArgument(nuevoLote.getCantidadInicial() > 0, "La cantidad ingresada debe ser mayor a cero");
        Preconditions.checkArgument(nuevoLote.getFechaVencimiento().isAfter(fechaReferencia),
                "RF-04 Violado: No se puede ingresar mercaderia ya vencida al almacen");

        loteDao.guardarLote(nuevoLote);
        EstadoSemaforoPeps semaforo = nuevoLote.calcularEstadoSemaforo(fechaReferencia);
        log.info("Entrada de Lote PEPS registrada: Lote={} EAN={} Cant={} Vence={} Semaforo={}",
                nuevoLote.getNumeroLote(), nuevoLote.getCodigoBarras(),
                nuevoLote.getCantidadDisponible(), nuevoLote.getFechaVencimiento(), semaforo);
    }

    public LoteProducto obtenerLotePrioritarioPepsParaVenta(String codigoBarras, int cantidadRequerida) {
        SecurityService.validarCodigoEan13(codigoBarras);
        Preconditions.checkArgument(cantidadRequerida > 0, "La cantidad requerida debe ser positiva");

        List<LoteProducto> lotesActivos = loteDao.listarLotesActivosOrdenadosPeps(codigoBarras).stream()
                .filter(l -> l.calcularEstadoSemaforo(fechaReferencia) != EstadoSemaforoPeps.VENCIDO)
                .collect(Collectors.toList());

        Preconditions.checkState(CollectionUtils.isNotEmpty(lotesActivos),
                "Quiebre de stock: No existen lotes vigentes disponibles para el producto " + codigoBarras);

        int stockTotalDisponible = lotesActivos.stream().mapToInt(LoteProducto::getCantidadDisponible).sum();
        Preconditions.checkState(stockTotalDisponible >= cantidadRequerida,
                "Stock insuficiente para " + codigoBarras + ". Solicitado: " + cantidadRequerida + ", Disponible: " + stockTotalDisponible);

        return lotesActivos.get(0); // El primero en vencer (PEPS estricto)
    }

    public void descontarStockPeps(String codigoBarras, int cantidadRequerida) {
        List<LoteProducto> lotesVigentes = loteDao.listarLotesActivosOrdenadosPeps(codigoBarras).stream()
                .filter(l -> l.calcularEstadoSemaforo(fechaReferencia) != EstadoSemaforoPeps.VENCIDO)
                .collect(Collectors.toList());

        int pendiente = cantidadRequerida;
        for (LoteProducto lote : lotesVigentes) {
            if (pendiente <= 0) break;
            int tomar = Math.min(lote.getCantidadDisponible(), pendiente);
            lote.setCantidadDisponible(lote.getCantidadDisponible() - tomar);
            loteDao.actualizarLote(lote);
            pendiente -= tomar;
            log.info("Descuento PEPS ejecutado: Lote={} Tomado={} Restante={}",
                    lote.getNumeroLote(), tomar, lote.getCantidadDisponible());
        }
    }

    public MermaInventario registrarBajaPorMerma(String codigoBarras, String numeroLote,
                                                 int cantidadBaja, String motivo, String usuario) {
        SecurityService.validarCodigoEan13(codigoBarras);
        Preconditions.checkArgument(StringUtils.isNotBlank(motivo), "Debe especificar la justificacion formal de la merma (RF-10)");
        Preconditions.checkArgument(cantidadBaja > 0, "La cantidad de baja debe ser mayor a cero");

        LoteProducto loteAfectado = loteDao.listarTodosLosLotesOrdenadosPeps().stream()
                .filter(l -> l.getCodigoBarras().equals(codigoBarras) && l.getNumeroLote().equals(numeroLote))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado: " + numeroLote));

        Preconditions.checkArgument(loteAfectado.getCantidadDisponible() >= cantidadBaja,
                "La cantidad de merma supera el stock actual del lote");

        loteAfectado.setCantidadDisponible(loteAfectado.getCantidadDisponible() - cantidadBaja);
        loteDao.actualizarLote(loteAfectado);

        double costoPerdida = Math.round(cantidadBaja * loteAfectado.getCostoUnitario() * 100.0) / 100.0;
        MermaInventario merma = new MermaInventario(
                loteDao.listarMermas().size() + 1,
                codigoBarras,
                loteAfectado.getNombreProducto(),
                numeroLote,
                cantidadBaja,
                motivo,
                costoPerdida,
                usuario,
                LocalDateTime.now()
        );
        loteDao.registrarMerma(merma);
        log.warn("MERMA REGISTRADA (RF-10): Lote={} Producto='{}' Cant={} Perdida=S/.{} Motivo='{}'",
                numeroLote, loteAfectado.getNombreProducto(), cantidadBaja, costoPerdida, motivo);
        return merma;
    }

    public ImmutableList<LoteProducto> listarInventarioConSemaforoPeps() {
        return ImmutableList.copyOf(loteDao.listarTodosLosLotesOrdenadosPeps());
    }

    public int obtenerStockTotalVigentes(String codigoBarras) {
        return loteDao.listarLotesActivosOrdenadosPeps(codigoBarras).stream()
                .filter(l -> l.calcularEstadoSemaforo(fechaReferencia) != EstadoSemaforoPeps.VENCIDO)
                .mapToInt(LoteProducto::getCantidadDisponible)
                .sum();
    }

    public LocalDate getFechaReferencia() {
        return fechaReferencia;
    }
}
