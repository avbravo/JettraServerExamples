package io.jettra.server.example.aura;

import io.jettra.aura.ajax.AuraAjaxButton;
import io.jettra.aura.ajax.AuraAjaxLink;
import io.jettra.aura.annotation.AuraPath;
import io.jettra.aura.components.AuraDropDownChoice;
import io.jettra.aura.components.AuraFeedbackPanel;
import io.jettra.aura.components.AuraForm;
import io.jettra.aura.components.AuraLabel;
import io.jettra.aura.components.AuraListView;
import io.jettra.aura.components.AuraModal;
import io.jettra.aura.components.AuraTextField;
import io.jettra.aura.core.AuraContainer;
import io.jettra.aura.model.CompoundPropertyModel;
import io.jettra.aura.model.LambdaModel;
import io.jettra.aura.model.LoadableDetachableModel;
import io.jettra.server.example.model.Customer;
import io.jettra.server.example.service.CustomerClientService;
import jakarta.inject.Inject;
import java.util.List;

/**
 * Página reactiva JettraAura para la gestión CRUD de Clientes consumiendo BackEndExample
 * mediante Eclipse MicroProfile Rest Client y autenticación Jettra JWT Bearer.
 */
@AuraPath("/aura/customers")
public class CustomerAuraPage extends AuraTemplatePage {

    private static final List<String> TIERS = List.of("STANDARD", "PREMIUM", "ENTERPRISE");

    @Inject
    private CustomerClientService customerService;

    private final Customer newCustomer = new Customer("", "", "", "STANDARD", true);
    private final Customer registerCustomer = new Customer("", "", "", "STANDARD", true);
    private final Customer modalCustomer = new Customer("", "", "", "STANDARD", true);
    private Customer selectedForDelete;

    private AuraFeedbackPanel feedbackPanel;
    private AuraContainer customerTableContainer;
    private AuraModal registerCustomerModal;
    private AuraModal editCustomerModal;
    private AuraModal deleteCustomerModal;

    public CustomerAuraPage() {
        setPageTitle("Gestión de Clientes - JettraAura & MicroProfile REST");
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        if (customerService == null) {
            customerService = new CustomerClientService();
        }

        // Panel de notificaciones y feedback
        feedbackPanel = new AuraFeedbackPanel("feedbackMessages");
        add(feedbackPanel);

        // Modelo de carga diferida
        LoadableDetachableModel<List<Customer>> customersModel = new LoadableDetachableModel<>() {
            @Override
            protected List<Customer> load() {
                return customerService.listAll();
            }
        };

        // Formulario rápido en línea con CompoundPropertyModel
        AuraForm<Customer> customerInlineForm = new AuraForm<>("customerForm", CompoundPropertyModel.of(newCustomer));
        customerInlineForm.add(new AuraTextField<>("name").setRequired(true));
        customerInlineForm.add(new AuraTextField<>("email").setRequired(true));
        customerInlineForm.add(new AuraTextField<>("company"));
        customerInlineForm.add(new AuraDropDownChoice<>("tier", TIERS));

        customerInlineForm.add(new AuraAjaxButton("btnSaveCustomer", target -> {
            if (newCustomer.getName() == null || newCustomer.getName().isBlank()) {
                error("El nombre del cliente es obligatorio.");
                target.add(feedbackPanel);
                return;
            }
            Customer saved = customerService.save(new Customer(newCustomer.getName(), newCustomer.getEmail(), newCustomer.getCompany(), newCustomer.getTier(), true));
            info("Cliente '" + saved.getName() + "' registrado exitosamente vía MicroProfile Rest Client.");
            newCustomer.setName("");
            newCustomer.setEmail("");
            newCustomer.setCompany("");
            newCustomer.setTier("STANDARD");
            target.add(feedbackPanel, customerTableContainer);
        }));

        customerInlineForm.add(new AuraAjaxLink("btnOpenRegisterModal", target -> {
            registerCustomer.setName("");
            registerCustomer.setEmail("");
            registerCustomer.setCompany("");
            registerCustomer.setTier("STANDARD");
            registerCustomerModal.show(target);
        }));

        add(customerInlineForm);

        // Contenedor principal de la tabla AJAX
        customerTableContainer = new AuraContainer("customerTableContainer");
        customerTableContainer.setOutputMarkupId(true);

        customerTableContainer.add(new AuraLabel("customerCountBadge", () -> customerService.listAll().size() + " Clientes"));

        customerTableContainer.add(new AuraAjaxLink("btnOpenCreateModal", target -> {
            registerCustomer.setName("");
            registerCustomer.setEmail("");
            registerCustomer.setCompany("");
            registerCustomer.setTier("STANDARD");
            registerCustomerModal.show(target);
        }));

        // Lista repetidora dinámica
        AuraListView<Customer> listView = new AuraListView<>("customerRows", customersModel, item -> {
            Customer c = item.getModelData();

            item.add(new AuraLabel("id", "#" + c.getId()));
            item.add(new AuraLabel("name", c.getName()));
            item.add(new AuraLabel("email", c.getEmail()));
            item.add(new AuraLabel("company", c.getCompany()));
            item.add(new AuraLabel("tier", c.getTier()));
            item.add(new AuraLabel("active", c.isActive() ? "● ACTIVO" : "○ INACTIVO"));

            // Botón Editar -> abre editCustomerModal
            item.add(new AuraAjaxButton("btnOpenEdit", target -> {
                modalCustomer.setId(c.getId());
                modalCustomer.setName(c.getName());
                modalCustomer.setEmail(c.getEmail());
                modalCustomer.setCompany(c.getCompany());
                modalCustomer.setTier(c.getTier());
                modalCustomer.setActive(c.isActive());
                editCustomerModal.setTitle("✏️ Editar Cliente (#" + c.getId() + ")");
                editCustomerModal.show(target);
            }));

            // Botón Eliminar -> abre deleteCustomerModal
            item.add(new AuraAjaxButton("btnOpenDelete", target -> {
                selectedForDelete = c;
                deleteCustomerModal.show(target);
            }));
        });
        customerTableContainer.add(listView);
        add(customerTableContainer);

        // 1. DIÁLOGO MODAL: REGISTRAR NUEVO CLIENTE (AuraModal)
        registerCustomerModal = new AuraModal("registerCustomerModal", "➕ Registrar Nuevo Cliente (MicroProfile REST)");
        AuraForm<Customer> registerForm = new AuraForm<>("registerModalForm", CompoundPropertyModel.of(registerCustomer));
        registerForm.add(new AuraTextField<>("regName", LambdaModel.of(registerCustomer::getName, registerCustomer::setName)).setRequired(true));
        registerForm.add(new AuraTextField<>("regEmail", LambdaModel.of(registerCustomer::getEmail, registerCustomer::setEmail)).setRequired(true));
        registerForm.add(new AuraTextField<>("regCompany", LambdaModel.of(registerCustomer::getCompany, registerCustomer::setCompany)));
        registerForm.add(new AuraDropDownChoice<>("regTier", LambdaModel.of(registerCustomer::getTier, registerCustomer::setTier), TIERS));

        registerForm.add(new AuraAjaxButton("btnRegisterSave", target -> {
            if (registerCustomer.getName() == null || registerCustomer.getName().isBlank()) {
                error("El nombre del cliente no puede estar vacío.");
                target.add(feedbackPanel);
                return;
            }
            Customer saved = customerService.save(new Customer(registerCustomer.getName(), registerCustomer.getEmail(), registerCustomer.getCompany(), registerCustomer.getTier(), true));
            registerCustomerModal.hide(target);
            info("Cliente '" + saved.getName() + "' creado en BackEndExample con ID #" + saved.getId() + ".");
            target.add(feedbackPanel, customerTableContainer);
        }));

        registerForm.add(new AuraAjaxButton("btnCancelRegister", target -> {
            registerCustomerModal.hide(target);
        }));

        registerCustomerModal.add(registerForm);
        add(registerCustomerModal);

        // 2. DIÁLOGO MODAL: EDITAR CLIENTE (AuraModal)
        editCustomerModal = new AuraModal("editCustomerModal", "✏️ Editar Cliente");
        AuraForm<Customer> editForm = new AuraForm<>("editModalForm", CompoundPropertyModel.of(modalCustomer));
        editForm.add(new AuraTextField<>("editName", LambdaModel.of(modalCustomer::getName, modalCustomer::setName)).setRequired(true));
        editForm.add(new AuraTextField<>("editEmail", LambdaModel.of(modalCustomer::getEmail, modalCustomer::setEmail)).setRequired(true));
        editForm.add(new AuraTextField<>("editCompany", LambdaModel.of(modalCustomer::getCompany, modalCustomer::setCompany)));
        editForm.add(new AuraDropDownChoice<>("editTier", LambdaModel.of(modalCustomer::getTier, modalCustomer::setTier), TIERS));

        editForm.add(new AuraAjaxButton("btnEditSave", target -> {
            if (modalCustomer.getName() == null || modalCustomer.getName().isBlank()) {
                error("El nombre del cliente es obligatorio.");
                target.add(feedbackPanel);
                return;
            }
            Customer updated = customerService.save(new Customer(modalCustomer.getId(), modalCustomer.getName(), modalCustomer.getEmail(), modalCustomer.getCompany(), modalCustomer.getTier(), modalCustomer.isActive()));
            editCustomerModal.hide(target);
            info("Cliente #" + updated.getId() + " (" + updated.getName() + ") actualizado exitosamente vía MicroProfile REST Client.");
            target.add(feedbackPanel, customerTableContainer);
        }));

        editForm.add(new AuraAjaxButton("btnCancelEdit", target -> {
            editCustomerModal.hide(target);
        }));

        editCustomerModal.add(editForm);
        add(editCustomerModal);

        // 3. DIÁLOGO MODAL: CONFIRMAR ELIMINACIÓN (AuraModal)
        deleteCustomerModal = new AuraModal("deleteCustomerModal", "🗑️ Confirmar Eliminación de Cliente");
        deleteCustomerModal.add(new AuraLabel("deleteCustomerMessage", () -> {
            if (selectedForDelete != null) {
                return "¿Está seguro de eliminar al cliente '" + selectedForDelete.getName() + "' (ID #" + selectedForDelete.getId() + ") del microservicio BackEndExample?";
            }
            return "¿Desea eliminar este cliente?";
        }));

        deleteCustomerModal.add(new AuraAjaxButton("btnConfirmDelete", target -> {
            if (selectedForDelete != null && selectedForDelete.getId() != null) {
                Long id = selectedForDelete.getId();
                String name = selectedForDelete.getName();
                customerService.delete(id);
                deleteCustomerModal.hide(target);
                warn("Cliente #" + id + " (" + name + ") eliminado del microservicio BackEndExample.");
                selectedForDelete = null;
                target.add(feedbackPanel, customerTableContainer);
            }
        }));

        deleteCustomerModal.add(new AuraAjaxButton("btnCancelDelete", target -> {
            deleteCustomerModal.hide(target);
            selectedForDelete = null;
        }));

        add(deleteCustomerModal);
    }
}
