package io.jettra.server.example.client;

import io.jettra.server.example.model.ClienteDto;
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
import java.util.Map;

/**
 * Eclipse MicroProfile Rest Client para los endpoints de Clientes ({@code @RestRepository ClienteRest})
 * expuestos por {@code JettraStoreExampleMicroservices} sobre {@code example_factura_db}.
 */
@RegisterRestClient(configKey = "cliente-client", baseUri = "http://localhost:8083")
@Path("/api/v1/clientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ClienteMicroserviceRestClient {

    @GET
    List<ClienteDto> listClientes(@HeaderParam("Authorization") String authHeader);

    @GET
    @Path("/{id}")
    ClienteDto getCliente(@HeaderParam("Authorization") String authHeader, @PathParam("id") String id);

    @GET
    @Path("/find-by-categoria")
    List<ClienteDto> findByCategoria(@HeaderParam("Authorization") String authHeader, @QueryParam("categoria") String categoria);

    @GET
    @Path("/count")
    Map<String, Object> count(@HeaderParam("Authorization") String authHeader);

    @POST
    ClienteDto createCliente(@HeaderParam("Authorization") String authHeader, ClienteDto cliente);

    @PUT
    @Path("/{id}")
    ClienteDto updateCliente(@HeaderParam("Authorization") String authHeader, @PathParam("id") String id, ClienteDto cliente);

    @DELETE
    @Path("/{id}")
    void deleteCliente(@HeaderParam("Authorization") String authHeader, @PathParam("id") String id);
}
