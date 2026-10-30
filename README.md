# Sistema de Gestión Comercial, Control de Inventarios PEPS y Créditos Vecinales — Minimarket Don Pepe E.I.R.L.

**Proyecto:** Sistema de Gestión Comercial y Control de Inventarios  
**Empresa Cliente:** Minimarket y Comercial Don Pepe E.I.R.L. (RUC: 20608912345 — La Molina / Ate, Lima)  
**Desarrollo:** DevTeam / Software Engineering  
**Licencia / Distribución:** Repositorio Académico Público

---

## 1. Arquitectura de Software y Patrones Aplicados

El proyecto implementa una arquitectura monolítica desacoplada en capas sobre **Java 17 LTS**, aplicando rigurosamente los siguientes pilares de diseño:

1. **Patrón MVC (Modelo - Vista - Controlador):**
   - **Modelo (`pe.com.donpepe.minimarket.model`):** Entidades fuertemente tipadas (`Usuario`, `Producto`, `LoteProducto`, `ClienteVecinal`, `Venta`, `DetalleVenta`, `AmortizacionCredito`, `MermaInventario`, `TurnoCaja`, `EstadoSemaforoPeps`, `RolUsuario`).
   - **Vista (`pe.com.donpepe.minimarket.view`):** Interfaz gráfica de escritorio (`MinimarketMainFrame`) con las 5 pantallas operativas y soporte de atajos de función `F1` a `F12`.
   - **Controlador (`pe.com.donpepe.minimarket.controller`):** `SistemaMinimarketController` que desacopla los eventos de interfaz respecto de las reglas transaccionales.
2. **Patrón DAO (Data Access Object):**
   - Interfaces segregadas en `pe.com.donpepe.minimarket.dao` (`UsuarioDao`, `ProductoDao`, `LoteProductoDao`, `ClienteVecinalDao`, `VentaDao`, `TurnoCajaDao`) e implementación transaccional `SqliteDataStore` sobre el esquema normalizado en 3FN (`src/main/resources/schema_sqlite.sql`).
3. **Principios SOLID:**
   - **S (Single Responsibility Principle):** Separación estricta entre `InventarioPepsService` (lotes y mermas), `VentaPosService` (facturación e IGV), `CreditoVecinalService` (fiados y abonos), `ArqueoCajaService` (turnos e Informe Z), `SecurityService` (cifrado y RBAC) y `ExcelReportExporterService` (reportes POI).
   - **O (Open/Closed Principle) & L (Liskov Substitution Principle):** Interfaz `PoliticaDescuentoStrategy` e implementación `DescuentoPepsAutomaticoStrategy` que liquida automáticamente lotes en Semáforo Rojo (`-20%`) sin alterar la clase `VentaPosService`.
   - **I (Interface Segregation Principle):** Interfaces DAO específicas por agregado de dominio en lugar de una interfaz monolítica.
   - **D (Dependency Inversion Principle):** Los servicios reciben las abstracciones DAO por inyección en constructor.
4. **Desarrollo Guiado por Pruebas — TDD (JUnit 5):**
   - Suite automatizada `MinimarketTddTestSuite` con **9 pruebas unitarias JUnit 5 (`@Test`, `@BeforeEach`, `@DisplayName`)** que validan el 100% de los Requisitos Funcionales (`RF-01` al `RF-12`).
5. **Seguridad de la Información:**
   - Cifrado de contraseñas con **SHA-256 + Salt aleatorio de 16 bytes y 12,000 iteraciones** (`SecurityService.hashPassword`).
   - **Bloqueo automático de cuenta al 3er intento fallido** (`RF-01`).
   - Control de acceso basado en roles (**RBAC**: `ADMINISTRADOR`, `CAJERO`, `ALMACENERO`).
   - Sanitización de entradas y **enmascaramiento de datos personales (PII: DNI `****9384`)** en las bitácoras de auditoría.

---

## 2. Librerías Java Incorporadas (`pom.xml`)

| Librería | Artefacto Maven | Uso Real en el Proyecto |
| :--- | :--- | :--- |
| **Google Guava** | `com.google.guava:guava:33.0.0-jre` | `CacheBuilder` y `Cache<String, Producto>` para búsqueda EAN-13 en `< 3ms` (`RNF-01`), `Preconditions.checkNotNull / checkArgument` y colecciones inmutables `ImmutableList`. |
| **Apache POI** | `org.apache.poi:poi-ooxml:5.2.5` | `XSSFWorkbook`, `Sheet`, `Row`, `CellStyle` en `ExcelReportExporterService` para exportar el Kardex PEPS y Padrón de Fiados a `.xlsx`. |
| **Apache Commons** | `commons-lang3:3.14.0` & `commons-collections4:4.4` | `StringUtils`, `Validate.notBlank` y `CollectionUtils.isNotEmpty` para validación y sanitización de cadenas, DNI y códigos de barras EAN-13. |
| **Logback + SLF4J** | `ch.qos.logback:logback-classic:1.4.14` | Auditoría estructurada en consola y archivo rotativo `logs/minimarket-donpepe-audit.log` configurada en `src/main/resources/logback.xml`. |

---

## 3. Cobertura del 100% de las Interfaces Gráficas (5 Pantallas)

1. **Pantalla 1 (`[F1]` Login y Apertura de Turno):** Autenticación RBAC y registro obligatorio de fondo inicial de sencillo (`RF-01`, `RF-02`).
2. **Pantalla 2 (`[F2]` Terminal POS y Pagos Mixtos):** Lectura EAN-13, asignación automática del lote más próximo a vencer (PEPS), cálculo de Subtotal/IGV (18%)/Total, cobro mixto (Efectivo + Yape/Plin + Tarjeta + Fiado) y emisión de Boleta en ticketera térmica de 80mm (`RF-06`, `RF-07`, `RF-08`).
3. **Pantalla 3 (`[F3]` Inventario PEPS, Semáforo y Mermas):** Semáforo cromático de caducidad (`ROJO < 15 días`, `AMARILLO 15-30 días`, `VERDE > 30 días`), recepción de lotes, baja de mermas y exportación a Excel con Apache POI (`RF-03`, `RF-04`, `RF-05`, `RF-10`).
4. **Pantalla 4 (`[F4]` Cuaderno Digital de Fiados):** Padrón de clientes vecinales por DNI, control automático de línea de crédito y registro de amortizaciones/abonos (`RF-08`, `RF-09`).
5. **Pantalla 5 (`[F5]` Arqueo de Caja, Cierre e Informe Z):** Cuadre ciego por medio de pago, validación de descuadres `> S/. 2.00`, emisión de Informe Z y respaldo automático `.db.zip` (`RF-11`, `RF-12`, `RNF-08`).
