package io.jettra.server.example;

import io.jettra.server.core.JettraServer;
import io.jettra.server.core.storage.StorageMode;
import io.jettra.server.example.model.Product;
import io.jettra.server.example.service.ProductService;
import io.jettra.test.annotation.DisplayName;
import io.jettra.test.annotation.Test;
import io.jettra.test.core.JettraAssert;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Suite de pruebas integradas de WebExample validada con el framework JettraTest.
 * 100% libre de dependencias JUnit.
 */
public class WebExampleTest {

    private static final int TEST_PORT = 9333;

    @Test
    @DisplayName("1. Verificar renderizado del Formulario de Login PrimeFaces")
    public void testLoginPageRendering() throws Exception {
        JettraServer server = App.startApplication(TEST_PORT, StorageMode.JVM_RAM);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + TEST_PORT + "/login.xhtml"))
                .GET()
                .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            JettraAssert.assertEquals(200, res.statusCode(), "El formulario de Login debe responder HTTP 200");
            JettraAssert.assertTrue(res.body().contains("JettraServer"), "Debe contener el título de la app");
            JettraAssert.assertTrue(res.body().contains("name=\"username\""), "Debe contener el campo de usuario");
            JettraAssert.assertTrue(res.body().contains("name=\"password\""), "Debe contener el campo de contraseña");
            JettraAssert.assertTrue(res.body().contains("Iniciar Sesión"), "Debe contener el botón de inicio de sesión");
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("2. Verificar validación de credenciales en Login (Rechazo de credenciales inválidas)")
    public void testLoginValidationFailure() throws Exception {
        JettraServer server = App.startApplication(TEST_PORT + 1, StorageMode.JVM_RAM);
        try {
            HttpClient client = HttpClient.newHttpClient();
            String formData = "username=usuario_invalido&password=clave_erronea";

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 1) + "/login.xhtml"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            JettraAssert.assertEquals(200, res.statusCode(), "Debe recargar el formulario con error");
            JettraAssert.assertTrue(res.body().contains("Credenciales inválidas"), "Debe mostrar el mensaje de validación de error");
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("3. Verificar autenticación exitosa y redirección al Dashboard")
    public void testLoginSuccessRedirect() throws Exception {
        JettraServer server = App.startApplication(TEST_PORT + 2, StorageMode.JVM_RAM);
        try {
            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
            String formData = "username=admin&password=admin123";

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 2) + "/login.xhtml"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            JettraAssert.assertEquals(302, res.statusCode(), "Las credenciales válidas deben redirigir con HTTP 302");
            String location = res.headers().firstValue("Location").orElse("");
            JettraAssert.assertTrue(location.contains("/dashboard.xhtml"), "Debe redirigir al Dashboard");
            String setCookie = res.headers().firstValue("Set-Cookie").orElse("");
            JettraAssert.assertTrue(setCookie.contains("JSESSIONID="), "Debe emitir cookie de sesión JSESSIONID");
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("4. Verificar estructura completa de la Plantilla Dashboard (Top, Left, Center, Footer)")
    public void testDashboardTemplateStructure() throws Exception {
        JettraServer server = App.startApplication(TEST_PORT + 3, StorageMode.JVM_RAM);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 3) + "/dashboard.xhtml"))
                .GET()
                .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            JettraAssert.assertEquals(200, res.statusCode(), "El dashboard debe responder HTTP 200");
            String html = res.body();

            // TOP: Header, Título, Modo Persistencia, Usuario y Logout
            JettraAssert.assertTrue(html.contains("ui-layout-top"), "Debe contener la sección TOP (Header)");
            JettraAssert.assertTrue(html.contains("Cerrar Sesión"), "Debe contener el botón de Logout en TOP");
            JettraAssert.assertTrue(html.contains("hamburger-btn"), "Debe contener el botón responsivo de menú hamburguesa");
            JettraAssert.assertTrue(html.contains("theme-toggle-btn"), "Debe contener el cambiador de temas Claro / Oscuro");

            // LEFT: Menú lateral con enlaces a páginas de ejemplo y JettraFlux
            JettraAssert.assertTrue(html.contains("ui-layout-left"), "Debe contener la sección LEFT (Sidebar Menu)");
            JettraAssert.assertTrue(html.contains("/swagger-ui"), "Debe incluir enlace a Swagger-UI en menú lateral");
            JettraAssert.assertTrue(html.contains("view=metrics"), "Debe incluir enlace a vista Flux de métricas");
            JettraAssert.assertTrue(html.contains("view=health"), "Debe incluir enlace a vista Flux de estado de salud");
            JettraAssert.assertTrue(html.contains("/flux/storage-monitor"), "Debe incluir enlace a vista JettraFlux Storage");
            JettraAssert.assertTrue(html.contains("/flux/telemetry"), "Debe incluir enlace a vista JettraFlux Telemetry");
            JettraAssert.assertTrue(html.contains("/flux/counter"), "Debe incluir enlace a vista JettraFlux Counter");
            JettraAssert.assertTrue(html.contains("/productos.xhtml"), "Debe incluir enlace a página de Catálogo JPA");

            // CENTER: Tarjetas KPI, métricas y contenido principal
            JettraAssert.assertTrue(html.contains("ui-layout-center"), "Debe contener la sección CENTER (Contenido Principal)");
            JettraAssert.assertTrue(html.contains("Modo Persistencia"), "Debe mostrar tarjeta de modo de persistencia");
            JettraAssert.assertTrue(html.contains("Catálogo Productos (JPA)"), "Debe mostrar métricas de productos");
            JettraAssert.assertTrue(html.contains("Concurrencia Loom"), "Debe mostrar el indicador de Virtual Threads");

            // FOOTER: Barra inferior de estado del servidor
            JettraAssert.assertTrue(html.contains("ui-layout-footer"), "Debe contener la sección FOOTER (Pie de página)");
            JettraAssert.assertTrue(html.contains("OPERATIVO"), "Debe mostrar estado operativo en FOOTER");
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("5. Verificar invocación y renderizado de Vistas Reactivas JettraFlux desde el menú")
    public void testJettraFluxViewsInvocation() throws Exception {
        JettraServer server = App.startApplication(TEST_PORT + 4, StorageMode.JVM_RAM);
        try {
            HttpClient client = HttpClient.newHttpClient();

            // 1. Storage Monitor JettraFlux
            HttpRequest reqStorage = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 4) + "/flux/storage-monitor"))
                .GET()
                .build();
            HttpResponse<String> resStorage = client.send(reqStorage, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resStorage.statusCode(), "Vista JettraFlux Storage debe responder 200");
            JettraAssert.assertTrue(resStorage.body().contains("Monitor de Almacenamiento Reactivo"), "Debe contener el título del widget JettraFlux");
            JettraAssert.assertTrue(resStorage.body().contains("JVM_RAM"), "Debe reportar el modo activo");

            // 2. Telemetry JettraFlux
            HttpRequest reqTelemetry = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 4) + "/flux/telemetry"))
                .GET()
                .build();
            HttpResponse<String> resTelemetry = client.send(reqTelemetry, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resTelemetry.statusCode(), "Vista JettraFlux Telemetry debe responder 200");
            JettraAssert.assertTrue(resTelemetry.body().contains("Telemetría en Tiempo Real"), "Debe renderizar la telemetría");

            // 3. Counter JettraFlux
            HttpRequest reqCounter = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 4) + "/flux/counter"))
                .GET()
                .build();
            HttpResponse<String> resCounter = client.send(reqCounter, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resCounter.statusCode(), "Vista JettraFlux Counter debe responder 200");
            JettraAssert.assertTrue(resCounter.body().contains("Demostración Interactiva JettraFlux"), "Debe renderizar el contador");
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("6. Verificar API RESTful de Productos y Server Info")
    public void testRestEndpoints() throws Exception {
        JettraServer server = App.startApplication(TEST_PORT + 5, StorageMode.JVM_RAM);
        try {
            HttpClient client = HttpClient.newHttpClient();

            // /api/productos
            HttpRequest reqProds = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 5) + "/api/productos"))
                .GET()
                .build();
            HttpResponse<String> resProds = client.send(reqProds, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resProds.statusCode(), "Endpoint REST debe responder HTTP 200");
            JettraAssert.assertTrue(resProds.body().contains("PRD-01"), "Debe incluir productos del catálogo");

            // /api/info
            HttpRequest reqInfo = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 5) + "/api/info"))
                .GET()
                .build();
            HttpResponse<String> resInfo = client.send(reqInfo, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resInfo.statusCode(), "Endpoint Info debe responder HTTP 200");
            JettraAssert.assertTrue(resInfo.body().contains("WebExample"), "Debe reportar el nombre de la aplicación");
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("7. Verificar vista reactiva CatalogoFlux (JettraFlux) y renderizado I18N")
    public void testCatalogoFluxViewAndI18n() throws Exception {
        JettraServer server = App.startApplication(TEST_PORT + 6, StorageMode.JVM_RAM);
        try {
            HttpClient client = HttpClient.newHttpClient();

            // Petición a /flux/catalogo
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 6) + "/flux/catalogo"))
                .GET()
                .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            JettraAssert.assertEquals(200, res.statusCode(), "CatalogoFlux debe responder HTTP 200");
            String body = res.body();

            // Verificar renderizado en la sección central del Dashboard
            JettraAssert.assertTrue(body.contains("ui-layout-center"), "Debe estar integrado en la sección CENTER del layout");
            JettraAssert.assertTrue(body.contains("Catálogo Reactivo de Productos"), "Debe contener el título desde messages.properties");
            JettraAssert.assertTrue(body.contains("Java 25 Reactive DSL"), "Debe contener el badge reactivo DSL");
            JettraAssert.assertTrue(body.contains("PRD-01"), "Debe contener los productos de la base de datos");
            JettraAssert.assertTrue(body.contains("Registrar Producto"), "Debe contener el botón de guardado I18N");

            // Verificar clase utilitaria Messages
            String appTitle = io.jettra.server.example.util.Messages.get("app.title");
            JettraAssert.assertEquals("JettraServer Enterprise", appTitle, "El título de la aplicación debe coincidir con messages.properties");
            String catalogTitle = io.jettra.server.example.util.Messages.get("flux.catalog.title");
            JettraAssert.assertEquals("Catálogo Reactivo de Productos", catalogTitle, "La etiqueta debe provenir de messages.properties");
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("8. Verificar integración de Swagger-UI y especificación OpenAPI en WebExample")
    public void testSwaggerUiIntegration() throws Exception {
        JettraServer server = App.startApplication(TEST_PORT + 7, StorageMode.JVM_RAM);
        try {
            HttpClient client = HttpClient.newHttpClient();

            // 1. Swagger-UI
            HttpRequest reqSwagger = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 7) + "/swagger-ui"))
                .GET()
                .build();
            HttpResponse<String> resSwagger = client.send(reqSwagger, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resSwagger.statusCode(), "Swagger-UI debe responder HTTP 200");
            JettraAssert.assertTrue(resSwagger.body().contains("OpenAPI 3.0.0"), "Debe contener el badge de OpenAPI 3.0.0");
            JettraAssert.assertTrue(resSwagger.body().contains("Panel de Acreditación de Usuario"), "Debe incluir el panel de acreditación");
            JettraAssert.assertTrue(resSwagger.body().contains("Administración de Usuarios y Roles (JettraSecurityDB)"), "Debe incluir el panel de gestión de usuarios y roles");
            JettraAssert.assertTrue(resSwagger.body().contains("jettraLoadUsers"), "Debe contener la función JS de carga de usuarios");
            JettraAssert.assertTrue(resSwagger.body().contains("jettraLogin"), "Debe contener la función JS de autenticación");

            // 2. OpenAPI Spec JSON
            HttpRequest reqOpenApi = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 7) + "/openapi.json"))
                .GET()
                .build();
            HttpResponse<String> resOpenApi = client.send(reqOpenApi, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resOpenApi.statusCode(), "openapi.json debe responder HTTP 200");
            JettraAssert.assertTrue(resOpenApi.body().contains("\"openapi\""), "Debe tener clave openapi");
            JettraAssert.assertTrue(resOpenApi.body().contains("3.0.0"), "Debe ser versión OpenAPI 3.0.0");
            JettraAssert.assertTrue(resOpenApi.body().contains("/api/productos"), "Debe listar el endpoint de productos");
            JettraAssert.assertTrue(resOpenApi.body().contains("/api/security/users"), "Debe documentar endpoints de administración de seguridad");
            JettraAssert.assertTrue(resOpenApi.body().contains("/api/info"), "Debe listar el endpoint de info");
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("9. Verificar Vistas Reactivas JettraFlux de Metrics y Health")
    public void testMetricsAndHealthFluxViews() throws Exception {
        JettraServer server = App.startApplication(TEST_PORT + 8, StorageMode.JVM_RAM);
        try {
            HttpClient client = HttpClient.newHttpClient();

            // 1. Metrics Flux View
            HttpRequest reqMetrics = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 8) + "/flux/metrics"))
                .GET()
                .build();
            HttpResponse<String> resMetrics = client.send(reqMetrics, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resMetrics.statusCode(), "Vista JettraFlux Metrics debe responder 200");
            JettraAssert.assertTrue(resMetrics.body().contains("Métricas y Telemetría del Servidor"), "Debe contener el título de métricas");
            JettraAssert.assertTrue(resMetrics.body().contains("Memoria JVM Usada"), "Debe mostrar el KPI de memoria");

            // 2. Health Flux View
            HttpRequest reqHealth = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 8) + "/flux/health"))
                .GET()
                .build();
            HttpResponse<String> resHealth = client.send(reqHealth, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resHealth.statusCode(), "Vista JettraFlux Health debe responder 200");
            JettraAssert.assertTrue(resHealth.body().contains("Estado de Salud del Servidor"), "Debe contener el título de salud");
            JettraAssert.assertTrue(resHealth.body().contains("UP"), "Debe reportar estado UP");

            // 3. Embebido en Dashboard Center (?view=metrics)
            HttpRequest reqDashMetrics = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 8) + "/dashboard.xhtml?view=metrics"))
                .GET()
                .build();
            HttpResponse<String> resDashMetrics = client.send(reqDashMetrics, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resDashMetrics.statusCode(), "Dashboard con vista metrics debe responder 200");
            JettraAssert.assertTrue(resDashMetrics.body().contains("Métricas del Servidor - JettraFlux"), "Debe contener el título de la vista");

            // 4. Embebido en Dashboard Center (?view=health)
            HttpRequest reqDashHealth = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 8) + "/dashboard.xhtml?view=health"))
                .GET()
                .build();
            HttpResponse<String> resDashHealth = client.send(reqDashHealth, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resDashHealth.statusCode(), "Dashboard con vista health debe responder 200");
            JettraAssert.assertTrue(resDashHealth.body().contains("Estado de Salud (Health) - JettraFlux"), "Debe contener el título de la vista");
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("10. Verificar configuración de StorageMode (OFF_HEAP, DISK_MEMORY, JVM_RAM) y visualización en el Top Bar")
    public void testStorageModeConfigurationAndTopBarDisplay() throws Exception {
        JettraServer server = App.startApplication(TEST_PORT + 9, StorageMode.OFF_HEAP);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 9) + "/dashboard.xhtml"))
                .GET()
                .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, res.statusCode(), "Debe responder 200");
            String html = res.body();
            JettraAssert.assertTrue(html.contains("Modo: <strong style=\"color:#38bdf8;\">OFF_HEAP</strong>"),
                "El top bar debe mostrar el modo de almacenamiento configurado (OFF_HEAP)");
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("11. Verificar Suite JettraAura: Portal (/aura/demo) y Catálogo con AuraListView (/aura/catalogo)")
    public void testJettraAuraPortalAndCatalog() throws Exception {
        JettraServer server = App.startApplication(TEST_PORT + 10, StorageMode.JVM_RAM);
        try {
            HttpClient client = HttpClient.newHttpClient();

            // 1. Portal JettraAura
            HttpRequest reqPortal = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 10) + "/aura/demo"))
                .GET()
                .build();
            HttpResponse<String> resPortal = client.send(reqPortal, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resPortal.statusCode(), "El portal de JettraAura debe responder HTTP 200");
            JettraAssert.assertTrue(resPortal.body().contains("JettraAura: Arquitectura Web Orientada a Componentes"), "Debe contener el título del portal");
            JettraAssert.assertTrue(resPortal.body().contains("JVM_RAM"), "Debe mostrar el modo de almacenamiento");
            JettraAssert.assertTrue(resPortal.body().contains("/aura/catalogo"), "Debe contener el enlace al catálogo");

            // 2. Catálogo Dinámico con AuraListView y AuraModal
            HttpRequest reqCatalog = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 10) + "/aura/catalogo"))
                .GET()
                .build();
            HttpResponse<String> resCatalog = client.send(reqCatalog, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resCatalog.statusCode(), "El catálogo de JettraAura debe responder HTTP 200");
            JettraAssert.assertTrue(resCatalog.body().contains("Catálogo JettraAura"), "Debe contener el encabezado del catálogo");
            JettraAssert.assertTrue(resCatalog.body().contains("PRD-01"), "Debe listar el producto PRD-01 de JPA");
            JettraAssert.assertTrue(resCatalog.body().contains("Servidor JettraEdge 1U"), "Debe listar el nombre del producto");
            JettraAssert.assertTrue(resCatalog.body().contains("Registrar Nuevo Producto (JPA)"), "Debe contener el formulario de registro");
            JettraAssert.assertTrue(resCatalog.body().contains("aura-productModal"), "Debe renderizar el diálogo AuraModal de creación/edición");
            JettraAssert.assertTrue(resCatalog.body().contains("aura-deleteModal"), "Debe renderizar el diálogo AuraModal de confirmación de eliminación");
            JettraAssert.assertTrue(resCatalog.body().contains("Editar"), "Debe incluir el botón Editar por fila");
            JettraAssert.assertTrue(resCatalog.body().contains("Eliminar"), "Debe incluir el botón Eliminar por fila");

        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("12. Verificar Suite JettraAura: Formulario con CompoundPropertyModel (/aura/formulario) y Contador AJAX (/aura/contador)")
    public void testJettraAuraFormAndCounter() throws Exception {
        JettraServer server = App.startApplication(TEST_PORT + 11, StorageMode.JVM_RAM);
        try {
            HttpClient client = HttpClient.newHttpClient();

            // 1. Formulario Reactivo con CompoundPropertyModel
            HttpRequest reqForm = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 11) + "/aura/formulario"))
                .GET()
                .build();
            HttpResponse<String> resForm = client.send(reqForm, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resForm.statusCode(), "El formulario de JettraAura debe responder HTTP 200");
            JettraAssert.assertTrue(resForm.body().contains("Formulario Reactivo JettraAura"), "Debe contener el título del formulario");
            JettraAssert.assertTrue(resForm.body().contains("jettraAdmin"), "Debe contener el valor inicial del modelo");
            JettraAssert.assertTrue(resForm.body().contains("admin@jettra.io"), "Debe contener el email del modelo");

            // 2. Contador AJAX Parcial
            HttpRequest reqCounter = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + (TEST_PORT + 11) + "/aura/contador"))
                .GET()
                .build();
            HttpResponse<String> resCounter = client.send(reqCounter, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resCounter.statusCode(), "El contador de JettraAura debe responder HTTP 200");
            JettraAssert.assertTrue(resCounter.body().contains("Contador AJAX JettraAura"), "Debe contener el título del contador");
            JettraAssert.assertTrue(resCounter.body().contains("btnIncrement"), "Debe contener el botón de incremento");

        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("13. Verificar operaciones CRUD (Guardar, Editar, Eliminar) y diálogos AuraModal en /aura/catalogo")
    public void testJettraAuraCatalogCrudAndDialogs() throws Exception {
        int port = TEST_PORT + 12;
        JettraServer server = App.startApplication(port, StorageMode.JVM_RAM);
        try {
            HttpClient client = HttpClient.newHttpClient();
            String baseUrl = "http://127.0.0.1:" + port + "/aura/catalogo";

            // 1. Cargar página inicial y obtener cookie AURA_SESSION_ID
            HttpRequest reqInit = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .GET()
                .build();
            HttpResponse<String> resInit = client.send(reqInit, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resInit.statusCode(), "Catálogo inicial debe responder 200");
            String cookie = resInit.headers().firstValue("Set-Cookie").orElse("");

            // 2. GUARDAR nuevo producto (PRD-99) mediante el diálogo AuraModal (btnModalSave vía AJAX)
            String savePayload = "aura:ajax=true&aura:target=productModal:modalProductForm:btnModalSave&aura:event=click"
                + "&id=PRD-99&name=Router+JettraCore+X1&category=Redes&price=3200.50&stock=12";
            HttpRequest reqSave = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", cookie)
                .POST(HttpRequest.BodyPublishers.ofString(savePayload))
                .build();
            HttpResponse<String> resSave = client.send(reqSave, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resSave.statusCode(), "Guardado AJAX debe responder 200");
            JettraAssert.assertTrue(resSave.body().contains("PRD-99"), "La respuesta AJAX debe incluir el nuevo producto PRD-99");
            JettraAssert.assertTrue(resSave.body().contains("Router JettraCore X1"), "La respuesta AJAX debe incluir el nombre del producto guardado");

            // 3. EDITAR producto existente: abrir diálogo AuraModal para la fila 0 (PRD-01)
            String openEditPayload = "aura:ajax=true&aura:target=catalogContainer:productList:0:btnEditProduct&aura:event=click";
            HttpRequest reqOpenEdit = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", cookie)
                .POST(HttpRequest.BodyPublishers.ofString(openEditPayload))
                .build();
            HttpResponse<String> resOpenEdit = client.send(reqOpenEdit, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resOpenEdit.statusCode(), "Apertura de modal de edición debe responder 200");
            JettraAssert.assertTrue(resOpenEdit.body().contains("Editar Producto (PRD-01)"), "El diálogo AuraModal debe abrirse con los datos de PRD-01");
            JettraAssert.assertTrue(resOpenEdit.body().contains("display: flex;"), "El diálogo AuraModal debe mostrarse abierto");

            // Guardar edición de PRD-01 desde el diálogo modal
            String editSavePayload = "aura:ajax=true&aura:target=productModal:modalProductForm:btnModalSave&aura:event=click"
                + "&id=PRD-01&name=Servidor+JettraEdge+2U+Pro&category=Hardware&price=2999.00&stock=25";
            HttpRequest reqEditSave = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", cookie)
                .POST(HttpRequest.BodyPublishers.ofString(editSavePayload))
                .build();
            HttpResponse<String> resEditSave = client.send(reqEditSave, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resEditSave.statusCode(), "Actualización en modal debe responder 200");
            JettraAssert.assertTrue(resEditSave.body().contains("Servidor JettraEdge 2U Pro"), "La tabla debe reflejar el nombre editado");
            JettraAssert.assertTrue(resEditSave.body().contains("actualizado exitosamente"), "Debe mostrar mensaje de éxito de actualización");

            // 4. ELIMINAR producto: abrir diálogo AuraModal de confirmación para PRD-99 (fila 5)
            String openDeletePayload = "aura:ajax=true&aura:target=catalogContainer:productList:5:btnDeleteProduct&aura:event=click";
            HttpRequest reqOpenDelete = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", cookie)
                .POST(HttpRequest.BodyPublishers.ofString(openDeletePayload))
                .build();
            HttpResponse<String> resOpenDelete = client.send(reqOpenDelete, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resOpenDelete.statusCode(), "Apertura de modal de eliminación debe responder 200");
            JettraAssert.assertTrue(resOpenDelete.body().contains("PRD-99"), "El diálogo de confirmación debe mostrar el producto PRD-99 a eliminar");

            // Confirmar eliminación en el diálogo deleteModal
            String confirmDeletePayload = "aura:ajax=true&aura:target=deleteModal:btnConfirmDelete&aura:event=click";
            HttpRequest reqConfirmDelete = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", cookie)
                .POST(HttpRequest.BodyPublishers.ofString(confirmDeletePayload))
                .build();
            HttpResponse<String> resConfirmDelete = client.send(reqConfirmDelete, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertEquals(200, resConfirmDelete.statusCode(), "Confirmación de eliminación debe responder 200");
            JettraAssert.assertTrue(resConfirmDelete.body().contains("eliminado del catálogo JPA"), "Debe confirmar la eliminación en el FeedbackPanel");

            // Verificar que PRD-99 ya no está en la tabla al consultar la página
            HttpRequest reqAfterDelete = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Cookie", cookie)
                .GET()
                .build();
            HttpResponse<String> resAfterDelete = client.send(reqAfterDelete, HttpResponse.BodyHandlers.ofString());
            JettraAssert.assertFalse(resAfterDelete.body().contains("Router JettraCore X1"), "El producto eliminado ya no debe aparecer en la tabla");
            JettraAssert.assertTrue(resAfterDelete.body().contains("Servidor JettraEdge 2U Pro"), "El producto editado debe permanecer en la tabla");

        } finally {
            server.stop();
        }
    }
}

