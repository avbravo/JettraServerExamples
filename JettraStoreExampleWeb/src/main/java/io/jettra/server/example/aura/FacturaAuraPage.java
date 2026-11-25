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
import io.jettra.server.example.model.DetalleFacturaDto;
import io.jettra.server.example.model.FacturaDto;
import io.jettra.server.example.service.FacturaClientService;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.List;

/**
 * Página reactiva JettraAura (Jettra Astra) para operaciones CRUD completas de Facturas
 * ({@code example_factura_db}) consumiendo {@code JettraStoreExampleMicroservices} vía RestClient + JWT.
 */
@AuraPath("/aura/facturas")
public class FacturaAuraPage extends AuraTemplatePage {

    @Inject
    private FacturaClientService facturaService;

    private String newFolio = "";
    private String newClienteId = "CLI-001";
    private String newClienteNombre = "Acme Corporation S.A.";
    private String newDescripcion = "Servicio JettraStore";
    private String newCantidad = "1";
    private String newPrecio = "150.00";

    private String editId;
    private String editFolio = "";
    private String editClienteNombre = "";
    private String editEstado = "EMITIDA";
    private FacturaDto selectedForDelete;

    private AuraFeedbackPanel feedbackPanel;
    private AuraContainer tableContainer;
    private AuraModal editModal;
    private AuraModal deleteModal;

    public FacturaAuraPage() {
        setPageTitle("Facturación (example_factura_db) - JettraAura & RestClient + JWT");
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        if (facturaService == null) {
            facturaService = new FacturaClientService();
        }

        feedbackPanel = new AuraFeedbackPanel("feedbackMessages");
        add(feedbackPanel);

        LoadableDetachableModel<List<FacturaDto>> model = new LoadableDetachableModel<>() {
            @Override
            protected List<FacturaDto> load() {
                return facturaService.listAll();
            }
        };

        AuraForm<Void> createForm = new AuraForm<>("facturaForm");
        createForm.add(new AuraTextField<>("folio", LambdaModel.of(() -> newFolio, v -> newFolio = v)));
        createForm.add(new AuraTextField<>("clienteId", LambdaModel.of(() -> newClienteId, v -> newClienteId = v)).setRequired(true));
        createForm.add(new AuraTextField<>("clienteNombre", LambdaModel.of(() -> newClienteNombre, v -> newClienteNombre = v)).setRequired(true));
        createForm.add(new AuraTextField<>("descripcion", LambdaModel.of(() -> newDescripcion, v -> newDescripcion = v)).setRequired(true));
        createForm.add(new AuraTextField<>("cantidad", LambdaModel.of(() -> newCantidad, v -> newCantidad = v)).setRequired(true));
        createForm.add(new AuraTextField<>("precio", LambdaModel.of(() -> newPrecio, v -> newPrecio = v)).setRequired(true));

        createForm.add(new AuraAjaxButton("btnEmitirFactura", target -> {
            if (newDescripcion == null || newDescripcion.isBlank()) {
                error("La descripción del ítem es obligatoria.");
                target.add(feedbackPanel);
                return;
            }
            int qty = parseIntOrDefault(newCantidad, 1);
            double price = parseDoubleOrDefault(newPrecio, 100.0);
            DetalleFacturaDto det = DetalleFacturaDto.of("SRV-AURA", newDescripcion.trim(), qty, price);
            FacturaDto created = facturaService.create(new FacturaDto(
                    null, newFolio, newClienteId, newClienteNombre, LocalDate.now().toString(),
                    "EMITIDA", List.of(det), det.subtotal(), 0.0, 0.0));
            info("Factura '" + created.folio() + "' emitida vía RestClient + JWT.");
            newFolio = "";
            target.add(feedbackPanel, tableContainer);
        }));
        add(createForm);

        tableContainer = new AuraContainer("facturaTableContainer");
        tableContainer.setOutputMarkupId(true);
        tableContainer.add(new AuraLabel("facturaCountBadge", () -> facturaService.listAll().size() + " Facturas"));

        AuraListView<FacturaDto> listView = new AuraListView<>("facturaRows", model, item -> {
            FacturaDto f = item.getModelData();
            item.add(new AuraLabel("folio", f.folio()));
            item.add(new AuraLabel("cliente", f.clienteNombre()));
            item.add(new AuraLabel("fecha", f.fechaEmision()));
            item.add(new AuraLabel("estado", f.estado()));
            item.add(new AuraLabel("total", String.format("$%,.2f", f.total())));

            item.add(new AuraAjaxButton("btnEditFactura", target -> {
                editId = f.jettraId();
                editFolio = f.folio();
                editClienteNombre = f.clienteNombre();
                editEstado = f.estado();
                editModal.setTitle("✏️ Editar Factura " + f.folio());
                editModal.show(target);
            }));

            item.add(new AuraAjaxButton("btnAnularFactura", target -> {
                facturaService.anular(f.jettraId());
                warn("Factura '" + f.folio() + "' anulada.");
                target.add(feedbackPanel, tableContainer);
            }));

            item.add(new AuraAjaxButton("btnDeleteFactura", target -> {
                selectedForDelete = f;
                deleteModal.show(target);
            }));
        });
        tableContainer.add(listView);
        add(tableContainer);

        // Modal Editar
        editModal = new AuraModal("editFacturaModal", "✏️ Editar Factura");
        AuraForm<Void> editForm = new AuraForm<>("editFacturaForm");
        editForm.add(new AuraTextField<>("editFolio", LambdaModel.of(() -> editFolio, v -> editFolio = v)));
        editForm.add(new AuraTextField<>("editClienteNombre", LambdaModel.of(() -> editClienteNombre, v -> editClienteNombre = v)));
        editForm.add(new AuraTextField<>("editEstado", LambdaModel.of(() -> editEstado, v -> editEstado = v)));
        editForm.add(new AuraAjaxButton("btnSaveEditFactura", target -> {
            if (editId != null) {
                facturaService.findById(editId).ifPresent(existing -> {
                    FacturaDto updated = new FacturaDto(editId, editFolio, existing.clienteId(), editClienteNombre,
                            existing.fechaEmision(), editEstado, existing.detalles(),
                            existing.subtotal(), existing.impuesto(), existing.total());
                    facturaService.update(editId, updated);
                });
                editModal.hide(target);
                info("Factura actualizada vía RestClient + JWT.");
                target.add(feedbackPanel, tableContainer);
            }
        }));
        editForm.add(new AuraAjaxButton("btnCancelEditFactura", target -> editModal.hide(target)));
        editModal.add(editForm);
        add(editModal);

        // Modal Confirmar Eliminación
        deleteModal = new AuraModal("deleteFacturaModal", "🗑️ Confirmar Eliminación de Factura");
        deleteModal.add(new AuraLabel("deleteFacturaMessage", () ->
                selectedForDelete != null
                        ? "¿Seguro de eliminar la factura '" + selectedForDelete.folio() + "'?"
                        : "¿Desea eliminar esta factura?"));
        deleteModal.add(new AuraAjaxButton("btnConfirmDeleteFactura", target -> {
            if (selectedForDelete != null) {
                facturaService.delete(selectedForDelete.jettraId());
                deleteModal.hide(target);
                warn("Factura '" + selectedForDelete.folio() + "' eliminada.");
                selectedForDelete = null;
                target.add(feedbackPanel, tableContainer);
            }
        }));
        deleteModal.add(new AuraAjaxButton("btnCancelDeleteFactura", target -> {
            deleteModal.hide(target);
            selectedForDelete = null;
        }));
        add(deleteModal);
    }

    private static int parseIntOrDefault(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static double parseDoubleOrDefault(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }
}
