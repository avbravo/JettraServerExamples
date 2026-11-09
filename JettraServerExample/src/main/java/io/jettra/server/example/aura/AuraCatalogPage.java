package io.jettra.server.example.aura;

import io.jettra.aura.annotation.AuraPath;
import io.jettra.aura.components.AuraButton;
import io.jettra.aura.components.AuraDropDownChoice;
import io.jettra.aura.components.AuraFeedbackPanel;
import io.jettra.aura.components.AuraForm;
import io.jettra.aura.components.AuraLabel;
import io.jettra.aura.components.AuraLink;
import io.jettra.aura.components.AuraListView;
import io.jettra.aura.components.AuraTextField;
import io.jettra.aura.core.AuraWebPage;
import io.jettra.aura.model.CompoundPropertyModel;
import io.jettra.aura.model.LoadableDetachableModel;
import io.jettra.aura.server.AuraApplication;
import io.jettra.server.example.model.Product;
import io.jettra.server.example.service.ProductService;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.List;

/**
 * Página de catálogo dinámico implementada con JettraAura.
 * Demuestra:
 * - AuraListView y AuraListItem para iterar sobre entidades JPA.
 * - Formulario tipado con CompoundPropertyModel para altas de productos.
 * - FeedbackPanel para mensajes flash.
 */
@AuraPath("/aura/catalogo")
public class AuraCatalogPage extends AuraTemplatePage {

    @Inject
    private ProductService productService;

    private final Product newProduct = new Product();

    public AuraCatalogPage() {
        setPageTitle("Catálogo Dinámico - JettraAura");

        // Resolver servicio si no fue inyectado por CDI
        resolveServices();

        // 1. Mensajes Flash de Retroalimentación
        add(new AuraFeedbackPanel("feedbackMessages"));

        // 2. Formulario de Alta con CompoundPropertyModel
        newProduct.setCategory("Hardware");
        newProduct.setPrice(100.0);
        newProduct.setStock(10);

        AuraForm<Product> form = new AuraForm<>("productForm", CompoundPropertyModel.of(newProduct), () -> {
            if (newProduct.getId() == null || newProduct.getId().isBlank()) {
                error("El código ID del producto es obligatorio.");
                return;
            }
            if (newProduct.getName() == null || newProduct.getName().isBlank()) {
                error("El nombre del producto no puede estar vacío.");
                return;
            }

            try {
                if (productService != null) {
                    productService.save(new Product(
                        newProduct.getId().trim(),
                        newProduct.getName().trim(),
                        newProduct.getCategory(),
                        newProduct.getPrice(),
                        newProduct.getStock()
                    ));
                    success("Producto " + newProduct.getId() + " - " + newProduct.getName() + " guardado exitosamente en JPA.");
                    // Limpiar formulario para nuevo registro
                    newProduct.setId("");
                    newProduct.setName("");
                    newProduct.setPrice(0.0);
                    newProduct.setStock(0);
                } else {
                    error("ProductService no disponible en este contexto.");
                }
            } catch (Exception e) {
                error("Error al guardar producto: " + e.getMessage());
            }
        });

        form.add(new AuraTextField<>("id"));
        form.add(new AuraTextField<>("name"));
        form.add(new AuraDropDownChoice<String>("category", List.of("Hardware", "Software", "Redes", "Almacenamiento")));
        form.add(new AuraTextField<Double>("price", Double.class));
        form.add(new AuraTextField<Integer>("stock", Integer.class));
        form.add(new AuraButton("btnSaveProduct"));
        add(form);

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

        // 4. Badge contador de productos
        add(new AuraLabel("catalogCountBadge", () -> productsModel.getObject().size() + " Productos en JPA"));

        // 5. Tabla iterativa AuraListView
        add(new AuraListView<Product>("productList", productsModel, item -> {
            Product p = item.getModelData();
            item.add(new AuraLabel("prodId", p.getId()));
            item.add(new AuraLabel("prodName", p.getName()));
            item.add(new AuraLabel("prodCategory", p.getCategory()));
            item.add(new AuraLabel("prodPrice", String.format("$%.2f", p.getPrice())));
            item.add(new AuraLabel("prodStock", String.valueOf(p.getStock())));

            // Enlace para eliminar producto
            item.add(new AuraLink("btnDeleteProduct", () -> {
                if (productService != null) {
                    productService.delete(p.getId());
                    success("Producto " + p.getId() + " eliminado.");
                    setResponsePage(AuraCatalogPage.class);
                }
            }));
        }));
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
                // Si el contenedor CDI de la aplicación tiene ProductService registrado
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
}
