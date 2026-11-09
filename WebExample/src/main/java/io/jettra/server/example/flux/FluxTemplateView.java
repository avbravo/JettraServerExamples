package io.jettra.server.example.flux;

import io.jettra.flux.core.FluxView;
import io.jettra.flux.core.Widget;
import io.jettra.flux.theme.Themes;
import io.jettra.flux.widgets.RawHtml;
import io.jettra.server.core.JettraServer;
import io.jettra.server.core.storage.StorageMetrics;
import io.jettra.server.example.model.User;
import io.jettra.server.example.util.Messages;
import java.io.PrintWriter;

/**
 * Plantilla (Template) exclusiva para el área de ejemplos de JettraFlux en WebExample.
 * Diseñada específicamente para envolver las vistas reactivas de JettraFlux
 * (/flux/catalogo, /flux/metrics, /flux/health, /flux/storage-monitor, /flux/telemetry, /flux/counter)
 * sin afectar la plantilla de Jakarta Faces (.xhtml) ni la plantilla de JettraAura (/aura/*).
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
        if (prop == null || prop.isBlank()) prop = System.getProperty("jettra.storage.mode");
        return (prop != null && !prop.isBlank()) ? prop.trim() : "JVM_RAM";
    }

    @Override
    public Widget build() {
        return RawHtml.of(renderFullDocument());
    }

    public String renderFullDocument() {
        String renderedChild = contentWidget != null ? contentWidget.render(Themes.Core()) : "";
        StringBuilder sb = new StringBuilder(16384);

        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"es\" data-theme=\"dark\">\n");
        sb.append("<head>\n");
        sb.append("  <meta charset=\"UTF-8\" />\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n");
        sb.append("  <title>").append(escapeHtml(title)).append(" - JettraFlux Template</title>\n");
        sb.append(getFluxTemplateStylesAndScripts());
        sb.append("</head>\n");
        sb.append("<body class=\"app-body flux-template-body\" style=\"margin: 0; display: flex; flex-direction: column; min-height: 100vh;\">\n");

        // Backdrop para sidebar móvil
        sb.append("  <div id=\"sidebarBackdrop\" class=\"sidebar-backdrop\" onclick=\"toggleSidebar()\"></div>\n");

        // 1. TOPBAR EXCLUSIVO DE JETTRAFLUX
        sb.append("  <header class=\"ui-layout-top flux-layout-top\">\n");
        sb.append("    <div style=\"display: flex; align-items: center; gap: 0.75rem;\">\n");
        sb.append("      <button class=\"hamburger-btn\" type=\"button\" onclick=\"toggleSidebar()\" aria-label=\"Alternar menú JettraFlux\" title=\"Colapsar / Expandir menú JettraFlux\">\n");
        sb.append("        <span class=\"hamburger-bar\"></span>\n");
        sb.append("        <span class=\"hamburger-bar\"></span>\n");
        sb.append("        <span class=\"hamburger-bar\"></span>\n");
        sb.append("      </button>\n");
        sb.append("      <div class=\"flux-brand-badge\">JF</div>\n");
        sb.append("      <div style=\"display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap;\">\n");
        sb.append("        <span style=\"font-weight: 800; font-size: 1.12rem; letter-spacing: -0.02em; color: #ffffff;\">JettraFlux Studio</span>\n");
        sb.append("        <span class=\"hide-mobile\" style=\"font-size: 0.72rem; font-weight: 700; background: rgba(168, 85, 247, 0.28); border: 1px solid rgba(192, 132, 252, 0.45); color: #f3e8ff; padding: 0.15rem 0.55rem; border-radius: 9999px;\">Pure Java 25 Reactive UI</span>\n");
        sb.append("      </div>\n");
        sb.append("    </div>\n");

        sb.append("    <div class=\"top-right-group\">\n");
        sb.append("      <button onclick=\"toggleTheme()\" class=\"theme-toggle-btn\" type=\"button\" title=\"Alternar Tema Claro / Oscuro\" id=\"btnThemeToggle\">\n");
        sb.append("        <span id=\"themeIcon\">🌙</span> <span id=\"themeLabel\" class=\"hide-mobile\">Oscuro</span>\n");
        sb.append("      </button>\n");
        sb.append("      <span class=\"hide-mobile\" style=\"background: rgba(15, 23, 42, 0.45); border: 1px solid rgba(148, 163, 184, 0.25); color: #e2e8f0; font-size: 0.8rem; padding: 0.3rem 0.65rem; border-radius: 6px;\">Modo: <strong style=\"color:#38bdf8;\">").append(escapeHtml(storageMode)).append("</strong></span>\n");
        sb.append("      <span class=\"hide-mobile\" style=\"font-size: 0.85rem; color: #f1f5f9;\">👤 ").append(escapeHtml(user.getFullName())).append("</span>\n");
        sb.append("      <a href=\"/logout\" class=\"app-btn-danger\" style=\"text-decoration: none; font-size: 0.8rem; padding: 0.35rem 0.75rem;\">Cerrar Sesión</a>\n");
        sb.append("    </div>\n");
        sb.append("  </header>\n");

        // 2. WRAPPER PRINCIPAL (SIDEBAR EXCLUSIVO JETTRAFLUX + ÁREA CENTRAL)
        sb.append("  <div class=\"ui-layout-wrapper flux-layout-wrapper\">\n");

        // LEFT SIDEBAR EXCLUSIVO DE JETTRAFLUX
        sb.append("    <aside id=\"appSidebar\" class=\"ui-layout-left flux-layout-left\">\n");
        sb.append("      <div class=\"flux-sidebar-banner\">\n");
        sb.append("        <div style=\"font-weight: 800; font-size: 0.88rem; color: #c084fc; display: flex; align-items: center; gap: 0.4rem;\">⚡ Motor JettraFlux</div>\n");
        sb.append("        <div style=\"font-size: 0.75rem; color: var(--text-muted); margin-top: 0.2rem;\">Plantilla exclusiva para vistas reactivas en Java 25 puro</div>\n");
        sb.append("      </div>\n");

        sb.append("      <div class=\"flux-menu-heading\">⚡ Suite Ejemplos JettraFlux</div>\n");
        sb.append("      <nav style=\"display: flex; flex-direction: column; gap: 0.28rem; margin-bottom: 1.6rem;\">\n");
        sb.append("        <a href=\"/flux/catalogo\" class=\"nav-link ").append(isActive("catalogo-flux", "catalogo") ? "active" : "").append("\">🛍️ Catálogo Reactivo (Flux)</a>\n");
        sb.append("        <a href=\"/flux/metrics\" class=\"nav-link ").append(isActive("metrics") ? "active" : "").append("\">📊 Métricas del Servidor</a>\n");
        sb.append("        <a href=\"/flux/health\" class=\"nav-link ").append(isActive("health") ? "active" : "").append("\">🩺 Estado de Salud (Health)</a>\n");
        sb.append("        <a href=\"/flux/storage-monitor\" class=\"nav-link ").append(isActive("storage-monitor") ? "active" : "").append("\">🚀 Monitor Almacenamiento</a>\n");
        sb.append("        <a href=\"/flux/telemetry\" class=\"nav-link ").append(isActive("telemetry") ? "active" : "").append("\">📈 Telemetría en Tiempo Real</a>\n");
        sb.append("        <a href=\"/flux/counter\" class=\"nav-link ").append(isActive("counter") ? "active" : "").append("\">🎛️ Contador Reactivo</a>\n");
        sb.append("      </nav>\n");

        sb.append("      <div class=\"flux-menu-heading\">🖥️ Área Jakarta Faces (.xhtml)</div>\n");
        sb.append("      <nav style=\"display: flex; flex-direction: column; gap: 0.25rem; margin-bottom: 1.6rem;\">\n");
        sb.append("        <a href=\"/dashboard.xhtml\" class=\"nav-link\">📊 Dashboard Jakarta Faces</a>\n");
        sb.append("        <a href=\"/productos.xhtml\" class=\"nav-link\">📦 Catálogo JPA (.xhtml)</a>\n");
        sb.append("        <a href=\"/usuarios.xhtml\" class=\"nav-link\">👥 Usuarios Security</a>\n");
        sb.append("      </nav>\n");

        sb.append("      <div class=\"flux-menu-heading\">✨ Área Suite JettraAura</div>\n");
        sb.append("      <nav style=\"display: flex; flex-direction: column; gap: 0.25rem; margin-bottom: 1.6rem;\">\n");
        sb.append("        <a href=\"/aura/demo\" class=\"nav-link\">✨ Portal JettraAura</a>\n");
        sb.append("        <a href=\"/aura/catalogo\" class=\"nav-link\">📦 Catálogo Dinámico (Aura)</a>\n");
        sb.append("        <a href=\"/aura/formulario\" class=\"nav-link\">📝 Formulario Reactivo (CPM)</a>\n");
        sb.append("        <a href=\"/aura/contador\" class=\"nav-link\">⚡ Contador AJAX Parcial</a>\n");
        sb.append("      </nav>\n");

        sb.append("      <div class=\"flux-menu-heading\">🔌 OpenAPI &amp; Endpoints REST</div>\n");
        sb.append("      <nav style=\"display: flex; flex-direction: column; gap: 0.25rem;\">\n");
        sb.append("        <a href=\"/swagger-ui\" target=\"_blank\" class=\"nav-link\">📖 Swagger-UI (OpenAPI)</a>\n");
        sb.append("        <a href=\"/api/productos\" target=\"_blank\" class=\"nav-link\">/api/productos (JSON)</a>\n");
        sb.append("        <a href=\"/api/info\" target=\"_blank\" class=\"nav-link\">/api/info (JSON)</a>\n");
        sb.append("        <a href=\"/metrics\" target=\"_blank\" class=\"nav-link\">/metrics (Métricas JSON)</a>\n");
        sb.append("        <a href=\"/health\" target=\"_blank\" class=\"nav-link\">/health (Health JSON)</a>\n");
        sb.append("      </nav>\n");
        sb.append("    </aside>\n");

        // 3. ÁREA CENTRAL DE CONTENIDO JETTRAFLUX
        sb.append("    <main class=\"ui-layout-center flux-layout-center\">\n");
        sb.append("      <div class=\"flux-center-toolbar\">\n");
        sb.append("        <div>\n");
        sb.append("          <div style=\"display: flex; align-items: center; gap: 0.55rem; flex-wrap: wrap;\">\n");
        sb.append("            <span class=\"flux-pill-badge\">⚡ JETTRAFLUX TEMPLATE</span>\n");
        sb.append("            <h1 style=\"font-size: 1.45rem; font-weight: 800; color: var(--text-main); margin: 0;\">").append(escapeHtml(title)).append("</h1>\n");
        sb.append("          </div>\n");
        sb.append("          <p style=\"color: var(--text-muted); margin: 0.25rem 0 0 0; font-size: 0.85rem;\">Renderizado mediante la plantilla exclusiva de ejemplos JettraFlux en Java 25 puro</p>\n");
        sb.append("        </div>\n");
        sb.append("        <div class=\"flux-tabs\">\n");
        sb.append("          <a href=\"/flux/catalogo\" class=\"tab-pill ").append(isActive("catalogo-flux", "catalogo") ? "active" : "").append("\">🛍️ ").append(escapeHtml(Messages.get("flux.catalog.title"))).append("</a>\n");
        sb.append("          <a href=\"/flux/metrics\" class=\"tab-pill ").append(isActive("metrics") ? "active" : "").append("\">📊 Métricas</a>\n");
        sb.append("          <a href=\"/flux/health\" class=\"tab-pill ").append(isActive("health") ? "active" : "").append("\">🩺 Health</a>\n");
        sb.append("          <a href=\"/flux/storage-monitor\" class=\"tab-pill ").append(isActive("storage-monitor") ? "active" : "").append("\">🚀 Storage</a>\n");
        sb.append("          <a href=\"/flux/telemetry\" class=\"tab-pill ").append(isActive("telemetry") ? "active" : "").append("\">📈 Telemetría</a>\n");
        sb.append("          <a href=\"/flux/counter\" class=\"tab-pill ").append(isActive("counter") ? "active" : "").append("\">🎛️ Contador</a>\n");
        sb.append("        </div>\n");
        sb.append("      </div>\n");

        sb.append("      <div id=\"jettra-flux-root\" class=\"ui-card flux-content-surface\">\n");
        sb.append(renderedChild).append("\n");
        if (extraCenterHtml != null && !extraCenterHtml.isBlank()) {
            sb.append(extraCenterHtml).append("\n");
        }
        sb.append("      </div>\n");
        sb.append("    </main>\n");

        sb.append("  </div>\n");

        // 4. FOOTER EXCLUSIVO DE JETTRAFLUX
        sb.append("  <footer class=\"ui-layout-footer flux-layout-footer\">\n");
        sb.append("    <div>JettraFlux Reactive UI Template • Java 25 &amp; Project Loom Virtual Threads • Cero HTML Manual</div>\n");
        sb.append("    <div>Estado: <span style=\"color: var(--success); font-weight: bold;\">● OPERATIVO</span> • Modo Persistencia: <strong>[").append(escapeHtml(storageMode)).append("]</strong></div>\n");
        sb.append("  </footer>\n");

        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString();
    }

    public void renderTo(PrintWriter out) {
        if (out != null) {
            out.print(renderFullDocument());
        }
    }

    private boolean isActive(String... keys) {
        for (String k : keys) {
            if (k != null && k.equalsIgnoreCase(activeSection)) {
                return true;
            }
        }
        return false;
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

            body.flux-template-body {
              font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              background-color: var(--bg-body);
              color: var(--text-main);
              transition: background-color 0.2s ease, color 0.2s ease;
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
              display: inline-flex;
              flex-direction: column;
              justify-content: space-around;
              width: 36px;
              height: 36px;
              background: rgba(255, 255, 255, 0.12);
              border: 1px solid rgba(255, 255, 255, 0.25);
              border-radius: 8px;
              cursor: pointer;
              padding: 8px;
              transition: background 0.2s;
            }
            .hamburger-btn:hover { background: rgba(255, 255, 255, 0.22); }
            .hamburger-bar {
              width: 100%;
              height: 2px;
              background-color: #ffffff;
              border-radius: 2px;
            }

            .theme-toggle-btn {
              background: rgba(255, 255, 255, 0.14);
              color: #ffffff;
              border: 1px solid rgba(255, 255, 255, 0.28);
              padding: 0.35rem 0.75rem;
              border-radius: 9999px;
              cursor: pointer;
              font-size: 0.8rem;
              font-weight: 600;
              display: inline-flex;
              align-items: center;
              gap: 0.35rem;
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

