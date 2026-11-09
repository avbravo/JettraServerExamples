package io.jettra.server.example.flux;

import io.jettra.flux.core.FluxView;
import io.jettra.flux.core.Modifier;
import io.jettra.flux.core.Widget;
import io.jettra.flux.theme.Themes;
import io.jettra.flux.widgets.Badge;
import io.jettra.flux.widgets.Card;
import io.jettra.flux.widgets.Column;
import io.jettra.flux.widgets.Datatable;
import io.jettra.flux.widgets.Div;
import io.jettra.flux.widgets.Header;
import io.jettra.flux.widgets.Link;
import io.jettra.flux.widgets.MetricCard;
import io.jettra.flux.widgets.Paragraph;
import io.jettra.flux.widgets.RawHtml;
import io.jettra.flux.widgets.Row;
import io.jettra.flux.widgets.Text;
import io.jettra.server.core.JettraServer;
import io.jettra.server.core.storage.StorageMetrics;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Vista de Métricas del Servidor construida íntegramente con componentes JettraFlux en Java puro.
 * Provee visualización de consumo de memoria JVM, concurrencia Project Loom y
 * rendimiento del motor de almacenamiento tri-modal (StorageEngine).
 */
public class MetricsFluxView extends FluxView {

    private final JettraServer server;

    public MetricsFluxView() {
        this(null);
    }

    public MetricsFluxView(JettraServer server) {
        this.server = server;
    }

    @Override
    public Widget build() {
        Runtime rt = Runtime.getRuntime();
        long freeMb = rt.freeMemory() / (1024 * 1024);
        long totalMb = rt.totalMemory() / (1024 * 1024);
        long maxMb = rt.maxMemory() / (1024 * 1024);
        long usedMb = totalMb - freeMb;
        int cores = rt.availableProcessors();
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long uptimeSec = uptimeMs / 1000;
        long hours = uptimeSec / 3600;
        long minutes = (uptimeSec % 3600) / 60;
        long seconds = uptimeSec % 60;
        String uptimeStr = String.format("%02dh %02dm %02ds", hours, minutes, seconds);

        StorageMetrics sm = (server != null && server.storage() != null)
                ? server.storage().getMetrics()
                : null;

        String storageMode = sm != null ? sm.mode().name() : System.getProperty("jettra.storage.mode", "JVM_RAM");
        long totalRecords = sm != null ? sm.totalRecords() : 5;
        String memoryReadable = sm != null ? sm.humanReadableMemory() : "512 B";
        double hitRate = sm != null ? sm.hitRatePercentage() : 100.0;
        long reads = sm != null ? sm.readOperations() : 12;
        long writes = sm != null ? sm.writeOperations() : 5;

        // 1. Cabecera principal
        Widget titleBadge = Badge.of("JettraFlux Metrics").severity("primary")
                .modifier(new Modifier().style("margin-right: 0.5rem; font-weight: 700;"));
        Widget titleHeader = Header.of(2, "📊 Métricas y Telemetría del Servidor")
                .modifier(new Modifier().style("margin: 0; font-size: 1.35rem; font-weight: 700; color: var(--text-main);"));
        Widget subtitle = Paragraph.of("Inspección de memoria JVM, hilos virtuales Project Loom y subsistema de persistencia en tiempo real.")
                .modifier(new Modifier().style("margin: 0.35rem 0 0 0; font-size: 0.85rem; color: var(--text-muted);"));
        Widget techBadge = Badge.active("Java 25 Virtual Threads")
                .modifier(new Modifier().style("font-weight: 700; padding: 0.35rem 0.75rem; border-radius: 6px; font-size: 0.8rem;"));

        Widget headerLeft = Column.of(
                Row.of(titleBadge, titleHeader).modifier(new Modifier().style("display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap;")),
                subtitle
        );

        Widget headerSection = Row.of(headerLeft, techBadge)
                .modifier(new Modifier().style("display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border-subtle); padding-bottom: 1rem; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 0.5rem; width: 100%;"));

        // 2. Tarjetas KPI de Métricas Clave
        Widget cardMemoria = MetricCard.builder()
                .title("Memoria JVM Usada")
                .value(usedMb + " MB")
                .subtext("De " + totalMb + " MB comprometidos (Máx: " + maxMb + " MB)")
                .icon("🧠")
                .color("#2563eb")
                .badge(Badge.info("Heap GC"))
                .build()
                .modifier(new Modifier().cssClass("ui-card kpi-card"));

        Widget cardLoom = MetricCard.builder()
                .title("Hilos Virtuales Loom")
                .value("Activos (M:N)")
                .subtext(cores + " Núcleos de Hardware Asignados")
                .icon("⚡")
                .color("#8b5cf6")
                .badge(Badge.active("Loom 25"))
                .build()
                .modifier(new Modifier().cssClass("ui-card kpi-card"));

        Widget cardStorage = MetricCard.builder()
                .title("Modo Persistencia")
                .value(storageMode)
                .subtext(totalRecords + " Registros | " + memoryReadable)
                .icon("🚀")
                .color("#059669")
                .badge(Badge.success("Sincronizado"))
                .build()
                .modifier(new Modifier().cssClass("ui-card kpi-card"));

        Widget cardUptime = MetricCard.builder()
                .title("Tiempo de Actividad")
                .value(uptimeStr)
                .subtext("Cold-Start < 2ms | Salud: UP")
                .icon("⏱️")
                .color("#d97706")
                .badge(Badge.of("Saludable", "success"))
                .build()
                .modifier(new Modifier().cssClass("ui-card kpi-card"));

        Widget kpiGrid = Row.of(cardMemoria, cardLoom, cardStorage, cardUptime)
                .modifier(new Modifier().cssClass("kpi-grid").style("display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1.25rem; margin-bottom: 2rem; width: 100%;"));

        // 3. Tabla detallada de Métricas del Servidor
        List<Widget> tableHeaders = List.of(
                Text.of("Categoría").modifier(new Modifier().style("font-weight: 700;")),
                Text.of("Métrica del Sistema").modifier(new Modifier().style("font-weight: 700;")),
                Text.of("Valor Registrado").modifier(new Modifier().style("font-weight: 700;")),
                Text.of("Estado / Diagnóstico").modifier(new Modifier().style("font-weight: 700;")),
                Text.of("Descripción Técnica").modifier(new Modifier().style("font-weight: 700;"))
        );

        List<List<Widget>> tableRows = new ArrayList<>();

        // Fila 1: Loom
        tableRows.add(List.of(
                Badge.info("Concurrencia"),
                Text.of("Project Loom Virtual Threads"),
                Text.of("Ilimitados (M:N)").modifier(new Modifier().style("font-weight: 700; color: var(--primary);")),
                Badge.success("Óptimo"),
                Text.of("Planificación cooperativa de hilos virtuales sobre el pool ForkJoin del carrier.")
        ));

        // Fila 2: Cores
        tableRows.add(List.of(
                Badge.info("Hardware"),
                Text.of("Procesadores Disponibles"),
                Text.of(cores + " Cores de CPU").modifier(new Modifier().style("font-weight: 700;")),
                Badge.info("Detectado"),
                Text.of("Núcleos de procesamiento físico/lógico detectados por el runtime de Java.")
        ));

        // Fila 3: Memoria Usada
        tableRows.add(List.of(
                Badge.warning("Memoria JVM"),
                Text.of("Memoria Heap Utilizada"),
                Text.of(usedMb + " MB").modifier(new Modifier().style("font-weight: 700; color: var(--warning);")),
                Badge.success("Normal"),
                Text.of("Memoria actualmente ocupada por objetos activos en la memoria principal.")
        ));

        // Fila 4: Memoria Total
        tableRows.add(List.of(
                Badge.warning("Memoria JVM"),
                Text.of("Memoria Heap Comprometida"),
                Text.of(totalMb + " MB").modifier(new Modifier().style("font-weight: 700;")),
                Badge.info("Asignada"),
                Text.of("Cantidad total de memoria reservada por el sistema operativo para la JVM.")
        ));

        // Fila 5: Memoria Máxima
        tableRows.add(List.of(
                Badge.warning("Memoria JVM"),
                Text.of("Memoria Heap Máxima (-Xmx)"),
                Text.of(maxMb + " MB").modifier(new Modifier().style("font-weight: 700;")),
                Badge.info("Límite"),
                Text.of("Límite de memoria que la JVM intentará utilizar.")
        ));

        // Fila 6: Modo Storage
        tableRows.add(List.of(
                Badge.success("Persistencia"),
                Text.of("Modo StorageEngine Activo"),
                Text.of(storageMode).modifier(new Modifier().style("font-weight: 700; color: var(--success);")),
                Badge.success("Activo"),
                Text.of("Arquitectura de almacenamiento conmutable: JVM_RAM, DISK_MEMORY u OFF_HEAP Slotted-Page.")
        ));

        // Fila 7: Registros Almacenados
        tableRows.add(List.of(
                Badge.success("Persistencia"),
                Text.of("Registros Totales en Almacén"),
                Text.of(totalRecords + " Entidades").modifier(new Modifier().style("font-weight: 700;")),
                Badge.info("Indexado"),
                Text.of("Número de documentos u objetos indexados en la estructura primaria.")
        ));

        // Fila 8: Memoria del Almacén
        tableRows.add(List.of(
                Badge.success("Persistencia"),
                Text.of("Espacio Usado en Almacén"),
                Text.of(memoryReadable).modifier(new Modifier().style("font-weight: 700;")),
                Badge.success("Zero-Copy"),
                Text.of("Espacio en bytes consumido por las estructuras de almacenamiento Off-Heap/Heap.")
        ));

        // Fila 9: Tasa de Aciertos
        tableRows.add(List.of(
                Badge.success("Persistencia"),
                Text.of("Tasa de Aciertos (Hit Rate)"),
                Text.of(String.format("%.1f%%", hitRate)).modifier(new Modifier().style("font-weight: 700; color: var(--primary);")),
                Badge.success("Excelente"),
                Text.of("Porcentaje de consultas resueltas exitosamente directamente desde la MemTable.")
        ));

        // Fila 10: Operaciones E/S
        tableRows.add(List.of(
                Badge.success("Persistencia"),
                Text.of("Operaciones Lectura / Escritura"),
                Text.of("Lecturas: " + reads + " | Escrituras: " + writes).modifier(new Modifier().style("font-weight: 700;")),
                Badge.info("Balanceado"),
                Text.of("Contador acumulado de transacciones I/O procesadas desde el arranque.")
        ));

        Widget dataTable = Datatable.ofWidgets(tableHeaders, tableRows)
                .modifier(new Modifier().cssClass("app-table").style("width: 100%; border-collapse: collapse;"));

        Widget tableContainer = Div.of(dataTable)
                .modifier(new Modifier().cssClass("table-container ui-card").style("border: 1px solid var(--border-subtle); border-radius: 10px; overflow: hidden; margin-bottom: 1.5rem; width: 100%;"));

        // 4. Botones de Acción y Navegación
        Widget btnRefresh = RawHtml.of("<button type=\"button\" class=\"app-btn-primary\" onclick=\"window.location.reload()\">🔄 Actualizar Métricas</button>");
        Widget linkRawJson = Link.of("/metrics", "💾 Ver Métricas JSON en Bruto (/metrics)")
                .target("_blank")
                .modifier(new Modifier().cssClass("app-btn-secondary").style("text-decoration:none;"));
        Widget linkHealthFlux = Link.of("/dashboard.xhtml?view=health", "🩺 Ir a Estado de Salud (Flux)")
                .modifier(new Modifier().cssClass("app-btn-secondary").style("text-decoration:none;"));
        Widget linkDashboard = Link.of("/dashboard.xhtml", "⬅ Volver al Dashboard General")
                .modifier(new Modifier().cssClass("app-btn-secondary").style("text-decoration:none;"));

        Widget actionRow = Row.of(btnRefresh, linkRawJson, linkHealthFlux, linkDashboard)
                .modifier(new Modifier().style("display: flex; gap: 0.75rem; flex-wrap: wrap; margin-top: 1rem; width: 100%;"));

        return Column.of(
                headerSection,
                kpiGrid,
                tableContainer,
                actionRow
        ).modifier(new Modifier().style("width: 100%; display: flex; flex-direction: column; gap: 0.5rem;"));
    }

    public String renderSectionHtml() {
        return build().render(Themes.Core());
    }

    public void renderSectionHtml(PrintWriter out) {
        if (out != null) {
            out.println(renderSectionHtml());
        }
    }
}

