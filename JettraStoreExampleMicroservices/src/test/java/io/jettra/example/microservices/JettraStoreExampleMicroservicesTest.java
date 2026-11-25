package io.jettra.example.microservices;

import io.jettra.driver.config.JettraClientConfig;
import io.jettra.example.microservices.config.ClusterConfigLoader;
import io.jettra.example.microservices.model.Cliente;
import io.jettra.example.microservices.model.DetalleFactura;
import io.jettra.example.microservices.model.Factura;
import io.jettra.example.microservices.repository.ClienteRest;
import io.jettra.example.microservices.repository.FacturaRest;
import io.jettra.json.JettraJson;
import io.jettra.rest.annotations.DELETE;
import io.jettra.rest.annotations.GET;
import io.jettra.rest.annotations.POST;
import io.jettra.rest.annotations.PUT;
import io.jettra.rest.annotations.Path;
import io.jettra.rest.annotations.PathParam;
import io.jettra.rest.annotations.PermitAll;
import io.jettra.rest.annotations.QueryParam;
import io.jettra.rest.annotations.Secured;
import io.jettra.rest.annotations.accreditation.DeclareRoles;
import io.jettra.rest.annotations.accreditation.RolesAllowed;
import io.jettra.test.annotation.AfterAll;
import io.jettra.test.annotation.BeforeAll;
import io.jettra.test.annotation.DisplayName;
import io.jettra.test.annotation.NotRequiresRunningServer;
import io.jettra.test.annotation.Test;

import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static io.jettra.test.core.JettraAssert.*;

/**
 * Suite de pruebas con JettraTest (sin JUnit) para JettraStoreExampleMicroservices (RF-23).
 */
@NotRequiresRunningServer
public class JettraStoreExampleMicroservicesTest {

    private static final JettraJson JSON = new JettraJson();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static java.nio.file.Path tempRootDir;
    private static java.nio.file.Path tempClusterFile;
    private static App app;
    private static int port;
    private static String adminToken;

    @BeforeAll
    public static void setUp() throws Exception {
        tempRootDir = Files.createTempDirectory("jettra_microservices_rf23_");
        java.nio.file.Path storageDir = tempRootDir.resolve("storage");
        java.nio.file.Path dataDir = tempRootDir.resolve("server-data");
        Files.createDirectories(storageDir);
        Files.createDirectories(dataDir);

        // cluster.config aislado para pruebas sin interferir con nodos de producción
        tempClusterFile = tempRootDir.resolve("cluster.config");
        Files.writeString(tempClusterFile, """
                cluster.name=jettra-microservices-test
                cluster.multinode.active=on
                cluster.consensus.protocol=RAFT
                cluster.healthcheck.timeout.ms=60
                jettra.database=example_factura_db
                jettra.user=admin
                jettra.password=admin-jettra
                jettra.role=admin
                cluster.node.1.id=node-01
                cluster.node.1.role=PRIMARY
                cluster.node.1.ip=127.0.0.1
                cluster.node.1.grpc.port=59401
                cluster.node.1.rest.port=69401
                cluster.node.2.id=node-02
                cluster.node.2.role=SECONDARY
                cluster.node.2.ip=127.0.0.1
                cluster.node.2.grpc.port=59402
                cluster.node.2.rest.port=69402
                cluster.node.3.id=node-03
                cluster.node.3.role=SECONDARY
                cluster.node.3.ip=127.0.0.1
                cluster.node.3.grpc.port=59403
                cluster.node.3.rest.port=69403
                """);

        port = findFreePort();
        System.setProperty("cluster.config.path", tempClusterFile.toAbsolutePath().toString());
        System.setProperty("jettra.storage.path", storageDir.toAbsolutePath().toString());
        System.setProperty("jettra.database", "example_factura_db");
        System.setProperty("server.port", String.valueOf(port));
        System.setProperty("data.dir", dataDir.toAbsolutePath().toString());
        System.setProperty("security.db.path", dataDir.resolve("securitydb").toAbsolutePath().toString());

        app = new App();
        app.startApplication(new String[0]);

        // Autenticación JWT una sola vez para toda la suite
        HttpResponse<String> loginRes = HTTP.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/api/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"admin\",\"password\":\"admin\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, loginRes.statusCode(), "Login JWT debe responder HTTP 200");
        Map<?, ?> auth = JSON.fromJson(loginRes.body(), Map.class);
        adminToken = String.valueOf(auth.get("rawToken"));
        assertNotNull(adminToken);
        assertFalse(adminToken.isBlank());
    }

    @AfterAll
    public static void tearDown() throws Exception {
        if (app != null) {
            app.stop();
        }
        System.clearProperty("cluster.config.path");
        System.clearProperty("jettra.storage.path");
        System.clearProperty("jettra.database");
        System.clearProperty("server.port");
        System.clearProperty("data.dir");
        System.clearProperty("security.db.path");
        if (tempRootDir != null && Files.exists(tempRootDir)) {
            try (var walk = Files.walk(tempRootDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                });
            }
        }
    }

    @Test
    @DisplayName("RF-23.1: Carga limpia de cluster.config y configuración de JettraStoreDriver hacia example_factura_db")
    public void testClusterConfigLoaderBuildsDriverConfig() {
        JettraClientConfig cfg = ClusterConfigLoader.buildClientConfig();
        assertEquals("example_factura_db", cfg.getDefaultDatabase());
        assertEquals("admin", cfg.getUsername());
        var nodes = io.jettra.store.core.JettraConfigValidator.parseClusterNodes(ClusterConfigLoader.loadClusterProperties());
        assertEquals(3, nodes.size());
        assertEquals(59401, nodes.get(0).grpcPort(), "El nodo PRIMARY debe estar configurado en puerto 59401");
    }

    @Test
    @DisplayName("RF-23.2: @RestRepository genera ClienteRest y FacturaRest en tiempo de compilación con seguridad")
    public void testCompileTimeGeneratedRestRepositories() throws Exception {
        // Verificar ClienteRest generado
        Class<?> cliRest = ClienteRest.class;
        assertEquals("/api/v1/clientes", cliRest.getAnnotation(Path.class).value());
        assertTrue(cliRest.isAnnotationPresent(Secured.class));
        assertEquals(List.of("ADMIN", "MANAGER", "USER"), Arrays.asList(cliRest.getAnnotation(RolesAllowed.class).value()));
        assertEquals(List.of("ADMIN", "MANAGER", "USER"), Arrays.asList(cliRest.getAnnotation(DeclareRoles.class).value()));

        Method cliFindById = cliRest.getMethod("findById", String.class);
        assertTrue(cliFindById.isAnnotationPresent(GET.class));
        assertEquals("/{id}", cliFindById.getAnnotation(Path.class).value());
        assertEquals("id", cliFindById.getParameters()[0].getAnnotation(PathParam.class).value());

        Method cliFindAll = cliRest.getMethod("findAll");
        assertTrue(cliFindAll.isAnnotationPresent(GET.class));
        assertTrue(cliFindAll.isAnnotationPresent(PermitAll.class));

        Method cliSave = cliRest.getMethod("save", Cliente.class);
        assertTrue(cliSave.isAnnotationPresent(POST.class));
        assertEquals(List.of("ADMIN", "MANAGER"), Arrays.asList(cliSave.getAnnotation(RolesAllowed.class).value()));

        Method cliUpdate = cliRest.getMethod("update", String.class, Cliente.class);
        assertTrue(cliUpdate.isAnnotationPresent(PUT.class));
        assertEquals("/{id}", cliUpdate.getAnnotation(Path.class).value());

        Method cliDelete = cliRest.getMethod("deleteById", String.class);
        assertTrue(cliDelete.isAnnotationPresent(DELETE.class));
        assertEquals(List.of("ADMIN"), Arrays.asList(cliDelete.getAnnotation(RolesAllowed.class).value()));

        Method cliByCat = cliRest.getMethod("findByCategoria", String.class);
        assertEquals("/find-by-categoria", cliByCat.getAnnotation(Path.class).value());
        assertEquals("categoria", cliByCat.getParameters()[0].getAnnotation(QueryParam.class).value());

        // Verificar FacturaRest generado
        Class<?> facRest = FacturaRest.class;
        assertEquals("/api/v1/facturas-auto", facRest.getAnnotation(Path.class).value());
        assertTrue(facRest.isAnnotationPresent(Secured.class));
        assertTrue(facRest.getMethod("findByClienteId", String.class).isAnnotationPresent(GET.class));
        assertEquals("/count", facRest.getMethod("count").getAnnotation(Path.class).value());
    }

    @Test
    @DisplayName("RF-23.3: Persistencia CRUD de Cliente y Factura sobre example_factura_db mediante JettraStoreDriver")
    public void testRepositoryAndServiceCrudOnExampleFacturaDb() {
        // 1. Crear, consultar, actualizar y eliminar cliente
        Cliente creado = app.getClienteService().crear(
                new Cliente(null, "155000111-9-2026", "Industrias del Canal S.A.", "pagos@canal.pa", "VIP", true));
        assertNotNull(creado.jettraId());
        assertFalse(creado.jettraId().isBlank());
        assertEquals("Industrias del Canal S.A.", app.getClienteService().findById(creado.jettraId()).orElseThrow().nombre());

        // 2. Emitir factura calculando subtotal, ITBMS (7%) y total
        Factura emitida = app.getFacturaService().emitirFactura(new Factura(
                null,
                "FAC-2026-9901",
                creado.jettraId(),
                null,
                "2026-11-24",
                "EMITIDA",
                List.of(
                        DetalleFactura.of("CPU-25", "Nodo JettraStore NVMe", 2, 500.00),
                        DetalleFactura.of("LIC-01", "Suscripción Anual", 1, 200.00)
                ),
                0.0, 0.0, 0.0
        ));
        assertNotNull(emitida.jettraId());
        assertEquals(1200.00, emitida.subtotal());
        assertEquals(84.00, emitida.impuesto());
        assertEquals(1284.00, emitida.total());
        assertEquals("Industrias del Canal S.A.", emitida.clienteNombre());

        // 3. Búsqueda por clienteId y anulación
        assertEquals(1, app.getFacturaService().findByClienteId(creado.jettraId()).size());
        Factura anulada = app.getFacturaService().anularFactura(emitida.jettraId()).orElseThrow();
        assertEquals("ANULADA", anulada.estado());

        // 4. Delegación directa vía FacturaRest generado por @RestRepository
        assertEquals("FAC-2026-9901", app.getFacturaRest().findById(emitida.jettraId()).folio());

        // 5. Limpieza
        assertTrue(app.getFacturaService().eliminarFactura(emitida.jettraId()));
        assertTrue(app.getClienteService().eliminar(creado.jettraId()));
    }

    @Test
    @DisplayName("RF-23.4: Endpoints HTTP, seguridad JWT (401/200), rutas @RestRepository y Swagger-UI")
    public void testHttpEndpointsSecurityAndSwaggerUi() throws Exception {
        // 1. Ping e Info públicos
        HttpResponse<String> ping = HTTP.send(
                HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + port + "/api/v1/system/ping")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, ping.statusCode());
        assertTrue(ping.body().contains("JettraStoreExampleMicroservices"));
        assertTrue(ping.body().contains("example_factura_db"));

        // 2. Endpoint público @PermitAll en ClienteRest (GET /api/v1/clientes)
        HttpResponse<String> publicClientes = HTTP.send(
                HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + port + "/api/v1/clientes")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, publicClientes.statusCode());
        assertTrue(publicClientes.body().contains("Acme Corporation"));

        // 3. Endpoint protegido /api/v1/facturas sin JWT -> 401
        HttpResponse<String> unauth = HTTP.send(
                HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + port + "/api/v1/facturas")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(401, unauth.statusCode());

        // 4. Endpoint protegido /api/v1/facturas y /api/v1/facturas-auto con JWT -> 200
        HttpResponse<String> facturasRes = HTTP.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/facturas"))
                        .header("Authorization", "Bearer " + adminToken)
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, facturasRes.statusCode());
        assertTrue(facturasRes.body().contains("FAC-2026-0001"));

        HttpResponse<String> autoRes = HTTP.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/facturas-auto/count"))
                        .header("Authorization", "Bearer " + adminToken)
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, autoRes.statusCode());
        assertTrue(autoRes.body().contains("\"count\":"));

        // 5. Crear cliente vía POST /api/v1/clientes (ClienteRest) con JWT -> 201
        String cliBody = "{\"ruc\":\"8-888-999\",\"nombre\":\"Cliente HTTP RF23\",\"email\":\"rf23@jettra.io\",\"categoria\":\"PYME\",\"activo\":true}";
        HttpResponse<String> createCli = HTTP.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/clientes"))
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(cliBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createCli.statusCode());
        assertTrue(createCli.body().contains("Cliente HTTP RF23"));

        // 6. Swagger-UI activo
        HttpResponse<String> swagger = HTTP.send(
                HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + port + "/swagger-ui")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, swagger.statusCode());
        assertTrue(swagger.body().contains("JettraStoreExampleMicroservices"));
    }

    private static int findFreePort() {
        try (ServerSocket ss = new ServerSocket(0)) {
            ss.setReuseAddress(true);
            return ss.getLocalPort();
        } catch (Exception e) {
            return 9483;
        }
    }
}
