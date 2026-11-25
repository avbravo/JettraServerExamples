package io.jettra.example.microservices.rest;

import io.jettra.rest.annotations.GET;
import io.jettra.rest.annotations.Path;
import io.jettra.server.core.http.HttpRequest;
import io.jettra.server.core.http.HttpResponse;
import io.jettra.server.openapi.annotations.Operation;
import io.jettra.server.openapi.annotations.Tag;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoints públicos de telemetría, salud y estado del clúster JettraStore.
 */
@Path("/api/v1/system")
@Tag(name = "Sistema & Telemetría", description = "Salud y metadatos del microservicio JettraStoreExampleMicroservices")
public class SystemInfoResource {

    private final String databaseName;
    private final int clusterNodesCount;

    public SystemInfoResource(String databaseName, int clusterNodesCount) {
        this.databaseName = databaseName;
        this.clusterNodesCount = clusterNodesCount;
    }

    @GET
    @Path("/ping")
    @Operation(summary = "Ping de disponibilidad")
    public void ping(HttpRequest req, HttpResponse res) {
        res.json(Map.of(
                "status", "UP",
                "service", "JettraStoreExampleMicroservices",
                "database", databaseName,
                "timestamp", Instant.now().toString()
        ));
    }

    @GET
    @Path("/info")
    @Operation(summary = "Información del entorno y clúster JettraStore")
    public void info(HttpRequest req, HttpResponse res) {
        res.json(Map.of(
                "service", "JettraStoreExampleMicroservices",
                "javaVersion", System.getProperty("java.version"),
                "database", databaseName,
                "clusterNodesConfigured", clusterNodesCount,
                "restRepositoryGenerated", "ClienteRest, FacturaRest"
        ));
    }
}
