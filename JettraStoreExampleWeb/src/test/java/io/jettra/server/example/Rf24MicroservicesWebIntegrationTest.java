package io.jettra.server.example;

import com.sun.net.httpserver.HttpServer;
import io.jettra.json.JettraJson;
import io.jettra.jwt.JettraJWT;
import io.jettra.server.core.JettraServer;
import io.jettra.server.core.config.ServerConfig;
import io.jettra.server.core.storage.StorageMode;
import io.jettra.server.example.bean.ClienteMicroserviceBean;
import io.jettra.server.example.bean.FacturaBean;
import io.jettra.server.example.client.JettraJwtAuthManager;
import io.jettra.server.example.flux.ClienteMicroserviceFluxView;
import io.jettra.server.example.flux.FacturaFluxView;
import io.jettra.server.example.model.ClienteDto;
import io.jettra.server.example.model.DetalleFacturaDto;
import io.jettra.server.example.model.FacturaDto;
import io.jettra.server.example.service.ClienteMicroserviceClientService;
import io.jettra.server.example.service.FacturaClientService;
import io.jettra.server.flux.FluxRenderer;
import io.jettra.test.annotation.AfterAll;
import io.jettra.test.annotation.BeforeAll;
import io.jettra.test.annotation.DisplayName;
import io.jettra.test.annotation.NotRequiresRunningServer;
import io.jettra.test.annotation.Test;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static io.jettra.test.core.JettraAssert.*;

/**
 * Suite integral de pruebas JettraTest (sin JUnit) para RF-24:
 * - Comunicación de JettraStoreExampleWeb con JettraStoreExampleMicroservices vía RestClient + JWT.
 * - Renovación automática de token ante 401.
 * - Formularios CRUD completos en las 3 categorías: Jakarta Faces, JettraFlux y JettraAura (Astra).
 */
@NotRequiresRunningServer
public class Rf24MicroservicesWebIntegrationTest {

    private static final String TEST_JWT_SECRET = "jettra_microservices_super_secret_jwt_key_2026";
    private static final JettraJson JSON = new JettraJson();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static HttpServer mockMicroservice;
    private static int mockPort;
    private static JettraServer webServer;
    private static int webPort;
    private static Path tempDataDir;

    private static final Map<String, ClienteDto> REMOTE_CLIENTES = new ConcurrentHashMap<>();
    private static final Map<String, FacturaDto> REMOTE_FACTURAS = new ConcurrentHashMap<>();
    private static final AtomicInteger LOGIN_CALLS = new AtomicInteger(0);
    private static final AtomicInteger SIMULATE_EXPIRED_ONCE = new AtomicInteger(0);

    @BeforeAll
    public static void setUp() throws Exception {
        JettraJWT jwt = new JettraJWT(TEST_JWT_SECRET, 3600000L);
        REMOTE_CLIENTES.clear();
        REMOTE_FACTURAS.clear();
        REMOTE_CLIENTES.put("CLI-100", new ClienteDto("CLI-100", "155-100-2026", "Acme Microservice S.A.", "billing@acme.io", "ENTERPRISE", true));
        REMOTE_FACTURAS.put("FAC-100", new FacturaDto(
                "FAC-100", "FAC-2026-0100", "CLI-100", "Acme Microservice S.A.", "2026-11-24", "EMITIDA",
                List.of(DetalleFacturaDto.of("SRV-01", "Licencia JettraStore", 1, 500.00)), 500.00, 35.00, 535.00));

        // 1. Iniciar microservicio mock compatible con JettraStoreExampleMicroservices (JWT Bearer + CRUD)
        mockMicroservice = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockPort = mockMicroservice.getAddress().getPort();

        mockMicroservice.createContext("/api/auth/login", ex -> {
            LOGIN_CALLS.incrementAndGet();
            String token = jwt.generateToken("admin", List.of("ADMIN", "MANAGER", "USER"));
            byte[] resp = ("{\"rawToken\":\"" + token + "\"}").getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(200, resp.length);
            ex.getResponseBody().write(resp);
            ex.close();
        });

        mockMicroservice.createContext("/api/v1/clientes", ex -> {
            String auth = ex.getRequestHeaders().getFirst("Authorization");
            if (SIMULATE_EXPIRED_ONCE.compareAndSet(1, 0)) {
                sendJson(ex, 401, "{\"error\":\"Token expirado\"}");
                return;
            }
            if (!isValidBearer(jwt, auth)) {
                sendJson(ex, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            String suffix = path.length() > "/api/v1/clientes".length() ? path.substring("/api/v1/clientes".length()) : "";

            if ("GET".equals(method) && suffix.isEmpty()) {
                sendJson(ex, 200, JSON.toJson(List.copyOf(REMOTE_CLIENTES.values())));
            } else if ("GET".equals(method) && suffix.startsWith("/")) {
                String id = suffix.substring(1);
                ClienteDto found = REMOTE_CLIENTES.get(id);
                if (found != null) sendJson(ex, 200, JSON.toJson(found));
                else sendJson(ex, 404, "{\"error\":\"Not found\"}");
            } else if ("POST".equals(method)) {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                ClienteDto in = JSON.fromJson(body, ClienteDto.class);
                String id = (in.jettraId() != null && !in.jettraId().isBlank()) ? in.jettraId() : "CLI-" + UUID.randomUUID().toString().substring(0, 6);
                ClienteDto saved = in.withJettraId(id);
                REMOTE_CLIENTES.put(id, saved);
                sendJson(ex, 201, JSON.toJson(saved));
            } else if ("PUT".equals(method) && suffix.startsWith("/")) {
                String id = suffix.substring(1);
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                ClienteDto in = JSON.fromJson(body, ClienteDto.class).withJettraId(id);
                REMOTE_CLIENTES.put(id, in);
                sendJson(ex, 200, JSON.toJson(in));
            } else if ("DELETE".equals(method) && suffix.startsWith("/")) {
                String id = suffix.substring(1);
                REMOTE_CLIENTES.remove(id);
                sendJson(ex, 200, "{\"deleted\":true}");
            } else {
                sendJson(ex, 404, "{\"error\":\"Not found\"}");
            }
        });

        mockMicroservice.createContext("/api/v1/facturas", ex -> {
            String auth = ex.getRequestHeaders().getFirst("Authorization");
            if (!isValidBearer(jwt, auth)) {
                sendJson(ex, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            String suffix = path.length() > "/api/v1/facturas".length() ? path.substring("/api/v1/facturas".length()) : "";

            if ("GET".equals(method) && suffix.isEmpty()) {
                sendJson(ex, 200, JSON.toJson(List.copyOf(REMOTE_FACTURAS.values())));
            } else if ("GET".equals(method) && suffix.startsWith("/")) {
                String id = suffix.substring(1);
                FacturaDto found = REMOTE_FACTURAS.get(id);
                if (found != null) sendJson(ex, 200, JSON.toJson(found));
                else sendJson(ex, 404, "{\"error\":\"Not found\"}");
            } else if ("POST".equals(method)) {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                FacturaDto in = FacturaDto.fromMap(JSON.fromJson(body, Map.class));
                String id = (in.jettraId() != null && !in.jettraId().isBlank()) ? in.jettraId() : "FAC-" + UUID.randomUUID().toString().substring(0, 6);
                double sub = in.detalles() != null && !in.detalles().isEmpty()
                        ? in.detalles().stream().mapToDouble(DetalleFacturaDto::subtotal).sum() : in.subtotal();
                double tax = Math.round(sub * 0.07 * 100.0) / 100.0;
                double tot = Math.round((sub + tax) * 100.0) / 100.0;
                FacturaDto saved = new FacturaDto(id, in.folio() != null && !in.folio().isBlank() ? in.folio() : "FAC-2026-" + id,
                        in.clienteId(), in.clienteNombre(), in.fechaEmision(), "EMITIDA", in.detalles(), sub, tax, tot);
                REMOTE_FACTURAS.put(id, saved);
                sendJson(ex, 201, JSON.toJson(saved));
            } else if ("PUT".equals(method) && suffix.endsWith("/anular")) {
                String id = suffix.substring(1, suffix.length() - "/anular".length());
                FacturaDto existing = REMOTE_FACTURAS.get(id);
                if (existing != null) {
                    FacturaDto anulada = existing.withEstado("ANULADA");
                    REMOTE_FACTURAS.put(id, anulada);
                    sendJson(ex, 200, JSON.toJson(anulada));
                } else {
                    sendJson(ex, 404, "{\"error\":\"Not found\"}");
                }
            } else if ("PUT".equals(method) && suffix.startsWith("/")) {
                String id = suffix.substring(1);
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                FacturaDto in = FacturaDto.fromMap(JSON.fromJson(body, Map.class));
                FacturaDto updated = new FacturaDto(id, in.folio(), in.clienteId(), in.clienteNombre(),
                        in.fechaEmision(), in.estado(), in.detalles(), in.subtotal(), in.impuesto(), in.total());
                REMOTE_FACTURAS.put(id, updated);
                sendJson(ex, 200, JSON.toJson(updated));
            } else if ("DELETE".equals(method) && suffix.startsWith("/")) {
                String id = suffix.substring(1);
                REMOTE_FACTURAS.remove(id);
                sendJson(ex, 200, "{\"deleted\":true}");
            } else {
                sendJson(ex, 404, "{\"error\":\"Not found\"}");
            }
        });
        mockMicroservice.start();

        System.setProperty("backend.api.url", "http://127.0.0.1:" + mockPort);
        System.setProperty("backend.jwt.secret", TEST_JWT_SECRET);
        JettraJwtAuthManager.getInstance().invalidateToken();

        // 2. Iniciar JettraStoreExampleWeb en puerto aislado
        webPort = findFreePort();
        tempDataDir = Files.createTempDirectory("jettra_web_rf24_");
        ServerConfig cfg = ServerConfig.load()
                .setPort(webPort)
                .setStorageMode(StorageMode.JVM_RAM);
        System.setProperty("data.dir", tempDataDir.toAbsolutePath().toString());
        webServer = App.startApplication(cfg);
    }

    @AfterAll
    public static void tearDown() throws Exception {
        if (webServer != null) webServer.stop();
        if (mockMicroservice != null) mockMicroservice.stop(0);
        System.clearProperty("backend.api.url");
        System.clearProperty("backend.jwt.secret");
        System.clearProperty("data.dir");
        JettraJwtAuthManager.getInstance().invalidateToken();
        if (tempDataDir != null && Files.exists(tempDataDir)) {
            try (var walk = Files.walk(tempDataDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                });
            }
        }
    }

    @Test
    @DisplayName("RF-24.1: RestClient + JWT realiza CRUD completo contra microservicio y renueva token automáticamente ante 401")
    public void testRestClientJwtCrudAnd401AutoRefresh() {
        String baseUrl = "http://127.0.0.1:" + mockPort;
        ClienteMicroserviceClientService cliSvc = new ClienteMicroserviceClientService(baseUrl);
        FacturaClientService facSvc = new FacturaClientService(baseUrl);

        // Simular 401 en la primera llamada -> proxy debe renovar JWT y reintentar con éxito
        SIMULATE_EXPIRED_ONCE.set(1);
        int loginsBefore = LOGIN_CALLS.get();
        List<ClienteDto> clientes = cliSvc.listAll();
        assertTrue(clientes.size() >= 1, "Debe recuperar clientes tras renovación automática de JWT");
        assertTrue(LOGIN_CALLS.get() > loginsBefore, "Debe haber invocado /api/auth/login para renovar JWT");

        // CRUD Cliente vía RestClient + JWT
        ClienteDto createdCli = cliSvc.create(new ClienteDto(null, "8-999-111", "Cliente RestClient", "rc@jettra.io", "VIP", true));
        assertNotNull(createdCli.jettraId());
        ClienteDto updatedCli = cliSvc.update(createdCli.jettraId(),
                new ClienteDto(createdCli.jettraId(), "8-999-111", "Cliente Actualizado", "rc@jettra.io", "ENTERPRISE", true));
        assertEquals("Cliente Actualizado", updatedCli.nombre());
        assertTrue(cliSvc.delete(createdCli.jettraId()));

        // CRUD Factura vía RestClient + JWT
        FacturaDto createdFac = facSvc.create(new FacturaDto(
                null, "FAC-2026-RF24", "CLI-100", "Acme Microservice S.A.", "2026-11-24", "EMITIDA",
                List.of(DetalleFacturaDto.of("ITEM-1", "Cluster Node", 2, 300.00)), 600.00, 42.00, 642.00));
        assertNotNull(createdFac.jettraId());
        assertEquals(642.00, createdFac.total());
        FacturaDto anulada = facSvc.anular(createdFac.jettraId()).orElseThrow();
        assertEquals("ANULADA", anulada.estado());
        assertTrue(facSvc.delete(createdFac.jettraId()));
    }

    @Test
    @DisplayName("RF-24.2: Categoría 1 (Jakarta Faces) — Beans y página /facturas.xhtml CRUD con RestClient + JWT")
    public void testCategory1JakartaFacesCrud() throws Exception {
        String baseUrl = "http://127.0.0.1:" + mockPort;
        FacturaBean facBean = new FacturaBean();
        facBean.setFacturaService(new FacturaClientService(baseUrl));
        facBean.init();
        assertFalse(facBean.getFacturas().isEmpty());

        facBean.setFolio("FAC-2026-JSF1");
        facBean.setClienteId("CLI-100");
        facBean.setClienteNombre("Cliente JSF");
        facBean.setItemDescripcion("Servicio Faces");
        facBean.setItemCantidad(2);
        facBean.setItemPrecio(100.00);
        facBean.save();
        assertTrue(facBean.getFacturas().stream().anyMatch(f -> "FAC-2026-JSF1".equals(f.folio())));

        ClienteMicroserviceBean cliBean = new ClienteMicroserviceBean();
        cliBean.setClienteService(new ClienteMicroserviceClientService(baseUrl));
        cliBean.init();
        assertFalse(cliBean.getClientes().isEmpty());

        HttpResponse<String> facesRes = HTTP.send(
                HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + webPort + "/facturas.xhtml")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, facesRes.statusCode(), "GET /facturas.xhtml debe responder 200");
        assertTrue(facesRes.body().contains("example_factura_db"), "Body /facturas.xhtml sin example_factura_db: len=" + facesRes.body().length());
        assertTrue(facesRes.body().contains("FAC-2026-0100"), "Body /facturas.xhtml sin FAC-2026-0100");
    }

    @Test
    @DisplayName("RF-24.3: Categoría 2 (JettraFlux) — Vistas CRUD puras sin window.confirm y con Modal nativo")
    public void testCategory2JettraFluxCrud() throws Exception {
        String baseUrl = "http://127.0.0.1:" + mockPort;
        FluxRenderer renderer = new FluxRenderer();
        String facHtml = renderer.render(
                new FacturaFluxView(new FacturaClientService(baseUrl), null, "FAC-100", null).build(),
                "Facturas Flux");
        assertTrue(facHtml.contains("FAC-2026-0100"));
        assertTrue(facHtml.contains("deleteFacturaFluxModal"), "Debe usar Modal nativo de JettraFlux para confirmar borrado");
        assertFalse(facHtml.contains("window.confirm"), "No debe usar window.confirm en JettraFlux");

        String cliHtml = renderer.render(
                new ClienteMicroserviceFluxView(new ClienteMicroserviceClientService(baseUrl), null, "CLI-100", null).build(),
                "Clientes Flux");
        assertTrue(cliHtml.contains("Acme Microservice S.A."));
        assertTrue(cliHtml.contains("deleteClienteFluxModal"));
        assertFalse(cliHtml.contains("window.confirm"));

        HttpResponse<String> liveFac = HTTP.send(
                HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + webPort + "/flux/facturas")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, liveFac.statusCode());
        assertTrue(liveFac.body().contains("Facturación Electrónica"));

        HttpResponse<String> liveCli = HTTP.send(
                HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + webPort + "/flux/clientes-ms")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, liveCli.statusCode());
        assertTrue(liveCli.body().contains("Clientes Microservices"));
    }

    @Test
    @DisplayName("RF-24.4: Categoría 3 (JettraAura / Astra) — Páginas CRUD /aura/facturas y /aura/clientes-ms")
    public void testCategory3JettraAuraCrud() throws Exception {
        HttpResponse<String> auraFac = HTTP.send(
                HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + webPort + "/aura/facturas")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, auraFac.statusCode(), "GET /aura/facturas debe responder 200. Body: " + auraFac.body());
        assertTrue(auraFac.body().contains("Emitir Factura"), "Body /aura/facturas sin Emitir Factura");
        assertTrue(auraFac.body().contains("FAC-2026-0100"), "Body /aura/facturas sin FAC-2026-0100");

        HttpResponse<String> auraCli = HTTP.send(
                HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + webPort + "/aura/clientes-ms")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, auraCli.statusCode(), "GET /aura/clientes-ms debe responder 200. Body: " + auraCli.body());
        assertTrue(auraCli.body().contains("example_factura_db"), "Body /aura/clientes-ms sin example_factura_db");
        assertTrue(auraCli.body().contains("Acme Microservice S.A."), "Body /aura/clientes-ms sin Acme Microservice S.A.");
    }

    private static boolean isValidBearer(JettraJWT jwt, String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return false;
        String token = authHeader.substring("Bearer ".length()).trim();
        try {
            return jwt.validateToken(token);
        } catch (Exception e) {
            return false;
        }
    }

    private static void sendJson(com.sun.net.httpserver.HttpExchange ex, int status, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private static int findFreePort() {
        try (ServerSocket ss = new ServerSocket(0)) {
            ss.setReuseAddress(true);
            return ss.getLocalPort();
        } catch (Exception e) {
            return 9494;
        }
    }
}
