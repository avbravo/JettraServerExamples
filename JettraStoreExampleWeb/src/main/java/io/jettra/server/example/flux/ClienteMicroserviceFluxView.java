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
import io.jettra.server.example.model.ClienteDto;
import io.jettra.server.example.service.ClienteMicroserviceClientService;

import java.util.ArrayList;
import java.util.List;

/**
 * Vista reactiva 100% JettraFlux puro (sin RawHtml ni window.confirm) para el CRUD completo
 * de Clientes ({@code example_factura_db} / {@code @RestRepository ClienteRest}) vía RestClient + JWT.
 */
public class ClienteMicroserviceFluxView extends FluxView {

    private final ClienteMicroserviceClientService clienteService;
    private final String editId;
    private final String confirmDeleteId;
    private final String feedbackMessage;

    public ClienteMicroserviceFluxView(ClienteMicroserviceClientService clienteService) {
        this(clienteService, null, null, null);
    }

    public ClienteMicroserviceFluxView(ClienteMicroserviceClientService clienteService, String editId,
                                       String confirmDeleteId, String feedbackMessage) {
        this.clienteService = clienteService != null ? clienteService : new ClienteMicroserviceClientService();
        this.editId = editId;
        this.confirmDeleteId = confirmDeleteId;
        this.feedbackMessage = feedbackMessage;
    }

    @Override
    public Widget build() {
        List<ClienteDto> clientes = clienteService.listAll();
        Widget badges = Row.of(
                Badge.of("JettraFlux + @RestRepository ClienteRest", "info"),
                Badge.of("🔒 JETTRA JWT BEARER", "success")
        ).modifier(new Modifier().style("display:flex; gap:0.5rem; flex-wrap:wrap; margin-bottom:0.4rem;"));

        Header title = Header.of(1, "👥 Clientes Microservices (example_factura_db)");
        title.modifier(new Modifier().style("font-size:1.45rem; font-weight:800; color:var(--text-main); margin:0;"));

        Paragraph sub = Paragraph.of("CRUD completo en JettraFlux puro consumiendo "
                + clienteService.resolveBackendUrl() + "/api/v1/clientes con JWT Bearer.");
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

        ClienteDto editing = (editId != null && !editId.isBlank())
                ? clienteService.findById(editId).orElse(null)
                : null;
        root.add(buildFormCard(editing));
        root.add(buildTableCard(clientes));

        if (confirmDeleteId != null && !confirmDeleteId.isBlank()) {
            ClienteDto target = clienteService.findById(confirmDeleteId).orElse(null);
            if (target != null) {
                Modal deleteModal = Modal.of(buildDeleteConfirmDialog(target)).open(true);
                deleteModal.id("deleteClienteFluxModal");
                root.add(deleteModal);
            }
        }

        return Column.of(root.toArray(new Widget[0]))
                .modifier(new Modifier().style("display:flex; flex-direction:column; gap:1rem; width:100%;"));
    }

    private Widget buildFormCard(ClienteDto editing) {
        boolean isEdit = editing != null;
        Header h = Header.of(2, isEdit ? "✏️ Editar Cliente " + editing.nombre() : "➕ Registrar Cliente (@RestRepository)");
        h.modifier(new Modifier().style("font-size:1.05rem; font-weight:700; margin:0 0 0.85rem; color:var(--text-main);"));

        String inputStyle = "padding:0.5rem 0.7rem; border-radius:6px; border:1px solid var(--border-subtle); background:var(--bg-body); color:var(--text-main); width:100%;";

        TextInput act = TextInput.of("action", "");
        act.modifier(new Modifier().attribute("type", "hidden").attribute("value", isEdit ? "update" : "create"));

        TextInput id = TextInput.of("id", "");
        id.modifier(new Modifier().attribute("type", "hidden").attribute("value", isEdit ? editing.jettraId() : ""));

        TextInput ruc = TextInput.of("ruc", "155678901-2-2026");
        ruc.modifier(new Modifier().style(inputStyle).attribute("value", isEdit && editing.ruc() != null ? editing.ruc() : ""));

        TextInput nombre = TextInput.of("nombre", "Razón Social");
        nombre.modifier(new Modifier().style(inputStyle).attribute("value", isEdit && editing.nombre() != null ? editing.nombre() : ""));

        TextInput email = TextInput.of("email", "billing@empresa.com");
        email.modifier(new Modifier().style(inputStyle).attribute("value", isEdit && editing.email() != null ? editing.email() : ""));

        TextInput cat = TextInput.of("categoria", "ENTERPRISE");
        cat.modifier(new Modifier().style(inputStyle).attribute("value", isEdit && editing.categoria() != null ? editing.categoria() : "ENTERPRISE"));

        Button submitBtn = Button.of(isEdit ? "💾 Actualizar Cliente" : "💾 Guardar Cliente");
        submitBtn.modifier(new Modifier().attribute("type", "submit")
                .style("background:#0284c7; color:#fff; padding:0.55rem 1.15rem; font-weight:700; border-radius:6px; border:none; cursor:pointer;"));

        List<Widget> fields = new ArrayList<>();
        fields.add(act);
        fields.add(id);
        fields.add(field("RUC", ruc, "flex:1; min-width:150px;"));
        fields.add(field("Nombre", nombre, "flex:1.8; min-width:180px;"));
        fields.add(field("Email", email, "flex:1.5; min-width:170px;"));
        fields.add(field("Categoría", cat, "flex:1; min-width:130px;"));
        fields.add(Div.of(submitBtn).modifier(new Modifier().style("align-self:flex-end;")));

        Form form = Form.of(Div.of(fields.toArray(new Widget[0]))
                .modifier(new Modifier().style("display:flex; flex-wrap:wrap; gap:0.75rem; align-items:flex-end;")));
        form.modifier(new Modifier().attribute("method", "post").attribute("action", "/flux/clientes-ms"));

        return Card.of(Column.of(h, form))
                .modifier(new Modifier().style("background:var(--bg-card); border:1px solid var(--border-subtle); border-radius:12px; padding:1.25rem;"));
    }

    private Widget buildTableCard(List<ClienteDto> clientes) {
        Header h = Header.of(2, "📋 Directorio de Clientes (" + clientes.size() + ")");
        h.modifier(new Modifier().style("font-size:1.05rem; font-weight:700; margin:0 0 0.85rem; color:var(--text-main);"));

        List<Widget> rows = new ArrayList<>();
        rows.add(Div.of(
                cell("RUC", "flex:1.1; font-weight:700; color:var(--text-muted);"),
                cell("Nombre", "flex:1.8; font-weight:700; color:var(--text-muted);"),
                cell("Email", "flex:1.5; font-weight:700; color:var(--text-muted);"),
                cell("Categoría", "width:120px; font-weight:700; color:var(--text-muted);"),
                cell("Acciones", "width:170px; text-align:right; font-weight:700; color:var(--text-muted);")
        ).modifier(new Modifier().style("display:flex; align-items:center; padding:0.7rem 0.9rem; background:var(--bg-subtle); border-bottom:1px solid var(--border-subtle); font-size:0.78rem; text-transform:uppercase;")));

        for (ClienteDto c : clientes) {
            Link editLink = Link.of("/flux/clientes-ms?edit=" + c.jettraId(), "✏️ Editar");
            editLink.modifier(new Modifier().style("padding:0.25rem 0.55rem; border-radius:5px; background:#0284c7; color:#fff; text-decoration:none; font-size:0.78rem; margin-right:0.35rem;"));
            Link delLink = Link.of("/flux/clientes-ms?confirm_delete=" + c.jettraId(), "🗑️ Eliminar");
            delLink.modifier(new Modifier().style("padding:0.25rem 0.55rem; border-radius:5px; background:#dc2626; color:#fff; text-decoration:none; font-size:0.78rem;"));

            rows.add(Div.of(
                    cell(c.ruc(), "flex:1.1; font-weight:700; color:#38bdf8;"),
                    cell(c.nombre(), "flex:1.8; color:var(--text-main); font-weight:600;"),
                    cell(c.email(), "flex:1.5; color:var(--text-muted);"),
                    cell(c.categoria(), "width:120px; color:#c084fc; font-weight:700;"),
                    Div.of(editLink, delLink).modifier(new Modifier().style("width:170px; display:flex; justify-content:flex-end;"))
            ).modifier(new Modifier().style("display:flex; align-items:center; padding:0.7rem 0.9rem; border-bottom:1px solid var(--border-subtle); font-size:0.85rem;")));
        }

        return Card.of(Column.of(h, Column.of(rows.toArray(new Widget[0]))))
                .modifier(new Modifier().style("background:var(--bg-card); border:1px solid var(--border-subtle); border-radius:12px; padding:1.25rem;"));
    }

    private Widget buildDeleteConfirmDialog(ClienteDto target) {
        Header h = Header.of(3, "⚠️ Confirmar Eliminación de Cliente");
        h.modifier(new Modifier().style("margin:0 0 0.6rem; color:#f87171;"));
        Paragraph p = Paragraph.of("¿Seguro de eliminar al cliente " + target.nombre() + "?");
        p.modifier(new Modifier().style("margin:0 0 1rem; color:var(--text-main);"));

        Link cancel = Link.of("/flux/clientes-ms", "Cancelar");
        cancel.modifier(new Modifier().style("padding:0.45rem 0.9rem; border-radius:6px; background:var(--bg-subtle); color:var(--text-main); text-decoration:none;"));

        TextInput act = TextInput.of("action", "");
        act.modifier(new Modifier().attribute("type", "hidden").attribute("value", "delete"));
        TextInput id = TextInput.of("id", "");
        id.modifier(new Modifier().attribute("type", "hidden").attribute("value", target.jettraId()));
        Button confirmBtn = Button.of("Confirmar");
        confirmBtn.modifier(new Modifier().attribute("type", "submit")
                .style("padding:0.45rem 1rem; border-radius:6px; background:#dc2626; color:#fff; border:none; cursor:pointer; font-weight:700;"));
        Form delForm = Form.of(act, id, confirmBtn);
        delForm.modifier(new Modifier().attribute("method", "post").attribute("action", "/flux/clientes-ms").style("display:inline; margin:0;"));

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
