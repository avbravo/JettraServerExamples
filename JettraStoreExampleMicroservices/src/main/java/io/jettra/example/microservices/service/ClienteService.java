package io.jettra.example.microservices.service;

import io.jettra.example.microservices.model.Cliente;
import io.jettra.example.microservices.repository.ClienteRepository;

import java.util.List;
import java.util.Optional;

/**
 * Capa de servicio de negocio para la gestión de clientes en {@code example_factura_db}.
 */
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    public void seedDefaultsIfEmpty() {
        if (repository.count() == 0) {
            repository.save(new Cliente(null, "155678901-2-2026", "Acme Corporation S.A.", "billing@acme.io", "ENTERPRISE", true));
            repository.save(new Cliente(null, "155992310-1-2026", "Global Tech Panamá", "finanzas@globaltech.pa", "CORPORATIVO", true));
        }
    }

    public List<Cliente> findAll() {
        return repository.findAll();
    }

    public Optional<Cliente> findById(String id) {
        return Optional.ofNullable(repository.findById(id));
    }

    public List<Cliente> findByCategoria(String categoria) {
        return repository.findByCategoria(categoria);
    }

    public Cliente crear(Cliente cliente) {
        if (cliente == null || cliente.nombre() == null || cliente.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio");
        }
        if (cliente.ruc() == null || cliente.ruc().isBlank()) {
            throw new IllegalArgumentException("El RUC del cliente es obligatorio");
        }
        if (cliente.email() == null || !cliente.email().contains("@")) {
            throw new IllegalArgumentException("El email del cliente es inválido");
        }
        String cat = (cliente.categoria() == null || cliente.categoria().isBlank()) ? "ESTANDAR" : cliente.categoria().trim();
        return repository.save(new Cliente(cliente.jettraId(), cliente.ruc().trim(), cliente.nombre().trim(), cliente.email().trim(), cat, true));
    }

    public Cliente actualizar(String id, Cliente cliente) {
        if (repository.findById(id) == null) {
            throw new IllegalArgumentException("Cliente no encontrado: " + id);
        }
        return repository.update(id, cliente);
    }

    public boolean eliminar(String id) {
        return repository.deleteById(id);
    }

    public long count() {
        return repository.count();
    }
}
