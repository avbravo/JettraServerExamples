package io.jettra.server.example.model;

/**
 * Ítem o línea de detalle inmutable de una factura ({@code example_factura_db}).
 */
public record DetalleFacturaDto(
        String sku,
        String descripcion,
        int cantidad,
        double precioUnitario,
        double subtotal
) {
    public static DetalleFacturaDto of(String sku, String descripcion, int cantidad, double precioUnitario) {
        double sub = Math.round(cantidad * precioUnitario * 100.0) / 100.0;
        return new DetalleFacturaDto(sku, descripcion, cantidad, precioUnitario, sub);
    }
}
