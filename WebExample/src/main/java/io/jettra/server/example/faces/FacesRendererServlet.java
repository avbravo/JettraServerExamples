package io.jettra.server.example.faces;

import io.jettra.server.core.JettraServer;
import io.jettra.server.core.storage.StorageMetrics;
import io.jettra.server.example.bean.DashboardBean;
import io.jettra.server.example.bean.LoginBean;
import io.jettra.server.example.flux.CatalogoFlux;
import io.jettra.server.example.flux.FluxTemplateView;
import io.jettra.server.example.flux.HealthFluxView;
import io.jettra.server.example.flux.InteractiveCounterFluxView;
import io.jettra.server.example.flux.MetricsFluxView;
import io.jettra.server.example.flux.StorageMonitorFluxView;
import io.jettra.server.example.flux.TelemetryFluxView;
import io.jettra.server.example.model.Product;
import io.jettra.server.example.model.User;
import io.jettra.server.example.service.ProductService;
import io.jettra.server.example.service.SecurityService;
import io.jettra.server.example.util.Messages;
import io.jettra.server.jakarta.cdi.CdiContainer;
import io.jettra.server.jakarta.servlet.HttpServlet;
import io.jettra.server.jakarta.servlet.HttpServletRequest;
import io.jettra.server.jakarta.servlet.HttpServletResponse;
import io.jettra.server.jakarta.servlet.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Servlet compatible con Jakarta Servlet 6.1 y Jakarta Faces 4.0.
 * Provee:
 *  1. Selector dinámico de temas (Dark / White) con persistencia vía localStorage y cookie.
 *  2. Renderizado de vistas JettraFlux dentro de la sección central (Center) del Dashboard.
 *  3. Diseño 100% responsivo adaptable a dispositivos móviles, tablets y escritorios.
 */
public class FacesRendererServlet extends HttpServlet {

    private final CdiContainer cdiContainer;
    private final SecurityService securityService;
    private final ProductService productService;
    private final JettraServer server;

    public FacesRendererServlet(CdiContainer cdiContainer, SecurityService securityService, ProductService productService) {
        this(cdiContainer, securityService, productService, null);
    }

    public FacesRendererServlet(CdiContainer cdiContainer, SecurityService securityService, ProductService productService, JettraServer server) {
        this.cdiContainer = cdiContainer;
        this.securityService = securityService;
        this.productService = productService;
        this.server = server;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String uri = req.getRequestURI();
        if (uri.endsWith("/logout")) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            res.sendRedirect("/login.xhtml");
            return;
        }

        // Vistas JettraFlux embebidas en la sección central del Dashboard
        if (uri.endsWith("/flux/storage-monitor") || uri.endsWith("/flux/storage")) {
            renderDashboardWithFlux(req, res, "storage-monitor");
        } else if (uri.endsWith("/flux/telemetry")) {
            renderDashboardWithFlux(req, res, "telemetry");
        } else if (uri.endsWith("/flux/counter")) {
            renderDashboardWithFlux(req, res, "counter");
        } else if (uri.endsWith("/flux/catalogo") || uri.endsWith("/flux/catalogo-flux")) {
            renderDashboardWithFlux(req, res, "catalogo-flux");
        } else if (uri.endsWith("/flux/metrics")) {
            renderDashboardWithFlux(req, res, "metrics");
        } else if (uri.endsWith("/flux/health")) {
            renderDashboardWithFlux(req, res, "health");
        } else if (uri.endsWith("/dashboard.xhtml") || uri.equals("/") || uri.equals("/dashboard")) {
            String view = req.getParameter("view");
            if (view != null && !view.isBlank()) {
                renderDashboardWithFlux(req, res, view.trim());
            } else {
                renderDashboard(req, res);
            }
        } else if (uri.endsWith("/productos.xhtml") || uri.endsWith("/productos")) {
            renderProductos(req, res);
        } else if (uri.endsWith("/usuarios.xhtml") || uri.endsWith("/usuarios")) {
            renderUsuarios(req, res);
        } else {
            renderLogin(req, res, null);
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String uri = req.getRequestURI();

        if (uri.endsWith("/login.xhtml") || uri.endsWith("/login")) {
            String u = req.getParameter("username");
            String p = req.getParameter("password");

            LoginBean loginBean = new LoginBean(securityService);
            loginBean.setUsername(u);
            loginBean.setPassword(p);

            if (loginBean.validateCredentials()) {
                HttpSession session = req.getSession(true);
                session.setAttribute("user", loginBean.getCurrentUser());
                session.setAttribute("loginBean", loginBean);
                res.sendRedirect("/dashboard.xhtml");
            } else {
                renderLogin(req, res, loginBean.getErrorMessage());
            }
        } else if (uri.endsWith("/productos.xhtml") || uri.endsWith("/productos")) {
            String action = req.getParameter("action");
            if ("save".equalsIgnoreCase(action)) {
                String id = req.getParameter("id");
                String name = req.getParameter("name");
                String cat = req.getParameter("category");
                double price = 0.0;
                int stock = 0;
                try { price = Double.parseDouble(req.getParameter("price")); } catch (Exception ignored) {}
                try { stock = Integer.parseInt(req.getParameter("stock")); } catch (Exception ignored) {}

                if (id != null && !id.isBlank() && name != null && !name.isBlank()) {
                    productService.save(new Product(id, name, cat, price, stock));
                }
            } else if ("delete".equalsIgnoreCase(action)) {
                String id = req.getParameter("id");
                if (id != null) productService.delete(id);
            }
            String returnTo = req.getParameter("returnTo");
            if (returnTo != null && !returnTo.isBlank()) {
                res.sendRedirect(returnTo);
                return;
            }
            res.sendRedirect("/productos.xhtml");
        } else {
            res.sendRedirect("/dashboard.xhtml");
        }
    }

    private void renderLogin(HttpServletRequest req, HttpServletResponse res, String errorMessage) throws IOException {
        res.setContentType("text/html; charset=UTF-8");
        PrintWriter out = res.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"es\">");
        out.println("<head>");
        out.println("  <meta charset=\"UTF-8\" />");
        out.println("  <title>JettraServer - Inicio de Sesión PrimeFaces</title>");
        out.println(commonHeadStylesAndScripts());
        out.println("</head>");
        out.println("<body class=\"app-body\" style=\"display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; padding: 1rem;\">");

        // Botón flotante para alternar tema en el Login
        out.println("  <div style=\"position: fixed; top: 1rem; right: 1rem; z-index: 1000;\">");
        out.println("    <button onclick=\"toggleTheme()\" class=\"theme-toggle-btn\" type=\"button\" title=\"Cambiar tema Claro / Oscuro\">");
        out.println("      <span id=\"themeIcon\">🌙</span> <span id=\"themeLabel\" class=\"hide-mobile\">Tema</span>");
        out.println("    </button>");
        out.println("  </div>");

        out.println("  <div class=\"ui-card\" style=\"width: 100%; max-width: 420px; padding: 2.5rem; border-radius: 14px; box-shadow: var(--shadow-lg); border: 1px solid var(--border-subtle); background: var(--bg-card);\">");
        out.println("    <div style=\"text-align: center; margin-bottom: 2rem;\">");
        out.println("      <div style=\"display: inline-flex; align-items: center; justify-content: center; width: 64px; height: 64px; border-radius: 16px; background: linear-gradient(135deg, #2563eb, #1d4ed8); color: white; font-weight: bold; font-size: 28px; margin-bottom: 1rem;\">JS</div>");
        out.println("      <h2 style=\"margin: 0; font-size: 1.5rem; color: var(--text-main); font-weight: 700;\">JettraServer</h2>");
        out.println("      <p style=\"margin: 0.35rem 0 0; color: var(--text-muted); font-size: 0.875rem;\">Jakarta EE 11 • PrimeFaces 14.0 • Java 25 Loom</p>");
        out.println("    </div>");

        if (errorMessage != null && !errorMessage.isBlank()) {
            out.println("    <div style=\"padding: 0.75rem 1rem; margin-bottom: 1.5rem; background-color: var(--danger-light); border: 1px solid var(--danger); color: var(--danger); border-radius: 8px; font-size: 0.875rem;\">");
            out.println("      <span style=\"font-weight: bold; margin-right: 0.5rem;\">✖</span>");
            out.println("      <span>" + escapeHtml(errorMessage) + "</span>");
            out.println("    </div>");
        }

        out.println("    <form method=\"post\" action=\"/login.xhtml\">");
        out.println("      <div style=\"margin-bottom: 1.25rem;\">");
        out.println("        <label for=\"username\" style=\"display: block; font-size: 0.875rem; font-weight: 600; color: var(--text-main); margin-bottom: 0.5rem;\">Usuario</label>");
        out.println("        <input type=\"text\" id=\"username\" name=\"username\" value=\"admin\" required=\"required\" class=\"app-input\" placeholder=\"admin\" />");
        out.println("      </div>");

        out.println("      <div style=\"margin-bottom: 1.75rem;\">");
        out.println("        <label for=\"password\" style=\"display: block; font-size: 0.875rem; font-weight: 600; color: var(--text-main); margin-bottom: 0.5rem;\">Contraseña</label>");
        out.println("        <input type=\"password\" id=\"password\" name=\"password\" value=\"admin123\" required=\"required\" class=\"app-input\" placeholder=\"admin123\" />");
        out.println("      </div>");

        out.println("      <button type=\"submit\" class=\"app-btn-primary\" style=\"width: 100%; padding: 0.75rem; font-size: 1rem;\">");
        out.println("        Iniciar Sesión");
        out.println("      </button>");
        out.println("    </form>");

        out.println("    <div style=\"margin-top: 1.5rem; text-align: center; font-size: 0.8rem; color: var(--text-muted);\">");
        out.println("      <span>Credenciales por defecto: <strong>admin</strong> / <strong>admin123</strong></span>");
        out.println("    </div>");
        out.println("  </div>");

        out.println("</body>");
        out.println("</html>");
    }

    private void renderDashboard(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        User sessionUser = session != null ? (User) session.getAttribute("user") : null;
        final User user = sessionUser != null ? sessionUser : new User("admin", "admin123", "Administrador Principal", "ADMIN", "admin@jettra.io");

        DashboardBean dbBean = new DashboardBean();
        StorageMetrics sm = (server != null && server.storage() != null) ? server.storage().getMetrics() : null;
        dbBean.refreshMetrics(sm);

        res.setContentType("text/html; charset=UTF-8");
        PrintWriter out = res.getWriter();

        String activeStorage = getEffectiveStorageMode();

        renderTemplate(out, "Dashboard Principal", user, activeStorage, "general", () -> {
            out.println("      <div style=\"display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem;\">");
            out.println("        <div>");
            out.println("          <h1 style=\"font-size: 1.6rem; font-weight: 700; color: var(--text-main); margin: 0 0 0.4rem 0;\">Panel de Control General</h1>");
            out.println("          <p style=\"color: var(--text-muted); margin: 0;\">Bienvenido, <strong>" + escapeHtml(user.getFullName()) + "</strong>. Servidor ejecutando arquitectura Jakarta EE y Virtual Threads.</p>");
            out.println("        </div>");
            out.println("        <div class=\"flux-tabs\">");
            out.println("          <a href=\"/dashboard.xhtml\" class=\"tab-pill active\">📊 General</a>");
            out.println("          <a href=\"/aura/demo\" class=\"tab-pill\">✨ Portal Aura</a>");
            out.println("          <a href=\"/aura/catalogo\" class=\"tab-pill\">📦 Catálogo Aura</a>");
            out.println("          <a href=\"/flux/catalogo\" class=\"tab-pill\">🛍️ " + escapeHtml(Messages.get("flux.catalog.title")) + "</a>");
            out.println("          <a href=\"/dashboard.xhtml?view=metrics\" class=\"tab-pill\">📊 Métricas Flux</a>");
            out.println("          <a href=\"/dashboard.xhtml?view=health\" class=\"tab-pill\">🩺 Health Flux</a>");
            out.println("          <a href=\"/dashboard.xhtml?view=storage-monitor\" class=\"tab-pill\">🚀 Storage Flux</a>");
            out.println("          <a href=\"/dashboard.xhtml?view=telemetry\" class=\"tab-pill\">📈 Telemetría Flux</a>");
            out.println("          <a href=\"/dashboard.xhtml?view=counter\" class=\"tab-pill\">🎛️ Contador Flux</a>");
            out.println("        </div>");
            out.println("      </div>");

            // Tarjetas KPI Responsivas
            out.println("      <div class=\"kpi-grid\" style=\"margin-bottom: 2rem;\">");

            out.println("        <div class=\"ui-card kpi-card\">");
            out.println("          <div class=\"kpi-title\">Modo Persistencia</div>");
            out.println("          <div class=\"kpi-value\" style=\"color: var(--primary);\">" + activeStorage + "</div>");
            out.println("          <div class=\"kpi-sub\" style=\"color: var(--success);\">● Activo y Sincronizado</div>");
            out.println("        </div>");

            out.println("        <div class=\"ui-card kpi-card\">");
            out.println("          <div class=\"kpi-title\">Catálogo Productos (JPA)</div>");
            out.println("          <div class=\"kpi-value\" style=\"color: var(--text-main);\">" + productService.listAll().size() + " Registros</div>");
            out.println("          <div class=\"kpi-sub\"><a href=\"/productos.xhtml\" style=\"color: var(--primary); text-decoration: none;\">Ver catálogo completo →</a></div>");
            out.println("        </div>");

            out.println("        <div class=\"ui-card kpi-card\">");
            out.println("          <div class=\"kpi-title\">Concurrencia Loom</div>");
            out.println("          <div class=\"kpi-value\" style=\"color: #8b5cf6;\">Virtual Threads</div>");
            out.println("          <div class=\"kpi-sub\">" + Runtime.getRuntime().availableProcessors() + " Cores de CPU Disponibles</div>");
            out.println("        </div>");

            out.println("        <div class=\"ui-card kpi-card\">");
            out.println("          <div class=\"kpi-title\">Tiempo de Actividad</div>");
            out.println("          <div class=\"kpi-value\" style=\"color: var(--success);\">" + dbBean.getUptimeFormatted() + "</div>");
            out.println("          <div class=\"kpi-sub\" style=\"color: var(--success);\">Estado Salud: OK (UP)</div>");
            out.println("        </div>");

            out.println("      </div>");

            // Sección Embebida JettraAura
            out.println("      <div class=\"ui-panel\" style=\"background: var(--bg-card); padding: 1.5rem; border-radius: 12px; border: 1px solid var(--border-subtle); margin-bottom: 2rem; box-shadow: var(--shadow-sm);\">");
            out.println("        <div style=\"display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.25rem; flex-wrap: wrap; gap: 0.5rem;\">");
            out.println("          <div>");
            out.println("            <h3 style=\"margin: 0; font-size: 1.2rem; color: var(--text-main);\">✨ Suite de Ejemplos JettraAura)</h3>");
            out.println("            <p style=\"margin: 0.25rem 0 0; color: var(--text-muted); font-size: 0.875rem;\">Arquitectura web orientada a componentes con marcado HTML puro, POJOs y Virtual Threads de Java 25</p>");
            out.println("          </div>");
            out.println("          <span style=\"background: #0284c7; color: white; padding: 0.35rem 0.85rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 700;\">JettraAura 1:1</span>");
            out.println("        </div>");
            out.println("        <div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1rem;\">");
            out.println("          <a href=\"/aura/demo\" class=\"flux-card-link\">");
            out.println("            <div style=\"font-weight: 700; color: #38bdf8; margin-bottom: 0.35rem;\">✨ Portal JettraAura</div>");
            out.println("            <div style=\"font-size: 0.85rem; color: var(--text-muted);\">Hub central de navegación, métricas en vivo y explicación arquitectónica.</div>");
            out.println("          </a>");
            out.println("          <a href=\"/aura/catalogo\" class=\"flux-card-link\">");
            out.println("            <div style=\"font-weight: 700; color: #10b981; margin-bottom: 0.35rem;\">📦 Catálogo Dinámico (AuraListView)</div>");
            out.println("            <div style=\"font-size: 0.85rem; color: var(--text-muted);\">Iterador de colecciones JPA con formulario de alta rápida de productos.</div>");
            out.println("          </a>");
            out.println("          <a href=\"/aura/formulario\" class=\"flux-card-link\">");
            out.println("            <div style=\"font-weight: 700; color: #f59e0b; margin-bottom: 0.35rem;\">📝 Formulario Reactivo (CPM)</div>");
            out.println("            <div style=\"font-size: 0.85rem; color: var(--text-muted);\">CompoundPropertyModel con vinculación automática y FeedbackPanel.</div>");
            out.println("          </a>");
            out.println("          <a href=\"/aura/contador\" class=\"flux-card-link\">");
            out.println("            <div style=\"font-weight: 700; color: #a855f7; margin-bottom: 0.35rem;\">⚡ Contador AJAX Parcial</div>");
            out.println("            <div style=\"font-size: 0.85rem; color: var(--text-muted);\">Actualizaciones quirúrgicas del DOM con Virtual Threads de Loom.</div>");
            out.println("          </a>");
            out.println("        </div>");
            out.println("      </div>");

            // Sección Embebida JettraFlux dentro de la sección Center
            out.println("      <div class=\"ui-panel\" style=\"background: var(--bg-card); padding: 1.5rem; border-radius: 12px; border: 1px solid var(--border-subtle); margin-bottom: 2rem; box-shadow: var(--shadow-sm);\">");
            out.println("        <div style=\"display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.25rem; flex-wrap: wrap; gap: 0.5rem;\">");
            out.println("          <div>");
            out.println("            <h3 style=\"margin: 0; font-size: 1.2rem; color: var(--text-main);\">⚡ Componentes JettraFlux Embebidos en Sección Center</h3>");
            out.println("            <p style=\"margin: 0.25rem 0 0; color: var(--text-muted); font-size: 0.875rem;\">Renderizado de UI Reactiva en Java puro con comunicación cliente-servidor inmediata</p>");
            out.println("          </div>");
            out.println("          <span style=\"background: var(--primary-light); color: var(--primary); padding: 0.35rem 0.85rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 700;\">Java 25 Reactive</span>");
            out.println("        </div>");

            // Módulos interactivos Flux
            out.println("        <div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1rem;\">");

            out.println("          <a href=\"/flux/catalogo\" class=\"flux-card-link\">");
            out.println("            <div style=\"font-weight: 700; color: #0284c7; margin-bottom: 0.35rem;\">🛍️ " + escapeHtml(Messages.get("flux.catalog.title")) + "</div>");
            out.println("            <div style=\"font-size: 0.85rem; color: var(--text-muted);\">" + escapeHtml(Messages.get("flux.catalog.subtitle")) + "</div>");
            out.println("          </a>");

            out.println("          <a href=\"/dashboard.xhtml?view=metrics\" class=\"flux-card-link\">");
            out.println("            <div style=\"font-weight: 700; color: #2563eb; margin-bottom: 0.35rem;\">📊 Métricas del Servidor (Flux)</div>");
            out.println("            <div style=\"font-size: 0.85rem; color: var(--text-muted);\">Visualización reactiva de memoria JVM, Virtual Threads Loom y rendimiento Storage.</div>");
            out.println("          </a>");

            out.println("          <a href=\"/dashboard.xhtml?view=health\" class=\"flux-card-link\">");
            out.println("            <div style=\"font-weight: 700; color: #059669; margin-bottom: 0.35rem;\">🩺 Estado de Salud (Flux)</div>");
            out.println("            <div style=\"font-size: 0.85rem; color: var(--text-muted);\">Auditoría reactiva de sondas Liveness y Readiness según MicroProfile Health.</div>");
            out.println("          </a>");

            out.println("          <a href=\"/swagger-ui\" target=\"_blank\" class=\"flux-card-link\">");
            out.println("            <div style=\"font-weight: 700; color: #8b5cf6; margin-bottom: 0.35rem;\">📖 Swagger-UI (OpenAPI)</div>");
            out.println("            <div style=\"font-size: 0.85rem; color: var(--text-muted);\">Explorador interactivo de microservicios REST con acreditación de tokens JWT.</div>");
            out.println("          </a>");

            out.println("          <a href=\"/dashboard.xhtml?view=storage-monitor\" class=\"flux-card-link\">");
            out.println("            <div style=\"font-weight: 700; color: var(--primary); margin-bottom: 0.35rem;\">🚀 Monitor de Almacenamiento</div>");
            out.println("            <div style=\"font-size: 0.85rem; color: var(--text-muted);\">Inspección en tiempo real de JVM_RAM, DISK_MEMORY y OFF_HEAP Slotted-Page.</div>");
            out.println("          </a>");

            out.println("          <a href=\"/dashboard.xhtml?view=telemetry\" class=\"flux-card-link\">");
            out.println("            <div style=\"font-weight: 700; color: var(--success); margin-bottom: 0.35rem;\">📈 Telemetría del Servidor</div>");
            out.println("            <div style=\"font-size: 0.85rem; color: var(--text-muted);\">Métricas en vivo de hilos Loom, canales NIO y tiempos de cold start.</div>");
            out.println("          </a>");

            out.println("          <a href=\"/dashboard.xhtml?view=counter\" class=\"flux-card-link\">");
            out.println("            <div style=\"font-weight: 700; color: var(--warning); margin-bottom: 0.35rem;\">🎛️ Contador Reactivo Interactivo</div>");
            out.println("            <div style=\"font-size: 0.85rem; color: var(--text-muted);\">Demostración de ciclo reactivo de eventos sin recarga completa de página.</div>");
            out.println("          </a>");

            out.println("        </div>");
            out.println("      </div>");
        });
    }

    private void renderDashboardWithFlux(HttpServletRequest req, HttpServletResponse res, String viewName) throws IOException {
        HttpSession session = req.getSession(false);
        User sessionUser = session != null ? (User) session.getAttribute("user") : null;
        final User user = sessionUser != null ? sessionUser : new User("admin", "admin123", "Administrador Principal", "ADMIN", "admin@jettra.io");

        res.setContentType("text/html; charset=UTF-8");
        PrintWriter out = res.getWriter();

        String activeStorage = getEffectiveStorageMode();
        String title = switch (viewName) {
            case "metrics" -> "Métricas del Servidor - JettraFlux";
            case "health" -> "Estado de Salud (Health) - JettraFlux";
            case "storage-monitor" -> "Monitor de Almacenamiento - JettraFlux";
            case "telemetry" -> "Telemetría del Servidor - JettraFlux";
            case "counter" -> "Contador Reactivo - JettraFlux";
            case "catalogo-flux", "catalogo" -> Messages.get("flux.catalog.title") + " - JettraFlux";
            default -> "Vista JettraFlux";
        };

        io.jettra.flux.core.Widget contentWidget;
        String extraHtml = null;

        if ("catalogo-flux".equals(viewName) || "catalogo".equals(viewName)) {
            contentWidget = new CatalogoFlux(productService);
            java.io.StringWriter sw = new java.io.StringWriter();
            renderCatalogDialogsAndScript(new PrintWriter(sw), "/flux/catalogo");
            extraHtml = sw.toString();
        } else if ("metrics".equals(viewName)) {
            contentWidget = new MetricsFluxView(server);
        } else if ("health".equals(viewName)) {
            contentWidget = new HealthFluxView(server);
        } else if ("storage-monitor".equals(viewName)) {
            String desc = (server != null && server.storage() != null && server.storage().getMode() != null)
                    ? server.storage().getMode().getDescription()
                    : "Memoria RAM Heap estándar";
            contentWidget = new StorageMonitorFluxView(activeStorage, desc, server);
        } else if ("telemetry".equals(viewName)) {
            contentWidget = new TelemetryFluxView();
        } else if ("counter".equals(viewName)) {
            contentWidget = new InteractiveCounterFluxView();
        } else {
            contentWidget = new CatalogoFlux(productService);
        }

        FluxTemplateView fluxTemplate = new FluxTemplateView(
                title,
                viewName,
                activeStorage,
                user,
                contentWidget,
                extraHtml
        );
        fluxTemplate.renderTo(out);
    }

    private void renderFluxStorageMonitor(PrintWriter out, String activeStorage) {
        StorageMetrics metrics = server != null ? server.storage().getMetrics() : null;
        long records = metrics != null ? metrics.totalRecords() : 5;
        String memUsed = metrics != null ? metrics.humanReadableMemory() : "448 B";

        out.println("        <div style=\"display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border-subtle); padding-bottom: 1rem; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 0.5rem;\">");
        out.println("          <div>");
        out.println("            <h2 style=\"margin: 0; font-size: 1.25rem; color: var(--text-main);\">⚡ Monitor de Almacenamiento Reactivo - JettraFlux</h2>");
        out.println("            <span style=\"font-size: 0.85rem; color: var(--text-muted);\">Modo persistencia actual: <strong>" + activeStorage + "</strong></span>");
        out.println("          </div>");
        out.println("          <span style=\"background: var(--success-light); color: var(--success); font-weight: 700; padding: 0.35rem 0.75rem; border-radius: 6px; font-size: 0.8rem;\">Motor Sincronizado</span>");
        out.println("        </div>");

        out.println("        <div class=\"kpi-grid\" style=\"margin-bottom: 1.5rem;\">");
        out.println("          <div class=\"ui-card\" style=\"background: var(--bg-subtle); padding: 1.2rem; border-radius: 8px; border: 1px solid var(--border-subtle);\">");
        out.println("            <div style=\"font-size: 0.75rem; text-transform: uppercase; color: var(--text-muted); font-weight: 600;\">Modo Persistencia</div>");
        out.println("            <div style=\"font-size: 1.3rem; font-weight: 700; color: var(--primary); margin: 0.35rem 0;\">" + activeStorage + "</div>");
        out.println("            <div style=\"font-size: 0.8rem; color: var(--text-muted);\">Triple modo conmutable</div>");
        out.println("          </div>");
        out.println("          <div class=\"ui-card\" style=\"background: var(--bg-subtle); padding: 1.2rem; border-radius: 8px; border: 1px solid var(--border-subtle);\">");
        out.println("            <div style=\"font-size: 0.75rem; text-transform: uppercase; color: var(--text-muted); font-weight: 600;\">Registros en Almacén</div>");
        out.println("            <div style=\"font-size: 1.3rem; font-weight: 700; color: var(--text-main); margin: 0.35rem 0;\">" + records + " Entidades</div>");
        out.println("            <div style=\"font-size: 0.8rem; color: var(--text-muted);\">Sincronización Off-Heap</div>");
        out.println("          </div>");
        out.println("          <div class=\"ui-card\" style=\"background: var(--bg-subtle); padding: 1.2rem; border-radius: 8px; border: 1px solid var(--border-subtle);\">");
        out.println("            <div style=\"font-size: 0.75rem; text-transform: uppercase; color: var(--text-muted); font-weight: 600;\">Consumo de Memoria</div>");
        out.println("            <div style=\"font-size: 1.3rem; font-weight: 700; color: var(--success); margin: 0.35rem 0;\">" + memUsed + "</div>");
        out.println("            <div style=\"font-size: 0.8rem; color: var(--text-muted);\">Zero-GC Paging Activo</div>");
        out.println("          </div>");
        out.println("        </div>");

        out.println("        <div style=\"background: var(--bg-subtle); border: 1px solid var(--border-subtle); border-radius: 8px; padding: 1rem; margin-bottom: 1.5rem; font-size: 0.875rem;\">");
        out.println("          <div style=\"font-weight: 700; color: var(--text-main); margin-bottom: 0.5rem;\">Detalles de Arquitectura del Motor Seleccionado:</div>");
        out.println("          <ul style=\"margin: 0; padding-left: 1.25rem; color: var(--text-muted);\">");
        out.println("            <li><strong>JVM_RAM:</strong> Almacenamiento estándar en memoria Heap/Stack mediante estructuras compactas <code>UnifiedMap</code>.</li>");
        out.println("            <li><strong>DISK_MEMORY:</strong> Motor LSM directo Off-Heap estructurado en disco y MemTable sincronizado con latencia predecible.</li>");
        out.println("            <li><strong>OFF_HEAP:</strong> Motor nativo Slotted-Page mediante Panama Foreign Function & Memory (FFM) con zero-copy.</li>");
        out.println("          </ul>");
        out.println("        </div>");

        out.println("        <div style=\"display: flex; gap: 0.75rem; flex-wrap: wrap;\">");
        out.println("          <button class=\"app-btn-primary\" onclick=\"alert('MemTable sincronizada exitosamente con el disco')\">Forzar Sincronización MemTable</button>");
        out.println("          <a href=\"/metrics\" target=\"_blank\" class=\"app-btn-secondary\" style=\"text-decoration:none;\">Ver Métricas JSON en Bruto</a>");
        out.println("          <a href=\"/dashboard.xhtml\" class=\"app-btn-secondary\" style=\"text-decoration:none;\">Volver al Dashboard General</a>");
        out.println("        </div>");
    }

    private void renderFluxTelemetry(PrintWriter out) {
        Runtime rt = Runtime.getRuntime();
        long freeMb = rt.freeMemory() / (1024 * 1024);
        long totalMb = rt.totalMemory() / (1024 * 1024);
        long maxMb = rt.maxMemory() / (1024 * 1024);
        int cores = rt.availableProcessors();
        long uptimeSec = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        out.println("        <div style=\"display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border-subtle); padding-bottom: 1rem; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 0.5rem;\">");
        out.println("          <div>");
        out.println("            <h2 style=\"margin: 0; font-size: 1.25rem; color: var(--text-main);\">📈 Telemetría en Tiempo Real de JettraServer</h2>");
        out.println("            <span style=\"font-size: 0.85rem; color: var(--text-muted);\">Project Loom Virtual Threads & Asynchronous NIO Channels</span>");
        out.println("          </div>");
        out.println("          <span style=\"background: var(--primary-light); color: var(--primary); font-weight: 700; padding: 0.35rem 0.75rem; border-radius: 6px; font-size: 0.8rem;\">Cold Start &lt; 2ms</span>");
        out.println("        </div>");

        out.println("        <div class=\"kpi-grid\" style=\"margin-bottom: 1.5rem;\">");
        out.println("          <div class=\"ui-card\" style=\"background: var(--bg-subtle); padding: 1.2rem; border-radius: 8px; border: 1px solid var(--border-subtle);\">");
        out.println("            <div style=\"font-size: 0.75rem; text-transform: uppercase; color: var(--text-muted); font-weight: 600;\">Memoria JVM Usada</div>");
        out.println("            <div style=\"font-size: 1.3rem; font-weight: 700; color: var(--primary); margin: 0.35rem 0;\">" + (totalMb - freeMb) + " MB</div>");
        out.println("            <div style=\"font-size: 0.8rem; color: var(--text-muted);\">De " + totalMb + " MB asignados (Máx: " + maxMb + " MB)</div>");
        out.println("          </div>");
        out.println("          <div class=\"ui-card\" style=\"background: var(--bg-subtle); padding: 1.2rem; border-radius: 8px; border: 1px solid var(--border-subtle);\">");
        out.println("            <div style=\"font-size: 0.75rem; text-transform: uppercase; color: var(--text-muted); font-weight: 600;\">Hilos Virtuales Loom</div>");
        out.println("            <div style=\"font-size: 1.3rem; font-weight: 700; color: var(--success); margin: 0.35rem 0;\">Activos (M:N)</div>");
        out.println("            <div style=\"font-size: 0.8rem; color: var(--text-muted);\">" + cores + " núcleos de hardware mapeados</div>");
        out.println("          </div>");
        out.println("          <div class=\"ui-card\" style=\"background: var(--bg-subtle); padding: 1.2rem; border-radius: 8px; border: 1px solid var(--border-subtle);\">");
        out.println("            <div style=\"font-size: 0.75rem; text-transform: uppercase; color: var(--text-muted); font-weight: 600;\">Tiempo Activo</div>");
        out.println("            <div style=\"font-size: 1.3rem; font-weight: 700; color: var(--warning); margin: 0.35rem 0;\">" + uptimeSec + "s</div>");
        out.println("            <div style=\"font-size: 0.8rem; color: var(--text-muted);\">Generational ZGC activo</div>");
        out.println("          </div>");
        out.println("        </div>");

        out.println("        <div style=\"display: flex; gap: 0.75rem; flex-wrap: wrap;\">");
        out.println("          <button class=\"app-btn-primary\" onclick=\"window.location.reload()\">Actualizar Telemetría</button>");
        out.println("          <a href=\"/health\" target=\"_blank\" class=\"app-btn-secondary\" style=\"text-decoration:none;\">Ver Health Check JSON</a>");
        out.println("          <a href=\"/dashboard.xhtml\" class=\"app-btn-secondary\" style=\"text-decoration:none;\">Volver al Dashboard General</a>");
        out.println("        </div>");
    }

    private void renderFluxCounter(PrintWriter out) {
        out.println("        <div style=\"display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border-subtle); padding-bottom: 1rem; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 0.5rem;\">");
        out.println("          <div>");
        out.println("            <h2 style=\"margin: 0; font-size: 1.25rem; color: var(--text-main);\">🎛️ Demostración Interactiva JettraFlux</h2>");
        out.println("            <span style=\"font-size: 0.85rem; color: var(--text-muted);\">Control de estado reactivo bidireccional cliente-servidor en tiempo real</span>");
        out.println("          </div>");
        out.println("          <span style=\"background: var(--warning-light); color: var(--warning); font-weight: 700; padding: 0.35rem 0.75rem; border-radius: 6px; font-size: 0.8rem;\">State Reactive Component</span>");
        out.println("        </div>");

        out.println("        <div style=\"text-align: center; padding: 2.5rem 1rem; background: var(--bg-subtle); border-radius: 12px; border: 1px solid var(--border-subtle); margin-bottom: 1.5rem;\">");
        out.println("          <div style=\"font-size: 0.9rem; text-transform: uppercase; color: var(--text-muted); font-weight: 700; letter-spacing: 0.05em;\">Estado Actual del Componente Flux</div>");
        out.println("          <div id=\"fluxCounterValue\" style=\"font-size: 4rem; font-weight: 800; color: var(--primary); margin: 0.75rem 0;\">42</div>");
        out.println("          <p style=\"color: var(--text-muted); font-size: 0.875rem; max-width: 450px; margin: 0 auto 1.5rem auto;\">Los botones a continuación disparan eventos reactivos que mutan el estado en la sección central instantáneamente sin recarga de página.</p>");
        out.println("          <div style=\"display: inline-flex; gap: 0.75rem; flex-wrap: wrap; justify-content: center;\">");
        out.println("            <button class=\"app-btn-primary\" style=\"font-size: 1.1rem; padding: 0.6rem 1.25rem;\" onclick=\"changeCounter(1)\">➕ Incrementar (+1)</button>");
        out.println("            <button class=\"app-btn-secondary\" style=\"font-size: 1.1rem; padding: 0.6rem 1.25rem;\" onclick=\"changeCounter(-1)\">➖ Decrementar (-1)</button>");
        out.println("            <button class=\"app-btn-secondary\" style=\"font-size: 1.1rem; padding: 0.6rem 1.25rem; color: var(--danger); border-color: var(--danger);\" onclick=\"resetCounter()\">🔄 Reiniciar (0)</button>");
        out.println("          </div>");
        out.println("        </div>");

        out.println("        <script>");
        out.println("          let currentCount = 42;");
        out.println("          function changeCounter(delta) {");
        out.println("            currentCount += delta;");
        out.println("            const el = document.getElementById('fluxCounterValue');");
        out.println("            if (el) {");
        out.println("              el.textContent = currentCount;");
        out.println("              el.style.transform = 'scale(1.1)';");
        out.println("              setTimeout(() => el.style.transform = 'scale(1.0)', 150);");
        out.println("            }");
        out.println("          }");
        out.println("          function resetCounter() {");
        out.println("            currentCount = 0;");
        out.println("            const el = document.getElementById('fluxCounterValue');");
        out.println("            if (el) el.textContent = '0';");
        out.println("          }");
        out.println("        </script>");

        out.println("        <div style=\"display: flex; gap: 0.75rem; flex-wrap: wrap;\">");
        out.println("          <a href=\"/dashboard.xhtml\" class=\"app-btn-secondary\" style=\"text-decoration:none;\">Volver al Dashboard General</a>");
        out.println("        </div>");
    }

    private void renderProductos(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : new User("admin", "", "Administrador", "ADMIN", "");

        List<Product> products = productService.listAll();
        res.setContentType("text/html; charset=UTF-8");
        PrintWriter out = res.getWriter();

        String activeStorage = getEffectiveStorageMode();

        renderTemplate(out, Messages.get("catalog.title"), user, activeStorage, "productos", () -> {
            out.println("      <div style=\"display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 0.75rem;\">");
            out.println("        <div>");
            out.println("          <h1 style=\"font-size: 1.5rem; font-weight: 700; color: var(--text-main); margin: 0;\">" + escapeHtml(Messages.get("catalog.title")) + " (Jakarta JPA 3.2)</h1>");
            out.println("          <p style=\"color: var(--text-muted); margin: 0.25rem 0 0;\">" + escapeHtml(Messages.get("catalog.subtitle", activeStorage)) + "</p>");
            out.println("        </div>");
            out.println("        <div>");
            out.println("          <button type=\"button\" class=\"app-btn-primary\" style=\"background: #10b981;\" onclick=\"openCatalogRegisterDialog()\">➕ Registrar Nuevo Producto (Diálogo)</button>");
            out.println("        </div>");
            out.println("      </div>");

            // Formulario para añadir producto rápido (Responsivo)
            out.println("      <div class=\"ui-panel\" style=\"background: var(--bg-card); padding: 1.25rem; border-radius: 10px; border: 1px solid var(--border-subtle); margin-bottom: 1.5rem; box-shadow: var(--shadow-sm);\">");
            out.println("        <form method=\"post\" action=\"/productos.xhtml\" class=\"responsive-form\">");
            out.println("          <input type=\"hidden\" name=\"action\" value=\"save\" />");
            out.println("          <div style=\"flex: 1; min-width: 110px;\"><label style=\"display:block; font-size:0.75rem; color:var(--text-muted); font-weight:600; margin-bottom:0.25rem;\">" + escapeHtml(Messages.get("catalog.id")) + "</label><input type=\"text\" name=\"id\" required class=\"app-input\" placeholder=\"PRD-09\" /></div>");
            out.println("          <div style=\"flex: 2; min-width: 180px;\"><label style=\"display:block; font-size:0.75rem; color:var(--text-muted); font-weight:600; margin-bottom:0.25rem;\">" + escapeHtml(Messages.get("catalog.name")) + "</label><input type=\"text\" name=\"name\" required class=\"app-input\" placeholder=\"Nombre del producto\" /></div>");
            out.println("          <div style=\"flex: 1.5; min-width: 130px;\"><label style=\"display:block; font-size:0.75rem; color:var(--text-muted); font-weight:600; margin-bottom:0.25rem;\">" + escapeHtml(Messages.get("catalog.category")) + "</label><input type=\"text\" name=\"category\" value=\"Hardware\" class=\"app-input\" /></div>");
            out.println("          <div style=\"flex: 1; min-width: 100px;\"><label style=\"display:block; font-size:0.75rem; color:var(--text-muted); font-weight:600; margin-bottom:0.25rem;\">" + escapeHtml(Messages.get("catalog.price")) + " ($)</label><input type=\"number\" step=\"0.01\" name=\"price\" value=\"199.99\" class=\"app-input\" /></div>");
            out.println("          <div style=\"flex: 1; min-width: 80px;\"><label style=\"display:block; font-size:0.75rem; color:var(--text-muted); font-weight:600; margin-bottom:0.25rem;\">" + escapeHtml(Messages.get("catalog.stock")) + "</label><input type=\"number\" name=\"stock\" value=\"25\" class=\"app-input\" /></div>");
            out.println("          <div style=\"align-self: flex-end; display: flex; gap: 0.5rem;\">");
            out.println("            <button type=\"submit\" class=\"app-btn-primary\">+ " + escapeHtml(Messages.get("catalog.button.add")) + "</button>");
            out.println("            <button type=\"button\" class=\"app-btn-secondary\" onclick=\"openCatalogRegisterDialog()\">➕ Diálogo</button>");
            out.println("          </div>");
            out.println("        </form>");
            out.println("      </div>");

            // Tabla PrimeFaces Responsiva
            out.println("      <div class=\"table-container ui-card\" style=\"border: 1px solid var(--border-subtle); border-radius: 10px; overflow: hidden;\">");
            out.println("        <table class=\"app-table\">");
            out.println("          <thead>");
            out.println("            <tr>");
            out.println("              <th>" + escapeHtml(Messages.get("catalog.id")) + "</th>");
            out.println("              <th>" + escapeHtml(Messages.get("catalog.name")) + "</th>");
            out.println("              <th>" + escapeHtml(Messages.get("catalog.category")) + "</th>");
            out.println("              <th>" + escapeHtml(Messages.get("catalog.price")) + "</th>");
            out.println("              <th>" + escapeHtml(Messages.get("catalog.stock")) + "</th>");
            out.println("              <th style=\"text-align: right;\">" + escapeHtml(Messages.get("catalog.actions")) + "</th>");
            out.println("            </tr>");
            out.println("          </thead>");
            out.println("          <tbody>");

            for (Product p : products) {
                String jsId = escapeJs(p.getId());
                String jsName = escapeJs(p.getName());
                String jsCat = escapeJs(p.getCategory() != null ? p.getCategory() : "Hardware");
                String jsPrice = String.format(java.util.Locale.US, "%.2f", p.getPrice());
                String jsStock = String.valueOf(p.getStock());

                out.println("            <tr>");
                out.println("              <td style=\"font-weight: 700; color: var(--primary);\">" + escapeHtml(p.getId()) + "</td>");
                out.println("              <td style=\"font-weight: 500; color: var(--text-main);\">" + escapeHtml(p.getName()) + "</td>");
                out.println("              <td><span style=\"background: var(--bg-subtle); color: var(--text-muted); padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.8rem; border: 1px solid var(--border-subtle);\">" + escapeHtml(p.getCategory()) + "</span></td>");
                out.println("              <td style=\"color: var(--success); font-weight: 700;\">$" + String.format("%.2f", p.getPrice()) + "</td>");
                out.println("              <td style=\"color: var(--text-main);\">" + p.getStock() + " un.</td>");
                out.println("              <td style=\"text-align: right;\">");
                out.println("                <div style=\"display: inline-flex; gap: 0.45rem; align-items: center;\">");
                out.println("                  <button type=\"button\" class=\"app-btn-secondary\" style=\"padding: 0.25rem 0.65rem; font-size: 0.8rem; color: var(--primary); border-color: var(--primary);\" onclick=\"openCatalogEditDialog('" + jsId + "', '" + jsName + "', '" + jsCat + "', '" + jsPrice + "', '" + jsStock + "')\">✏️ Editar</button>");
                out.println("                  <button type=\"button\" class=\"app-btn-danger\" style=\"padding: 0.25rem 0.65rem; font-size: 0.8rem;\" onclick=\"openCatalogDeleteDialog('" + jsId + "', '" + jsName + "', '" + jsCat + "', '" + jsPrice + "', '" + jsStock + "')\">🗑️ " + escapeHtml(Messages.get("catalog.button.delete")) + "</button>");
                out.println("                </div>");
                out.println("              </td>");
                out.println("            </tr>");
            }

            out.println("          </tbody>");
            out.println("        </table>");
            out.println("      </div>");

            renderCatalogDialogsAndScript(out, "/productos.xhtml");
        });
    }

    private void renderCatalogDialogsAndScript(PrintWriter out, String returnToUrl) {
        String safeReturn = escapeHtml(returnToUrl != null ? returnToUrl : "/productos.xhtml");
        // 1. Diálogo Modal para Registrar Nuevo Producto
        out.println("      <div id=\"catalogRegisterDialog\" style=\"display:none; position:fixed; inset:0; background:rgba(15,23,42,0.75); backdrop-filter:blur(4px); z-index:9999; align-items:center; justify-content:center; padding:1rem;\">");
        out.println("        <div class=\"ui-card\" style=\"background:var(--bg-card); border:1px solid var(--border-subtle); border-radius:14px; width:100%; max-width:520px; padding:1.5rem; box-shadow:0 20px 25px -5px rgba(0,0,0,0.5);\">");
        out.println("          <div style=\"display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid var(--border-subtle); padding-bottom:0.85rem; margin-bottom:1rem;\">");
        out.println("            <h3 style=\"margin:0; font-size:1.15rem; font-weight:700; color:var(--text-main);\">➕ Registrar Nuevo Producto</h3>");
        out.println("            <button type=\"button\" onclick=\"closeCatalogDialog('catalogRegisterDialog')\" style=\"background:transparent; border:none; color:var(--text-muted); font-size:1.25rem; cursor:pointer;\">✕</button>");
        out.println("          </div>");
        out.println("          <form method=\"post\" action=\"/productos.xhtml\" style=\"display:flex; flex-direction:column; gap:0.85rem;\">");
        out.println("            <input type=\"hidden\" name=\"action\" value=\"save\" />");
        out.println("            <input type=\"hidden\" name=\"returnTo\" value=\"" + safeReturn + "\" />");
        out.println("            <div><label style=\"display:block; font-size:0.8rem; color:var(--text-muted); font-weight:600; margin-bottom:0.3rem;\">Código ID:</label><input type=\"text\" id=\"regDialogId\" name=\"id\" required class=\"app-input\" placeholder=\"PRD-XX\" /></div>");
        out.println("            <div><label style=\"display:block; font-size:0.8rem; color:var(--text-muted); font-weight:600; margin-bottom:0.3rem;\">Nombre del Producto:</label><input type=\"text\" id=\"regDialogName\" name=\"name\" required class=\"app-input\" placeholder=\"Ej. Switch Óptico Jettra\" /></div>");
        out.println("            <div><label style=\"display:block; font-size:0.8rem; color:var(--text-muted); font-weight:600; margin-bottom:0.3rem;\">Categoría:</label>");
        out.println("              <select id=\"regDialogCategory\" name=\"category\" class=\"app-input\">");
        out.println("                <option value=\"Hardware\">Hardware</option><option value=\"Software\">Software</option><option value=\"Redes\">Redes</option><option value=\"Almacenamiento\">Almacenamiento</option><option value=\"Add-on\">Add-on</option>");
        out.println("              </select>");
        out.println("            </div>");
        out.println("            <div style=\"display:grid; grid-template-columns:1fr 1fr; gap:0.85rem;\">");
        out.println("              <div><label style=\"display:block; font-size:0.8rem; color:var(--text-muted); font-weight:600; margin-bottom:0.3rem;\">Precio ($):</label><input type=\"number\" step=\"0.01\" id=\"regDialogPrice\" name=\"price\" value=\"199.99\" required class=\"app-input\" /></div>");
        out.println("              <div><label style=\"display:block; font-size:0.8rem; color:var(--text-muted); font-weight:600; margin-bottom:0.3rem;\">Stock Inicial:</label><input type=\"number\" id=\"regDialogStock\" name=\"stock\" value=\"15\" required class=\"app-input\" /></div>");
        out.println("            </div>");
        out.println("            <div style=\"display:flex; justify-content:flex-end; gap:0.75rem; margin-top:0.5rem; padding-top:0.85rem; border-top:1px solid var(--border-subtle);\">");
        out.println("              <button type=\"button\" class=\"app-btn-secondary\" onclick=\"closeCatalogDialog('catalogRegisterDialog')\">Cancelar</button>");
        out.println("              <button type=\"submit\" class=\"app-btn-primary\" style=\"background:#10b981;\">➕ Registrar Producto</button>");
        out.println("            </div>");
        out.println("          </form>");
        out.println("        </div>");
        out.println("      </div>");

        // 2. Diálogo Modal para Editar Producto
        out.println("      <div id=\"catalogEditDialog\" style=\"display:none; position:fixed; inset:0; background:rgba(15,23,42,0.75); backdrop-filter:blur(4px); z-index:9999; align-items:center; justify-content:center; padding:1rem;\">");
        out.println("        <div class=\"ui-card\" style=\"background:var(--bg-card); border:1px solid var(--border-subtle); border-radius:14px; width:100%; max-width:520px; padding:1.5rem; box-shadow:0 20px 25px -5px rgba(0,0,0,0.5);\">");
        out.println("          <div style=\"display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid var(--border-subtle); padding-bottom:0.85rem; margin-bottom:1rem;\">");
        out.println("            <h3 id=\"editDialogTitle\" style=\"margin:0; font-size:1.15rem; font-weight:700; color:var(--text-main);\">✏️ Editar Producto</h3>");
        out.println("            <button type=\"button\" onclick=\"closeCatalogDialog('catalogEditDialog')\" style=\"background:transparent; border:none; color:var(--text-muted); font-size:1.25rem; cursor:pointer;\">✕</button>");
        out.println("          </div>");
        out.println("          <form method=\"post\" action=\"/productos.xhtml\" style=\"display:flex; flex-direction:column; gap:0.85rem;\">");
        out.println("            <input type=\"hidden\" name=\"action\" value=\"save\" />");
        out.println("            <input type=\"hidden\" name=\"returnTo\" value=\"" + safeReturn + "\" />");
        out.println("            <div><label style=\"display:block; font-size:0.8rem; color:var(--text-muted); font-weight:600; margin-bottom:0.3rem;\">Código ID:</label><input type=\"text\" id=\"editDialogId\" name=\"id\" readonly class=\"app-input\" style=\"opacity:0.85;\" /></div>");
        out.println("            <div><label style=\"display:block; font-size:0.8rem; color:var(--text-muted); font-weight:600; margin-bottom:0.3rem;\">Nombre del Producto:</label><input type=\"text\" id=\"editDialogName\" name=\"name\" required class=\"app-input\" /></div>");
        out.println("            <div><label style=\"display:block; font-size:0.8rem; color:var(--text-muted); font-weight:600; margin-bottom:0.3rem;\">Categoría:</label>");
        out.println("              <select id=\"editDialogCategory\" name=\"category\" class=\"app-input\">");
        out.println("                <option value=\"Hardware\">Hardware</option><option value=\"Software\">Software</option><option value=\"Redes\">Redes</option><option value=\"Almacenamiento\">Almacenamiento</option><option value=\"Add-on\">Add-on</option>");
        out.println("              </select>");
        out.println("            </div>");
        out.println("            <div style=\"display:grid; grid-template-columns:1fr 1fr; gap:0.85rem;\">");
        out.println("              <div><label style=\"display:block; font-size:0.8rem; color:var(--text-muted); font-weight:600; margin-bottom:0.3rem;\">Precio ($):</label><input type=\"number\" step=\"0.01\" id=\"editDialogPrice\" name=\"price\" required class=\"app-input\" /></div>");
        out.println("              <div><label style=\"display:block; font-size:0.8rem; color:var(--text-muted); font-weight:600; margin-bottom:0.3rem;\">Stock Disponible:</label><input type=\"number\" id=\"editDialogStock\" name=\"stock\" required class=\"app-input\" /></div>");
        out.println("            </div>");
        out.println("            <div style=\"display:flex; justify-content:flex-end; gap:0.75rem; margin-top:0.5rem; padding-top:0.85rem; border-top:1px solid var(--border-subtle);\">");
        out.println("              <button type=\"button\" class=\"app-btn-secondary\" onclick=\"closeCatalogDialog('catalogEditDialog')\">Cancelar</button>");
        out.println("              <button type=\"submit\" class=\"app-btn-primary\">💾 Guardar Cambios</button>");
        out.println("            </div>");
        out.println("          </form>");
        out.println("        </div>");
        out.println("      </div>");

        // 3. Diálogo Modal para Confirmar Eliminación
        out.println("      <div id=\"catalogDeleteDialog\" style=\"display:none; position:fixed; inset:0; background:rgba(15,23,42,0.75); backdrop-filter:blur(4px); z-index:9999; align-items:center; justify-content:center; padding:1rem;\">");
        out.println("        <div class=\"ui-card\" style=\"background:var(--bg-card); border:1px solid var(--border-subtle); border-radius:14px; width:100%; max-width:480px; padding:1.5rem; box-shadow:0 20px 25px -5px rgba(0,0,0,0.5);\">");
        out.println("          <div style=\"display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid var(--border-subtle); padding-bottom:0.85rem; margin-bottom:1rem;\">");
        out.println("            <h3 style=\"margin:0; font-size:1.15rem; font-weight:700; color:var(--text-main);\">🗑️ Confirmar Eliminación de Producto</h3>");
        out.println("            <button type=\"button\" onclick=\"closeCatalogDialog('catalogDeleteDialog')\" style=\"background:transparent; border:none; color:var(--text-muted); font-size:1.25rem; cursor:pointer;\">✕</button>");
        out.println("          </div>");
        out.println("          <p id=\"deleteDialogMsg\" style=\"font-size:0.95rem; font-weight:600; color:var(--text-main); margin:0 0 0.85rem 0;\">¿Está seguro que desea eliminar este producto?</p>");
        out.println("          <div id=\"deleteDialogDetails\" style=\"background:var(--danger-light); border:1px solid var(--danger); color:var(--danger); padding:0.75rem 1rem; border-radius:8px; font-size:0.85rem; margin-bottom:1rem;\"></div>");
        out.println("          <form method=\"post\" action=\"/productos.xhtml\" style=\"display:flex; justify-content:flex-end; gap:0.75rem; padding-top:0.85rem; border-top:1px solid var(--border-subtle);\">");
        out.println("            <input type=\"hidden\" name=\"action\" value=\"delete\" />");
        out.println("            <input type=\"hidden\" name=\"returnTo\" value=\"" + safeReturn + "\" />");
        out.println("            <input type=\"hidden\" id=\"deleteDialogId\" name=\"id\" value=\"\" />");
        out.println("            <button type=\"button\" class=\"app-btn-secondary\" onclick=\"closeCatalogDialog('catalogDeleteDialog')\">Cancelar</button>");
        out.println("            <button type=\"submit\" class=\"app-btn-danger\">🗑️ Confirmar Eliminación</button>");
        out.println("          </form>");
        out.println("        </div>");
        out.println("      </div>");

        out.println("      <script>");
        out.println("        function openCatalogRegisterDialog() {");
        out.println("          const d = document.getElementById('catalogRegisterDialog');");
        out.println("          if (d) d.style.display = 'flex';");
        out.println("        }");
        out.println("        function openCatalogEditDialog(id, name, category, price, stock) {");
        out.println("          document.getElementById('editDialogTitle').textContent = '✏️ Editar Producto (' + id + ')';");
        out.println("          document.getElementById('editDialogId').value = id;");
        out.println("          document.getElementById('editDialogName').value = name;");
        out.println("          document.getElementById('editDialogCategory').value = category || 'Hardware';");
        out.println("          document.getElementById('editDialogPrice').value = price;");
        out.println("          document.getElementById('editDialogStock').value = stock;");
        out.println("          document.getElementById('catalogEditDialog').style.display = 'flex';");
        out.println("        }");
        out.println("        function openCatalogDeleteDialog(id, name, category, price, stock) {");
        out.println("          document.getElementById('deleteDialogId').value = id;");
        out.println("          document.getElementById('deleteDialogMsg').textContent = '¿Está seguro que desea eliminar el producto \"' + name + '\" (' + id + ')?';");
        out.println("          document.getElementById('deleteDialogDetails').textContent = 'Categoría: ' + category + ' | Precio: $' + price + ' | Stock: ' + stock + ' unidades';");
        out.println("          document.getElementById('catalogDeleteDialog').style.display = 'flex';");
        out.println("        }");
        out.println("        function closeCatalogDialog(dialogId) {");
        out.println("          const d = document.getElementById(dialogId);");
        out.println("          if (d) d.style.display = 'none';");
        out.println("        }");
        out.println("      </script>");
    }

    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "&quot;");
    }

    private void renderUsuarios(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : new User("admin", "", "Administrador", "ADMIN", "");
        res.setContentType("text/html; charset=UTF-8");
        PrintWriter out = res.getWriter();

        String activeStorage = getEffectiveStorageMode();

        renderTemplate(out, "Gestión de Usuarios", user, activeStorage, "usuarios", () -> {
            out.println("      <h1 style=\"font-size: 1.5rem; font-weight: 700; color: var(--text-main); margin-top: 0;\">Usuarios del Sistema (Jakarta Security)</h1>");
            out.println("      <p style=\"color: var(--text-muted); margin-bottom: 1.5rem;\">Credenciales aseguradas y roles RBAC activos.</p>");

            out.println("      <div class=\"table-container ui-card\" style=\"border: 1px solid var(--border-subtle); border-radius: 10px; overflow: hidden;\">");
            out.println("        <table class=\"app-table\">");
            out.println("          <thead><tr><th>Usuario</th><th>Nombre Completo</th><th>Rol</th><th>Email</th></tr></thead>");
            out.println("          <tbody>");
            out.println("            <tr><td style=\"font-weight: 700; color: var(--primary);\">admin</td><td>Administrador Principal</td><td><span style=\"background:var(--primary-light);color:var(--primary);padding:0.2rem 0.5rem;border-radius:4px;font-weight:600;\">ADMIN</span></td><td>admin@jettra.io</td></tr>");
            out.println("            <tr><td style=\"font-weight: 700; color: var(--primary);\">operador</td><td>Operador de Planta</td><td><span style=\"background:var(--warning-light);color:var(--warning);padding:0.2rem 0.5rem;border-radius:4px;font-weight:600;\">OPERATOR</span></td><td>operador@jettra.io</td></tr>");
            out.println("          </tbody>");
            out.println("        </table>");
            out.println("      </div>");
        });
    }

    private void renderTemplate(PrintWriter out, String title, User user, String storageMode, String activeSection, Runnable centerContent) {
        out.println("<!DOCTYPE html>");
        out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"es\">");
        out.println("<head>");
        out.println("  <meta charset=\"UTF-8\" />");
        out.println("  <title>" + escapeHtml(title) + " - JettraServer</title>");
        out.println(commonHeadStylesAndScripts());
        out.println("</head>");
        out.println("<body class=\"app-body\" style=\"margin: 0; display: flex; flex-direction: column; min-height: 100vh;\">");

        // Telón de fondo para menú móvil
        out.println("  <div id=\"sidebarBackdrop\" class=\"sidebar-backdrop\" onclick=\"toggleSidebar()\"></div>");

        // TOP SECTION (Header)
        out.println("  <!-- TOP: Header y Barra Superior Responsiva -->");
        out.println("  <header class=\"ui-layout-top\">");
        out.println("    <div style=\"display: flex; align-items: center; gap: 0.75rem;\">");
        out.println("      <button class=\"hamburger-btn\" type=\"button\" onclick=\"toggleSidebar()\" aria-label=\"Alternar menú lateral\" title=\"Colapsar / Expandir menú\">");
        out.println("        <span class=\"hamburger-bar\"></span>");
        out.println("        <span class=\"hamburger-bar\"></span>");
        out.println("        <span class=\"hamburger-bar\"></span>");
        out.println("      </button>");
        out.println("      <div style=\"background: var(--primary); color: white; width: 34px; height: 34px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-weight: bold;\">JS</div>");
        out.println("      <span style=\"font-weight: 700; font-size: 1.1rem; letter-spacing: -0.02em; color: white;\">JettraServer <span class=\"hide-mobile\" style=\"font-size: 0.75rem; font-weight: normal; background: rgba(255,255,255,0.15); padding: 0.15rem 0.45rem; border-radius: 4px; margin-left: 0.25rem;\">Jakarta EE 11</span></span>");
        out.println("    </div>");
        out.println("    <div class=\"top-right-group\">");
        out.println("      <button onclick=\"toggleTheme()\" class=\"theme-toggle-btn\" type=\"button\" title=\"Alternar Tema Claro / Oscuro\" id=\"btnThemeToggle\">");
        out.println("        <span id=\"themeIcon\">🌙</span> <span id=\"themeLabel\" class=\"hide-mobile\">Oscuro</span>");
        out.println("      </button>");
        out.println("      <span class=\"hide-mobile\" style=\"background: rgba(255,255,255,0.1); color: #cbd5e1; font-size: 0.8rem; padding: 0.3rem 0.6rem; border-radius: 6px;\">Modo: <strong style=\"color:#38bdf8;\">" + storageMode + "</strong></span>");
        out.println("      <span class=\"hide-mobile\" style=\"font-size: 0.85rem; color: #cbd5e1;\">👤 " + escapeHtml(user.getFullName()) + "</span>");
        out.println("      <a href=\"/logout\" class=\"app-btn-danger\" style=\"text-decoration: none; font-size: 0.8rem; padding: 0.35rem 0.65rem;\">Cerrar Sesión</a>");
        out.println("    </div>");
        out.println("  </header>");

        // WRAPPER (LEFT + CENTER)
        out.println("  <div class=\"ui-layout-wrapper\">");

        // LEFT SECTION (Sidebar Menu)
        out.println("    <!-- LEFT: Menú Lateral de Navegación Responsivo -->");
        out.println("    <aside id=\"appSidebar\" class=\"ui-layout-left\">");
        out.println("      <div style=\"font-size: 0.75rem; text-transform: uppercase; font-weight: 700; color: var(--text-light); margin-bottom: 0.75rem; letter-spacing: 0.05em;\">Navegación General</div>");
        out.println("      <nav style=\"display: flex; flex-direction: column; gap: 0.25rem; margin-bottom: 1.75rem;\">");
        out.println("        <a href=\"/dashboard.xhtml\" class=\"nav-link " + ("general".equals(activeSection) ? "active" : "") + "\">📊 Dashboard</a>");
        out.println("        <a href=\"/productos.xhtml\" class=\"nav-link " + ("productos".equals(activeSection) ? "active" : "") + "\">📦 Catálogo JPA</a>");
        out.println("        <a href=\"/usuarios.xhtml\" class=\"nav-link " + ("usuarios".equals(activeSection) ? "active" : "") + "\">👥 Usuarios Security</a>");
        out.println("      </nav>");

        out.println("      <div style=\"font-size: 0.75rem; text-transform: uppercase; font-weight: 700; color: var(--text-light); margin-bottom: 0.75rem; letter-spacing: 0.05em;\">⚡ Vistas JettraFlux (Java Puro)</div>");
        out.println("      <nav style=\"display: flex; flex-direction: column; gap: 0.25rem; margin-bottom: 1.75rem;\">");
        out.println("        <a href=\"/flux/catalogo\" class=\"nav-link " + (("catalogo-flux".equals(activeSection) || "catalogo".equals(activeSection)) ? "active" : "") + "\">🛍️ Catálogo Reactivo (Flux)</a>");
        out.println("        <a href=\"/dashboard.xhtml?view=metrics\" class=\"nav-link " + ("metrics".equals(activeSection) ? "active" : "") + "\">📊 Métricas del Servidor (Flux)</a>");
        out.println("        <a href=\"/dashboard.xhtml?view=health\" class=\"nav-link " + ("health".equals(activeSection) ? "active" : "") + "\">🩺 Estado de Salud (Flux)</a>");
        out.println("        <a href=\"/flux/storage-monitor\" class=\"nav-link " + ("storage-monitor".equals(activeSection) ? "active" : "") + "\">🚀 Monitor Almacenamiento</a>");
        out.println("        <a href=\"/flux/telemetry\" class=\"nav-link " + ("telemetry".equals(activeSection) ? "active" : "") + "\">📈 Telemetría de Red</a>");
        out.println("        <a href=\"/flux/counter\" class=\"nav-link " + ("counter".equals(activeSection) ? "active" : "") + "\">🎛️ Contador Reactivo</a>");
        out.println("      </nav>");

        out.println("      <div style=\"font-size: 0.75rem; text-transform: uppercase; font-weight: 700; color: var(--text-light); margin-bottom: 0.75rem; letter-spacing: 0.05em;\">✨ Ejemplos JettraAura)</div>");
        out.println("      <nav style=\"display: flex; flex-direction: column; gap: 0.25rem; margin-bottom: 1.75rem;\">");
        out.println("        <a href=\"/aura/demo\" class=\"nav-link\">✨ Portal JettraAura</a>");
        out.println("        <a href=\"/aura/catalogo\" class=\"nav-link\">📦 Catálogo Dinámico (ListView)</a>");
        out.println("        <a href=\"/aura/formulario\" class=\"nav-link\">📝 Formulario Reactivo (CPM)</a>");
        out.println("        <a href=\"/aura/contador\" class=\"nav-link\">⚡ Contador AJAX Parcial</a>");
        out.println("      </nav>");

        out.println("      <div style=\"font-size: 0.75rem; text-transform: uppercase; font-weight: 700; color: var(--text-light); margin-bottom: 0.75rem; letter-spacing: 0.05em;\">🔌 Servicios RESTful &amp; Docs</div>");
        out.println("      <nav style=\"display: flex; flex-direction: column; gap: 0.25rem;\">");
        out.println("        <a href=\"/swagger-ui\" target=\"_blank\" class=\"nav-link\">📖 Swagger-UI (OpenAPI)</a>");
        out.println("        <a href=\"/api/productos\" target=\"_blank\" class=\"nav-link\">/api/productos (JSON)</a>");
        out.println("        <a href=\"/api/info\" target=\"_blank\" class=\"nav-link\">/api/info (JSON)</a>");
        out.println("        <a href=\"/metrics\" target=\"_blank\" class=\"nav-link\">/metrics (Métricas JSON)</a>");
        out.println("        <a href=\"/health\" target=\"_blank\" class=\"nav-link\">/health (Health JSON)</a>");
        out.println("      </nav>");
        out.println("    </aside>");

        // CENTER SECTION (Main Content)
        out.println("    <!-- CENTER: Área Principal de Contenido Dinámico -->");
        out.println("    <main class=\"ui-layout-center\">");
        centerContent.run();
        out.println("    </main>");

        out.println("  </div>");

        // FOOTER SECTION
        out.println("  <!-- FOOTER: Pie de Página Responsivo -->");
        out.println("  <footer class=\"ui-layout-footer\">");
        out.println("    <div>JettraServer Enterprise • Java 25 &amp; Loom Virtual Threads • PrimeFaces 14.0 Jakarta</div>");
        out.println("    <div>Estado: <span style=\"color: var(--success); font-weight: bold;\">● OPERATIVO</span> • Modo: <strong>" + storageMode + "</strong></div>");
        out.println("  </footer>");

        out.println("</body>");
        out.println("</html>");
    }

    private String getEffectiveStorageMode() {
        if (server != null && server.storage() != null && server.storage().getMode() != null) {
            return server.storage().getMode().name();
        }
        if (server != null && server.getConfig() != null && server.getConfig().getStorageMode() != null) {
            return server.getConfig().getStorageMode().name();
        }
        String p = System.getProperty("storage.mode");
        if (p == null || p.isBlank()) p = System.getProperty("jettra.storage.mode");
        if (p != null && !p.isBlank()) return io.jettra.server.core.storage.StorageMode.fromString(p).name();
        return "JVM_RAM";
    }

    private static String commonHeadStylesAndScripts() {
        return """
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <style>
            :root {
              --bg-app: #f8fafc;
              --bg-card: #ffffff;
              --bg-topbar: #0f172a;
              --bg-sidebar: #ffffff;
              --bg-footer: #0f172a;
              --bg-subtle: #f1f5f9;
              --text-main: #0f172a;
              --text-muted: #64748b;
              --text-light: #94a3b8;
              --border-subtle: #e2e8f0;
              --border-focus: #2563eb;
              --primary: #2563eb;
              --primary-hover: #1d4ed8;
              --primary-light: #eff6ff;
              --success: #059669;
              --success-light: #ecfdf5;
              --warning: #d97706;
              --warning-light: #fffbeb;
              --danger: #dc2626;
              --danger-light: #fef2f2;
              --shadow-sm: 0 1px 3px rgba(0,0,0,0.06);
              --shadow-lg: 0 10px 25px -5px rgba(0,0,0,0.1), 0 8px 10px -6px rgba(0,0,0,0.1);
            }

            [data-theme="dark"] {
              --bg-app: #0b1120;
              --bg-card: #1e293b;
              --bg-topbar: #020617;
              --bg-sidebar: #0f172a;
              --bg-footer: #020617;
              --bg-subtle: #0f172a;
              --text-main: #f8fafc;
              --text-muted: #94a3b8;
              --text-light: #64748b;
              --border-subtle: #334155;
              --border-focus: #38bdf8;
              --primary: #38bdf8;
              --primary-hover: #0284c7;
              --primary-light: #082f49;
              --success: #34d399;
              --success-light: #064e3b;
              --warning: #fbbf24;
              --warning-light: #78350f;
              --danger: #f87171;
              --danger-light: #7f1d1d;
              --shadow-sm: 0 1px 3px rgba(0,0,0,0.5);
              --shadow-lg: 0 10px 25px -5px rgba(0,0,0,0.6);
            }

            * { box-sizing: border-box; }
            body.app-body {
              background-color: var(--bg-app);
              color: var(--text-main);
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
              transition: background-color 0.25s ease, color 0.25s ease;
            }

            .ui-layout-top {
              height: 60px;
              background: var(--bg-topbar);
              color: white;
              display: flex;
              align-items: center;
              justify-content: space-between;
              padding: 0 1.5rem;
              position: sticky;
              top: 0;
              z-index: 100;
              border-bottom: 1px solid var(--border-subtle);
            }

            .ui-layout-wrapper {
              display: flex;
              flex: 1;
              min-height: calc(100vh - 100px);
            }

            .hamburger-btn {
              display: inline-flex;
              flex-direction: column;
              justify-content: space-around;
              width: 34px;
              height: 34px;
              background: rgba(255, 255, 255, 0.12);
              border: 1px solid rgba(255, 255, 255, 0.25);
              border-radius: 6px;
              cursor: pointer;
              padding: 7px;
              transition: background 0.2s, transform 0.2s;
              box-sizing: border-box;
            }
            .hamburger-btn:hover {
              background: rgba(255, 255, 255, 0.25);
              transform: scale(1.05);
            }
            .hamburger-bar {
              width: 100%;
              height: 2px;
              background-color: #ffffff;
              border-radius: 2px;
              transition: all 0.25s ease-in-out;
            }

            .ui-layout-left {
              width: 260px;
              background: var(--bg-sidebar);
              border-right: 1px solid var(--border-subtle);
              padding: 1.5rem 1rem;
              box-sizing: border-box;
              transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1),
                          transform 0.3s cubic-bezier(0.4, 0, 0.2, 1),
                          opacity 0.25s ease,
                          background 0.25s ease;
              flex-shrink: 0;
            }

            /* Escritorio (>900px): Colapsar y expandir sidebar ocupando el 100% de la pantalla */
            @media (min-width: 901px) {
              body.sidebar-collapsed .ui-layout-left {
                margin-left: -260px;
                opacity: 0;
                pointer-events: none;
              }
            }

            .ui-layout-center {
              flex: 1;
              padding: 2rem;
              box-sizing: border-box;
              overflow-y: auto;
              transition: padding 0.2s ease;
            }

            .ui-layout-footer {
              min-height: 40px;
              background: var(--bg-footer);
              color: var(--text-light);
              display: flex;
              align-items: center;
              justify-content: space-between;
              padding: 0.5rem 1.5rem;
              font-size: 0.75rem;
              border-top: 1px solid var(--border-subtle);
              flex-wrap: wrap;
              gap: 0.5rem;
            }

            .kpi-grid {
              display: grid;
              grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
              gap: 1.25rem;
            }

            .kpi-card {
              background: var(--bg-card);
              padding: 1.25rem;
              border-radius: 12px;
              border: 1px solid var(--border-subtle);
              box-shadow: var(--shadow-sm);
            }

            .kpi-title {
              font-size: 0.75rem;
              text-transform: uppercase;
              letter-spacing: 0.05em;
              color: var(--text-muted);
              font-weight: 700;
            }

            .kpi-value {
              font-size: 1.6rem;
              font-weight: 800;
              margin: 0.4rem 0;
            }

            .kpi-sub {
              font-size: 0.8rem;
              color: var(--text-muted);
            }

            .nav-link {
              display: flex;
              align-items: center;
              gap: 0.5rem;
              padding: 0.6rem 0.75rem;
              color: var(--text-muted);
              text-decoration: none;
              border-radius: 8px;
              font-weight: 500;
              font-size: 0.875rem;
              transition: background 0.15s, color 0.15s;
            }

            .nav-link:hover {
              background: var(--bg-subtle);
              color: var(--text-main);
            }

            .nav-link.active {
              background: var(--primary-light);
              color: var(--primary);
              font-weight: 700;
            }

            .flux-card-link {
              text-decoration: none;
              color: inherit;
              padding: 1.25rem;
              background: var(--bg-subtle);
              border: 1px solid var(--border-subtle);
              border-radius: 10px;
              display: block;
              transition: transform 0.2s, border-color 0.2s;
            }

            .flux-card-link:hover {
              transform: translateY(-2px);
              border-color: var(--primary);
            }

            .flux-tabs {
              display: inline-flex;
              background: var(--bg-subtle);
              padding: 0.25rem;
              border-radius: 8px;
              gap: 0.25rem;
              border: 1px solid var(--border-subtle);
            }

            .tab-pill {
              padding: 0.4rem 0.85rem;
              border-radius: 6px;
              font-size: 0.8rem;
              font-weight: 600;
              text-decoration: none;
              color: var(--text-muted);
              transition: background 0.15s, color 0.15s;
            }

            .tab-pill.active {
              background: var(--bg-card);
              color: var(--primary);
              box-shadow: 0 1px 2px rgba(0,0,0,0.1);
            }

            .app-input {
              width: 100%;
              padding: 0.65rem 0.75rem;
              background: var(--bg-card);
              border: 1px solid var(--border-subtle);
              color: var(--text-main);
              border-radius: 8px;
              font-size: 0.9rem;
              transition: border 0.2s;
            }

            .app-input:focus {
              border-color: var(--border-focus);
              outline: none;
            }

            .app-btn-primary {
              background: var(--primary);
              color: white;
              border: none;
              border-radius: 8px;
              padding: 0.65rem 1.25rem;
              font-weight: 600;
              font-size: 0.9rem;
              cursor: pointer;
              transition: opacity 0.2s;
            }
            .app-btn-primary:hover { opacity: 0.9; }

            .app-btn-secondary {
              background: var(--bg-card);
              color: var(--text-main);
              border: 1px solid var(--border-subtle);
              border-radius: 8px;
              padding: 0.65rem 1.25rem;
              font-weight: 600;
              font-size: 0.9rem;
              cursor: pointer;
              transition: background 0.2s;
            }
            .app-btn-secondary:hover { background: var(--bg-subtle); }

            .app-btn-danger {
              background: var(--danger-light);
              color: var(--danger);
              border: 1px solid var(--danger);
              border-radius: 6px;
              padding: 0.4rem 0.8rem;
              font-weight: 600;
              cursor: pointer;
            }

            .theme-toggle-btn {
              background: rgba(255,255,255,0.12);
              color: white;
              border: 1px solid rgba(255,255,255,0.25);
              border-radius: 20px;
              padding: 0.35rem 0.75rem;
              font-size: 0.8rem;
              font-weight: 600;
              cursor: pointer;
              display: inline-flex;
              align-items: center;
              gap: 0.35rem;
              transition: background 0.2s;
            }
            .theme-toggle-btn:hover { background: rgba(255,255,255,0.22); }

            .mobile-toggle-btn {
              display: none;
              background: transparent;
              color: white;
              border: none;
              font-size: 1.5rem;
              cursor: pointer;
              padding: 0.25rem 0.5rem;
            }

            .table-container {
              width: 100%;
              overflow-x: auto;
              background: var(--bg-card);
            }

            .app-table {
              width: 100%;
              border-collapse: collapse;
              text-align: left;
              font-size: 0.9rem;
            }

            .app-table th {
              background: var(--bg-subtle);
              padding: 0.75rem 1rem;
              color: var(--text-muted);
              font-weight: 600;
              border-bottom: 2px solid var(--border-subtle);
            }

            .app-table td {
              padding: 0.75rem 1rem;
              border-bottom: 1px solid var(--border-subtle);
            }

            .responsive-form {
              display: flex;
              gap: 0.75rem;
              flex-wrap: wrap;
              align-items: flex-end;
            }

            .top-right-group {
              display: flex;
              align-items: center;
              gap: 0.85rem;
            }

            .sidebar-backdrop {
              display: none;
            }

            /* BREAKPOINTS RESPONSIVOS */
            @media (max-width: 900px) {
              .ui-layout-left {
                position: fixed;
                top: 60px;
                left: 0;
                bottom: 0;
                width: 270px;
                z-index: 1000;
                transform: translateX(-100%);
                box-shadow: 6px 0 25px rgba(0,0,0,0.3);
                margin-left: 0 !important;
                opacity: 1 !important;
                pointer-events: auto !important;
                max-width: 82vw;
                overflow-y: auto;
              }
              body.mobile-sidebar-open .ui-layout-left,
              .ui-layout-left.open {
                transform: translateX(0);
              }
              .sidebar-backdrop {
                display: none;
                position: fixed;
                top: 60px;
                left: 0;
                right: 0;
                bottom: 0;
                background: rgba(0,0,0,0.5);
                z-index: 999;
                backdrop-filter: blur(2px);
              }
              body.mobile-sidebar-open .sidebar-backdrop,
              .sidebar-backdrop.active {
                display: block;
              }
              .ui-layout-center {
                padding: 1.25rem;
              }
            }

            @media (max-width: 640px) {
              .hide-mobile {
                display: none !important;
              }
              .kpi-grid {
                grid-template-columns: 1fr;
              }
              .responsive-form {
                flex-direction: column;
                align-items: stretch;
              }
              .ui-layout-footer {
                flex-direction: column;
                text-align: center;
              }
            }
          </style>

          <script>
            function getInitialTheme() {
              const cookieMatch = document.cookie.match(/jettra_theme=([^;]+)/);
              if (cookieMatch) return cookieMatch[1];
              const saved = localStorage.getItem('jettra-theme');
              if (saved) return saved;
              return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'white';
            }

            function applyTheme(theme) {
              document.documentElement.setAttribute('data-theme', theme);
              localStorage.setItem('jettra-theme', theme);
              document.cookie = "jettra_theme=" + theme + "; path=/; max-age=31536000";

              const icon = document.getElementById('themeIcon');
              const label = document.getElementById('themeLabel');
              if (icon) icon.textContent = (theme === 'dark') ? '☀️' : '🌙';
              if (label) label.textContent = (theme === 'dark') ? 'Claro' : 'Oscuro';
            }

            function toggleTheme() {
              const current = document.documentElement.getAttribute('data-theme') || getInitialTheme();
              const next = (current === 'dark') ? 'white' : 'dark';
              applyTheme(next);
            }

            function toggleSidebar() {
              const isMobile = window.innerWidth <= 900;
              if (isMobile) {
                const open = document.body.classList.toggle('mobile-sidebar-open');
                const sidebar = document.getElementById('appSidebar');
                const backdrop = document.getElementById('sidebarBackdrop');
                if (sidebar) sidebar.classList.toggle('open', open);
                if (backdrop) backdrop.classList.toggle('active', open);
              } else {
                const collapsed = document.body.classList.toggle('sidebar-collapsed');
                localStorage.setItem('jettra_sidebar_collapsed', collapsed ? 'true' : 'false');
              }
            }

            function closeMobileSidebar() {
              document.body.classList.remove('mobile-sidebar-open');
              const sidebar = document.getElementById('appSidebar');
              const backdrop = document.getElementById('sidebarBackdrop');
              if (sidebar) sidebar.classList.remove('open');
              if (backdrop) backdrop.classList.remove('active');
            }

            // Aplicar tema inmediatamente para evitar parpadeos
            applyTheme(getInitialTheme());
            window.addEventListener('DOMContentLoaded', function() {
              applyTheme(getInitialTheme());
              if (window.innerWidth > 900) {
                const savedCollapsed = localStorage.getItem('jettra_sidebar_collapsed');
                if (savedCollapsed === 'true') {
                  document.body.classList.add('sidebar-collapsed');
                }
              }
              window.addEventListener('resize', function() {
                if (window.innerWidth > 900) {
                  closeMobileSidebar();
                }
              });
            });
          </script>
        """;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
