package io.jettra.example.microservices.model;

/**
 * Ítem o línea de detalle inmutable dentro de una {@link Factura}.
 */
public record DetalleFactura(
        String sku,
        String descripcion,
        int cantidad,
        double precioUnitario,
        double subtotal
) {
    public static DetalleFactura of(String sku, String descripcion, int cantidad, double precioUnitario) {
        double sub = Math.round(cantidad * precioUnitario * 100.0) / 100.0;
        return new DetalleFactura(sku, descripcion, cantidad, precioUnitario, sub);
    }
}
