package pe.com.donpepe.minimarket.model;

public class DetalleVenta {
    private String codigoBarras;
    private String nombreProducto;
    private String numeroLotePeps;
    private int cantidad;
    private double precioUnitario;
    private double descuentoUnitario;
    private double subtotalLinea;

    public DetalleVenta(String codigoBarras, String nombreProducto, String numeroLotePeps,
                        int cantidad, double precioUnitario, double descuentoUnitario) {
        this.codigoBarras = codigoBarras;
        this.nombreProducto = nombreProducto;
        this.numeroLotePeps = numeroLotePeps;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.descuentoUnitario = descuentoUnitario;
        this.subtotalLinea = Math.round(cantidad * (precioUnitario - descuentoUnitario) * 100.0) / 100.0;
    }

    public String getCodigoBarras() { return codigoBarras; }
    public String getNombreProducto() { return nombreProducto; }
    public String getNumeroLotePeps() { return numeroLotePeps; }
    public int getCantidad() { return cantidad; }
    public double getPrecioUnitario() { return precioUnitario; }
    public double getDescuentoUnitario() { return descuentoUnitario; }
    public double getSubtotalLinea() { return subtotalLinea; }
}
