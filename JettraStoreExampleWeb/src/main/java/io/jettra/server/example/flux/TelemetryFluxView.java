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
import java.lang.management.ManagementFactory;

/**
 * Vista reactiva de telemetría y rendimiento del servidor escrita con JettraFlux en Java puro.
 */
public class TelemetryFluxView extends FluxView {

    @Override
    public Widget build() {
        Runtime rt = Runtime.getRuntime();
        long freeMb = rt.freeMemory() / (1024 * 1024);
        long totalMb = rt.totalMemory() / (1024 * 1024);
        long maxMb = rt.maxMemory() / (1024 * 1024);
        int cores = rt.availableProcessors();
        long uptimeSec = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        Widget headerLeft = Column.of(
                Header.of(2, "📈 Telemetría en Tiempo Real de JettraServer")
                        .modifier(new Modifier().style("margin: 0; font-size: 1.25rem; color: var(--text-main);")),
                Paragraph.of("Project Loom Virtual Threads & Asynchronous NIO Channels")
                        .modifier(new Modifier().style("margin: 0.25rem 0 0 0; font-size: 0.85rem; color: var(--text-muted);"))
        );

        Widget badgeColdStart = Badge.of("Cold Start < 2ms", "primary")
                .modifier(new Modifier().style("font-weight: 700; padding: 0.35rem 0.75rem; border-radius: 6px; font-size: 0.8rem;"));

        Widget headerRow = Row.of(headerLeft, badgeColdStart)
                .modifier(new Modifier().style("display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border-subtle); padding-bottom: 1rem; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 0.5rem; width: 100%;"));

        Widget cardMem = Card.of(Column.of(
                Text.of("Memoria JVM Usada").modifier(new Modifier().cssClass("kpi-title")),
                Text.of((totalMb - freeMb) + " MB").modifier(new Modifier().cssClass("kpi-value").style("color: var(--primary);")),
                Text.of("De " + totalMb + " MB asignados (Máx: " + maxMb + " MB)").modifier(new Modifier().cssClass("kpi-sub"))
        )).modifier(new Modifier().cssClass("ui-card").style("background: var(--bg-subtle);"));

        Widget cardLoom = Card.of(Column.of(
                Text.of("Hilos Virtuales Loom").modifier(new Modifier().cssClass("kpi-title")),
                Text.of("Activos (M:N)").modifier(new Modifier().cssClass("kpi-value").style("color: var(--success);")),
                Text.of(cores + " núcleos de hardware mapeados").modifier(new Modifier().cssClass("kpi-sub"))
        )).modifier(new Modifier().cssClass("ui-card").style("background: var(--bg-subtle);"));

        Widget cardUptime = Card.of(Column.of(
                Text.of("Tiempo Activo").modifier(new Modifier().cssClass("kpi-title")),
                Text.of(uptimeSec + "s").modifier(new Modifier().cssClass("kpi-value").style("color: var(--warning);")),
                Text.of("Generational ZGC activo").modifier(new Modifier().cssClass("kpi-sub"))
        )).modifier(new Modifier().cssClass("ui-card").style("background: var(--bg-subtle);"));

        Widget kpiGrid = Div.of(cardMem, cardLoom, cardUptime)
                .modifier(new Modifier().cssClass("kpi-grid").style("margin-bottom: 1.5rem; width: 100%;"));

        Widget btnRefresh = RawHtml.of("<button type=\"button\" class=\"app-btn-primary\" onclick=\"window.location.reload()\">Actualizar Telemetría</button>");
        Widget linkHealth = Link.of("/health", "Ver Health Check JSON")
                .target("_blank")
                .modifier(new Modifier().cssClass("app-btn-secondary").style("text-decoration:none;"));
        Widget linkMetricsFlux = Link.of("/flux/metrics", "Ver Métricas Detalladas (Flux)")
                .modifier(new Modifier().cssClass("app-btn-secondary").style("text-decoration:none;"));

        Widget actionsRow = Row.of(btnRefresh, linkHealth, linkMetricsFlux)
                .modifier(new Modifier().style("display: flex; gap: 0.75rem; flex-wrap: wrap;"));

        return Column.of(headerRow, kpiGrid, actionsRow)
                .modifier(new Modifier().style("width: 100%; display: flex; flex-direction: column; gap: 0.5rem;"));
    }

    public String renderSectionHtml() {
        return build().render(Themes.Core());
    }
}
