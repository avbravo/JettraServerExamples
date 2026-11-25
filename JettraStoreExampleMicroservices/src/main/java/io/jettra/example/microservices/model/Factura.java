package io.jettra.example.microservices.model;

import java.util.List;

/**
 * Entidad inmutable de Factura persistida en el bucket {@code Facturas} de {@code example_factura_db}.
 */
public record Factura(
        String jettraId,
        String folio,
        String clienteId,
        String clienteNombre,
        String fechaEmision,
        String estado,
        List<DetalleFactura> detalles,
        double subtotal,
        double impuesto,
        double total
) {
    public Factura withJettraId(String id) {
        return new Factura(id, folio, clienteId, clienteNombre, fechaEmision, estado, detalles, subtotal, impuesto, total);
    }

    public Factura withEstado(String nuevoEstado) {
        return new Factura(jettraId, folio, clienteId, clienteNombre, fechaEmision, nuevoEstado, detalles, subtotal, impuesto, total);
    }
}
