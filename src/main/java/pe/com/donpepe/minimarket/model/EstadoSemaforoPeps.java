package pe.com.donpepe.minimarket.model;

/**
 * Clasificacion del Semaforo de Caducidad PEPS segun RF-05:
 * - ROJO: < 15 dias para vencer (Critico - requiere promocion inmediata o retiro)
 * - AMARILLO: 15 a 30 dias para vencer (Preventivo - priorizar salida en gondola)
 * - VERDE: > 30 dias para vencer (Optimo)
 * - VENCIDO: <= 0 dias (Bloqueado para venta, debe pasar a Merma RF-10)
 */
public enum EstadoSemaforoPeps {
    VENCIDO("VENCIDO (<= 0 dias)", "#8B0000"),
    ROJO("ROJO (< 15 dias - Critico)", "#DC2626"),
    AMARILLO("AMARILLO (15-30 dias - Preventivo)", "#D97706"),
    VERDE("VERDE (> 30 dias - Optimo)", "#16A34A");

    private final String etiqueta;
    private final String colorHex;

    EstadoSemaforoPeps(String etiqueta, String colorHex) {
        this.etiqueta = etiqueta;
        this.colorHex = colorHex;
    }

    public String getEtiqueta() { return etiqueta; }
    public String getColorHex() { return colorHex; }
}
