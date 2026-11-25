package io.jettra.server.example.aura;

import io.jettra.aura.components.AuraLabel;
import io.jettra.aura.core.AuraWebPage;
import io.jettra.aura.model.IModel;
import io.jettra.server.core.JettraServer;
import java.util.Map;

/**
 * Plantilla Maestra Base para todas las páginas JettraAura dentro de WebExample.
 * Provee:
 * - Topbar con Botón Hamburguesa, Selector de Tema (🌙/☀️ Oscuro/Claro), Badge de Modo de Almacenamiento y Botón de Salir.
 * - Menú Lateral (Sidebar) Responsivo con soporte para resoluciones móviles (off-canvas) y desktop (collapse).
 * - Marcador &lt;aura:child/&gt; en el área central donde cada página inyecta su contenido específico.
 * - Pie de página con información del motor y concurrencia Loom.
 */
public abstract class AuraTemplatePage extends AuraWebPage {

    public AuraTemplatePage() {
        super();
        initTemplateLayout();
    }

    public AuraTemplatePage(IModel<?> model) {
        super(model);
        initTemplateLayout();
    }

    public AuraTemplatePage(Map<String, String> parameters) {
        super(parameters);
        initTemplateLayout();
    }

    private void initTemplateLayout() {
        // 1. Badge del modo de almacenamiento en el Top Bar
        add(new AuraLabel("topStorageMode", () -> getEffectiveStorageMode()));

        // 2. Información de persistencia y concurrencia en el Footer
        add(new AuraLabel("footerStorageMode", () -> "Modo Persistencia: [" + getEffectiveStorageMode() + "] • Virtual Threads (Loom)"));
    }

    protected String getEffectiveStorageMode() {
        JettraServer server = JettraServer.getInstance();
        if (server != null && server.storage() != null) {
            return server.storage().getMode().name();
        }
        return "JVM_RAM";
    }
}

