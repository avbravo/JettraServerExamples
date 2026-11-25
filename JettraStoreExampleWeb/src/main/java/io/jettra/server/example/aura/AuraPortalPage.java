package io.jettra.server.example.aura;

import io.jettra.aura.annotation.AuraPath;
import io.jettra.aura.components.AuraLabel;
import io.jettra.aura.core.AuraWebPage;
import io.jettra.server.core.JettraServer;
import io.jettra.server.example.service.ProductService;
import jakarta.inject.Inject;

/**
 * Página principal del portal de ejemplos JettraAura dentro de WebExample.
 * Demuestra la vinculación 1:1 entre AuraPortalPage.html y AuraPortalPage.java.
 */
@AuraPath("/aura/demo")
public class AuraPortalPage extends AuraTemplatePage {

    @Inject
    private ProductService productService;

    public AuraPortalPage() {
        setPageTitle("JettraAura Portal - Ejemplos de Arquitectura Web");

        add(new AuraLabel("welcomeTitle", "JettraAura: Arquitectura Web Orientada a Componentes"));
        add(new AuraLabel("welcomeDescription", 
            "Bienvenido a la suite de demostración interactiva de JettraAura. Cada página HTML está vinculada 1:1 " +
            "a una clase Java limpia y fuertemente tipada, ejecutándose de manera nativa sobre Virtual Threads de Project Loom."
        ));

        // Obtener métricas del servidor
        JettraServer server = JettraServer.getInstance();
        String storageMode = (server != null && server.storage() != null) 
            ? server.storage().getMode().name() 
            : "JVM_RAM";
        int cpuCores = Runtime.getRuntime().availableProcessors();

        add(new AuraLabel("storageModeValue", storageMode));
        add(new AuraLabel("cpuCoresValue", String.valueOf(cpuCores)));

        // Total de productos en JPA (dinámico durante el render)
        add(new AuraLabel("totalProductsValue", () -> {
            ProductService ps = getProductService();
            if (ps != null) {
                try {
                    return String.valueOf(ps.listAll().size());
                } catch (Exception ignored) {}
            }
            return "0";
        }));

        add(new AuraLabel("footerEngineInfo", "JettraServer • JettraAura 1.0.0-SNAPSHOT • Java " + System.getProperty("java.version")));
        add(new AuraLabel("footerUptime", "Memoria JVM: " + (Runtime.getRuntime().totalMemory() / (1024 * 1024)) + " MB"));
    }

    public ProductService getProductService() {
        if (productService == null && io.jettra.aura.server.AuraApplication.get() != null) {
            try {
                var cdi = io.jettra.aura.server.AuraApplication.get().getCdiContainer();
                if (cdi != null) {
                    this.productService = cdi.getBean(ProductService.class);
                }
            } catch (Exception ignored) {}
        }
        return productService;
    }

    public AuraPortalPage setProductService(ProductService productService) {
        this.productService = productService;
        return this;
    }
}
