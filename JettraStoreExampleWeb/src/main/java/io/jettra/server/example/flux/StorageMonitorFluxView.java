package io.jettra.server.example.flux;

import io.jettra.flux.core.FluxView;
import io.jettra.flux.core.Modifier;
import io.jettra.flux.core.Widget;
import io.jettra.flux.theme.Themes;
import io.jettra.flux.widgets.Badge;
import io.jettra.flux.widgets.Card;
import io.jettra.flux.widgets.Column;
import io.jettra.flux.widgets.Div;
import io.jettra.flux.widgets.Header;
import io.jettra.flux.widgets.Link;
import io.jettra.flux.widgets.Paragraph;
import io.jettra.flux.widgets.RawHtml;
import io.jettra.flux.widgets.Row;
import io.jettra.flux.widgets.Text;
import io.jettra.server.core.JettraServer;
import io.jettra.server.core.storage.StorageMetrics;

/**
 * Vista reactiva construida en Java puro con JettraFlux.
 * Muestra el estado del subsistema de persistencia de triple modo (JVM_RAM, DISK_MEMORY, OFF_HEAP).
 */
public class StorageMonitorFluxView extends FluxView {

    private final String mode;
    private final String description;
    private final JettraServer server;

    public StorageMonitorFluxView() {
        this("JVM_RAM", "Memoria RAM Heap estándar", null);
    }

    public StorageMonitorFluxView(String mode, String description) {
        this(mode, description, null);
    }

    public StorageMonitorFluxView(String mode, String description, JettraServer server) {
        this.mode = mode != null ? mode : "JVM_RAM";
        this.description = description != null ? description : "Memoria RAM Heap estándar";
        this.server = server;
    }

    @Override
    public Widget build() {
        StorageMetrics metrics = (server != null && server.storage() != null) ? server.storage().getMetrics() : null;
        long records = metrics != null ? metrics.totalRecords() : 5;
        String memUsed = metrics != null ? metrics.humanReadableMemory() : "448 B";

        Widget headerLeft = Column.of(
                Header.of(2, "⚡ Monitor de Almacenamiento Reactivo - JettraFlux")
                        .modifier(new Modifier().style("margin: 0; font-size: 1.25rem; color: var(--text-main);")),
                Paragraph.of("Modo persistencia actual: " + mode + " — " + description)
                        .modifier(new Modifier().style("margin: 0.25rem 0 0 0; font-size: 0.85rem; color: var(--text-muted);"))
        );

        Widget syncBadge = Badge.success("Motor Sincronizado")
                .modifier(new Modifier().style("font-weight: 700; padding: 0.35rem 0.75rem; border-radius: 6px; font-size: 0.8rem;"));

        Widget headerRow = Row.of(headerLeft, syncBadge)
                .modifier(new Modifier().style("display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border-subtle); padding-bottom: 1rem; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 0.5rem; width: 100%;"));

        Widget cardMode = Card.of(Column.of(
                Text.of("Modo Persistencia").modifier(new Modifier().cssClass("kpi-title")),
                Text.of(mode).modifier(new Modifier().cssClass("kpi-value").style("color: var(--primary);")),
                Text.of("Triple modo conmutable").modifier(new Modifier().cssClass("kpi-sub"))
        )).modifier(new Modifier().cssClass("ui-card").style("background: var(--bg-subtle);"));

        Widget cardRecords = Card.of(Column.of(
                Text.of("Registros en Almacén").modifier(new Modifier().cssClass("kpi-title")),
                Text.of(records + " Entidades").modifier(new Modifier().cssClass("kpi-value").style("color: var(--text-main);")),
                Text.of("Sincronización Off-Heap").modifier(new Modifier().cssClass("kpi-sub"))
        )).modifier(new Modifier().cssClass("ui-card").style("background: var(--bg-subtle);"));

        Widget cardMem = Card.of(Column.of(
                Text.of("Consumo de Memoria").modifier(new Modifier().cssClass("kpi-title")),
                Text.of(memUsed).modifier(new Modifier().cssClass("kpi-value").style("color: var(--success);")),
                Text.of("Zero-GC Paging Activo").modifier(new Modifier().cssClass("kpi-sub"))
        )).modifier(new Modifier().cssClass("ui-card").style("background: var(--bg-subtle);"));

        Widget kpiGrid = Div.of(cardMode, cardRecords, cardMem)
                .modifier(new Modifier().cssClass("kpi-grid").style("margin-bottom: 1.5rem; width: 100%;"));

        Widget detailsBox = RawHtml.of("""
            <div style="background: var(--bg-subtle); border: 1px solid var(--border-subtle); border-radius: 8px; padding: 1rem; margin-bottom: 1.5rem; font-size: 0.875rem;">
              <div style="font-weight: 700; color: var(--text-main); margin-bottom: 0.5rem;">Detalles de Arquitectura del Motor Seleccionado (Panama FFM &amp; Zero-GC Paging):</div>
              <ul style="margin: 0; padding-left: 1.25rem; color: var(--text-muted);">
                <li><strong>JVM_RAM:</strong> Almacenamiento estándar en memoria Heap/Stack mediante estructuras compactas <code>UnifiedMap</code>.</li>
                <li><strong>DISK_MEMORY:</strong> Motor LSM directo Off-Heap estructurado en disco y MemTable sincronizado con latencia predecible.</li>
                <li><strong>OFF_HEAP:</strong> Motor nativo Slotted-Page mediante Panama Foreign Function &amp; Memory (FFM) con zero-copy.</li>
              </ul>
            </div>
            """);

        Widget btnForceSync = RawHtml.of("<button type=\"button\" class=\"app-btn-primary\" onclick=\"alert('MemTable sincronizada exitosamente con el disco')\">Forzar Sincronización MemTable</button>");
        Widget linkRawMetrics = Link.of("/metrics", "Ver Métricas JSON en Bruto")
                .target("_blank")
                .modifier(new Modifier().cssClass("app-btn-secondary").style("text-decoration:none;"));
        Widget linkCatalog = Link.of("/flux/catalogo", "Ir al Catálogo Reactivo (Flux)")
                .modifier(new Modifier().cssClass("app-btn-secondary").style("text-decoration:none;"));

        Widget actionsRow = Row.of(btnForceSync, linkRawMetrics, linkCatalog)
                .modifier(new Modifier().style("display: flex; gap: 0.75rem; flex-wrap: wrap;"));

        return Column.of(headerRow, kpiGrid, detailsBox, actionsRow)
                .modifier(new Modifier().style("width: 100%; display: flex; flex-direction: column; gap: 0.5rem;"));
    }

    public String renderSectionHtml() {
        return build().render(Themes.Core());
    }
}
