package io.jettra.server.example.flux;

import io.jettra.flux.core.FluxView;
import io.jettra.flux.core.Modifier;
import io.jettra.flux.core.Widget;
import io.jettra.flux.widgets.Badge;
import io.jettra.flux.widgets.Button;
import io.jettra.flux.widgets.Div;
import io.jettra.flux.widgets.Header;
import io.jettra.flux.widgets.Link;
import io.jettra.flux.widgets.Paragraph;
import io.jettra.flux.widgets.RawHtml;
import io.jettra.flux.widgets.Row;
import io.jettra.flux.widgets.Text;
import io.jettra.server.core.JettraServer;
import io.jettra.server.core.storage.StorageMetrics;
import io.jettra.server.example.model.User;
import io.jettra.server.example.util.Messages;
import io.jettra.server.flux.FluxRenderer;
import java.io.PrintWriter;

/**
 * Plantilla (Template) exclusiva para el área de ejemplos de JettraFlux en WebExample.
 * Construida íntegramente mediante un árbol de componentes Widget de JettraFlux
 * (Div, Row, Column, Header, Button, Link, Text, Badge, Paragraph, Modifier, RawHtml),
 * sin utilizar StringBuilder ni concatenaciones manuales de documentos HTML.
 *
 * Diseñada exclusivamente para envolver las vistas reactivas de JettraFlux
 * (/flux/catalogo, /flux/metrics, /flux/health, /flux/storage-monitor, /flux/telemetry, /flux/counter)
 * manteniendo total aislamiento visual y arquitectónico respecto a Jakarta Faces (.xhtml)
 * y la suite JettraAura (/aura/*).
 */
public class FluxTemplateView extends FluxView {

    private final String title;
    private final String activeSection;
    private final String storageMode;
    private final User user;
    private final Widget contentWidget;
    private final String extraCenterHtml;

    public FluxTemplateView(String title, String activeSection, JettraServer server, User user, Widget contentWidget) {
        this(title, activeSection, resolveStorageMode(server), user, contentWidget, null);
    }

    public FluxTemplateView(String title, String activeSection, String storageMode, User user, Widget contentWidget, String extraCenterHtml) {
        this.title = title != null ? title : "JettraFlux Reactive";
        this.activeSection = activeSection != null ? activeSection : "catalogo-flux";
        this.storageMode = (storageMode != null && !storageMode.isBlank()) ? storageMode : "JVM_RAM";
        this.user = user != null ? user : new User("admin", "admin123", "Administrador Principal", "ADMIN", "admin@jettra.io");
        this.contentWidget = contentWidget;
        this.extraCenterHtml = extraCenterHtml;
    }

    public static String resolveStorageMode(JettraServer server) {
        if (server != null && server.storage() != null) {
            StorageMetrics sm = server.storage().getMetrics();
            if (sm != null && sm.mode() != null) {
                return sm.mode().name();
            }
            if (server.storage().getMode() != null) {
                return server.storage().getMode().name();
            }
        }
        String prop = System.getProperty("storage.mode");
        if (prop == null || prop.isBlank()) {
            prop = System.getProperty("jettra.storage.mode");
        }
        return (prop != null && !prop.isBlank()) ? prop.trim() : "JVM_RAM";
    }

    @Override
    public Widget build() {
        return buildTemplateTree();
    }

    /**
     * Construye el árbol jerárquico de componentes JettraFlux que da forma a la plantilla.
     */
    private Widget buildTemplateTree() {
        // Estilos CSS y scripts reactivos encapsulados en un widget RawHtml
        Widget stylesAndScripts = RawHtml.of(getFluxTemplateStylesAndScripts());

        // Backdrop para apertura de menú en dispositivos móviles
        Div backdrop = Div.of();
        backdrop.id("sidebarBackdrop");
        backdrop.modifier(new Modifier()
                .cssClass("sidebar-backdrop")
                .attribute("onclick", "toggleSidebar()"));

        // Cabecera superior exclusiva JettraFlux
        Widget topbar = buildTopBar();

        // Contenedor principal: Sidebar lateral exclusivo + Área central
        Widget layoutWrapper = buildLayoutWrapper();

        // Pie de página exclusivo JettraFlux
        Widget footer = buildFooter();

        // Contenedor raíz de la plantilla
        Div pageRoot = Div.of(stylesAndScripts, backdrop, topbar, layoutWrapper, footer);
        pageRoot.modifier(new Modifier()
                .cssClass("app-body flux-template-body")
                .style("margin: 0; display: flex; flex-direction: column; min-height: 100vh; width: 100%;"));
        return pageRoot;
    }

    private Widget buildTopBar() {
        // Botón hamburguesa
        Button hamburgerBtn = Button.of(
                Div.of().modifier(new Modifier().cssClass("hamburger-bar")),
                Div.of().modifier(new Modifier().cssClass("hamburger-bar")),
                Div.of().modifier(new Modifier().cssClass("hamburger-bar"))
        );
        hamburgerBtn.modifier(new Modifier()
                .cssClass("hamburger-btn")
                .attribute("type", "button")
                .attribute("onclick", "toggleSidebar()")
                .attribute("aria-label", "Alternar menú JettraFlux")
                .attribute("title", "Colapsar / Expandir menú JettraFlux"));

        // Insignia de marca JF
        Div brandBadge = Div.of(Text.of("JF"));
        brandBadge.modifier(new Modifier().cssClass("flux-brand-badge"));

        // Título del estudio JettraFlux con insignia Java 25
        Div brandTitle = Div.of(
                Text.of("JettraFlux Studio").modifier(new Modifier().style("font-weight: 800; font-size: 1.12rem; letter-spacing: -0.02em; color: #ffffff;")),
                Badge.of("Pure Java 25 Reactive UI").modifier(new Modifier().cssClass("hide-mobile").style("font-size: 0.72rem; font-weight: 700; background: rgba(168, 85, 247, 0.28); border: 1px solid rgba(192, 132, 252, 0.45); color: #f3e8ff; padding: 0.15rem 0.55rem; border-radius: 9999px;"))
        );
        brandTitle.modifier(new Modifier().style("display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap;"));

        Row leftGroup = Row.of(hamburgerBtn, brandBadge, brandTitle);
        leftGroup.modifier(new Modifier().style("display: flex; align-items: center; gap: 0.75rem;"));

        // Conmutador de tema Claro / Oscuro
        Button themeBtn = Button.of(
                RawHtml.of("<span id=\"themeIcon\">🌙</span> <span id=\"themeLabel\" class=\"hide-mobile\">Oscuro</span>")
        );
        themeBtn.id("btnThemeToggle");
        themeBtn.modifier(new Modifier()
                .cssClass("theme-toggle-btn")
                .attribute("type", "button")
                .attribute("onclick", "toggleTheme()")
                .attribute("title", "Alternar Tema Claro / Oscuro"));

        // Indicador de modo de almacenamiento activo
        Div storageBadge = Div.of(RawHtml.of("Modo: <strong style=\"color:#38bdf8;\">" + escapeHtml(storageMode) + "</strong>"));
        storageBadge.modifier(new Modifier()
                .cssClass("hide-mobile")
                .style("background: rgba(15, 23, 42, 0.45); border: 1px solid rgba(148, 163, 184, 0.25); color: #e2e8f0; font-size: 0.8rem; padding: 0.3rem 0.65rem; border-radius: 6px;"));

        // Usuario autenticado
        Div userInfo = Div.of(Text.of("👤 " + (user != null ? escapeHtml(user.getFullName()) : "Administrador Principal")));
        userInfo.modifier(new Modifier()
                .cssClass("hide-mobile")
                .style("font-size: 0.85rem; color: #f1f5f9;"));

        // Enlace de cierre de sesión
        Link logoutLink = Link.of("/logout", "Cerrar Sesión");
        logoutLink.modifier(new Modifier()
                .cssClass("app-btn-danger")
                .style("text-decoration: none; font-size: 0.8rem; padding: 0.35rem 0.75rem; color: #ffffff; border-radius: 6px; font-weight: 600; display: inline-flex; align-items: center;"));

        Row rightGroup = Row.of(themeBtn, storageBadge, userInfo, logoutLink);
        rightGroup.modifier(new Modifier().cssClass("top-right-group"));

        Div topbar = Div.of(leftGroup, rightGroup);
        topbar.modifier(new Modifier().cssClass("ui-layout-top flux-layout-top"));
        return topbar;
    }

    private Widget buildLayoutWrapper() {
        Widget sidebar = buildSidebar();
        Widget center = buildCenter();

        Div layoutWrapper = Div.of(sidebar, center);
        layoutWrapper.modifier(new Modifier().cssClass("ui-layout-wrapper flux-layout-wrapper"));
        return layoutWrapper;
    }

    private Widget buildSidebar() {
        // Banner introductorio
        Div banner = Div.of(
                Div.of(Text.of("⚡ Motor JettraFlux")).modifier(new Modifier().style("font-weight: 800; font-size: 0.88rem; color: #c084fc; display: flex; align-items: center; gap: 0.4rem;")),
                Div.of(Text.of("Plantilla exclusiva para vistas reactivas en Java 25 puro")).modifier(new Modifier().style("font-size: 0.75rem; color: var(--text-muted); margin-top: 0.2rem;"))
        );
        banner.modifier(new Modifier().cssClass("flux-sidebar-banner"));

        // Grupo 1: Suite Ejemplos JettraFlux
        Widget headingFlux = Div.of(Text.of("⚡ Suite Ejemplos JettraFlux")).modifier(new Modifier().cssClass("flux-menu-heading"));
        Div navFlux = Div.of(
                createNavLink("/flux/catalogo", "🛍️ Catálogo Reactivo (Flux)", isActive("catalogo-flux", "catalogo")),
                createNavLink("/flux/metrics", "📊 Métricas del Servidor", isActive("metrics")),
                createNavLink("/flux/health", "🩺 Estado de Salud (Health)", isActive("health")),
                createNavLink("/flux/storage-monitor", "🚀 Monitor Almacenamiento", isActive("storage-monitor")),
                createNavLink("/flux/telemetry", "📈 Telemetría en Tiempo Real", isActive("telemetry")),
                createNavLink("/flux/counter", "🎛️ Contador Reactivo", isActive("counter"))
        );
        navFlux.modifier(new Modifier().style("display: flex; flex-direction: column; gap: 0.28rem; margin-bottom: 1.6rem;"));

        // Grupo 2: Área Jakarta Faces
        Widget headingFaces = Div.of(Text.of("🖥️ Área Jakarta Faces (.xhtml)")).modifier(new Modifier().cssClass("flux-menu-heading"));
        Div navFaces = Div.of(
                createNavLink("/dashboard.xhtml", "📊 Dashboard Jakarta Faces", false),
                createNavLink("/productos.xhtml", "📦 Catálogo JPA (.xhtml)", false),
                createNavLink("/usuarios.xhtml", "👥 Usuarios Security", false)
        );
        navFaces.modifier(new Modifier().style("display: flex; flex-direction: column; gap: 0.25rem; margin-bottom: 1.6rem;"));

        // Grupo 3: Área Suite JettraAura
        Widget headingAura = Div.of(Text.of("✨ Área Suite JettraAura")).modifier(new Modifier().cssClass("flux-menu-heading"));
        Div navAura = Div.of(
                createNavLink("/aura/demo", "✨ Portal JettraAura", false),
                createNavLink("/aura/catalogo", "📦 Catálogo Dinámico (Aura)", false),
                createNavLink("/aura/formulario", "📝 Formulario Reactivo (CPM)", false),
                createNavLink("/aura/contador", "⚡ Contador AJAX Parcial", false)
        );
        navAura.modifier(new Modifier().style("display: flex; flex-direction: column; gap: 0.25rem; margin-bottom: 1.6rem;"));

        // Grupo 4: OpenAPI & Endpoints REST
        Widget headingApi = Div.of(Text.of("🔌 OpenAPI & Endpoints REST")).modifier(new Modifier().cssClass("flux-menu-heading"));
        Div navApi = Div.of(
                createExternalNavLink("/swagger-ui", "📖 Swagger-UI (OpenAPI)"),
                createExternalNavLink("/api/productos", "/api/productos (JSON)"),
                createExternalNavLink("/api/info", "/api/info (JSON)"),
                createExternalNavLink("/metrics", "/metrics (Métricas JSON)"),
                createExternalNavLink("/health", "/health (Health JSON)")
        );
        navApi.modifier(new Modifier().style("display: flex; flex-direction: column; gap: 0.25rem;"));

        Div sidebar = Div.of(banner, headingFlux, navFlux, headingFaces, navFaces, headingAura, navAura, headingApi, navApi);
        sidebar.id("appSidebar");
        sidebar.modifier(new Modifier().cssClass("ui-layout-left flux-layout-left"));
        return sidebar;
    }

    private Widget buildCenter() {
        // Cabecera de la barra de herramientas central
        Badge pillBadge = Badge.of("⚡ JETTRAFLUX TEMPLATE");
        pillBadge.modifier(new Modifier().cssClass("flux-pill-badge"));

        Header titleHeader = Header.of(1, title);
        titleHeader.modifier(new Modifier().style("font-size: 1.45rem; font-weight: 800; color: var(--text-main); margin: 0;"));

        Div titleRow = Div.of(pillBadge, titleHeader);
        titleRow.modifier(new Modifier().style("display: flex; align-items: center; gap: 0.55rem; flex-wrap: wrap;"));

        Paragraph subtitle = Paragraph.of("Renderizado mediante la plantilla exclusiva de ejemplos JettraFlux en Java 25 puro");
        subtitle.modifier(new Modifier().style("color: var(--text-muted); margin: 0.25rem 0 0 0; font-size: 0.85rem;"));

        Div toolbarLeft = Div.of(titleRow, subtitle);

        // Barra de pestañas superiores del catálogo flux
        Div tabs = Div.of(
                createTabLink("/flux/catalogo", "🛍️ " + Messages.get("flux.catalog.title"), isActive("catalogo-flux", "catalogo")),
                createTabLink("/flux/metrics", "📊 Métricas", isActive("metrics")),
                createTabLink("/flux/health", "🩺 Health", isActive("health")),
                createTabLink("/flux/storage-monitor", "🚀 Storage", isActive("storage-monitor")),
                createTabLink("/flux/telemetry", "📈 Telemetría", isActive("telemetry")),
                createTabLink("/flux/counter", "🎛️ Contador", isActive("counter"))
        );
        tabs.modifier(new Modifier().cssClass("flux-tabs"));

        Div toolbar = Div.of(toolbarLeft, tabs);
        toolbar.modifier(new Modifier().cssClass("flux-center-toolbar"));

        // Superficie de contenido que encapsula el widget hijo y el HTML complementario
        Widget innerContent;
        if (contentWidget != null && extraCenterHtml != null && !extraCenterHtml.isBlank()) {
            innerContent = Div.of(contentWidget, RawHtml.of(extraCenterHtml));
        } else if (contentWidget != null) {
            innerContent = contentWidget;
        } else if (extraCenterHtml != null && !extraCenterHtml.isBlank()) {
            innerContent = RawHtml.of(extraCenterHtml);
        } else {
            innerContent = Div.of();
        }

        Div contentSurface = Div.of(innerContent);
        contentSurface.id("fluxContentSurface");
        contentSurface.modifier(new Modifier().cssClass("ui-card flux-content-surface"));

        Div center = Div.of(toolbar, contentSurface);
        center.modifier(new Modifier().cssClass("ui-layout-center flux-layout-center"));
        return center;
    }

    private Widget buildFooter() {
        Div footerLeft = Div.of(Text.of("JettraFlux Reactive UI Template • Java 25 & Project Loom Virtual Threads • Cero HTML Manual"));
        Div footerRight = Div.of(RawHtml.of("Estado: <span style=\"color: var(--success); font-weight: bold;\">● OPERATIVO</span> • Modo Persistencia: <strong>[" + escapeHtml(storageMode) + "]</strong>"));

        Div footer = Div.of(footerLeft, footerRight);
        footer.modifier(new Modifier().cssClass("ui-layout-footer flux-layout-footer"));
        return footer;
    }

    private Link createNavLink(String href, String label, boolean active) {
        Link link = Link.of(href, label);
        String classes = active ? "nav-link active" : "nav-link";
        link.modifier(new Modifier().cssClass(classes));
        return link;
    }

    private Link createExternalNavLink(String href, String label) {
        Link link = Link.of(href, label);
        link.target("_blank");
        link.modifier(new Modifier().cssClass("nav-link"));
        return link;
    }

    private Link createTabLink(String href, String label, boolean active) {
        Link link = Link.of(href, label);
        String classes = active ? "tab-pill active" : "tab-pill";
        link.modifier(new Modifier().cssClass(classes));
        return link;
    }

    private boolean isActive(String... keys) {
        for (String k : keys) {
            if (k != null && k.equalsIgnoreCase(activeSection)) {
                return true;
            }
        }
        return false;
    }

    public String renderFullDocument() {
        return new FluxRenderer().render(this, title + " - JettraFlux Template");
    }

    public void renderTo(PrintWriter out) {
        if (out != null) {
            out.print(renderFullDocument());
        }
    }

    private static String getFluxTemplateStylesAndScripts() {
        return """
          <style>
            :root, [data-theme="dark"] {
              --bg-body: #090d16;
              --bg-card: #131c31;
              --bg-subtle: #1e293b;
              --bg-top: linear-gradient(90deg, #2e1065 0%, #4c1d95 45%, #0f172a 100%);
              --bg-sidebar: #0f172a;
              --bg-footer: #060911;
              --text-main: #f8fafc;
              --text-muted: #94a3b8;
              --text-light: #a78bfa;
              --border-subtle: #2e3b52;
              --primary: #8b5cf6;
              --primary-hover: #7c3aed;
              --primary-light: rgba(139, 92, 246, 0.18);
              --success: #10b981;
              --success-light: rgba(16, 185, 129, 0.15);
              --warning: #f59e0b;
              --warning-light: rgba(245, 158, 11, 0.15);
              --danger: #ef4444;
              --danger-light: rgba(239, 68, 68, 0.15);
              --shadow-sm: 0 4px 12px rgba(0, 0, 0, 0.35);
              --sidebar-width: 268px;
            }

            [data-theme="white"] {
              --bg-body: #f5f3ff;
              --bg-card: #ffffff;
              --bg-subtle: #f8fafc;
              --bg-top: linear-gradient(90deg, #4f46e5 0%, #7c3aed 55%, #0284c7 100%);
              --bg-sidebar: #ffffff;
              --bg-footer: #ede9fe;
              --text-main: #0f172a;
              --text-muted: #475569;
              --text-light: #6d28d9;
              --border-subtle: #ddd6fe;
              --primary: #7c3aed;
              --primary-hover: #6d28d9;
              --primary-light: #ede9fe;
              --success: #059669;
              --success-light: #d1fae5;
              --warning: #d97706;
              --warning-light: #fef3c7;
              --danger: #dc2626;
              --danger-light: #fee2e2;
              --shadow-sm: 0 2px 8px rgba(109, 40, 217, 0.08);
            }

            * { box-sizing: border-box; }

            body.flux-template-body, .flux-template-body {
              font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              background-color: var(--bg-body) !important;
              color: var(--text-main) !important;
              transition: background-color 0.2s ease, color 0.2s ease;
            }

            #jettra-flux-root {
              min-height: 100vh;
              display: flex;
              flex-direction: column;
            }

            .flux-layout-top {
              height: 62px;
              background: var(--bg-top);
              color: #ffffff;
              display: flex;
              align-items: center;
              justify-content: space-between;
              padding: 0 1.25rem;
              position: sticky;
              top: 0;
              z-index: 100;
              box-shadow: 0 2px 10px rgba(0,0,0,0.3);
              border-bottom: 1px solid rgba(167, 139, 250, 0.25);
            }

            .flux-brand-badge {
              background: linear-gradient(135deg, #a855f7, #38bdf8);
              color: #ffffff;
              width: 36px;
              height: 36px;
              border-radius: 10px;
              display: flex;
              align-items: center;
              justify-content: center;
              font-weight: 900;
              font-size: 0.95rem;
              box-shadow: 0 2px 8px rgba(168, 85, 247, 0.45);
            }

            .top-right-group {
              display: flex;
              align-items: center;
              gap: 0.75rem;
            }

            .hamburger-btn {
              display: inline-flex !important;
              flex-direction: column !important;
              justify-content: space-around !important;
              width: 36px !important;
              height: 36px !important;
              background: rgba(255, 255, 255, 0.12) !important;
              border: 1px solid rgba(255, 255, 255, 0.25) !important;
              border-radius: 8px !important;
              cursor: pointer !important;
              padding: 8px !important;
              transition: background 0.2s !important;
            }
            .hamburger-btn:hover { background: rgba(255, 255, 255, 0.22) !important; }
            .hamburger-bar {
              width: 100%;
              height: 2px;
              background-color: #ffffff;
              border-radius: 2px;
              display: block;
            }

            .theme-toggle-btn {
              background: rgba(255, 255, 255, 0.14) !important;
              color: #ffffff !important;
              border: 1px solid rgba(255, 255, 255, 0.28) !important;
              padding: 0.35rem 0.75rem !important;
              border-radius: 9999px !important;
              cursor: pointer !important;
              font-size: 0.8rem !important;
              font-weight: 600 !important;
              display: inline-flex !important;
              align-items: center !important;
              gap: 0.35rem !important;
            }

            .flux-layout-wrapper {
              display: flex;
              flex: 1;
              position: relative;
            }

            .flux-layout-left {
              width: var(--sidebar-width);
              background: var(--bg-sidebar);
              border-right: 1px solid var(--border-subtle);
              padding: 1.25rem 1rem;
              flex-shrink: 0;
              transition: margin-left 0.28s ease, transform 0.28s ease, opacity 0.25s ease;
            }

            .flux-sidebar-banner {
              background: var(--primary-light);
              border: 1px solid rgba(139, 92, 246, 0.35);
              border-radius: 10px;
              padding: 0.75rem 0.85rem;
              margin-bottom: 1.25rem;
            }

            .flux-menu-heading {
              font-size: 0.72rem;
              text-transform: uppercase;
              font-weight: 800;
              color: var(--text-light);
              margin-bottom: 0.55rem;
              letter-spacing: 0.06em;
            }

            .nav-link {
              display: flex;
              align-items: center;
              gap: 0.5rem;
              padding: 0.55rem 0.8rem;
              border-radius: 8px;
              color: var(--text-muted);
              text-decoration: none;
              font-size: 0.88rem;
              font-weight: 500;
              transition: all 0.15s ease;
            }
            .nav-link:hover {
              background: var(--bg-subtle);
              color: var(--text-main);
            }
            .nav-link.active {
              background: var(--primary-light);
              color: var(--primary);
              font-weight: 700;
              border-left: 3px solid var(--primary);
            }

            .flux-layout-center {
              flex: 1;
              padding: 1.75rem;
              overflow-x: hidden;
              max-width: 1440px;
              margin: 0 auto;
              width: 100%;
            }

            .flux-center-toolbar {
              display: flex;
              justify-content: space-between;
              align-items: center;
              flex-wrap: wrap;
              gap: 1rem;
              margin-bottom: 1.5rem;
              padding-bottom: 1rem;
              border-bottom: 1px solid var(--border-subtle);
            }

            .flux-pill-badge {
              background: linear-gradient(135deg, #7c3aed, #2563eb);
              color: #ffffff;
              padding: 0.22rem 0.65rem;
              border-radius: 6px;
              font-size: 0.72rem;
              font-weight: 800;
              letter-spacing: 0.04em;
            }

            .flux-content-surface {
              background: var(--bg-card);
              padding: 1.75rem;
              border-radius: 14px;
              border: 1px solid var(--border-subtle);
              box-shadow: var(--shadow-sm);
            }

            .flux-tabs {
              display: flex;
              gap: 0.45rem;
              flex-wrap: wrap;
            }
            .tab-pill {
              padding: 0.4rem 0.8rem;
              border-radius: 9999px;
              font-size: 0.8rem;
              font-weight: 600;
              text-decoration: none;
              color: var(--text-muted);
              background: var(--bg-card);
              border: 1px solid var(--border-subtle);
              transition: all 0.15s ease;
            }
            .tab-pill:hover {
              color: var(--text-main);
              border-color: var(--primary);
            }
            .tab-pill.active {
              background: var(--primary);
              color: #ffffff;
              border-color: var(--primary);
            }

            .flux-layout-footer {
              background: var(--bg-footer);
              border-top: 1px solid var(--border-subtle);
              color: var(--text-muted);
              padding: 0.75rem 1.5rem;
              font-size: 0.8rem;
              display: flex;
              justify-content: space-between;
              align-items: center;
              flex-wrap: wrap;
              gap: 0.5rem;
            }

            .kpi-grid {
              display: grid;
              grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
              gap: 1.15rem;
            }
            .ui-card {
              background: var(--bg-card);
              border: 1px solid var(--border-subtle);
              border-radius: 12px;
              padding: 1.25rem;
            }
            .kpi-title { font-size: 0.78rem; text-transform: uppercase; color: var(--text-muted); font-weight: 700; }
            .kpi-value { font-size: 1.55rem; font-weight: 800; margin: 0.35rem 0; }
            .kpi-sub { font-size: 0.8rem; color: var(--text-muted); }

            .responsive-form {
              display: flex;
              flex-wrap: wrap;
              gap: 0.85rem;
              align-items: flex-end;
            }
            .app-input {
              width: 100%;
              padding: 0.55rem 0.75rem;
              border-radius: 6px;
              border: 1px solid var(--border-subtle);
              background: var(--bg-body);
              color: var(--text-main);
              font-size: 0.875rem;
              outline: none;
            }
            .app-btn-primary {
              background: var(--primary);
              color: #ffffff;
              border: none;
              padding: 0.55rem 1rem;
              border-radius: 6px;
              font-weight: 600;
              cursor: pointer;
              font-size: 0.875rem;
            }
            .app-btn-primary:hover { background: var(--primary-hover); }
            .app-btn-secondary {
              background: var(--bg-subtle);
              color: var(--text-main);
              border: 1px solid var(--border-subtle);
              padding: 0.5rem 0.95rem;
              border-radius: 6px;
              font-weight: 600;
              cursor: pointer;
              font-size: 0.85rem;
            }
            .app-btn-danger {
              background: var(--danger);
              color: #ffffff;
              border: none;
              padding: 0.5rem 0.9rem;
              border-radius: 6px;
              font-weight: 600;
              cursor: pointer;
              font-size: 0.85rem;
            }

            .app-table {
              width: 100%;
              border-collapse: collapse;
              text-align: left;
              font-size: 0.875rem;
            }
            .app-table th {
              background: var(--bg-subtle);
              color: var(--text-muted);
              font-size: 0.75rem;
              text-transform: uppercase;
              padding: 0.75rem 1rem;
              border-bottom: 1px solid var(--border-subtle);
            }
            .app-table td {
              padding: 0.8rem 1rem;
              border-bottom: 1px solid var(--border-subtle);
            }

            @media (min-width: 901px) {
              body.sidebar-collapsed .flux-layout-left {
                margin-left: calc(-1 * var(--sidebar-width));
                opacity: 0;
                pointer-events: none;
              }
            }
            @media (max-width: 900px) {
              .hide-mobile { display: none !important; }
              .flux-layout-left {
                position: fixed;
                top: 62px;
                left: 0;
                bottom: 0;
                z-index: 1001;
                transform: translateX(-100%);
                box-shadow: 4px 0 15px rgba(0,0,0,0.5);
              }
              body.mobile-sidebar-open .flux-layout-left {
                transform: translateX(0);
              }
              .sidebar-backdrop {
                display: none;
                position: fixed;
                inset: 62px 0 0 0;
                background: rgba(0,0,0,0.55);
                z-index: 1000;
              }
              body.mobile-sidebar-open .sidebar-backdrop {
                display: block;
              }
            }
          </style>
          <script>
            (function() {
              const savedTheme = localStorage.getItem('jettra-theme') || 'dark';
              document.documentElement.setAttribute('data-theme', savedTheme);
              if (document.body) {
                document.body.classList.add('flux-template-body', 'app-body');
              }
            })();

            function updateThemeButtonUI(theme) {
              const icon = document.getElementById('themeIcon');
              const label = document.getElementById('themeLabel');
              if (icon) icon.textContent = theme === 'white' ? '☀️' : '🌙';
              if (label) label.textContent = theme === 'white' ? 'Claro' : 'Oscuro';
            }

            function toggleTheme() {
              const current = document.documentElement.getAttribute('data-theme') || 'dark';
              const next = current === 'dark' ? 'white' : 'dark';
              document.documentElement.setAttribute('data-theme', next);
              localStorage.setItem('jettra-theme', next);
              document.cookie = "jettra_theme=" + next + "; path=/; max-age=31536000";
              updateThemeButtonUI(next);
            }

            function toggleSidebar() {
              if (window.innerWidth <= 900) {
                document.body.classList.toggle('mobile-sidebar-open');
              } else {
                const collapsed = document.body.classList.toggle('sidebar-collapsed');
                localStorage.setItem('jettra_sidebar_collapsed', collapsed ? 'true' : 'false');
              }
            }

            window.addEventListener('DOMContentLoaded', function() {
              if (document.body) {
                document.body.classList.add('flux-template-body', 'app-body');
              }
              updateThemeButtonUI(document.documentElement.getAttribute('data-theme') || 'dark');
              if (window.innerWidth > 900 && localStorage.getItem('jettra_sidebar_collapsed') === 'true') {
                document.body.classList.add('sidebar-collapsed');
              }
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
