package io.jettra.server.example.service;

import io.jettra.server.example.client.ClienteMicroserviceRestClient;
import io.jettra.server.example.client.JettraJwtAuthManager;
import io.jettra.server.example.model.ClienteDto;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio CDI (@ApplicationScoped) que consume los endpoints de Clientes
 * ({@code /api/v1/clientes}) en {@code JettraStoreExampleMicroservices} vía MicroProfile RestClient + JWT.
 */
@ApplicationScoped
public class ClienteMicroserviceClientService {

    private volatile ClienteMicroserviceRestClient restClient;
    private final JettraJwtAuthManager authManager;
    private final Map<String, ClienteDto> localFallbackCache = new ConcurrentHashMap<>();

    public ClienteMicroserviceClientService() {
        this.authManager = JettraJwtAuthManager.getInstance();
        this.restClient = buildClient(resolveBackendUrl());
        seedFallbackData();
    }

    public ClienteMicroserviceClientService(String baseUrl) {
        this.authManager = JettraJwtAuthManager.getInstance();
        this.restClient = buildClient(baseUrl);
        seedFallbackData();
    }

    public void rebind(String baseUrl) {
        this.restClient = buildClient(baseUrl);
    }

    private ClienteMicroserviceRestClient buildClient(String url) {
        try {
            return RestClientBuilder.newBuilder()
                    .baseUri(URI.create(url))
                    .build(ClienteMicroserviceRestClient.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void seedFallbackData() {
        localFallbackCache.put("CLI-001", new ClienteDto("CLI-001", "155678901-2-2026", "Acme Corporation S.A.", "billing@acme.io", "ENTERPRISE", true));
        localFallbackCache.put("CLI-002", new ClienteDto("CLI-002", "155992310-1-2026", "Global Tech Panamá", "finanzas@globaltech.pa", "CORPORATIVO", true));
    }

    public List<ClienteDto> listAll() {
        String token = authManager.getBearerToken();
        if (restClient != null) {
            try {
                List<ClienteDto> remote = restClient.listClientes(token);
                if (remote != null) {
                    localFallbackCache.clear();
                    for (ClienteDto c : remote) {
                        if (c != null && c.jettraId() != null) {
                            localFallbackCache.put(c.jettraId(), c);
                        }
                    }
                    return remote;
                }
            } catch (Exception ignored) {
            }
        }
        return new ArrayList<>(localFallbackCache.values());
    }

    public Optional<ClienteDto> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        String token = authManager.getBearerToken();
        if (restClient != null) {
            try {
                ClienteDto remote = restClient.getCliente(token, id.trim());
                if (remote != null) {
                    localFallbackCache.put(remote.jettraId(), remote);
                    return Optional.of(remote);
                }
            } catch (Exception ignored) {
            }
        }
        return Optional.ofNullable(localFallbackCache.get(id.trim()));
    }

    public List<ClienteDto> findByCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) return listAll();
        String token = authManager.getBearerToken();
        if (restClient != null) {
            try {
                List<ClienteDto> remote = restClient.findByCategoria(token, categoria.trim());
                if (remote != null) return remote;
            } catch (Exception ignored) {
            }
        }
        return localFallbackCache.values().stream()
                .filter(c -> c.categoria() != null && c.categoria().equalsIgnoreCase(categoria.trim()))
                .toList();
    }

    public ClienteDto create(ClienteDto cliente) {
        if (cliente == null) throw new IllegalArgumentException("El cliente no puede ser nulo");
        String token = authManager.getBearerToken();
        if (restClient != null) {
            try {
                ClienteDto created = restClient.createCliente(token, cliente);
                if (created != null) {
                    localFallbackCache.put(created.jettraId(), created);
                    return created;
                }
            } catch (Exception ignored) {
            }
        }
        String id = (cliente.jettraId() != null && !cliente.jettraId().isBlank())
                ? cliente.jettraId()
                : "CLI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ClienteDto saved = cliente.withJettraId(id);
        localFallbackCache.put(id, saved);
        return saved;
    }

    public ClienteDto update(String id, ClienteDto cliente) {
        if (id == null || id.isBlank() || cliente == null) {
            throw new IllegalArgumentException("ID y datos del cliente son obligatorios");
        }
        String cleanId = id.trim();
        String token = authManager.getBearerToken();
        if (restClient != null) {
            try {
                ClienteDto updated = restClient.updateCliente(token, cleanId, cliente.withJettraId(cleanId));
                if (updated != null) {
                    localFallbackCache.put(cleanId, updated);
                    return updated;
                }
            } catch (Exception ignored) {
            }
        }
        ClienteDto saved = cliente.withJettraId(cleanId);
        localFallbackCache.put(cleanId, saved);
        return saved;
    }

    public boolean delete(String id) {
        if (id == null || id.isBlank()) return false;
        String cleanId = id.trim();
        String token = authManager.getBearerToken();
        boolean remoteOk = false;
        if (restClient != null) {
            try {
                restClient.deleteCliente(token, cleanId);
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
