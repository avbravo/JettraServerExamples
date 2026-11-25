package io.jettra.server.example.flux;

import io.jettra.flux.core.FluxView;
import io.jettra.flux.core.Modifier;
import io.jettra.flux.core.Widget;
import io.jettra.flux.widgets.Badge;
import io.jettra.flux.widgets.Button;
import io.jettra.flux.widgets.Card;
import io.jettra.flux.widgets.Column;
import io.jettra.flux.widgets.Div;
import io.jettra.flux.widgets.Form;
import io.jettra.flux.widgets.Header;
import io.jettra.flux.widgets.Link;
import io.jettra.flux.widgets.Paragraph;
import io.jettra.flux.widgets.RawHtml;
import io.jettra.flux.widgets.Row;
import io.jettra.flux.widgets.Text;
import io.jettra.flux.widgets.TextInput;
import io.jettra.server.example.model.Customer;
import io.jettra.server.example.service.CustomerClientService;
import java.util.ArrayList;
import java.util.List;

/**
 * Vista reactiva JettraFlux construida íntegramente en Java 25 puro sin StringBuilder.
 * Provee la interfaz web CRUD para la gestión de clientes en BackEndExample
 * comunicándose mediante Eclipse MicroProfile Rest Client y Jettra JWT.
 */
public class CustomerFluxView extends FluxView {

    private final CustomerClientService customerService;
    private final List<Customer> customers;

    public CustomerFluxView() {
        this(new CustomerClientService());
    }

    public CustomerFluxView(CustomerClientService customerService) {
        this.customerService = customerService != null ? customerService : new CustomerClientService();
        this.customers = this.customerService.listAll();
    }

    public CustomerFluxView(CustomerClientService customerService, List<Customer> customers) {
        this.customerService = customerService != null ? customerService : new CustomerClientService();
        this.customers = customers != null ? customers : new ArrayList<>();
    }

    @Override
    public Widget build() {
        long totalCustomers = customers.size();
        long activeCount = customers.stream().filter(Customer::isActive).count();
        long enterpriseCount = customers.stream().filter(c -> "ENTERPRISE".equalsIgnoreCase(c.getTier())).count();
        String backendUrl = customerService.resolveBackendUrl();

        // 1. Cabecera e Insignias de Arquitectura
        Badge mpBadge = Badge.of("Eclipse MicroProfile Rest Client 3.0", "info");
        mpBadge.modifier(new Modifier().style("font-size: 0.72rem; font-weight: 800; background: linear-gradient(135deg, #0284c7, #2563eb); color: #fff; border: none; padding: 0.25rem 0.65rem; border-radius: 6px;"));

        Badge jwtBadge = Badge.of("🔒 JETTRA JWT AUTH", "success");
        jwtBadge.modifier(new Modifier().style("font-size: 0.72rem; font-weight: 800; background: rgba(16, 185, 129, 0.18); color: #34d399; border: 1px solid rgba(16, 185, 129, 0.35); padding: 0.25rem 0.65rem; border-radius: 6px;"));

        Row badgesRow = Row.of(mpBadge, jwtBadge);
        badgesRow.modifier(new Modifier().style("display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 0.4rem;"));

        Header mainHeader = Header.of(1, "👥 Gestión de Clientes (BackEndExample)");
        mainHeader.modifier(new Modifier().style("font-size: 1.5rem; font-weight: 800; color: var(--text-main); margin: 0;"));

        Paragraph subtitle = Paragraph.of("Consumo seguro de microservicios RESTful con Eclipse MicroProfile Rest Client, autenticación JWT Bearer y Project Loom Virtual Threads");
        subtitle.modifier(new Modifier().style("color: var(--text-muted); font-size: 0.88rem; margin: 0.25rem 0 1.25rem;"));

        Div headerBlock = Div.of(badgesRow, mainHeader, subtitle);

        // 2. Tarjetas KPI
        Widget kpiTotal = createKpiCard("Total Clientes", totalCustomers + " Registrados", "Sincronizados vía REST", "#38bdf8");
        Widget kpiActive = createKpiCard("Clientes Activos", activeCount + " Operativos", "Estado verificado", "#4ade80");
        Widget kpiEnterprise = createKpiCard("Nivel Enterprise", enterpriseCount + " Cuentas", "Prioridad alta", "#c084fc");
        Widget kpiEndpoint = createKpiCard("Endpoint Backend", "/api/v1/customers", backendUrl, "#f59e0b");

        Div kpiGrid = Div.of(kpiTotal, kpiActive, kpiEnterprise, kpiEndpoint);
        kpiGrid.modifier(new Modifier().style("display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1rem; margin-bottom: 1.5rem;"));

        // 3. Formulario Rápido de Registro / Edición
        Widget formCard = buildCustomerFormCard();

        // 4. Tabla de Clientes
        Widget tableCard = buildCustomerTableCard();

        return Column.of(headerBlock, kpiGrid, formCard, tableCard)
                .modifier(new Modifier().style("display: flex; flex-direction: column; gap: 1rem; width: 100%;"));
    }

    private Widget createKpiCard(String title, String value, String sub, String accentColor) {
        Div titleDiv = Div.of(Text.of(title));
        titleDiv.modifier(new Modifier().style("font-size: 0.75rem; text-transform: uppercase; font-weight: 700; color: var(--text-muted);"));

        Div valueDiv = Div.of(Text.of(value));
        valueDiv.modifier(new Modifier().style("font-size: 1.35rem; font-weight: 800; color: " + accentColor + "; margin: 0.35rem 0;"));

        Div subDiv = Div.of(Text.of(sub));
        subDiv.modifier(new Modifier().style("font-size: 0.78rem; color: var(--text-muted);"));

        return Card.of(Column.of(titleDiv, valueDiv, subDiv))
                .modifier(new Modifier().style("background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: 10px; padding: 1.15rem;"));
    }

    private Widget buildCustomerFormCard() {
        Header formTitle = Header.of(2, "➕ Registrar Nuevo Cliente (MicroProfile REST)");
        formTitle.modifier(new Modifier().style("font-size: 1.1rem; font-weight: 700; margin: 0 0 1rem; color: var(--text-main);"));

        TextInput inputName = TextInput.of("name", "Nombre Completo / Razón Social");
        inputName.modifier(new Modifier().style("padding: 0.55rem 0.75rem; border-radius: 6px; border: 1px solid var(--border-subtle); background: var(--bg-body); color: var(--text-main); width: 100%;"));

        TextInput inputEmail = TextInput.of("email", "Correo Corporativo");
        inputEmail.modifier(new Modifier().style("padding: 0.55rem 0.75rem; border-radius: 6px; border: 1px solid var(--border-subtle); background: var(--bg-body); color: var(--text-main); width: 100%;"));

        TextInput inputCompany = TextInput.of("company", "Empresa");
        inputCompany.modifier(new Modifier().style("padding: 0.55rem 0.75rem; border-radius: 6px; border: 1px solid var(--border-subtle); background: var(--bg-body); color: var(--text-main); width: 100%;"));

        TextInput inputTier = TextInput.of("tier", "Nivel (ENTERPRISE / PREMIUM / STANDARD)");
        inputTier.modifier(new Modifier().style("padding: 0.55rem 0.75rem; border-radius: 6px; border: 1px solid var(--border-subtle); background: var(--bg-body); color: var(--text-main); width: 100%;"));

        Button btnSave = Button.of("💾 Guardar Cliente vía MicroProfile");
        btnSave.modifier(new Modifier()
                .cssClass("app-btn-primary")
                .attribute("type", "submit")
                .style("background: #0284c7; color: #fff; padding: 0.6rem 1.25rem; font-weight: 700; border-radius: 6px; border: none; cursor: pointer;"));

        Div gridFields = Div.of(
                wrapField("Nombre del Cliente:", inputName, "flex: 2; min-width: 200px;"),
                wrapField("Email:", inputEmail, "flex: 2; min-width: 200px;"),
                wrapField("Empresa:", inputCompany, "flex: 1.5; min-width: 160px;"),
                wrapField("Nivel de Servicio:", inputTier, "flex: 1.5; min-width: 160px;"),
                wrapField("", btnSave, "align-self: flex-end;")
        );
        gridFields.modifier(new Modifier().style("display: flex; flex-wrap: wrap; gap: 0.85rem; align-items: flex-end;"));

        Form form = Form.of(gridFields);
        form.modifier(new Modifier().attribute("method", "post").attribute("action", "/clientes.xhtml"));

        return Card.of(Column.of(formTitle, form))
                .modifier(new Modifier().style("background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: 12px; padding: 1.35rem; margin-bottom: 1.5rem;"));
    }

    private Widget wrapField(String labelText, Widget control, String style) {
        if (labelText.isEmpty()) {
            return Div.of(control).modifier(new Modifier().style(style));
        }
        Div label = Div.of(Text.of(labelText));
        label.modifier(new Modifier().style("font-size: 0.78rem; font-weight: 600; color: var(--text-muted); margin-bottom: 0.35rem;"));
        return Column.of(label, control).modifier(new Modifier().style(style));
    }

    private Widget buildCustomerTableCard() {
        Header tableTitle = Header.of(2, "📋 Directorio de Clientes Registrados");
        tableTitle.modifier(new Modifier().style("font-size: 1.1rem; font-weight: 700; margin: 0 0 1rem; color: var(--text-main);"));

        // Renderizado semántico de filas mediante widgets JettraFlux
        List<Widget> rows = new ArrayList<>();

        // Fila de encabezado
        Widget headerRow = Div.of(
                createCell("ID", "width: 70px; font-weight: 700; color: var(--text-muted);"),
                createCell("Nombre de Cliente", "flex: 2; font-weight: 700; color: var(--text-muted);"),
                createCell("Email", "flex: 2; font-weight: 700; color: var(--text-muted);"),
                createCell("Empresa", "flex: 1.5; font-weight: 700; color: var(--text-muted);"),
                createCell("Nivel (Tier)", "width: 120px; text-align: center; font-weight: 700; color: var(--text-muted);"),
                createCell("Estado", "width: 100px; text-align: center; font-weight: 700; color: var(--text-muted);"),
                createCell("Acción", "width: 120px; text-align: center; font-weight: 700; color: var(--text-muted);")
        );
        headerRow.modifier(new Modifier().style("display: flex; align-items: center; padding: 0.75rem 1rem; background: var(--bg-subtle); border-bottom: 1px solid var(--border-subtle); border-radius: 8px 8px 0 0; font-size: 0.78rem; text-transform: uppercase;"));
        rows.add(headerRow);

        // Filas de datos
        for (Customer c : customers) {
            String tierColor = switch (c.getTier().toUpperCase()) {
                case "ENTERPRISE" -> "#c084fc";
                case "PREMIUM" -> "#38bdf8";
                default -> "#94a3b8";
            };

            Badge tierBadge = Badge.of(c.getTier());
            tierBadge.modifier(new Modifier().style("background: rgba(148, 163, 184, 0.15); color: " + tierColor + "; border: 1px solid " + tierColor + "40; padding: 0.15rem 0.55rem; border-radius: 9999px; font-size: 0.72rem; font-weight: 800;"));

            Badge statusBadge = Badge.of(c.isActive() ? "● ACTIVO" : "○ INACTIVO", c.isActive() ? "success" : "danger");
            statusBadge.modifier(new Modifier().style("font-size: 0.72rem; font-weight: 800;"));

            Link btnDelete = Link.of("/clientes.xhtml?action=delete&id=" + c.getId(), "🗑️ Eliminar");
            btnDelete.modifier(new Modifier().style("background: rgba(239, 68, 68, 0.15); color: #ef4444; border: 1px solid rgba(239, 68, 68, 0.35); padding: 0.25rem 0.65rem; border-radius: 6px; text-decoration: none; font-size: 0.78rem; font-weight: 700;"));

            Widget dataRow = Div.of(
                    createCell("#" + c.getId(), "width: 70px; font-weight: 800; color: var(--primary);"),
                    createCell(c.getName(), "flex: 2; font-weight: 600; color: var(--text-main);"),
                    createCell("📧 " + c.getEmail(), "flex: 2; color: var(--text-muted);"),
                    createCell("🏢 " + c.getCompany(), "flex: 1.5; color: var(--text-main);"),
                    Div.of(tierBadge).modifier(new Modifier().style("width: 120px; display: flex; justify-content: center;")),
                    Div.of(statusBadge).modifier(new Modifier().style("width: 100px; display: flex; justify-content: center;")),
                    Div.of(btnDelete).modifier(new Modifier().style("width: 120px; display: flex; justify-content: center;"))
            );
            dataRow.modifier(new Modifier().style("display: flex; align-items: center; padding: 0.85rem 1rem; border-bottom: 1px solid var(--border-subtle); font-size: 0.88rem;"));
            rows.add(dataRow);
        }

        Div tableContainer = Div.of(rows.toArray(new Widget[0]));
        tableContainer.modifier(new Modifier().style("background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: 10px; overflow: hidden;"));

        return Card.of(Column.of(tableTitle, tableContainer))
                .modifier(new Modifier().style("background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: 12px; padding: 1.35rem;"));
    }

    private Widget createCell(String text, String style) {
        return Div.of(Text.of(text)).modifier(new Modifier().style(style));
    }
}
