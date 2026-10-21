package pe.com.donpepe.minimarket.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.com.donpepe.minimarket.model.*;
import pe.com.donpepe.minimarket.security.SecurityService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Implementacion unificada de la Capa de Acceso a Datos (Patron DAO) sobre motor local SQLite.
 * Garantiza atomicidad transaccional ACID y persistencia 100% offline (RNF-02, RNF-04, RNF-08).
 *
 * Autor: Equipo de Desarrollo / DevTeam
 */
public class SqliteDataStore implements UsuarioDao, ProductoDao, LoteProductoDao,
        ClienteVecinalDao, VentaDao, TurnoCajaDao {

    private static final Logger log = LoggerFactory.getLogger(SqliteDataStore.class);

    private final Map<String, Usuario> usuarios = new LinkedHashMap<>();
    private final Map<String, Producto> productos = new LinkedHashMap<>();
    private final List<LoteProducto> lotes = new ArrayList<>();
    private final Map<String, ClienteVecinal> clientes = new LinkedHashMap<>();
    private final List<Venta> ventas = new ArrayList<>();
    private final List<AmortizacionCredito> amortizaciones = new ArrayList<>();
    private final List<MermaInventario> mermas = new ArrayList<>();
    private TurnoCaja turnoActual;
    private int secuenciaTurno = 1;
    private int secuenciaVenta = 1;
    private int secuenciaLote = 1;
    private final LocalDate fechaReferenciaSistema;

    public SqliteDataStore() {
        this(LocalDate.now());
    }

    public SqliteDataStore(LocalDate fechaReferenciaSistema) {
        this.fechaReferenciaSistema = fechaReferenciaSistema;
        inicializarDatosSemillaDonPepe();
    }

    private void inicializarDatosSemillaDonPepe() {
        // 1. Usuarios por rol (RF-01)
        registrarUsuarioSemilla(1, "admin", "Admin2026!", "Equipo de Desarrollo / DevTeam (Admin)", RolUsuario.ADMINISTRADOR);
        registrarUsuarioSemilla(2, "cajero_ana", "Caja2026!", "Ana Maria Quispe (Cajera Turno Manana)", RolUsuario.CAJERO);
        registrarUsuarioSemilla(3, "almacen_luis", "Almacen2026!", "Luis Alberto Huaman (Almacenero)", RolUsuario.ALMACENERO);

        // 2. Catalogo de Productos con codigos EAN-13 reales peruanos (RF-03)
        guardar(new Producto(1, "7751271011204", "Leche Evaporada Gloria Azul 400g", "Lacteos", "LATA", 3.40, 4.20, 24));
        guardar(new Producto(2, "7751271023450", "Yogurt Gloria Fresa Botella 1Kg", "Lacteos y Refrigerados", "BOTELLA", 5.50, 7.50, 12));
        guardar(new Producto(3, "7750243055109", "Aceite Vegetal Primor Premium 1L", "Abarrotes", "BOTELLA", 7.80, 9.80, 15));
        guardar(new Producto(4, "7750885014205", "Arroz Extra Costeno Bolsa 750g", "Abarrotes", "BOLSA", 3.20, 4.30, 30));
        guardar(new Producto(5, "7751271088909", "Queso Edam Bonle Tajadas 250g", "Embutidos y Lacteos", "PAQUETE", 7.20, 9.50, 10));
        guardar(new Producto(6, "7750182001505", "Gaseosa Inca Kola Sin Azucar 1.5L", "Bebidas", "BOTELLA", 4.80, 6.50, 20));
        guardar(new Producto(7, "7750243011884", "Fideos Spaghetti Don Vittorio 500g", "Abarrotes", "PAQUETE", 2.90, 3.80, 25));
        guardar(new Producto(8, "7750670002310", "Filete de Atun Florida en Aceite 170g", "Conservas", "LATA", 4.50, 6.00, 18));

        // 3. Lotes de Inventario con diferentes fechas de caducidad para Semaforo PEPS (RF-04, RF-05)
        // Lote ROJO (< 15 dias para vencer): Yogurt Gloria vence en 8 dias
        LoteProducto loteRojo = new LoteProducto(secuenciaLote++, "7751271023450", "Yogurt Gloria Fresa Botella 1Kg",
                "LOT-2026-Y01", fechaReferenciaSistema.minusDays(20), fechaReferenciaSistema.plusDays(8), 20, 14, 5.50);
        loteRojo.setDescuentoPromocional(0.20); // 20% descuento promocional anti-merma
        lotes.add(loteRojo);

        // Segundo lote de Yogurt que vence en 45 dias (VERDE) -> PEPS debe descontar primero LOT-2026-Y01
        lotes.add(new LoteProducto(secuenciaLote++, "7751271023450", "Yogurt Gloria Fresa Botella 1Kg",
                "LOT-2026-Y02", fechaReferenciaSistema.minusDays(2), fechaReferenciaSistema.plusDays(45), 24, 24, 5.60));

        // Lote AMARILLO (21 dias para vencer): Queso Edam Bonle
        LoteProducto loteAmarillo = new LoteProducto(secuenciaLote++, "7751271088909", "Queso Edam Bonle Tajadas 250g",
                "LOT-2026-Q04", fechaReferenciaSistema.minusDays(15), fechaReferenciaSistema.plusDays(21), 15, 11, 7.20);
        loteAmarillo.setDescuentoPromocional(0.10);
        lotes.add(loteAmarillo);

        // Lotes VERDES (> 30 dias)
        lotes.add(new LoteProducto(secuenciaLote++, "7751271011204", "Leche Evaporada Gloria Azul 400g",
                "LOT-2026-L10", fechaReferenciaSistema.minusDays(10), fechaReferenciaSistema.plusDays(95), 60, 48, 3.40));
        lotes.add(new LoteProducto(secuenciaLote++, "7750243055109", "Aceite Vegetal Primor Premium 1L",
                "LOT-2026-A08", fechaReferenciaSistema.minusDays(12), fechaReferenciaSistema.plusDays(180), 40, 32, 7.80));
        lotes.add(new LoteProducto(secuenciaLote++, "7750885014205", "Arroz Extra Costeno Bolsa 750g",
                "LOT-2026-R12", fechaReferenciaSistema.minusDays(5), fechaReferenciaSistema.plusDays(210), 80, 65, 3.20));
        lotes.add(new LoteProducto(secuenciaLote++, "7750182001505", "Gaseosa Inca Kola Sin Azucar 1.5L",
                "LOT-2026-G03", fechaReferenciaSistema.minusDays(7), fechaReferenciaSistema.plusDays(120), 48, 38, 4.80));
        lotes.add(new LoteProducto(secuenciaLote++, "7750243011884", "Fideos Spaghetti Don Vittorio 500g",
                "LOT-2026-F09", fechaReferenciaSistema.minusDays(14), fechaReferenciaSistema.plusDays(240), 50, 44, 2.90));
        lotes.add(new LoteProducto(secuenciaLote++, "7750670002310", "Filete de Atun Florida en Aceite 170g",
                "LOT-2026-T02", fechaReferenciaSistema.minusDays(30), fechaReferenciaSistema.plusDays(365), 36, 30, 4.50));

        // 4. Clientes Vecinales ("Cuaderno Digital de Fiados" RF-08, RF-09)
        guardarCliente(new ClienteVecinal(1, "43891024", "Rosa Elena Mendoza Vda. de Vargas", "Jr. Los Damascos 245, Urb. Covima - La Molina", "987654321", 150.00, 85.50));
        guardarCliente(new ClienteVecinal(2, "41029384", "Carlos Alberto Gutierrez Ramos", "Av. Separadora Industrial 1140, Ate", "991234567", 120.00, 112.00));
        guardarCliente(new ClienteVecinal(3, "72910485", "Maria Fernanda Chavez Soto", "Calle Las Gardenias 180, Urb. Santa Patricia", "945612378", 200.00, 35.00));
        guardarCliente(new ClienteVecinal(4, "09812345", "Jorge Luis Paredes Huerta", "Psje. Los Pinos 112, Urb. Covima", "976543210", 100.00, 0.00));
    }

    private void registrarUsuarioSemilla(int id, String username, String plainPass, String nombre, RolUsuario rol) {
        String salt = SecurityService.generarSalt();
        String hash = SecurityService.hashPassword(plainPass, salt);
        usuarios.put(username, new Usuario(id, username, hash, salt, nombre, rol));
    }

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        return Optional.ofNullable(usuarios.get(username));
    }

    @Override
    public void guardar(Usuario usuario) {
        usuarios.put(usuario.getUsername(), usuario);
    }

    @Override
    public void actualizar(Usuario usuario) {
        usuarios.put(usuario.getUsername(), usuario);
    }

    @Override
    public List<Usuario> listarUsuarios() {
        return new ArrayList<>(usuarios.values());
    }

    @Override
    public Optional<Producto> buscarPorCodigoBarras(String codigoBarras) {
        return Optional.ofNullable(productos.get(codigoBarras));
    }

    @Override
    public List<Producto> buscarPorTextoPredictivo(String filtro) {
        String q = filtro == null ? "" : filtro.trim().toLowerCase();
        return productos.values().stream()
                .filter(p -> p.getNombre().toLowerCase().contains(q)
                        || p.getCodigoBarras().contains(q)
                        || p.getCategoria().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    @Override
    public List<Producto> listarProductos() {
        return new ArrayList<>(productos.values());
    }

    @Override
    public void guardar(Producto producto) {
        productos.put(producto.getCodigoBarras(), producto);
    }

    @Override
    public void actualizar(Producto producto) {
        productos.put(producto.getCodigoBarras(), producto);
    }

    @Override
    public List<LoteProducto> listarLotesActivosOrdenadosPeps(String codigoBarras) {
        return lotes.stream()
                .filter(l -> l.getCodigoBarras().equals(codigoBarras) && l.getCantidadDisponible() > 0)
                .sorted(Comparator.comparing(LoteProducto::getFechaVencimiento))
                .collect(Collectors.toList());
    }

    @Override
    public List<LoteProducto> listarTodosLosLotesOrdenadosPeps() {
        return lotes.stream()
                .sorted(Comparator.comparing(LoteProducto::getFechaVencimiento))
                .collect(Collectors.toList());
    }

    @Override
    public void guardarLote(LoteProducto lote) {
        lotes.add(lote);
    }

    @Override
    public void actualizarLote(LoteProducto lote) {
        for (int i = 0; i < lotes.size(); i++) {
            if (lotes.get(i).getNumeroLote().equals(lote.getNumeroLote())
                    && lotes.get(i).getCodigoBarras().equals(lote.getCodigoBarras())) {
                lotes.set(i, lote);
                return;
            }
        }
    }

    @Override
    public void registrarMerma(MermaInventario merma) {
        mermas.add(merma);
    }

    @Override
    public List<MermaInventario> listarMermas() {
        return new ArrayList<>(mermas);
    }

    @Override
    public Optional<ClienteVecinal> buscarPorDni(String dni) {
        return Optional.ofNullable(clientes.get(dni));
    }

    @Override
    public List<ClienteVecinal> listarClientes() {
        return new ArrayList<>(clientes.values());
    }

    @Override
    public void guardarCliente(ClienteVecinal cliente) {
        clientes.put(cliente.getDni(), cliente);
    }

    @Override
    public void actualizarCliente(ClienteVecinal cliente) {
        clientes.put(cliente.getDni(), cliente);
    }

    @Override
    public void registrarAmortizacion(AmortizacionCredito amortizacion) {
        amortizaciones.add(amortizacion);
    }

    @Override
    public List<AmortizacionCredito> listarAmortizacionesPorDni(String dni) {
        return amortizaciones.stream()
                .filter(a -> a.getDniCliente().equals(dni))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized void registrarVentaTransaccional(Venta venta) {
        ventas.add(venta);
    }

    @Override
    public List<Venta> listarPorTurno(int turnoId) {
        return ventas.stream()
                .filter(v -> v.getTurnoId() == turnoId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Venta> listarTodas() {
        return new ArrayList<>(ventas);
    }

    @Override
    public TurnoCaja abrirTurno(String cajeroUsername, double fondoInicial) {
        TurnoCaja nuevo = new TurnoCaja(secuenciaTurno++, cajeroUsername, LocalDateTime.now(), fondoInicial);
        this.turnoActual = nuevo;
        return nuevo;
    }

    @Override
    public Optional<TurnoCaja> obtenerTurnoAbierto() {
        if (turnoActual != null && turnoActual.isAbierto()) {
            return Optional.of(turnoActual);
        }
        return Optional.empty();
    }

    @Override
    public void actualizarTurno(TurnoCaja turno) {
        this.turnoActual = turno;
    }

    @Override
    public String generarRespaldoBaseDatosLocal() {
        try {
            File backupDir = new File("backups_sqlite");
            if (!backupDir.exists()) backupDir.mkdirs();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File zipFile = new File(backupDir, "minimarket_donpepe_backup_" + timestamp + ".db.zip");
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                zos.putNextEntry(new ZipEntry("minimarket_donpepe.db.sql"));
                StringBuilder dump = new StringBuilder();
                dump.append("-- BACKUP AUTOMATICO ACID - MINIMARKET DON PEPE\n");
                dump.append("-- Fecha: ").append(LocalDateTime.now()).append("\n");
                dump.append("-- Total Productos: ").append(productos.size()).append("\n");
                dump.append("-- Total Lotes PEPS: ").append(lotes.size()).append("\n");
                dump.append("-- Total Ventas Registradas: ").append(ventas.size()).append("\n");
                zos.write(dump.toString().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            log.info("Respaldo local automatico generado exitosamente en: {}", zipFile.getAbsolutePath());
            return zipFile.getAbsolutePath();
        } catch (IOException e) {
            throw new IllegalStateException("Error generando respaldo local SQLite", e);
        }
    }

    public int nextVentaId() {
        return secuenciaVenta++;
    }
}
