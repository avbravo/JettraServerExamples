package io.jettra.example.microservices.rest;

import io.jettra.example.microservices.model.Factura;
import io.jettra.example.microservices.service.FacturaService;
import io.jettra.rest.annotations.DELETE;
import io.jettra.rest.annotations.GET;
import io.jettra.rest.annotations.POST;
import io.jettra.rest.annotations.PUT;
import io.jettra.rest.annotations.Path;
import io.jettra.rest.annotations.PathParam;
import io.jettra.rest.annotations.QueryParam;
import io.jettra.rest.annotations.accreditation.RolesAllowed;
import io.jettra.server.core.http.HttpRequest;
import io.jettra.server.core.http.HttpResponse;
import io.jettra.server.openapi.annotations.ApiResponse;
import io.jettra.server.openapi.annotations.Operation;
import io.jettra.server.openapi.annotations.Parameter;
import io.jettra.server.openapi.annotations.Tag;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Microservicio REST de Facturación sobre {@code example_factura_db}.
 */
@Path("/api/v1/facturas")
@Tag(name = "Microservicio de Facturación (example_factura_db)", description = "Emisión, consulta, actualización, anulación y eliminación de facturas")
public class FacturaResource {

    private final FacturaService facturaService;

    public FacturaResource(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @GET
    @RolesAllowed({"ADMIN", "MANAGER", "USER"})
    @Operation(summary = "Listar facturas", description = "Permite filtrar opcionalmente por clienteId o estado.")
    @ApiResponse(responseCode = "200", description = "Listado de facturas obtenido")
    public void listFacturas(@QueryParam("clienteId") String clienteId,
                             @QueryParam("estado") String estado,
                             HttpRequest req, HttpResponse res) {
        List<Factura> result;
        if (clienteId != null && !clienteId.isBlank()) {
            result = facturaService.findByClienteId(clienteId);
        } else if (estado != null && !estado.isBlank()) {
            result = facturaService.findByEstado(estado);
        } else {
            result = facturaService.findAll();
        }
        res.json(result);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "MANAGER", "USER"})
    @Operation(summary = "Obtener factura por JettraId")
    public void getFactura(@Parameter(description = "JettraId de la factura") @PathParam("id") String id,
                           HttpRequest req, HttpResponse res) {
        Optional<Factura> opt = facturaService.findById(id);
        if (opt.isPresent()) {
            res.json(opt.get());
        } else {
            res.status(404).json(Map.of("error", "Factura no encontrada: " + id));
        }
    }

    @POST
    @RolesAllowed({"ADMIN", "MANAGER"})
    @Operation(summary = "Emitir nueva factura", description = "Calcula automáticamente subtotal, impuesto (7%) y total.")
    public void createFactura(HttpRequest req, HttpResponse res) {
        try {
            Factura payload = req.getBodyAsJson(Factura.class);
            Factura saved = facturaService.emitirFactura(payload);
            res.status(201).json(saved);
        } catch (Exception e) {
            res.status(400).json(Map.of("error", e.getMessage()));
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "MANAGER"})
    @Operation(summary = "Actualizar factura existente")
    public void updateFactura(@PathParam("id") String id, HttpRequest req, HttpResponse res) {
        try {
            Factura payload = req.getBodyAsJson(Factura.class);
            Factura updated = facturaService.actualizarFactura(id, payload);
            res.json(updated);
        } catch (Exception e) {
            res.status(400).json(Map.of("error", e.getMessage()));
        }
    }

    @PUT
    @Path("/{id}/anular")
    @RolesAllowed({"ADMIN", "MANAGER"})
    @Operation(summary = "Anular factura")
    public void anularFactura(@PathParam("id") String id, HttpRequest req, HttpResponse res) {
        Optional<Factura> opt = facturaService.anularFactura(id);
        if (opt.isPresent()) {
            res.json(opt.get());
        } else {
            res.status(404).json(Map.of("error", "Factura no encontrada: " + id));
        }
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    @Operation(summary = "Eliminar factura")
    public void deleteFactura(@PathParam("id") String id, HttpRequest req, HttpResponse res) {
        if (facturaService.eliminarFactura(id)) {
            res.json(Map.of("message", "Factura eliminada", "id", id));
        } else {
            res.status(404).json(Map.of("error", "Factura no encontrada: " + id));
        }
    }
}
