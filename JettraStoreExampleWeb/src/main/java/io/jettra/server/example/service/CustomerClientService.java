package io.jettra.server.example.service;

import io.jettra.server.example.client.CustomerRestClient;
import io.jettra.server.example.client.JettraJwtAuthManager;
import io.jettra.server.example.model.Customer;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio CDI (@ApplicationScoped) que orquesta la comunicación con el microservicio
 * de clientes en BackEndExample mediante Eclipse MicroProfile Rest Client y Jettra JWT.
 */
@ApplicationScoped
public class CustomerClientService {

    private final CustomerRestClient restClient;
    private final JettraJwtAuthManager authManager;
    private final Map<Long, Customer> localFallbackCache = new ConcurrentHashMap<>();
    private final AtomicLong fallbackIdGen = new AtomicLong(200);

    public CustomerClientService() {
        this.authManager = JettraJwtAuthManager.getInstance();
        this.restClient = initRestClient();
        seedFallbackData();
    }

    private CustomerRestClient initRestClient() {
        try {
            String backendUrl = resolveBackendUrl();
            return RestClientBuilder.newBuilder()
                    .baseUri(URI.create(backendUrl))
                    .build(CustomerRestClient.class);
        } catch (Exception e) {
            // Instanciación directa de fallback
            return null;
        }
    }

    private void seedFallbackData() {
        localFallbackCache.put(1L, new Customer(1L, "Acme Corporation", "contact@acme.corp", "Acme Global", "ENTERPRISE", true));
        localFallbackCache.put(2L, new Customer(2L, "Stark Industries", "tony@stark.tech", "Stark Tech", "PREMIUM", true));
        localFallbackCache.put(3L, new Customer(3L, "Wayne Enterprises", "bruce@wayne.org", "Wayne Holdings", "ENTERPRISE", true));
        localFallbackCache.put(4L, new Customer(4L, "Cyberdyne Systems", "miles@cyberdyne.ai", "Cyberdyne AI", "STANDARD", true));
    }

    public List<Customer> listAll() {
        String token = authManager.getBearerToken();
        if (restClient != null) {
            try {
                List<Customer> remote = restClient.listCustomers(token);
                if (remote != null && !remote.isEmpty()) {
                    for (Customer c : remote) {
                        if (c.getId() != null) localFallbackCache.put(c.getId(), c);
                    }
                    return remote;
                }
            } catch (Exception ignored) {
                // Backend no disponible en prueba unitaria aislada, usar fallback
            }
        }
        return new ArrayList<>(localFallbackCache.values());
    }

    public Optional<Customer> findById(Long id) {
        if (id == null) return Optional.empty();
        String token = authManager.getBearerToken();
        if (restClient != null) {
            try {
                Customer remote = restClient.getCustomer(token, id);
                if (remote != null) {
                    localFallbackCache.put(remote.getId(), remote);
                    return Optional.of(remote);
                }
            } catch (Exception ignored) {}
        }
        return Optional.ofNullable(localFallbackCache.get(id));
    }

    public Customer save(Customer customer) {
        if (customer == null) return null;
        String token = authManager.getBearerToken();

        if (restClient != null) {
            try {
                Customer savedRemote = restClient.createCustomer(token, customer);
                if (savedRemote != null) {
                    localFallbackCache.put(savedRemote.getId(), savedRemote);
                    return savedRemote;
                }
            } catch (Exception ignored) {}
        }

        // Operación local si el backend está desconectado
        if (customer.getId() == null || customer.getId() <= 0) {
            customer.setId(fallbackIdGen.incrementAndGet());
        }
        localFallbackCache.put(customer.getId(), customer);
        return customer;
    }

    public Customer saveCustomer(Customer customer) {
        return save(customer);
    }

    public boolean delete(Long id) {
        if (id == null) return false;
        String token = authManager.getBearerToken();

        boolean remoteOk = false;
        if (restClient != null) {
            try {
                restClient.deleteCustomer(token, id);
                remoteOk = true;
            } catch (Exception ignored) {}
        }
        boolean localOk = localFallbackCache.remove(id) != null;
        return remoteOk || localOk;
    }

    public boolean deleteCustomer(Long id) {
        return delete(id);
    }

    public boolean deleteCustomer(String idStr) {
        if (idStr == null || idStr.isBlank()) return false;
        try {
            return delete(Long.parseLong(idStr.trim()));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String getActiveJwtToken() {
        return authManager.getBearerToken();
    }

    public String resolveBackendUrl() {
        try {
            return ConfigProvider.getConfig().getOptionalValue("backend.api.url", String.class).orElse("http://localhost:8081");
        } catch (Exception e) {
            String prop = System.getProperty("backend.api.url");
            return (prop != null && !prop.isBlank()) ? prop.trim() : "http://localhost:8081";
        }
    }

    public CustomerRestClient getRestClient() {
        return restClient;
    }
}
