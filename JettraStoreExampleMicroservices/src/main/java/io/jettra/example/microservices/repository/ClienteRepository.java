package io.jettra.example.microservices.repository;

import io.jettra.example.microservices.model.Cliente;
import io.jettra.rest.annotations.PermitAll;
import io.jettra.rest.annotations.Repository;
import io.jettra.rest.annotations.RestRepository;
import io.jettra.rest.annotations.accreditation.RolesAllowed;

import java.util.List;

/**
 * Repositorio de Clientes sobre {@code example_factura_db}.
 * <p>
 * Anotado con {@link Repository} y {@link RestRepository} para que {@code RestRepositoryProcessor}
 * genere automáticamente en tiempo de compilación la clase {@code ClienteRest} con todos sus
 * endpoints HTTP y reglas de seguridad JWT.
 */
@Repository
@RestRepository(
        path = "/api/v1/clientes",
        resourceName = "ClienteRest",
        secured = true,
        rolesAllowed = {"ADMIN", "MANAGER", "USER"},
        declareRoles = {"ADMIN", "MANAGER", "USER"}
)
public interface ClienteRepository {

    Cliente findById(String id);

    @PermitAll
    List<Cliente> findAll();

    @RolesAllowed({"ADMIN", "MANAGER"})
    Cliente save(Cliente cliente);

    @RolesAllowed({"ADMIN", "MANAGER"})
    Cliente update(String id, Cliente cliente);

    @RolesAllowed({"ADMIN"})
    boolean deleteById(String id);

    List<Cliente> findByCategoria(String categoria);

    long count();
}
