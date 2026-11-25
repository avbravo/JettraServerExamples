package io.jettra.server.example;

import io.jettra.aura.server.AuraModule;
import io.jettra.server.core.JettraServer;
import io.jettra.server.core.config.ServerConfig;
import io.jettra.server.core.storage.StorageMode;
import io.jettra.server.example.aura.AuraCatalogPage;
import io.jettra.server.example.aura.AuraCounterPage;
import io.jettra.server.example.aura.AuraPortalPage;
import io.jettra.server.example.aura.AuraUserFormPage;
import io.jettra.server.example.aura.ClienteMicroserviceAuraPage;
import io.jettra.server.example.aura.CustomerAuraPage;
import io.jettra.server.example.aura.FacturaAuraPage;
import io.jettra.server.example.faces.FacesRendererServlet;
import io.jettra.server.example.flux.CatalogoFlux;
import io.jettra.server.example.flux.ClienteMicroserviceFluxView;
import io.jettra.server.example.flux.CustomerFluxView;
import io.jettra.server.example.flux.FacturaFluxView;
import io.jettra.server.example.flux.FluxTemplateView;
import io.jettra.server.example.flux.HealthFluxView;
import io.jettra.server.example.flux.InteractiveCounterFluxView;
import io.jettra.server.example.flux.MetricsFluxView;
import io.jettra.server.example.flux.StorageMonitorFluxView;
import io.jettra.server.example.flux.TelemetryFluxView;
import io.jettra.server.example.model.ClienteDto;
import io.jettra.server.example.model.DetalleFacturaDto;
import io.jettra.server.example.model.FacturaDto;
import io.jettra.server.example.rest.ProductResource;
import io.jettra.server.example.rest.ServerInfoResource;
import io.jettra.server.example.service.ClienteMicroserviceClientService;
import io.jettra.server.example.service.CustomerClientService;
import io.jettra.server.example.service.FacturaClientService;
import io.jettra.server.example.service.ProductService;
import io.jettra.server.example.service.SecurityService;
import io.jettra.server.flux.FluxModule;
import io.jettra.server.jakarta.JakartaContainer;

/**
 * Clase principal de arranque de WebExample.
 * Despliega la aplicación web compatible con Jakarta EE 11, PrimeFaces 14 y JettraFlux.
 */
public class App {

    public static void main(String[] args) {
        ServerConfig config = ServerConfig.load();

        String portProp = System.getProperty("server.port", System.getenv("PORT"));
        if (portProp != null && !portProp.isBlank()) {
            try { config.setPort(Integer.parseInt(portProp.trim())); } catch (Exception ignored) {}
        }

        String storageProp = System.getProperty("storage.mode");
        if (storageProp == null || storageProp.isBlank()) storageProp = System.getProperty("jettra.storage.mode");
        if (storageProp == null || storageProp.isBlank()) storageProp = System.getenv("STORAGE_MODE");
        if (storageProp == null || storageProp.isBlank()) storageProp = System.getenv("JETTRA_STORAGE_MODE");
        if (storageProp != null && !storageProp.isBlank()) {
            config.setStorageMode(StorageMode.fromString(storageProp.trim()));
        }

        JettraServer server = startApplication(config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[App] Solicitud de apagado recibida. Deteniendo JettraServer...");
            server.stop();
        }));

        try {
            System.out.println("[App] Proceso en ejecución continua (escuchando conexiones). Presione Ctrl+C para detener.");
            server.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[App] Hilo principal interrumpido. Saliendo...");
        }
    }

    public static JettraServer startApplication(int port, StorageMode mode) {
        ServerConfig config = ServerConfig.load()
            .setPort(port)
            .setStorageMode(mode);
        return startApplication(config);
    }

    public static JettraServer startApplication(ServerConfig config) {
        int port = config.getPort();
        JettraServer server = JettraServer.create(config);

        // 1. Inicializar Contenedor Maestro Jakarta EE
        JakartaContainer jakarta = JakartaContainer.attach(server);

        // 2. Inicializar Servicios de Negocio y Datos
        SecurityService securityService = new SecurityService(jakarta.getEntityManagerFactory());
        securityService.initDefaultUsers();
        jakarta.getCdiContainer().registerSingleton(SecurityService.class, securityService);

        ProductService productService = new ProductService(jakarta.getEntityManagerFactory());
        productService.initSampleCatalog();
        jakarta.getCdiContainer().registerSingleton(ProductService.class, productService);

        CustomerClientService customerService = new CustomerClientService();
        jakarta.getCdiContainer().registerSingleton(CustomerClientService.class, customerService);

        ClienteMicroserviceClientService clienteMsService = new ClienteMicroserviceClientService();
        jakarta.getCdiContainer().registerSingleton(ClienteMicroserviceClientService.class, clienteMsService);

        FacturaClientService facturaService = new FacturaClientService();
        jakarta.getCdiContainer().registerSingleton(FacturaClientService.class, facturaService);

        // 3. Registrar Servlet de PrimeFaces / Jakarta Faces (Login y Dashboard con plantilla)
        FacesRendererServlet facesServlet = new FacesRendererServlet(
            jakarta.getCdiContainer(), securityService, productService, customerService, server
        );
        jakarta.getServletBridge().registerServlet("/", facesServlet);
        jakarta.getServletBridge().registerServlet("/login.xhtml", facesServlet);
        jakarta.getServletBridge().registerServlet("/dashboard.xhtml", facesServlet);
        jakarta.getServletBridge().registerServlet("/productos.xhtml", facesServlet);
        jakarta.getServletBridge().registerServlet("/clientes.xhtml", facesServlet);
        jakarta.getServletBridge().registerServlet("/clientes", facesServlet);
        jakarta.getServletBridge().registerServlet("/customers.xhtml", facesServlet);
        jakarta.getServletBridge().registerServlet("/customers", facesServlet);
        jakarta.getServletBridge().registerServlet("/facturas.xhtml", facesServlet);
        jakarta.getServletBridge().registerServlet("/facturas", facesServlet);
        jakarta.getServletBridge().registerServlet("/usuarios.xhtml", facesServlet);
        jakarta.getServletBridge().registerServlet("/flux/storage-monitor", facesServlet);
        jakarta.getServletBridge().registerServlet("/flux/telemetry", facesServlet);
        jakarta.getServletBridge().registerServlet("/flux/counter", facesServlet);
        jakarta.getServletBridge().registerServlet("/flux/catalogo", facesServlet);
        jakarta.getServletBridge().registerServlet("/flux/catalogo-flux", facesServlet);
        jakarta.getServletBridge().registerServlet("/flux/metrics", facesServlet);
        jakarta.getServletBridge().registerServlet("/flux/health", facesServlet);
        jakarta.getServletBridge().registerServlet("/logout", facesServlet);

        // 4. Registrar Recursos RESTful (JAX-RS)
        jakarta.getRestDispatcher().registerResource(new ProductResource(productService));
        jakarta.getRestDispatcher().registerResource(new ServerInfoResource(server));

        // 5. Conectar Módulo JettraFlux y Registrar Páginas Reactivas con su Plantilla Exclusiva (FluxTemplateView)
        FluxModule flux = FluxModule.attach(server);
        flux.page("/flux/storage-monitor", "JettraFlux - Monitor de Almacenamiento", () ->
            new FluxTemplateView(
                "Monitor de Almacenamiento - JettraFlux",
                "storage-monitor",
                server,
                null,
                new StorageMonitorFluxView(server.storage().getMode().name(), server.storage().getMode().getDescription(), server)
            )
        );
        flux.page("/flux/telemetry", "JettraFlux - Telemetría del Servidor", () ->
            new FluxTemplateView(
                "Telemetría del Servidor - JettraFlux",
                "telemetry",
                server,
                null,
                new TelemetryFluxView()
            )
        );
        flux.page("/flux/counter", "JettraFlux - Contador Reactivo", () ->
            new FluxTemplateView(
                "Contador Reactivo - JettraFlux",
                "counter",
                server,
                null,
                new InteractiveCounterFluxView()
            )
        );
        flux.page("/flux/catalogo", "JettraFlux - Catálogo de Productos", () ->
            new FluxTemplateView(
                "Catálogo Reactivo de Productos - JettraFlux",
                "catalogo-flux",
                server,
                null,
                new CatalogoFlux(productService)
            )
        );
        flux.page("/flux/customers", "JettraFlux - Clientes MicroProfile Rest Client", () ->
            new FluxTemplateView(
                "Gestión de Clientes (MP-Rest Client + JWT) - JettraFlux",
                "customers",
                server,
                null,
                new CustomerFluxView(customerService)
            )
        );
        flux.page("/flux/clientes", "JettraFlux - Clientes MicroProfile Rest Client", () ->
            new FluxTemplateView(
                "Gestión de Clientes (MP-Rest Client + JWT) - JettraFlux",
                "customers",
                server,
                null,
                new CustomerFluxView(customerService)
            )
        );
        flux.page("/flux/metrics", "JettraFlux - Métricas del Servidor", () ->
            new FluxTemplateView(
                "Métricas del Servidor - JettraFlux",
                "metrics",
                server,
                null,
                new MetricsFluxView(server)
            )
        );
        flux.page("/flux/health", "JettraFlux - Estado de Salud (Health)", () ->
            new FluxTemplateView(
                "Estado de Salud (Health) - JettraFlux",
                "health",
                server,
                null,
                new HealthFluxView(server)
            )
        );
        // Rutas JettraFlux CRUD para Facturas y Clientes (example_factura_db via RestClient + JWT)
        server.get("/flux/facturas", (req, res) -> {
            String editId = req.getQueryParam("edit");
            String confirmDel = req.getQueryParam("confirm_delete");
            String msg = req.getQueryParam("msg");
            FluxTemplateView tpl = new FluxTemplateView(
                "Facturación (example_factura_db) - JettraFlux",
                "facturas",
                server,
                null,
                new FacturaFluxView(facturaService, editId, confirmDel, msg)
            );
            res.html(new io.jettra.server.flux.FluxRenderer().render(tpl.build(), "Facturación - JettraFlux"));
        });
        server.post("/flux/facturas", (req, res) -> {
            var form = parseForm(req.getBodyAsString());
            String action = form.getOrDefault("action", "create");
            String id = form.get("id");
            if ("delete".equalsIgnoreCase(action) && id != null && !id.isBlank()) {
                facturaService.delete(id);
            } else if ("anular".equalsIgnoreCase(action) && id != null && !id.isBlank()) {
                facturaService.anular(id);
            } else {
                int qty = parseIntSafe(form.get("cantidad"), 1);
                double price = parseDoubleSafe(form.get("precio"), 100.0);
                DetalleFacturaDto det = DetalleFacturaDto.of("SRV-FLUX", form.getOrDefault("descripcion", "Servicio"), qty, price);
                FacturaDto dto = new FacturaDto(id, form.getOrDefault("folio", ""), form.getOrDefault("clienteId", "CLI-001"),
                        form.getOrDefault("clienteNombre", "Cliente"), java.time.LocalDate.now().toString(),
                        "EMITIDA", java.util.List.of(det), det.subtotal(), 0.0, 0.0);
                if ("update".equalsIgnoreCase(action) && id != null && !id.isBlank()) {
                    facturaService.update(id, dto);
                } else {
                    facturaService.create(dto);
                }
            }
            res.header("Location", "/flux/facturas").status(302).body("");
        });
        server.get("/flux/clientes-ms", (req, res) -> {
            String editId = req.getQueryParam("edit");
            String confirmDel = req.getQueryParam("confirm_delete");
            String msg = req.getQueryParam("msg");
            FluxTemplateView tpl = new FluxTemplateView(
                "Clientes (example_factura_db) - JettraFlux",
                "clientes-ms",
                server,
                null,
                new ClienteMicroserviceFluxView(clienteMsService, editId, confirmDel, msg)
            );
            res.html(new io.jettra.server.flux.FluxRenderer().render(tpl.build(), "Clientes - JettraFlux"));
        });
        server.post("/flux/clientes-ms", (req, res) -> {
            var form = parseForm(req.getBodyAsString());
            String action = form.getOrDefault("action", "create");
            String id = form.get("id");
            if ("delete".equalsIgnoreCase(action) && id != null && !id.isBlank()) {
                clienteMsService.delete(id);
            } else {
                ClienteDto dto = new ClienteDto(id, form.getOrDefault("ruc", ""), form.getOrDefault("nombre", ""),
                        form.getOrDefault("email", ""), form.getOrDefault("categoria", "ENTERPRISE"), true);
                if ("update".equalsIgnoreCase(action) && id != null && !id.isBlank()) {
                    clienteMsService.update(id, dto);
                } else {
                    clienteMsService.create(dto);
                }
            }
            res.header("Location", "/flux/clientes-ms").status(302).body("");
        });

        // 6. Registrar Swagger-UI con Acreditación de Seguridad usando JettraFlux
        flux.swagger("/swagger-ui", "WebExample - Catálogo & Microservicios API", java.util.List.of(
            ProductResource.class,
            ServerInfoResource.class
        ));

        // Alias de conveniencia para Swagger-UI
        server.get("/swagger", (req, res) -> res.header("Location", "/swagger-ui").status(302).body(""));
        server.get("/docs", (req, res) -> res.header("Location", "/swagger-ui").status(302).body(""));
        server.get("/api-docs", (req, res) -> res.header("Location", "/swagger-ui").status(302).body(""));

        // 7. Conectar Módulo JettraAura (Framework Web Orientado a Componentes 1:1)
        AuraModule aura = AuraModule.attach(server);
        aura.getApplication().setCdiContainer(jakarta.getCdiContainer());
        aura.page("/aura/demo", AuraPortalPage.class);
        aura.page("/aura", AuraPortalPage.class);
        aura.page("/aura/catalogo", AuraCatalogPage.class);
        aura.page("/aura/customers", CustomerAuraPage.class);
        aura.page("/aura/clientes", CustomerAuraPage.class);
        aura.page("/aura/clientes-ms", ClienteMicroserviceAuraPage.class);
        aura.page("/aura/facturas", FacturaAuraPage.class);
        aura.page("/aura/formulario", AuraUserFormPage.class);
        aura.page("/aura/contador", AuraCounterPage.class);

        // Iniciar el servidor
        try {
            server.start();
        } catch (java.net.BindException e) {
            System.err.println("❌ ERROR: El puerto " + port + " ya está ocupado por otro proceso o contenedor (ej. Docker).");
            System.err.println("   -> Puedes cambiar el puerto en 'microprofile-config.properties' (server.port=8080) o pasar '-Dserver.port=8085'.");
            throw new RuntimeException("El puerto " + port + " ya está en uso (BindException).", e);
        } catch (Exception e) {
            throw new RuntimeException("Error iniciando JettraServer en puerto " + port, e);
        }

        System.out.println("==================================================================================");
        System.out.println("🚀 [WebExample] Aplicación Web Jakarta EE + PrimeFaces + JettraFlux LISTA");
        System.out.println(" -> URL Login PrimeFaces : http://localhost:" + port + "/login.xhtml");
        System.out.println(" -> URL Dashboard        : http://localhost:" + port + "/dashboard.xhtml");
        System.out.println(" -> URL Catálogo JPA     : http://localhost:" + port + "/productos.xhtml");
        System.out.println(" -> URL Clientes Faces   : http://localhost:" + port + "/clientes.xhtml");
        System.out.println(" -> Swagger-UI (JettraFlux): http://localhost:" + port + "/swagger-ui");
        System.out.println(" -> Vistas JettraFlux    : http://localhost:" + port + "/flux/storage-monitor");
        System.out.println("                         : http://localhost:" + port + "/flux/telemetry");
        System.out.println("                         : http://localhost:" + port + "/flux/counter");
        System.out.println("                         : http://localhost:" + port + "/flux/catalogo");
        System.out.println("                         : http://localhost:" + port + "/flux/customers");
        System.out.println("                         : http://localhost:" + port + "/flux/metrics");
        System.out.println("                         : http://localhost:" + port + "/flux/health");
        System.out.println(" -> Vistas JettraAura :");
        System.out.println("                         : http://localhost:" + port + "/aura/demo");
        System.out.println("                         : http://localhost:" + port + "/aura/catalogo");
        System.out.println("                         : http://localhost:" + port + "/aura/customers");
        System.out.println("                         : http://localhost:" + port + "/aura/formulario");
        System.out.println("                         : http://localhost:" + port + "/aura/contador");
        System.out.println(" -> Endpoints REST       : http://localhost:" + port + "/api/productos");
        System.out.println("                         : http://localhost:" + port + "/api/info");
        System.out.println("==================================================================================");

        return server;
    }

    private static java.util.Map<String, String> parseForm(String body) {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        if (body == null || body.isBlank()) return map;
        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String k = java.net.URLDecoder.decode(pair.substring(0, eq), java.nio.charset.StandardCharsets.UTF_8);
                String v = java.net.URLDecoder.decode(pair.substring(eq + 1), java.nio.charset.StandardCharsets.UTF_8);
                map.put(k, v);
            }
        }
        return map;
    }

    private static int parseIntSafe(String s, int def) {
        try { return s != null ? Integer.parseInt(s.trim()) : def; } catch (Exception e) { return def; }
    }

    private static double parseDoubleSafe(String s, double def) {
        try { return s != null ? Double.parseDouble(s.trim()) : def; } catch (Exception e) { return def; }
    }
}
