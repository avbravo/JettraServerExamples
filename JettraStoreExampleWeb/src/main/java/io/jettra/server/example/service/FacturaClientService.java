package io.jettra.server.example.service;

import io.jettra.server.example.client.FacturaMicroserviceRestClient;
import io.jettra.server.example.client.JettraJwtAuthManager;
import io.jettra.server.example.model.DetalleFacturaDto;
import io.jettra.server.example.model.FacturaDto;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio CDI (@ApplicationScoped) que consume los endpoints de Facturación
 * ({@code /api/v1/facturas}) en {@code JettraStoreExampleMicroservices} vía MicroProfile RestClient + JWT.
 */
@ApplicationScoped
public class FacturaClientService {

    private static final double TAX_RATE = 0.07;

    private volatile FacturaMicroserviceRestClient restClient;
    private final JettraJwtAuthManager authManager;
    private final Map<String, FacturaDto> localFallbackCache = new ConcurrentHashMap<>();

    public FacturaClientService() {
        this.authManager = JettraJwtAuthManager.getInstance();
        this.restClient = buildClient(resolveBackendUrl());
        seedFallbackData();
    }

    public FacturaClientService(String baseUrl) {
        this.authManager = JettraJwtAuthManager.getInstance();
        this.restClient = buildClient(baseUrl);
        seedFallbackData();
    }

    public void rebind(String baseUrl) {
        this.restClient = buildClient(baseUrl);
    }

    private FacturaMicroserviceRestClient buildClient(String url) {
        try {
            return RestClientBuilder.newBuilder()
                    .baseUri(URI.create(url))
                    .build(FacturaMicroserviceRestClient.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void seedFallbackData() {
        DetalleFacturaDto d1 = DetalleFacturaDto.of("SRV-JETTRA-01", "Licencia JettraStore Cluster Enterprise", 1, 1200.00);
        DetalleFacturaDto d2 = DetalleFacturaDto.of("SUP-24X7", "Soporte 24x7 Clúster Raft", 2, 150.00);
        localFallbackCache.put("FAC-001", new FacturaDto(
                "FAC-001", "FAC-2026-0001", "CLI-001", "Acme Corporation S.A.",
                LocalDate.now().toString(), "EMITIDA", List.of(d1, d2), 1500.00, 105.00, 1605.00));
    }

    public List<FacturaDto> listAll() {
        String token = authManager.getBearerToken();
        if (restClient != null) {
            try {
                List<FacturaDto> remote = restClient.listFacturas(token);
                if (remote != null) {
                    localFallbackCache.clear();
                    for (FacturaDto f : remote) {
                        if (f != null && f.jettraId() != null) {
                            localFallbackCache.put(f.jettraId(), f);
                        }
                    }
                    return remote;
                }
            } catch (Exception ignored) {
            }
        }
        return new ArrayList<>(localFallbackCache.values());
    }

    public Optional<FacturaDto> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        String token = authManager.getBearerToken();
        if (restClient != null) {
            try {
                FacturaDto remote = restClient.getFactura(token, id.trim());
                if (remote != null) {
                    localFallbackCache.put(remote.jettraId(), remote);
                    return Optional.of(remote);
                }
            } catch (Exception ignored) {
            }
        }
        return Optional.ofNullable(localFallbackCache.get(id.trim()));
    }

    public List<FacturaDto> findByEstado(String estado) {
        if (estado == null || estado.isBlank()) return listAll();
        String token = authManager.getBearerToken();
        if (restClient != null) {
            try {
                List<FacturaDto> remote = restClient.listFacturasByEstado(token, estado.trim());
                if (remote != null) return remote;
            } catch (Exception ignored) {
            }
        }
        return localFallbackCache.values().stream()
                .filter(f -> f.estado() != null && f.estado().equalsIgnoreCase(estado.trim()))
                .toList();
    }

    public FacturaDto create(FacturaDto factura) {
        if (factura == null) throw new IllegalArgumentException("La factura no puede ser nula");
        String token = authManager.getBearerToken();
        if (restClient != null) {
            try {
                FacturaDto created = restClient.createFactura(token, factura);
                if (created != null) {
                    localFallbackCache.put(created.jettraId(), created);
                    return created;
                }
            } catch (Exception ignored) {
            }
        }
        double sub = factura.detalles() != null
                ? factura.detalles().stream().mapToDouble(DetalleFacturaDto::subtotal).sum()
                : factura.subtotal();
        sub = Math.round(sub * 100.0) / 100.0;
        double tax = Math.round(sub * TAX_RATE * 100.0) / 100.0;
        double tot = Math.round((sub + tax) * 100.0) / 100.0;
        String id = (factura.jettraId() != null && !factura.jettraId().isBlank())
                ? factura.jettraId()
                : "FAC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String folio = (factura.folio() != null && !factura.folio().isBlank()) ? factura.folio() : "FAC-2026-" + id.substring(4);
        String fecha = (factura.fechaEmision() != null && !factura.fechaEmision().isBlank()) ? factura.fechaEmision() : LocalDate.now().toString();
        FacturaDto saved = new FacturaDto(id, folio, factura.clienteId(), factura.clienteNombre(), fecha,
                factura.estado() != null && !factura.estado().isBlank() ? factura.estado() : "EMITIDA",
                factura.detalles() != null ? factura.detalles() : List.of(), sub, tax, tot);
        localFallbackCache.put(id, saved);
        return saved;
    }

    public FacturaDto update(String id, FacturaDto factura) {
        if (id == null || id.isBlank() || factura == null) {
            throw new IllegalArgumentException("ID y datos de factura son obligatorios");
        }
        String cleanId = id.trim();
        String token = authManager.getBearerToken();
        if (restClient != null) {
            try {
                FacturaDto updated = restClient.updateFactura(token, cleanId, factura);
                if (updated != null) {
                    localFallbackCache.put(cleanId, updated);
                    return updated;
                }
            } catch (Exception ignored) {
            }
        }
        FacturaDto saved = new FacturaDto(cleanId, factura.folio(), factura.clienteId(), factura.clienteNombre(),
                factura.fechaEmision(), factura.estado(), factura.detalles(), factura.subtotal(), factura.impuesto(), factura.total());
        localFallbackCache.put(cleanId, saved);
        return saved;
    }

    public Optional<FacturaDto> anular(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        String cleanId = id.trim();
        String token = authManager.getBearerToken();
        if (restClient != null) {
            try {
                FacturaDto updated = restClient.anularFactura(token, cleanId);
                if (updated != null) {
                    localFallbackCache.put(cleanId, updated);
                    return Optional.of(updated);
                }
            } catch (Exception ignored) {
            }
        }
        FacturaDto existing = localFallbackCache.get(cleanId);
        if (existing == null) return Optional.empty();
        FacturaDto anulada = existing.withEstado("ANULADA");
        localFallbackCache.put(cleanId, anulada);
        return Optional.of(anulada);
    }

    public boolean delete(String id) {
        if (id == null || id.isBlank()) return false;
        String cleanId = id.trim();
        String token = authManager.getBearerToken();
        boolean remoteOk = false;
        if (restClient != null) {
            try {
                restClient.deleteFactura(token, cleanId);
                remoteOk = true;
            } catch (Exception ignored) {
            }
        }
        boolean localOk = localFallbackCache.remove(cleanId) != null;
        return remoteOk || localOk;
    }

    public String getActiveJwtToken() {
        return authManager.getBearerToken();
    }

    public String resolveBackendUrl() {
        String sysProp = System.getProperty("backend.api.url");
        if (sysProp != null && !sysProp.isBlank()) return sysProp.trim();
        try {
            return ConfigProvider.getConfig().getOptionalValue("backend.api.url", String.class).orElse("http://localhost:8083");
        } catch (Exception e) {
            return "http://localhost:8083";
        }
    }
}
