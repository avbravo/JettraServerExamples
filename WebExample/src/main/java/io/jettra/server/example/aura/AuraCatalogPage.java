package io.jettra.server.example.aura;

import io.jettra.aura.ajax.AuraAjaxButton;
import io.jettra.aura.ajax.AuraAjaxLink;
import io.jettra.aura.ajax.AuraAjaxRequestTarget;
import io.jettra.aura.annotation.AuraPath;
import io.jettra.aura.components.AuraButton;
import io.jettra.aura.components.AuraDropDownChoice;
import io.jettra.aura.components.AuraFeedbackPanel;
import io.jettra.aura.components.AuraForm;
import io.jettra.aura.components.AuraLabel;
import io.jettra.aura.components.AuraListView;
import io.jettra.aura.components.AuraModal;
import io.jettra.aura.components.AuraTextField;
import io.jettra.aura.core.AuraContainer;
import io.jettra.aura.model.CompoundPropertyModel;
import io.jettra.aura.model.LoadableDetachableModel;
import io.jettra.aura.server.AuraApplication;
import io.jettra.server.example.model.Product;
import io.jettra.server.example.service.ProductService;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.List;

/**
 * Página de catálogo dinámico implementada con JettraAura (variante optimizada de JettraFlux
 * para plantillas .html y Jakarta Faces .xhtml).
 * Demuestra:
 * - AuraListView y AuraListItem para iterar sobre entidades JPA con eventos por fila.
 * - Operaciones completas CRUD: Guardar (Nuevo), Editar y Eliminar productos.
 * - Tres diálogos reactivos AuraModal:
 *   1. registerModal: Diálogo para Registrar Datos de Nuevo Producto.
 *   2. productModal: Diálogo para Editar Producto existente (y creación modal).
 *   3. deleteModal: Diálogo de Confirmación de Eliminación de Producto.
 * - Formulario tipado con CompoundPropertyModel y actualizaciones reactivas con AuraAjaxRequestTarget.
 * - AuraFeedbackPanel para mensajes flash.
 */
@AuraPath("/aura/catalogo")
public class AuraCatalogPage extends AuraTemplatePage {

    private static final List<String> CATEGORIES = List.of("Hardware", "Software", "Redes", "Almacenamiento", "Add-on");

    @Inject
    private ProductService productService;

    private final Product newProduct = new Product("", "", "Hardware", 100.0, 10);
    private final Product registerProduct = new Product("", "", "Hardware", 100.0, 10);
    private final Product modalProduct = new Product("", "", "Hardware", 100.0, 10);
    private Product selectedForDelete;
    private boolean editMode = false;

    private AuraFeedbackPanel feedbackPanel;
    private AuraContainer catalogContainer;
    private AuraModal registerModal;
    private AuraModal productModal;
    private AuraModal deleteModal;

    public AuraCatalogPage() {
        setPageTitle("Catálogo Dinámico - JettraAura");

        // Resolver servicio si no fue inyectado por CDI
        resolveServices();

        // 1. Mensajes Flash de Retroalimentación
        feedbackPanel = new AuraFeedbackPanel("feedbackMessages");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        // 2. Formulario de Alta Rápida en línea con CompoundPropertyModel + botón para abrir diálogo de registro
        AuraForm<Product> inlineForm = new AuraForm<>("productForm", CompoundPropertyModel.of(newProduct), () -> {
            saveProductModel(newProduct, false);
        });
        inlineForm.setOutputMarkupId(true);

        inlineForm.add(new AuraTextField<>("id"));
        inlineForm.add(new AuraTextField<>("name"));
        inlineForm.add(new AuraDropDownChoice<>("category", CATEGORIES));
        inlineForm.add(new AuraTextField<>("price", Double.class));
        inlineForm.add(new AuraTextField<>("stock", Integer.class));
        inlineForm.add(new AuraButton("btnSaveProduct"));

        AuraAjaxButton btnOpenRegisterModal = new AuraAjaxButton("btnOpenRegisterModal", target -> {
            resetRegisterProduct();
            registerModal.show(target);
        });
        btnOpenRegisterModal.setDefaultFormProcessing(false);
        inlineForm.add(btnOpenRegisterModal);
        add(inlineForm);

        // 3. Modelo desacoplado para la lista de productos
        LoadableDetachableModel<List<Product>> productsModel = new LoadableDetachableModel<>() {
            @Override
            protected List<Product> load() {
                ProductService ps = getProductService();
                if (ps != null) {
                    try {
                        return ps.listAll();
                    } catch (Exception ignored) {}
                }
                return Collections.emptyList();
            }
        };

        // 4. Contenedor reactivo de la tabla de catálogo
        catalogContainer = new AuraContainer("catalogContainer");
        catalogContainer.setOutputMarkupId(true);

        catalogContainer.add(new AuraLabel("catalogCountBadge", () -> productsModel.getObject().size() + " Productos en JPA"));

        // Botón en la cabecera de la tabla para abrir el diálogo de Registrar Nuevo Producto
        AuraAjaxButton btnOpenCreateModal = new AuraAjaxButton("btnOpenCreateModal", target -> {
            resetRegisterProduct();
            registerModal.show(target);
        });
        btnOpenCreateModal.setDefaultFormProcessing(false);
        catalogContainer.add(btnOpenCreateModal);

        // 5. Tabla iterativa AuraListView con acciones Editar y Eliminar con diálogos AuraModal
        catalogContainer.add(new AuraListView<Product>("productList", productsModel, item -> {
            Product p = item.getModelData();
            item.add(new AuraLabel("prodId", p.getId()));
            item.add(new AuraLabel("prodName", p.getName()));
            item.add(new AuraLabel("prodCategory", p.getCategory()));
            item.add(new AuraLabel("prodPrice", String.format("$%.2f", p.getPrice())));
            item.add(new AuraLabel("prodStock", String.valueOf(p.getStock())));

            // Acción Editar: abre el diálogo AuraModal de edición precargado con los datos del producto
            item.add(new AuraAjaxLink("btnEditProduct", target -> {
                editMode = true;
                modalProduct.setId(p.getId());
                modalProduct.setName(p.getName());
                modalProduct.setCategory(p.getCategory() != null ? p.getCategory() : "Hardware");
                modalProduct.setPrice(p.getPrice());
                modalProduct.setStock(p.getStock());
                productModal.show(target);
            }));

            // Acción Eliminar: abre el diálogo AuraModal de confirmación de eliminación
            item.add(new AuraAjaxLink("btnDeleteProduct", target -> {
                selectedForDelete = new Product(p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getStock());
                deleteModal.show(target);
            }));
        }));

        add(catalogContainer);

        // 6. Diálogo AuraModal dedicado para REGISTRAR DATOS (Nuevo Producto)
        registerModal = new AuraModal("registerModal", "➕ Registrar Nuevo Producto");
        registerModal.add(new AuraLabel("registerModalBadge", "MODO CREACIÓN • NUEVO REGISTRO EN CATÁLOGO JPA"));

        AuraForm<Product> registerForm = new AuraForm<>("registerProductForm", CompoundPropertyModel.of(registerProduct), () -> {
            if (saveProductModel(registerProduct, false)) {
                resetRegisterProduct();
                registerModal.hide();
            }
        });
        registerForm.add(new AuraTextField<>("id"));
        registerForm.add(new AuraTextField<>("name"));
        registerForm.add(new AuraDropDownChoice<>("category", CATEGORIES));
        registerForm.add(new AuraTextField<>("price", Double.class));
        registerForm.add(new AuraTextField<>("stock", Integer.class));

        registerForm.add(new AuraAjaxButton("btnRegisterSave", target -> {
            if (saveProductModel(registerProduct, false)) {
                resetRegisterProduct();
                registerModal.hide(target);
                refreshCatalogAjax(target);
            } else {
                target.add(feedbackPanel);
            }
        }));

        AuraAjaxButton btnRegisterCancel = new AuraAjaxButton("btnRegisterCancel", target -> {
            registerModal.hide(target);
        });
        btnRegisterCancel.setDefaultFormProcessing(false);
        registerForm.add(btnRegisterCancel);

        registerModal.add(registerForm);
        add(registerModal);

        // 7. Diálogo AuraModal para EDITAR Producto (también soporta guardar nuevos registros)
        productModal = new AuraModal("productModal", () -> (editMode && modalProduct.getId() != null && !modalProduct.getId().isBlank())
                ? "✏️ Editar Producto (" + modalProduct.getId() + ")"
                : "✏️ Editar / Guardar Producto");

        productModal.add(new AuraLabel("modalModeBadge", () -> editMode
                ? "MODO EDICIÓN • ACTUALIZACIÓN TRANSACCIONAL JPA"
                : "MODO CREACIÓN / EDICIÓN • PERSISTENCIA JPA"));

        AuraForm<Product> modalForm = new AuraForm<>("modalProductForm", CompoundPropertyModel.of(modalProduct), () -> {
            if (saveProductModel(modalProduct, editMode)) {
                productModal.hide();
            }
        });

        modalForm.add(new AuraTextField<>("id"));
        modalForm.add(new AuraTextField<>("name"));
        modalForm.add(new AuraDropDownChoice<>("category", CATEGORIES));
        modalForm.add(new AuraTextField<>("price", Double.class));
        modalForm.add(new AuraTextField<>("stock", Integer.class));

        modalForm.add(new AuraAjaxButton("btnModalSave", target -> {
            if (saveProductModel(modalProduct, editMode)) {
                productModal.hide(target);
                refreshCatalogAjax(target);
            } else {
                target.add(feedbackPanel);
            }
        }));

        AuraAjaxButton btnModalCancel = new AuraAjaxButton("btnModalCancel", target -> {
            productModal.hide(target);
        });
        btnModalCancel.setDefaultFormProcessing(false);
        modalForm.add(btnModalCancel);

        productModal.add(modalForm);
        add(productModal);

        // 8. Diálogo AuraModal para CONFIRMAR ELIMINACIÓN
        deleteModal = new AuraModal("deleteModal", "🗑️ Confirmar Eliminación de Producto");

        deleteModal.add(new AuraLabel("deleteConfirmMessage", () -> selectedForDelete != null
                ? "¿Está seguro que desea eliminar el producto \"" + selectedForDelete.getName() + "\" (" + selectedForDelete.getId() + ")?"
                : "¿Está seguro que desea eliminar el producto seleccionado?"));

        deleteModal.add(new AuraLabel("deleteProductDetails", () -> selectedForDelete != null
                ? "Categoría: " + selectedForDelete.getCategory()
                  + " | Precio: $" + String.format("%.2f", selectedForDelete.getPrice())
                  + " | Stock: " + selectedForDelete.getStock() + " unidades"
                : ""));

        deleteModal.add(new AuraAjaxButton("btnConfirmDelete", target -> {
            ProductService ps = getProductService();
            if (selectedForDelete != null && ps != null) {
                String deletedId = selectedForDelete.getId();
                String deletedName = selectedForDelete.getName();
                ps.delete(deletedId);
                success("Producto " + deletedId + " (" + deletedName + ") eliminado del catálogo JPA.");
                selectedForDelete = null;
            } else {
                error("No se pudo eliminar el producto seleccionado.");
            }
            deleteModal.hide(target);
            refreshCatalogAjax(target);
        }));

        AuraAjaxButton btnCancelDelete = new AuraAjaxButton("btnCancelDelete", target -> {
            selectedForDelete = null;
            deleteModal.hide(target);
        });
        btnCancelDelete.setDefaultFormProcessing(false);
        deleteModal.add(btnCancelDelete);

        add(deleteModal);
    }

    private void resetRegisterProduct() {
        int nextNum = 10;
        ProductService ps = getProductService();
        if (ps != null) {
            try {
                nextNum = ps.listAll().size() + 1;
            } catch (Exception ignored) {}
        }
        registerProduct.setId(String.format("PRD-%02d", nextNum));
        registerProduct.setName("");
        registerProduct.setCategory("Hardware");
        registerProduct.setPrice(100.0);
        registerProduct.setStock(10);
    }

    private boolean saveProductModel(Product source, boolean isEdit) {
        if (source.getId() == null || source.getId().isBlank()) {
            error("El código ID del producto es obligatorio.");
            return false;
        }
        if (source.getName() == null || source.getName().isBlank()) {
            error("El nombre del producto no puede estar vacío.");
            return false;
        }

        try {
            ProductService ps = getProductService();
            if (ps != null) {
                String cleanId = source.getId().trim();
                String cleanName = source.getName().trim();
                String cleanCategory = (source.getCategory() != null && !source.getCategory().isBlank())
                        ? source.getCategory().trim()
                        : "Hardware";
                ps.save(new Product(
                    cleanId,
                    cleanName,
                    cleanCategory,
                    source.getPrice(),
                    source.getStock()
                ));
                if (isEdit) {
                    success("Producto " + cleanId + " - " + cleanName + " actualizado exitosamente en JPA.");
                } else {
                    success("Producto " + cleanId + " - " + cleanName + " guardado exitosamente en JPA.");
                }
                if (source == newProduct) {
                    newProduct.setId("");
                    newProduct.setName("");
                    newProduct.setPrice(0.0);
                    newProduct.setStock(0);
                }
                return true;
            } else {
                error("ProductService no disponible en este contexto.");
                return false;
            }
        } catch (Exception e) {
            error("Error al guardar producto: " + e.getMessage());
            return false;
        }
    }

    private void refreshCatalogAjax(AuraAjaxRequestTarget target) {
        if (target != null) {
            target.add(feedbackPanel, catalogContainer);
        }
    }

    public ProductService getProductService() {
        if (this.productService == null) {
            resolveServices();
        }
        return this.productService;
    }

    private void resolveServices() {
        if (this.productService == null && AuraApplication.get() != null) {
            try {
                var cdi = AuraApplication.get().getCdiContainer();
                if (cdi != null) {
                    this.productService = cdi.getBean(ProductService.class);
                }
            } catch (Exception ignored) {}
        }
    }

    public AuraCatalogPage setProductService(ProductService productService) {
        this.productService = productService;
        return this;
    }

    public AuraModal getRegisterModal() {
        return registerModal;
    }

    public AuraModal getProductModal() {
        return productModal;
    }

    public AuraModal getDeleteModal() {
        return deleteModal;
    }

    public Product getRegisterProduct() {
        return registerProduct;
    }

    public Product getModalProduct() {
        return modalProduct;
    }

    public Product getSelectedForDelete() {
        return selectedForDelete;
    }
}
