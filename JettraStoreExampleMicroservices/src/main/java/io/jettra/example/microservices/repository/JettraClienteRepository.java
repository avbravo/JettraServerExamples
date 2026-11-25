package io.jettra.example.microservices.repository;

import io.jettra.driver.JettraClient;
import io.jettra.example.microservices.model.Cliente;
import io.jettra.store.core.JettraBucketSchema;
import io.jettra.store.core.JettraId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación persistente de {@link ClienteRepository} respaldada por {@link JettraClient}
 * (JettraStoreDriver) sobre la base de datos {@code example_factura_db} y el bucket {@code Clientes}.
 */
public class JettraClienteRepository implements ClienteRepository {

    public static final String ENGINE = "DOCUMENT";
    public static final String BUCKET = "Clientes";

    private final JettraClient client;
    private final String database;

    public JettraClienteRepository(JettraClient client, String database) {
        this.client = client;
        this.database = database;
    }

    public void initBucketAndIndexes() {
        List<JettraBucketSchema.AttributeRule> schema = List.of(
                new JettraBucketSchema.AttributeRule("jettraId", "String", "NOT_NULL", "Identificador único JettraId"),
                new JettraBucketSchema.AttributeRule("ruc", "String", "NOT_NULL", "RUC / Identificación fiscal"),
                new JettraBucketSchema.AttributeRule("nombre", "String", "NOT_NULL", "Nombre o razón social"),
                new JettraBucketSchema.AttributeRule("email", "String", "NOT_NULL", "Correo electrónico"),
                new JettraBucketSchema.AttributeRule("categoria", "String", "NONE", "Categoría comercial"),
                new JettraBucketSchema.AttributeRule("activo", "Boolean", "NONE", "Estado activo del cliente")
        );
        client.createBucket(database, ENGINE, BUCKET, JettraBucketSchema.StructureType.DEFINED, schema);
        if (client.getBucketIndex(database, "idx_clientes_ruc") == null) {
            client.createBucketIndex(database, BUCKET, "idx_clientes_ruc", "ruc", "BTREE", true);
        }
    }

    @Override
    public Cliente findById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        Map<String, Object> doc = client.findBucketRecordById(database, BUCKET, id.trim());
        return doc != null ? toRecord(doc) : null;
    }

    @Override
    public List<Cliente> findAll() {
        List<Map<String, Object>> docs = client.findAllBucketRecords(database, BUCKET);
        List<Cliente> result = new ArrayList<>();
        for (Map<String, Object> doc : docs) {
            if (doc != null) {
                result.add(toRecord(doc));
            }
        }
        return result;
    }

    @Override
    public Cliente save(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente no puede ser nulo");
        }
        String id = (cliente.jettraId() != null && !cliente.jettraId().isBlank())
                ? cliente.jettraId().trim()
                : JettraId.generate(database, BUCKET).toHexString();
        Map<String, Object> map = toMap(cliente.withJettraId(id));
        String storedId = client.insertBucketRecord(database, ENGINE, BUCKET, id, map);
        String finalId = (storedId != null && !storedId.isBlank()) ? storedId : id;
        Cliente saved = findById(finalId);
        return saved != null ? saved : cliente.withJettraId(finalId);
    }

    @Override
    public Cliente update(String id, Cliente cliente) {
        if (id == null || id.isBlank() || cliente == null) {
            throw new IllegalArgumentException("ID y datos del cliente son obligatorios para actualizar");
        }
        String cleanId = id.trim();
        Cliente updated = cliente.withJettraId(cleanId);
        client.upsertBucketRecord(database, ENGINE, BUCKET, cleanId, toMap(updated));
        Cliente reloaded = findById(cleanId);
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
    public List<Cliente> findByCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return findAll();
        }
        String target = categoria.trim();
        return findAll().stream()
                .filter(c -> c.categoria() != null && c.categoria().equalsIgnoreCase(target))
                .toList();
    }

    @Override
    public long count() {
        return findAll().size();
    }

    private static Map<String, Object> toMap(Cliente c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("jettraId", c.jettraId());
        map.put("ruc", c.ruc() != null ? c.ruc() : "");
        map.put("nombre", c.nombre() != null ? c.nombre() : "");
        map.put("email", c.email() != null ? c.email() : "");
        map.put("categoria", c.categoria() != null ? c.categoria() : "ESTANDAR");
        map.put("activo", c.activo());
        return map;
    }

    private static Cliente toRecord(Map<String, Object> doc) {
        String id = str(doc.get("jettraId"), str(doc.get("_id"), ""));
        String ruc = str(doc.get("ruc"), "");
        String nombre = str(doc.get("nombre"), "");
        String email = str(doc.get("email"), "");
        String categoria = str(doc.get("categoria"), "ESTANDAR");
        boolean activo = doc.get("activo") instanceof Boolean b ? b : Boolean.parseBoolean(str(doc.get("activo"), "true"));
        return new Cliente(id, ruc, nombre, email, categoria, activo);
    }

    private static String str(Object v, String def) {
        return v == null ? def : String.valueOf(v);
    }
}
