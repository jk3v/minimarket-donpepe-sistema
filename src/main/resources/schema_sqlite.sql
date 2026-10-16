-- =================================================================================
-- ESQUEMA RELACIONAL NORMALIZADO (3FN) - MINIMARKET Y COMERCIAL DON PEPE E.I.R.L.
-- Motor: SQLite 3 (WAL Mode, Transacciones ACID 100% Offline)
-- Autor: Equipo de Desarrollo / DevTeam
-- =================================================================================

PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;

CREATE TABLE IF NOT EXISTS USUARIOS (
    id_usuario INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(40) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    nombre_completo VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('ADMINISTRADOR', 'CAJERO', 'ALMACENERO')),
    intentos_fallidos INTEGER NOT NULL DEFAULT 0,
    bloqueado INTEGER NOT NULL DEFAULT 0,
    activo INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS PRODUCTOS (
    id_producto INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo_barras VARCHAR(13) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    unidad_medida VARCHAR(20) NOT NULL,
    precio_costo DECIMAL(10,2) NOT NULL CHECK (precio_costo > 0),
    precio_venta DECIMAL(10,2) NOT NULL CHECK (precio_venta >= precio_costo),
    stock_minimo INTEGER NOT NULL DEFAULT 5,
    activo INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_productos_ean13 ON PRODUCTOS(codigo_barras);

CREATE TABLE IF NOT EXISTS LOTES_PRODUCTO (
    id_lote INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo_barras VARCHAR(13) NOT NULL,
    numero_lote VARCHAR(40) NOT NULL,
    fecha_ingreso DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    cantidad_inicial INTEGER NOT NULL CHECK (cantidad_inicial > 0),
    cantidad_disponible INTEGER NOT NULL CHECK (cantidad_disponible >= 0),
    costo_unitario DECIMAL(10,2) NOT NULL,
    descuento_promocional DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    FOREIGN KEY (codigo_barras) REFERENCES PRODUCTOS(codigo_barras)
);

CREATE INDEX IF NOT EXISTS idx_lotes_vencimiento ON LOTES_PRODUCTO(codigo_barras, fecha_vencimiento ASC);

CREATE TABLE IF NOT EXISTS CLIENTES_VECINALES (
    id_cliente INTEGER PRIMARY KEY AUTOINCREMENT,
    dni VARCHAR(8) NOT NULL UNIQUE,
    nombres_apellidos VARCHAR(120) NOT NULL,
    direccion VARCHAR(150) NOT NULL,
    telefono VARCHAR(15) NOT NULL,
    limite_credito DECIMAL(10,2) NOT NULL CHECK (limite_credito >= 0),
    deuda_actual DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    bloqueado_credito INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_clientes_dni ON CLIENTES_VECINALES(dni);

CREATE TABLE IF NOT EXISTS TURNOS_CAJA (
    id_turno INTEGER PRIMARY KEY AUTOINCREMENT,
    cajero_username VARCHAR(40) NOT NULL,
    fecha_apertura DATETIME NOT NULL,
    fecha_cierre DATETIME,
    fondo_inicial DECIMAL(10,2) NOT NULL,
    total_efectivo DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_yape_plin DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_tarjeta DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_credito_fiado DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_amortizaciones_efectivo DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    efectivo_contado_real DECIMAL(10,2),
    diferencia_arqueo DECIMAL(10,2),
    justificacion_descuadre VARCHAR(255),
    estado VARCHAR(15) NOT NULL CHECK (estado IN ('ABIERTO', 'CERRADO'))
);

CREATE TABLE IF NOT EXISTS VENTAS (
    id_venta INTEGER PRIMARY KEY AUTOINCREMENT,
    numero_comprobante VARCHAR(20) NOT NULL UNIQUE,
    id_turno INTEGER NOT NULL,
    cajero_username VARCHAR(40) NOT NULL,
    dni_cliente VARCHAR(8),
    fecha_hora DATETIME NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    igv DECIMAL(10,2) NOT NULL,
    descuento_total DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total DECIMAL(10,2) NOT NULL,
    monto_efectivo DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    monto_yape_plin DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    monto_tarjeta DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    monto_fiado DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    vuelto DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    FOREIGN KEY (id_turno) REFERENCES TURNOS_CAJA(id_turno)
);

CREATE TABLE IF NOT EXISTS DETALLE_VENTAS (
    id_detalle INTEGER PRIMARY KEY AUTOINCREMENT,
    id_venta INTEGER NOT NULL,
    codigo_barras VARCHAR(13) NOT NULL,
    nombre_producto VARCHAR(120) NOT NULL,
    numero_lote_peps VARCHAR(40) NOT NULL,
    cantidad INTEGER NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    descuento_aplicado DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    subtotal_linea DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (id_venta) REFERENCES VENTAS(id_venta)
);

CREATE TABLE IF NOT EXISTS AMORTIZACIONES_CREDITO (
    id_amortizacion INTEGER PRIMARY KEY AUTOINCREMENT,
    dni_cliente VARCHAR(8) NOT NULL,
    id_turno INTEGER NOT NULL,
    fecha_hora DATETIME NOT NULL,
    monto_abonado DECIMAL(10,2) NOT NULL CHECK (monto_abonado > 0),
    medio_pago VARCHAR(25) NOT NULL,
    saldo_anterior DECIMAL(10,2) NOT NULL,
    saldo_restante DECIMAL(10,2) NOT NULL,
    observacion VARCHAR(150),
    FOREIGN KEY (dni_cliente) REFERENCES CLIENTES_VECINALES(dni)
);

CREATE TABLE IF NOT EXISTS MERMAS_INVENTARIO (
    id_merma INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo_barras VARCHAR(13) NOT NULL,
    numero_lote VARCHAR(40) NOT NULL,
    cantidad_baja INTEGER NOT NULL CHECK (cantidad_baja > 0),
    motivo VARCHAR(150) NOT NULL,
    costo_perdida DECIMAL(10,2) NOT NULL,
    usuario_registro VARCHAR(40) NOT NULL,
    fecha_hora DATETIME NOT NULL
);
