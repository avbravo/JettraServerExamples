package io.jettra.server.example.aura;

import io.jettra.aura.ajax.AuraAjaxButton;
import io.jettra.aura.annotation.AuraPath;
import io.jettra.aura.components.AuraFeedbackPanel;
import io.jettra.aura.components.AuraForm;
import io.jettra.aura.components.AuraLabel;
import io.jettra.aura.components.AuraListView;
import io.jettra.aura.components.AuraModal;
import io.jettra.aura.components.AuraTextField;
import io.jettra.aura.core.AuraContainer;
import io.jettra.aura.model.LambdaModel;
import io.jettra.aura.model.LoadableDetachableModel;
import io.jettra.server.example.model.ClienteDto;
import io.jettra.server.example.service.ClienteMicroserviceClientService;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Página reactiva JettraAura (Jettra Astra) para operaciones CRUD completas de Clientes
 * ({@code example_factura_db} / {@code @RestRepository ClienteRest}) vía RestClient + JWT.
 */
@AuraPath("/aura/clientes-ms")
public class ClienteMicroserviceAuraPage extends AuraTemplatePage {

    @Inject
    private ClienteMicroserviceClientService clienteService;

    private String newRuc = "";
    private String newNombre = "";
    private String newEmail = "";
    private String newCategoria = "ENTERPRISE";

    private String editId;
    private String editRuc = "";
    private String editNombre = "";
    private String editEmail = "";
    private String editCategoria = "ENTERPRISE";
    private ClienteDto selectedForDelete;

    private AuraFeedbackPanel feedbackPanel;
    private AuraContainer tableContainer;
    private AuraModal editModal;
    private AuraModal deleteModal;

    public ClienteMicroserviceAuraPage() {
        setPageTitle("Clientes (example_factura_db) - JettraAura & RestClient + JWT");
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        if (clienteService == null) {
            clienteService = new ClienteMicroserviceClientService();
        }

        feedbackPanel = new AuraFeedbackPanel("feedbackMessages");
        add(feedbackPanel);

        LoadableDetachableModel<List<ClienteDto>> model = new LoadableDetachableModel<>() {
            @Override
            protected List<ClienteDto> load() {
                return clienteService.listAll();
            }
        };

        AuraForm<Void> createForm = new AuraForm<>("clienteMsForm");
        createForm.add(new AuraTextField<>("ruc", LambdaModel.of(() -> newRuc, v -> newRuc = v)).setRequired(true));
        createForm.add(new AuraTextField<>("nombre", LambdaModel.of(() -> newNombre, v -> newNombre = v)).setRequired(true));
        createForm.add(new AuraTextField<>("email", LambdaModel.of(() -> newEmail, v -> newEmail = v)).setRequired(true));
        createForm.add(new AuraTextField<>("categoria", LambdaModel.of(() -> newCategoria, v -> newCategoria = v)));
        createForm.add(new AuraAjaxButton("btnSaveClienteMs", target -> {
            if (newNombre == null || newNombre.isBlank() || newRuc == null || newRuc.isBlank()) {
                error("RUC y nombre son obligatorios.");
                target.add(feedbackPanel);
                return;
            }
            ClienteDto created = clienteService.create(new ClienteDto(null, newRuc.trim(), newNombre.trim(), newEmail.trim(), newCategoria, true));
            info("Cliente '" + created.nombre() + "' registrado vía RestClient + JWT.");
            newRuc = "";
            newNombre = "";
            newEmail = "";
            target.add(feedbackPanel, tableContainer);
        }));
        add(createForm);

        tableContainer = new AuraContainer("clienteMsTableContainer");
        tableContainer.setOutputMarkupId(true);
        tableContainer.add(new AuraLabel("clienteMsCountBadge", () -> clienteService.listAll().size() + " Clientes"));

        AuraListView<ClienteDto> listView = new AuraListView<>("clienteMsRows", model, item -> {
            ClienteDto c = item.getModelData();
            item.add(new AuraLabel("ruc", c.ruc()));
            item.add(new AuraLabel("nombre", c.nombre()));
            item.add(new AuraLabel("email", c.email()));
            item.add(new AuraLabel("categoria", c.categoria()));

            item.add(new AuraAjaxButton("btnEditClienteMs", target -> {
                editId = c.jettraId();
                editRuc = c.ruc();
                editNombre = c.nombre();
                editEmail = c.email();
                editCategoria = c.categoria();
                editModal.setTitle("✏️ Editar Cliente " + c.nombre());
                editModal.show(target);
            }));
            item.add(new AuraAjaxButton("btnDeleteClienteMs", target -> {
                selectedForDelete = c;
                deleteModal.show(target);
            }));
        });
        tableContainer.add(listView);
        add(tableContainer);

        editModal = new AuraModal("editClienteMsModal", "✏️ Editar Cliente");
        AuraForm<Void> editForm = new AuraForm<>("editClienteMsForm");
        editForm.add(new AuraTextField<>("editRuc", LambdaModel.of(() -> editRuc, v -> editRuc = v)));
        editForm.add(new AuraTextField<>("editNombre", LambdaModel.of(() -> editNombre, v -> editNombre = v)));
        editForm.add(new AuraTextField<>("editEmail", LambdaModel.of(() -> editEmail, v -> editEmail = v)));
        editForm.add(new AuraTextField<>("editCategoria", LambdaModel.of(() -> editCategoria, v -> editCategoria = v)));
        editForm.add(new AuraAjaxButton("btnSaveEditClienteMs", target -> {
            if (editId != null) {
                clienteService.update(editId, new ClienteDto(editId, editRuc, editNombre, editEmail, editCategoria, true));
                editModal.hide(target);
                info("Cliente actualizado vía RestClient + JWT.");
                target.add(feedbackPanel, tableContainer);
            }
        }));
        editForm.add(new AuraAjaxButton("btnCancelEditClienteMs", target -> editModal.hide(target)));
        editModal.add(editForm);
        add(editModal);

        deleteModal = new AuraModal("deleteClienteMsModal", "🗑️ Confirmar Eliminación de Cliente");
        deleteModal.add(new AuraLabel("deleteClienteMsMessage", () ->
                selectedForDelete != null
                        ? "¿Seguro de eliminar al cliente '" + selectedForDelete.nombre() + "'?"
                        : "¿Desea eliminar este cliente?"));
        deleteModal.add(new AuraAjaxButton("btnConfirmDeleteClienteMs", target -> {
            if (selectedForDelete != null) {
                clienteService.delete(selectedForDelete.jettraId());
                deleteModal.hide(target);
                warn("Cliente '" + selectedForDelete.nombre() + "' eliminado.");
                selectedForDelete = null;
                target.add(feedbackPanel, tableContainer);
            }
        }));
        deleteModal.add(new AuraAjaxButton("btnCancelDeleteClienteMs", target -> {
            deleteModal.hide(target);
            selectedForDelete = null;
        }));
        add(deleteModal);
    }
}
