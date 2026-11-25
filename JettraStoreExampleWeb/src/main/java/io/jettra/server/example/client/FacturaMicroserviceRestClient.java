package io.jettra.server.example.client;

import io.jettra.server.example.model.FacturaDto;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * Eclipse MicroProfile Rest Client para los endpoints de Facturación
 * expuestos por {@code JettraStoreExampleMicroservices} sobre {@code example_factura_db}.
 */
@RegisterRestClient(configKey = "factura-client", baseUri = "http://localhost:8083")
@Path("/api/v1/facturas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface FacturaMicroserviceRestClient {

    @GET
    List<FacturaDto> listFacturas(@HeaderParam("Authorization") String authHeader);

    @GET
    List<FacturaDto> listFacturasByEstado(@HeaderParam("Authorization") String authHeader, @QueryParam("estado") String estado);

    @GET
    List<FacturaDto> listFacturasByCliente(@HeaderParam("Authorization") String authHeader, @QueryParam("clienteId") String clienteId);

    @GET
    @Path("/{id}")
    FacturaDto getFactura(@HeaderParam("Authorization") String authHeader, @PathParam("id") String id);

    @POST
    FacturaDto createFactura(@HeaderParam("Authorization") String authHeader, FacturaDto factura);

    @PUT
    @Path("/{id}")
    FacturaDto updateFactura(@HeaderParam("Authorization") String authHeader, @PathParam("id") String id, FacturaDto factura);

    @PUT
    @Path("/{id}/anular")
    FacturaDto anularFactura(@HeaderParam("Authorization") String authHeader, @PathParam("id") String id);

    @DELETE
    @Path("/{id}")
    void deleteFactura(@HeaderParam("Authorization") String authHeader, @PathParam("id") String id);
}
