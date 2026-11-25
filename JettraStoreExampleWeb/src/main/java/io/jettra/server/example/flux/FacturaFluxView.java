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
import io.jettra.flux.widgets.Modal;
import io.jettra.flux.widgets.Paragraph;
import io.jettra.flux.widgets.Row;
import io.jettra.flux.widgets.Text;
import io.jettra.flux.widgets.TextInput;
import io.jettra.server.example.model.FacturaDto;
import io.jettra.server.example.service.FacturaClientService;

import java.util.ArrayList;
import java.util.List;

/**
 * Vista reactiva 100% JettraFlux puro (sin RawHtml ni window.confirm) para el CRUD completo
 * de Facturas ({@code example_factura_db}) consumiendo {@code JettraStoreExampleMicroservices}
 * mediante MicroProfile RestClient + Jettra JWT.
 */
public class FacturaFluxView extends FluxView {

    private final FacturaClientService facturaService;
    private final String editId;
    private final String confirmDeleteId;
    private final String feedbackMessage;

    public FacturaFluxView(FacturaClientService facturaService) {
        this(facturaService, null, null, null);
    }

    public FacturaFluxView(FacturaClientService facturaService, String editId, String confirmDeleteId, String feedbackMessage) {
        this.facturaService = facturaService != null ? facturaService : new FacturaClientService();
        this.editId = editId;
        this.confirmDeleteId = confirmDeleteId;
        this.feedbackMessage = feedbackMessage;
    }

    @Override
    public Widget build() {
        List<FacturaDto> facturas = facturaService.listAll();
        long emitidas = facturas.stream().filter(f -> "EMITIDA".equalsIgnoreCase(f.estado())).count();
        double volumen = facturas.stream().mapToDouble(FacturaDto::total).sum();

        Badge mpBadge = Badge.of("JettraFlux + MicroProfile RestClient", "info");
        Badge jwtBadge = Badge.of("🔒 JETTRA JWT BEARER", "success");
        Widget badges = Row.of(mpBadge, jwtBadge)
                .modifier(new Modifier().style("display:flex; gap:0.5rem; flex-wrap:wrap; margin-bottom:0.4rem;"));

        Header title = Header.of(1, "🧾 Facturación Electrónica (example_factura_db)");
        title.modifier(new Modifier().style("font-size:1.45rem; font-weight:800; color:var(--text-main); margin:0;"));

        Paragraph sub = Paragraph.of("CRUD completo en JettraFlux puro conectado a "
                + facturaService.resolveBackendUrl() + "/api/v1/facturas con autenticación JWT.");
        sub.modifier(new Modifier().style("color:var(--text-muted); font-size:0.86rem; margin:0.25rem 0 1rem;"));

        List<Widget> root = new ArrayList<>();
        root.add(Div.of(badges, title, sub));

        if (feedbackMessage != null && !feedbackMessage.isBlank()) {
            Div flash = Div.of(Text.of(feedbackMessage));
            flash.modifier(new Modifier().style(
                    "background:rgba(16,185,129,0.14); border:1px solid rgba(16,185,129,0.4); color:#34d399; "
                            + "padding:0.75rem 1rem; border-radius:8px; font-weight:600;"));
            root.add(flash);
        }

        Div kpiGrid = Div.of(
                kpiCard("Total Facturas", String.valueOf(facturas.size()), "example_factura_db", "#38bdf8"),
                kpiCard("Emitidas", String.valueOf(emitidas), "Activas", "#4ade80"),
                kpiCard("Facturado", String.format("$%,.2f", volumen), "Incluye ITBMS 7%", "#c084fc"),
                kpiCard("Endpoint", "/api/v1/facturas", facturaService.resolveBackendUrl(), "#f59e0b")
        );
        kpiGrid.modifier(new Modifier().style("display:grid; grid-template-columns:repeat(auto-fit, minmax(200px, 1fr)); gap:1rem;"));
        root.add(kpiGrid);

        FacturaDto editing = (editId != null && !editId.isBlank())
                ? facturaService.findById(editId).orElse(null)
                : null;
        root.add(buildFormCard(editing));
        root.add(buildTableCard(facturas));

        if (confirmDeleteId != null && !confirmDeleteId.isBlank()) {
            FacturaDto target = facturaService.findById(confirmDeleteId).orElse(null);
            if (target != null) {
                Modal deleteModal = Modal.of(buildDeleteConfirmDialog(target)).open(true);
                deleteModal.id("deleteFacturaFluxModal");
                root.add(deleteModal);
            }
        }

        return Column.of(root.toArray(new Widget[0]))
                .modifier(new Modifier().style("display:flex; flex-direction:column; gap:1rem; width:100%;"));
    }

    private Widget kpiCard(String label, String value, String detail, String color) {
        return Card.of(Column.of(
                Div.of(Text.of(label)).modifier(new Modifier().style("font-size:0.74rem; text-transform:uppercase; font-weight:700; color:var(--text-muted);")),
                Div.of(Text.of(value)).modifier(new Modifier().style("font-size:1.3rem; font-weight:800; color:" + color + "; margin:0.3rem 0;")),
                Div.of(Text.of(detail)).modifier(new Modifier().style("font-size:0.76rem; color:var(--text-muted);"))
        )).modifier(new Modifier().style("background:var(--bg-card); border:1px solid var(--border-subtle); border-radius:10px; padding:1rem;"));
    }

    private Widget buildFormCard(FacturaDto editing) {
        boolean isEdit = editing != null;
        Header h = Header.of(2, isEdit ? "✏️ Editar Factura " + editing.folio() : "➕ Emitir Nueva Factura (RestClient + JWT)");
        h.modifier(new Modifier().style("font-size:1.05rem; font-weight:700; margin:0 0 0.85rem; color:var(--text-main);"));

        String inputStyle = "padding:0.5rem 0.7rem; border-radius:6px; border:1px solid var(--border-subtle); background:var(--bg-body); color:var(--text-main); width:100%;";

        TextInput actionInput = TextInput.of("action", "");
        actionInput.modifier(new Modifier().attribute("type", "hidden").attribute("value", isEdit ? "update" : "create"));

        TextInput idInput = TextInput.of("id", "");
        idInput.modifier(new Modifier().attribute("type", "hidden").attribute("value", isEdit ? editing.jettraId() : ""));

        TextInput folioInput = TextInput.of("folio", "FAC-2026-0002");
        folioInput.modifier(new Modifier().style(inputStyle).attribute("value", isEdit && editing.folio() != null ? editing.folio() : ""));

        TextInput clienteIdInput = TextInput.of("clienteId", "CLI-001");
        clienteIdInput.modifier(new Modifier().style(inputStyle).attribute("value", isEdit && editing.clienteId() != null ? editing.clienteId() : "CLI-001"));

        TextInput clienteNombreInput = TextInput.of("clienteNombre", "Acme Corporation S.A.");
        clienteNombreInput.modifier(new Modifier().style(inputStyle).attribute("value", isEdit && editing.clienteNombre() != null ? editing.clienteNombre() : ""));

        String descVal = (isEdit && editing.detalles() != null && !editing.detalles().isEmpty()) ? editing.detalles().get(0).descripcion() : "";
        String qtyVal = (isEdit && editing.detalles() != null && !editing.detalles().isEmpty()) ? String.valueOf(editing.detalles().get(0).cantidad()) : "1";
        String priceVal = (isEdit && editing.detalles() != null && !editing.detalles().isEmpty()) ? String.valueOf(editing.detalles().get(0).precioUnitario()) : "150.00";

        TextInput descInput = TextInput.of("descripcion", "Licencia JettraStore");
        descInput.modifier(new Modifier().style(inputStyle).attribute("value", descVal));

        TextInput qtyInput = TextInput.of("cantidad", "1");
        qtyInput.modifier(new Modifier().style(inputStyle).attribute("value", qtyVal));

        TextInput priceInput = TextInput.of("precio", "150.00");
        priceInput.modifier(new Modifier().style(inputStyle).attribute("value", priceVal));

        Button submitBtn = Button.of(isEdit ? "💾 Actualizar Factura" : "💾 Emitir Factura");
        submitBtn.modifier(new Modifier().attribute("type", "submit")
                .style("background:#0284c7; color:#fff; padding:0.55rem 1.15rem; font-weight:700; border-radius:6px; border:none; cursor:pointer;"));

        List<Widget> fields = new ArrayList<>();
        fields.add(actionInput);
        fields.add(idInput);
        fields.add(field("Folio", folioInput, "flex:1; min-width:140px;"));
        fields.add(field("ID Cliente", clienteIdInput, "flex:1; min-width:120px;"));
        fields.add(field("Nombre Cliente", clienteNombreInput, "flex:1.5; min-width:170px;"));
        fields.add(field("Descripción Ítem", descInput, "flex:2; min-width:190px;"));
        fields.add(field("Cantidad", qtyInput, "width:95px;"));
        fields.add(field("Precio ($)", priceInput, "width:115px;"));
        fields.add(Div.of(submitBtn).modifier(new Modifier().style("align-self:flex-end;")));
        if (isEdit) {
            Link cancelEdit = Link.of("/flux/facturas", "Cancelar");
            cancelEdit.modifier(new Modifier().style("padding:0.5rem 0.9rem; border-radius:6px; background:var(--bg-subtle); color:var(--text-main); text-decoration:none;"));
            fields.add(Div.of(cancelEdit).modifier(new Modifier().style("align-self:flex-end;")));
        }

        Widget row = Div.of(fields.toArray(new Widget[0]))
                .modifier(new Modifier().style("display:flex; flex-wrap:wrap; gap:0.75rem; align-items:flex-end;"));

        Form form = Form.of(row);
        form.modifier(new Modifier().attribute("method", "post").attribute("action", "/flux/facturas"));

        return Card.of(Column.of(h, form))
                .modifier(new Modifier().style("background:var(--bg-card); border:1px solid var(--border-subtle); border-radius:12px; padding:1.25rem;"));
    }

    private Widget buildTableCard(List<FacturaDto> facturas) {
        Header h = Header.of(2, "📋 Facturas Registradas (example_factura_db)");
        h.modifier(new Modifier().style("font-size:1.05rem; font-weight:700; margin:0 0 0.85rem; color:var(--text-main);"));

        List<Widget> rows = new ArrayList<>();
        rows.add(Div.of(
                cell("Folio", "flex:1.2; font-weight:700; color:var(--text-muted);"),
                cell("Cliente", "flex:1.8; font-weight:700; color:var(--text-muted);"),
                cell("Fecha", "width:110px; font-weight:700; color:var(--text-muted);"),
                cell("Estado", "width:110px; font-weight:700; color:var(--text-muted);"),
                cell("Total", "width:110px; font-weight:700; color:var(--text-muted);"),
                cell("Acciones", "width:230px; text-align:right; font-weight:700; color:var(--text-muted);")
        ).modifier(new Modifier().style("display:flex; align-items:center; padding:0.7rem 0.9rem; background:var(--bg-subtle); border-bottom:1px solid var(--border-subtle); font-size:0.78rem; text-transform:uppercase;")));

        for (FacturaDto f : facturas) {
            Link editLink = Link.of("/flux/facturas?edit=" + f.jettraId(), "✏️ Editar");
            editLink.modifier(new Modifier().style("padding:0.25rem 0.55rem; border-radius:5px; background:#0284c7; color:#fff; text-decoration:none; font-size:0.78rem; margin-right:0.3rem;"));

            TextInput anularAct = TextInput.of("action", "");
            anularAct.modifier(new Modifier().attribute("type", "hidden").attribute("value", "anular"));
            TextInput anularId = TextInput.of("id", "");
            anularId.modifier(new Modifier().attribute("type", "hidden").attribute("value", f.jettraId()));
            Button anularBtn = Button.of("Anular");
            anularBtn.modifier(new Modifier().attribute("type", "submit")
                    .style("padding:0.25rem 0.55rem; border-radius:5px; background:#d97706; color:#fff; border:none; cursor:pointer; font-size:0.78rem; margin-right:0.3rem;"));
            Form anularForm = Form.of(anularAct, anularId, anularBtn);
            anularForm.modifier(new Modifier().attribute("method", "post").attribute("action", "/flux/facturas").style("display:inline; margin:0;"));

            Link delLink = Link.of("/flux/facturas?confirm_delete=" + f.jettraId(), "🗑️ Eliminar");
            delLink.modifier(new Modifier().style("padding:0.25rem 0.55rem; border-radius:5px; background:#dc2626; color:#fff; text-decoration:none; font-size:0.78rem;"));

            Widget actions = Div.of(editLink, anularForm, delLink)
                    .modifier(new Modifier().style("width:230px; display:flex; justify-content:flex-end; align-items:center;"));

            rows.add(Div.of(
                    cell(f.folio(), "flex:1.2; font-weight:700; color:#38bdf8;"),
                    cell(f.clienteNombre(), "flex:1.8; color:var(--text-main);"),
                    cell(f.fechaEmision(), "width:110px; color:var(--text-muted);"),
                    cell(f.estado(), "width:110px; font-weight:700; color:" + ("ANULADA".equalsIgnoreCase(f.estado()) ? "#f87171" : "#34d399") + ";"),
                    cell(String.format("$%,.2f", f.total()), "width:110px; font-weight:700; color:var(--text-main);"),
                    actions
            ).modifier(new Modifier().style("display:flex; align-items:center; padding:0.7rem 0.9rem; border-bottom:1px solid var(--border-subtle); font-size:0.85rem;")));
        }

        return Card.of(Column.of(h, Column.of(rows.toArray(new Widget[0]))))
                .modifier(new Modifier().style("background:var(--bg-card); border:1px solid var(--border-subtle); border-radius:12px; padding:1.25rem;"));
    }

    private Widget buildDeleteConfirmDialog(FacturaDto target) {
        Header h = Header.of(3, "⚠️ Confirmar Eliminación de Factura");
        h.modifier(new Modifier().style("margin:0 0 0.6rem; color:#f87171;"));
        Paragraph p = Paragraph.of("¿Seguro de eliminar la factura " + target.folio() + "?");
        p.modifier(new Modifier().style("margin:0 0 1rem; color:var(--text-main);"));

        Link cancel = Link.of("/flux/facturas", "Cancelar");
        cancel.modifier(new Modifier().style("padding:0.45rem 0.9rem; border-radius:6px; background:var(--bg-subtle); color:var(--text-main); text-decoration:none;"));

        TextInput act = TextInput.of("action", "");
        act.modifier(new Modifier().attribute("type", "hidden").attribute("value", "delete"));
        TextInput id = TextInput.of("id", "");
        id.modifier(new Modifier().attribute("type", "hidden").attribute("value", target.jettraId()));
        Button confirmBtn = Button.of("Confirmar");
        confirmBtn.modifier(new Modifier().attribute("type", "submit")
                .style("padding:0.45rem 1rem; border-radius:6px; background:#dc2626; color:#fff; border:none; cursor:pointer; font-weight:700;"));
        Form delForm = Form.of(act, id, confirmBtn);
        delForm.modifier(new Modifier().attribute("method", "post").attribute("action", "/flux/facturas").style("display:inline; margin:0;"));

        Widget actions = Div.of(cancel, delForm)
                .modifier(new Modifier().style("display:flex; justify-content:flex-end; gap:0.65rem; align-items:center;"));
        return Div.of(h, p, actions).modifier(new Modifier().style("min-width:340px; padding:0.25rem;"));
    }

    private Widget field(String label, Widget input, String style) {
        Div lbl = Div.of(Text.of(label));
        lbl.modifier(new Modifier().style("font-size:0.75rem; font-weight:600; color:var(--text-muted); margin-bottom:0.25rem;"));
        return Column.of(lbl, input).modifier(new Modifier().style(style));
    }

    private Widget cell(String text, String style) {
        return Div.of(Text.of(text != null ? text : "")).modifier(new Modifier().style(style));
    }
}
