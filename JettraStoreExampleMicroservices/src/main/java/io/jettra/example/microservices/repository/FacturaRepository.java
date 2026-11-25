package io.jettra.example.microservices.repository;

import io.jettra.example.microservices.model.Factura;
import io.jettra.rest.annotations.Repository;
import io.jettra.rest.annotations.RestRepository;
import io.jettra.rest.annotations.accreditation.RolesAllowed;

import java.util.List;

/**
 * Repositorio de Facturas sobre {@code example_factura_db}.
 * <p>
 * Anotado con {@link Repository} y {@link RestRepository} para generar en tiempo de compilación
 * la clase {@code FacturaRest} con endpoints CRUD, búsquedas por cliente/estado y seguridad JWT.
 */
@Repository
@RestRepository(
        path = "/api/v1/facturas-auto",
        resourceName = "FacturaRest",
        secured = true,
        rolesAllowed = {"ADMIN", "MANAGER", "USER"},
        declareRoles = {"ADMIN", "MANAGER", "USER"}
)
public interface FacturaRepository {

    Factura findById(String id);

    List<Factura> findAll();

    List<Factura> findByClienteId(String clienteId);

    List<Factura> findByEstado(String estado);

    @RolesAllowed({"ADMIN", "MANAGER"})
    Factura save(Factura factura);

    @RolesAllowed({"ADMIN", "MANAGER"})
    Factura update(String id, Factura factura);

    @RolesAllowed({"ADMIN"})
    boolean deleteById(String id);

    long count();
}
