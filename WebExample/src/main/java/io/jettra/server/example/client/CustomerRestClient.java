package io.jettra.server.example.client;

import io.jettra.server.example.model.Customer;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.util.List;

/**
 * Cliente REST declarativo definido bajo la especificación Eclipse MicroProfile Rest Client 3.0.
 * Se comunica con el microservicio de clientes en BackEndExample protegido mediante Jettra JWT.
 */
@RegisterRestClient(configKey = "customer-client", baseUri = "http://localhost:8081")
@Path("/api/v1/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface CustomerRestClient {

    @GET
    List<Customer> listCustomers(@HeaderParam("Authorization") String authHeader);

    @GET
    List<Customer> listCustomers();

    @GET
    @Path("/{id}")
    Customer getCustomer(@HeaderParam("Authorization") String authHeader, @PathParam("id") Long id);

    @GET
    @Path("/{id}")
    Customer getCustomer(@PathParam("id") Long id);

    @POST
    Customer createCustomer(@HeaderParam("Authorization") String authHeader, Customer customer);

    @POST
    Customer createCustomer(Customer customer);

    @DELETE
    @Path("/{id}")
    void deleteCustomer(@HeaderParam("Authorization") String authHeader, @PathParam("id") Long id);

    @DELETE
    @Path("/{id}")
    void deleteCustomer(@PathParam("id") Long id);
}

