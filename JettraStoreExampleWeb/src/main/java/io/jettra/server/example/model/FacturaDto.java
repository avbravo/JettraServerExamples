package io.jettra.server.example.model;

import java.util.List;

/**
 * DTO inmutable de Factura sincronizado con {@code JettraStoreExampleMicroservices} ({@code example_factura_db}).
 */
public record FacturaDto(
        String jettraId,
        String folio,
        String clienteId,
        String clienteNombre,
        String fechaEmision,
        String estado,
        List<DetalleFacturaDto> detalles,
        double subtotal,
        double impuesto,
        double total
) {
    public FacturaDto withEstado(String nuevoEstado) {
        return new FacturaDto(jettraId, folio, clienteId, clienteNombre, fechaEmision, nuevoEstado, detalles, subtotal, impuesto, total);
    }

    public static FacturaDto fromMap(java.util.Map<?, ?> m) {
        if (m == null) return null;
        java.util.List<DetalleFacturaDto> items = new java.util.ArrayList<>();
        Object rawDet = m.get("detalles");
        java.util.List<?> list = null;
        if (rawDet instanceof java.util.List<?> l) {
            list = l;
        } else if (rawDet instanceof io.jettra.json.JsonArray ja) {
            list = ja.getList();
        }
        if (list != null) {
            for (Object o : list) {
                java.util.Map<?, ?> dm = null;
                if (o instanceof java.util.Map<?, ?> map) {
                    dm = map;
                } else if (o instanceof io.jettra.json.JsonObject jo) {
                    dm = jo.getMap();
                }
                if (dm != null) {
                    items.add(new DetalleFacturaDto(
                            str(dm.get("sku"), ""),
                            str(dm.get("descripcion"), ""),
                            asInt(dm.get("cantidad"), 1),
                            asDouble(dm.get("precioUnitario"), 0.0),
                            asDouble(dm.get("subtotal"), 0.0)
                    ));
                }
            }
        }
        return new FacturaDto(
                str(m.get("jettraId"), ""),
                str(m.get("folio"), ""),
                str(m.get("clienteId"), ""),
                str(m.get("clienteNombre"), ""),
                str(m.get("fechaEmision"), ""),
                str(m.get("estado"), "EMITIDA"),
                items,
                asDouble(m.get("subtotal"), 0.0),
                asDouble(m.get("impuesto"), 0.0),
                asDouble(m.get("total"), 0.0)
        );
    }

    private static String str(Object v, String def) {
        return v == null ? def : String.valueOf(v);
    }

    private static int asInt(Object v, int def) {
        if (v instanceof Number n) return n.intValue();
        try { return v != null ? Integer.parseInt(String.valueOf(v)) : def; } catch (Exception e) { return def; }
    }

    private static double asDouble(Object v, double def) {
        if (v instanceof Number n) return n.doubleValue();
        try { return v != null ? Double.parseDouble(String.valueOf(v)) : def; } catch (Exception e) { return def; }
    }
}
