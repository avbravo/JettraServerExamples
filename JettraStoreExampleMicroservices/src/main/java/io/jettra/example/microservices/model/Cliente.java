package io.jettra.example.microservices.model;

/**
 * Entidad inmutable de Cliente almacenada en el bucket {@code Clientes} de {@code example_factura_db}.
 */
public record Cliente(
        String jettraId,
        String ruc,
        String nombre,
        String email,
        String categoria,
        boolean activo
) {
    public Cliente withJettraId(String id) {
        return new Cliente(id, ruc, nombre, email, categoria, activo);
    }
}
