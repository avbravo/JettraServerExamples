package io.jettra.example.microservices.repository;

import io.jettra.driver.JettraClient;
import io.jettra.example.microservices.model.DetalleFactura;
import io.jettra.example.microservices.model.Factura;
import io.jettra.store.core.JettraBucketSchema;
import io.jettra.store.core.JettraId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación persistente de {@link FacturaRepository} respaldada por {@link JettraClient}
 * (JettraStoreDriver) sobre la base de datos {@code example_factura_db} y el bucket {@code Facturas}.
 */
public class JettraFacturaRepository implements FacturaRepository {

    public static final String ENGINE = "DOCUMENT";
    public static final String BUCKET = "Facturas";

    private final JettraClient client;
    private final String database;

    public JettraFacturaRepository(JettraClient client, String database) {
        this.client = client;
        this.database = database;
    }

    public void initBucketAndIndexes() {
        List<JettraBucketSchema.AttributeRule> schema = List.of(
                new JettraBucketSchema.AttributeRule("jettraId", "String", "NOT_NULL", "Identificador único JettraId"),
                new JettraBucketSchema.AttributeRule("folio", "String", "NOT_NULL", "Número de folio fiscal"),
                new JettraBucketSchema.AttributeRule("clienteId", "String", "NOT_NULL", "Referencia al cliente"),
                new JettraBucketSchema.AttributeRule("clienteNombre", "String", "NONE", "Nombre del cliente"),
                new JettraBucketSchema.AttributeRule("fechaEmision", "String", "NONE", "Fecha ISO de emisión"),
                new JettraBucketSchema.AttributeRule("estado", "String", "NOT_NULL", "Estado de la factura"),
                new JettraBucketSchema.AttributeRule("detalles", "List", "NONE", "Ítems o líneas de detalle"),
                new JettraBucketSchema.AttributeRule("subtotal", "Double", "NONE", "Subtotal antes de impuestos"),
                new JettraBucketSchema.AttributeRule("impuesto", "Double", "NONE", "Impuesto aplicado"),
                new JettraBucketSchema.AttributeRule("total", "Double", "NONE", "Monto total de la factura")
        );
        client.createBucket(database, ENGINE, BUCKET, JettraBucketSchema.StructureType.DEFINED, schema);
        if (client.getBucketIndex(database, "idx_facturas_folio") == null) {
            client.createBucketIndex(database, BUCKET, "idx_facturas_folio", "folio", "BTREE", true);
        }
    }

    @Override
    public Factura findById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        Map<String, Object> doc = client.findBucketRecordById(database, BUCKET, id.trim());
        return doc != null ? toRecord(doc) : null;
    }

    @Override
    public List<Factura> findAll() {
        List<Map<String, Object>> docs = client.findAllBucketRecords(database, BUCKET);
        List<Factura> result = new ArrayList<>();
        for (Map<String, Object> doc : docs) {
            if (doc != null) {
                result.add(toRecord(doc));
            }
        }
        return result;
    }

    @Override
    public List<Factura> findByClienteId(String clienteId) {
        if (clienteId == null || clienteId.isBlank()) {
            return List.of();
        }
        String target = clienteId.trim();
        return findAll().stream()
                .filter(f -> f.clienteId() != null && f.clienteId().equalsIgnoreCase(target))
                .toList();
    }

    @Override
    public List<Factura> findByEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return findAll();
        }
        String target = estado.trim();
        return findAll().stream()
                .filter(f -> f.estado() != null && f.estado().equalsIgnoreCase(target))
                .toList();
    }

    @Override
    public Factura save(Factura factura) {
        if (factura == null) {
            throw new IllegalArgumentException("La factura no puede ser nula");
        }
        String id = (factura.jettraId() != null && !factura.jettraId().isBlank())
                ? factura.jettraId().trim()
                : JettraId.generate(database, BUCKET).toHexString();
        Factura toSave = factura.withJettraId(id);
        String storedId = client.insertBucketRecord(database, ENGINE, BUCKET, id, toMap(toSave));
        String finalId = (storedId != null && !storedId.isBlank()) ? storedId : id;
        Factura saved = findById(finalId);
        return saved != null ? saved : toSave.withJettraId(finalId);
    }

    @Override
    public Factura update(String id, Factura factura) {
        if (id == null || id.isBlank() || factura == null) {
            throw new IllegalArgumentException("ID y datos de factura son obligatorios para actualizar");
        }
        String cleanId = id.trim();
        Factura updated = factura.withJettraId(cleanId);
        client.upsertBucketRecord(database, ENGINE, BUCKET, cleanId, toMap(updated));
        Factura reloaded = findById(cleanId);
        return reloaded != null ? reloaded : updated;
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return client.deleteBucketRecord(database, ENGINE, BUCKET, id.trim());
    }

    @Override
    public long count() {
        return findAll().size();
    }

    private static Map<String, Object> toMap(Factura f) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("jettraId", f.jettraId());
        map.put("folio", f.folio() != null ? f.folio() : "");
        map.put("clienteId", f.clienteId() != null ? f.clienteId() : "");
        map.put("clienteNombre", f.clienteNombre() != null ? f.clienteNombre() : "");
        map.put("fechaEmision", f.fechaEmision() != null ? f.fechaEmision() : "");
        map.put("estado", f.estado() != null ? f.estado() : "EMITIDA");
        List<Map<String, Object>> detallesList = new ArrayList<>();
        if (f.detalles() != null) {
            for (DetalleFactura d : f.detalles()) {
                Map<String, Object> dm = new LinkedHashMap<>();
                dm.put("sku", d.sku());
                dm.put("descripcion", d.descripcion());
                dm.put("cantidad", d.cantidad());
                dm.put("precioUnitario", d.precioUnitario());
                dm.put("subtotal", d.subtotal());
                detallesList.add(dm);
            }
        }
        map.put("detalles", detallesList);
        map.put("subtotal", f.subtotal());
        map.put("impuesto", f.impuesto());
        map.put("total", f.total());
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Factura toRecord(Map<String, Object> doc) {
        String id = str(doc.get("jettraId"), str(doc.get("_id"), ""));
        String folio = str(doc.get("folio"), "");
        String clienteId = str(doc.get("clienteId"), "");
        String clienteNombre = str(doc.get("clienteNombre"), "");
        String fechaEmision = str(doc.get("fechaEmision"), "");
        String estado = str(doc.get("estado"), "EMITIDA");
        List<DetalleFactura> detalles = new ArrayList<>();
        Object rawDetalles = doc.get("detalles");
        if (rawDetalles instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    detalles.add(new DetalleFactura(
                            str(m.get("sku"), ""),
                            str(m.get("descripcion"), ""),
                            asInt(m.get("cantidad"), 1),
                            asDouble(m.get("precioUnitario"), 0.0),
                            asDouble(m.get("subtotal"), 0.0)
                    ));
                }
            }
        }
        double subtotal = asDouble(doc.get("subtotal"), 0.0);
        double impuesto = asDouble(doc.get("impuesto"), 0.0);
        double total = asDouble(doc.get("total"), 0.0);
        return new Factura(id, folio, clienteId, clienteNombre, fechaEmision, estado, detalles, subtotal, impuesto, total);
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
