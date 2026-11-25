package io.jettra.server.example.model;

/**
 * DTO inmutable de Cliente sincronizado con {@code JettraStoreExampleMicroservices} ({@code example_factura_db}).
 */
public record ClienteDto(
        String jettraId,
        String ruc,
        String nombre,
        String email,
        String categoria,
        boolean activo
) {
    public ClienteDto withJettraId(String id) {
        return new ClienteDto(id, ruc, nombre, email, categoria, activo);
    }

    public static ClienteDto fromMap(java.util.Map<?, ?> m) {
        if (m == null) return null;
        String id = m.get("jettraId") != null ? String.valueOf(m.get("jettraId")) : "";
        String ruc = m.get("ruc") != null ? String.valueOf(m.get("ruc")) : "";
        String nombre = m.get("nombre") != null ? String.valueOf(m.get("nombre")) : "";
        String email = m.get("email") != null ? String.valueOf(m.get("email")) : "";
        String categoria = m.get("categoria") != null ? String.valueOf(m.get("categoria")) : "ESTANDAR";
        boolean activo = m.get("activo") instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(m.get("activo")));
        return new ClienteDto(id, ruc, nombre, email, categoria, activo);
    }
}
