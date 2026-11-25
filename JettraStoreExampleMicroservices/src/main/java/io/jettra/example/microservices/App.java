package io.jettra.example.microservices;

import io.jettra.driver.JettraClient;
import io.jettra.driver.config.JettraClientConfig;
import io.jettra.example.microservices.config.ClusterConfigLoader;
import io.jettra.example.microservices.model.Cliente;
import io.jettra.example.microservices.model.Factura;
import io.jettra.example.microservices.repository.ClienteRest;
import io.jettra.example.microservices.repository.FacturaRest;
import io.jettra.example.microservices.repository.JettraClienteRepository;
import io.jettra.example.microservices.repository.JettraFacturaRepository;
import io.jettra.example.microservices.rest.FacturaResource;
import io.jettra.example.microservices.rest.SystemInfoResource;
import io.jettra.example.microservices.service.ClienteService;
import io.jettra.example.microservices.service.FacturaService;
import io.jettra.server.core.JettraServer;
import io.jettra.server.core.config.ServerConfig;
import io.jettra.server.core.http.HttpFilter;
import io.jettra.server.core.security.entity.JUser;
import io.jettra.server.flux.FluxModule;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;

/**
 * Aplicación principal de {@code JettraStoreExampleMicroservices}.
 * <p>
 * Conecta con el clúster JettraStore vía {@link JettraClient} usando {@code cluster.config},
 * opera sobre la base de datos {@code example_factura_db} y expone:
 * <ul>
 *   <li>Recursos generados en tiempo de compilación por {@code @RestRepository}: {@link ClienteRest} y {@link FacturaRest}.</li>
 *   <li>Microservicio de negocio {@link FacturaResource} ({@code /api/v1/facturas}).</li>
 *   <li>Telemetría ({@code /api/v1/system/ping}, {@code /api/v1/system/info}) y Swagger-UI ({@code /swagger-ui}).</li>
 * </ul>
 */
public class App {

    private JettraServer server;
    private JettraClient jettraClient;
    private JettraClienteRepository clienteRepository;
    private JettraFacturaRepository facturaRepository;
    private ClienteService clienteService;
    private FacturaService facturaService;
    private ClienteRest clienteRest;
    private FacturaRest facturaRest;
    private FacturaResource facturaResource;
    private SystemInfoResource systemInfoResource;

    public static void main(String[] args) {
        App app = new App();
        app.startApplication(args);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[App] Deteniendo JettraStoreExampleMicroservices...");
            app.stop();
        }));

        try {
            app.getServer().await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void startApplication(String[] args) {
        // 1. Conectar al clúster JettraStore desde cluster.config
        JettraClientConfig clientConfig = ClusterConfigLoader.buildClientConfig();
        String database = clientConfig.getDefaultDatabase();
        this.jettraClient = JettraClient.connect(clientConfig);
        this.jettraClient.createDatabase(database);

        // 2. Inicializar repositorios sobre example_factura_db y recursos generados por @RestRepository
        this.clienteRepository = new JettraClienteRepository(jettraClient, database);
        this.facturaRepository = new JettraFacturaRepository(jettraClient, database);
        this.clienteRepository.initBucketAndIndexes();
        this.facturaRepository.initBucketAndIndexes();

        this.clienteService = new ClienteService(clienteRepository);
        this.facturaService = new FacturaService(facturaRepository, clienteRepository);
        this.clienteService.seedDefaultsIfEmpty();
        this.facturaService.seedDefaultsIfEmpty();

        this.clienteRest = new ClienteRest(clienteRepository);
        this.facturaRest = new FacturaRest(facturaRepository);
        this.facturaResource = new FacturaResource(facturaService);
        int configuredNodes = io.jettra.store.core.JettraConfigValidator
                .parseClusterNodes(ClusterConfigLoader.loadClusterProperties()).size();
        this.systemInfoResource = new SystemInfoResource(database, Math.max(1, configuredNodes));

        // 3. Configurar e iniciar JettraServer
        ServerConfig config = ServerConfig.load();
        int activePort = findAvailablePort(config.getPort());
        config.setPort(activePort);
        this.server = JettraServer.create(config);

        setupSecurityFilter();
        registerRoutes();

        List<Class<?>> controllers = List.of(
                ClienteRest.class,
                FacturaRest.class,
                FacturaResource.class,
                SystemInfoResource.class
        );
        FluxModule.attach(server)
                .swagger("/swagger-ui", "JettraStoreExampleMicroservices API (example_factura_db)", controllers);

        server.get("/", (req, res) -> res.header("Location", "/swagger-ui").status(302).body(""));
        server.get("/docs", (req, res) -> res.header("Location", "/swagger-ui").status(302).body(""));

        try {
            server.start();
            printSuccessBanner(activePort, database);
        } catch (IOException e) {
            throw new RuntimeException("Error iniciando JettraStoreExampleMicroservices en puerto " + activePort, e);
        }
    }

    private void setupSecurityFilter() {
        HttpFilter authFilter = (req, res, chain) -> {
            String path = req.getPath();
            String method = req.getMethod() != null ? req.getMethod().name() : "";

            // GET /api/v1/clientes tiene @PermitAll en ClienteRepository.findAll()
            boolean isPublicClientesList = "GET".equalsIgnoreCase(method) && "/api/v1/clientes".equals(path);
            boolean isProtected = (path.startsWith("/api/v1/clientes")
                    || path.startsWith("/api/v1/facturas")
                    || path.startsWith("/api/v1/facturas-auto"))
                    && !isPublicClientesList;

            if (!isProtected) {
                chain.doFilter(req, res);
                return;
            }

            String authHeader = req.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                res.status(401).json(Map.of(
                        "error", "Acreditación requerida",
                        "message", "Debe proveer un token Bearer JWT válido en Authorization",
                        "status", 401
                ));
                return;
            }

            var userOpt = server.getSecurityService().validateTokenAndGetUser(authHeader);
            if (userOpt.isEmpty()) {
                res.status(401).json(Map.of(
                        "error", "Token inválido o expirado",
                        "status", 401
                ));
                return;
            }
            JUser user = userOpt.get();
            req.setAttribute("currentUser", user);
            chain.doFilter(req, res);
        };
        server.filter(authFilter);
    }

    private void registerRoutes() {
        // Endpoints respaldados por ClienteRest (generado por @RestRepository)
        server.get("/api/v1/clientes", (req, res) -> res.json(clienteRest.findAll()));
        server.get("/api/v1/clientes/count", (req, res) -> res.json(Map.of("count", clienteRest.count())));
        server.get("/api/v1/clientes/find-by-categoria", (req, res) ->
                res.json(clienteRest.findByCategoria(req.getQueryParam("categoria"))));
        server.get("/api/v1/clientes/{id}", (req, res) -> {
            Cliente c = clienteRest.findById(req.getPathParam("id"));
            if (c != null) res.json(c);
            else res.status(404).json(Map.of("error", "Cliente no encontrado"));
        });
        server.post("/api/v1/clientes", (req, res) -> {
            try {
                Cliente payload = req.getBodyAsJson(Cliente.class);
                res.status(201).json(clienteRest.save(payload));
            } catch (Exception e) {
                res.status(400).json(Map.of("error", e.getMessage()));
            }
        });
        server.put("/api/v1/clientes/{id}", (req, res) -> {
            try {
                Cliente payload = req.getBodyAsJson(Cliente.class);
                res.json(clienteRest.update(req.getPathParam("id"), payload));
            } catch (Exception e) {
                res.status(400).json(Map.of("error", e.getMessage()));
            }
        });
        server.delete("/api/v1/clientes/{id}", (req, res) -> {
            boolean deleted = clienteRest.deleteById(req.getPathParam("id"));
            if (deleted) res.json(Map.of("deleted", true, "id", req.getPathParam("id")));
            else res.status(404).json(Map.of("error", "Cliente no encontrado"));
        });

        // Endpoints respaldados por FacturaRest (generado por @RestRepository)
        server.get("/api/v1/facturas-auto", (req, res) -> res.json(facturaRest.findAll()));
        server.get("/api/v1/facturas-auto/count", (req, res) -> res.json(Map.of("count", facturaRest.count())));
        server.get("/api/v1/facturas-auto/{id}", (req, res) -> {
            Factura f = facturaRest.findById(req.getPathParam("id"));
            if (f != null) res.json(f);
            else res.status(404).json(Map.of("error", "Factura no encontrada"));
        });

        // Endpoints de negocio FacturaResource (/api/v1/facturas)
        server.get("/api/v1/facturas", (req, res) ->
                facturaResource.listFacturas(req.getQueryParam("clienteId"), req.getQueryParam("estado"), req, res));
        server.get("/api/v1/facturas/{id}", (req, res) ->
                facturaResource.getFactura(req.getPathParam("id"), req, res));
        server.post("/api/v1/facturas", facturaResource::createFactura);
        server.put("/api/v1/facturas/{id}", (req, res) ->
                facturaResource.updateFactura(req.getPathParam("id"), req, res));
        server.put("/api/v1/facturas/{id}/anular", (req, res) ->
                facturaResource.anularFactura(req.getPathParam("id"), req, res));
        server.delete("/api/v1/facturas/{id}", (req, res) ->
                facturaResource.deleteFactura(req.getPathParam("id"), req, res));

        // Telemetría y Salud
        server.get("/api/v1/system/ping", systemInfoResource::ping);
        server.get("/api/v1/system/info", systemInfoResource::info);
    }

    private void printSuccessBanner(int port, String database) {
        System.out.println("==================================================================================");
        System.out.println(" [JettraStoreExampleMicroservices] Java 25+ | DB: " + database);
        System.out.println("==================================================================================");
        System.out.println(" -> Swagger-UI (JettraFlux)     : http://localhost:" + port + "/swagger-ui");
        System.out.println(" -> Auth JWT (Login)            : http://localhost:" + port + "/api/auth/login");
        System.out.println(" -> Clientes (@RestRepository)  : http://localhost:" + port + "/api/v1/clientes");
        System.out.println(" -> Facturas (@RestRepository)  : http://localhost:" + port + "/api/v1/facturas-auto");
        System.out.println(" -> Facturas (Business Service) : http://localhost:" + port + "/api/v1/facturas");
        System.out.println(" -> Telemetría                  : http://localhost:" + port + "/api/v1/system/info");
        System.out.println("==================================================================================");
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop();
        }
        if (jettraClient != null) {
            try {
                jettraClient.close();
            } catch (Exception ignored) {
            }
        }
    }

    public JettraServer getServer() {
        return server;
    }

    public ClienteService getClienteService() {
        return clienteService;
    }

    public FacturaService getFacturaService() {
        return facturaService;
    }

    public ClienteRest getClienteRest() {
        return clienteRest;
    }

    public FacturaRest getFacturaRest() {
        return facturaRest;
    }

    private int findAvailablePort(int preferredPort) {
        if (isPortAvailable(preferredPort)) {
            return preferredPort;
        }
        for (int p = preferredPort + 1; p < preferredPort + 50; p++) {
            if (isPortAvailable(p)) {
                return p;
            }
        }
        return preferredPort;
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket ss = new ServerSocket(port)) {
            ss.setReuseAddress(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
