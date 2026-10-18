package pe.com.donpepe.minimarket.model;

public class Producto {
    private int id;
    private String codigoBarras;
    private String nombre;
    private String categoria;
    private String unidadMedida;
    private double precioCosto;
    private double precioVenta;
    private int stockMinimo;
    private boolean activo;

    public Producto(int id, String codigoBarras, String nombre, String categoria,
                    String unidadMedida, double precioCosto, double precioVenta, int stockMinimo) {
        this.id = id;
        this.codigoBarras = codigoBarras;
        this.nombre = nombre;
        this.categoria = categoria;
        this.unidadMedida = unidadMedida;
        this.precioCosto = precioCosto;
        this.precioVenta = precioVenta;
        this.stockMinimo = stockMinimo;
        this.activo = true;
    }

    public int getId() { return id; }
    public String getCodigoBarras() { return codigoBarras; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public String getUnidadMedida() { return unidadMedida; }
    public double getPrecioCosto() { return precioCosto; }
    public double getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(double precioVenta) { this.precioVenta = precioVenta; }
    public int getStockMinimo() { return stockMinimo; }
    public boolean isActivo() { return activo; }
    public double getMargenUnitario() { return Math.round((precioVenta - precioCosto) * 100.0) / 100.0; }
}
