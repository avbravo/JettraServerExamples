package io.jettra.server.example.bean;

import io.jettra.server.example.model.DetalleFacturaDto;
import io.jettra.server.example.model.FacturaDto;
import io.jettra.server.example.service.FacturaClientService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Managed Bean Jakarta Faces / CDI para operaciones CRUD de Facturas ({@code example_factura_db})
 * consumiendo {@code JettraStoreExampleMicroservices} vía MicroProfile RestClient + JWT.
 */
@Named("facturaBean")
@SessionScoped
public class FacturaBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private FacturaClientService facturaService;

    private List<FacturaDto> facturas = new ArrayList<>();
    private String editId;
    private String folio = "";
    private String clienteId = "CLI-001";
    private String clienteNombre = "";
    private String estado = "EMITIDA";
    private String itemSku = "SRV-01";
    private String itemDescripcion = "Servicio JettraStore";
    private int itemCantidad = 1;
    private double itemPrecio = 100.00;
    private String filterTerm = "";
    private String message;
    private String messageSeverity = "info";

    @PostConstruct
    public void init() {
        if (facturaService == null) {
            facturaService = new FacturaClientService();
        }
        refreshList();
    }

    public void setFacturaService(FacturaClientService facturaService) {
        this.facturaService = facturaService;
    }

    public void refreshList() {
        if (facturaService != null) {
            this.facturas = facturaService.listAll();
        }
    }

    public void openNew() {
        this.editId = null;
        this.folio = "";
        this.clienteId = "CLI-001";
        this.clienteNombre = "";
        this.estado = "EMITIDA";
        this.itemSku = "SRV-01";
        this.itemDescripcion = "Servicio JettraStore";
        this.itemCantidad = 1;
        this.itemPrecio = 100.00;
        this.message = null;
    }

    public void edit(FacturaDto f) {
        if (f != null) {
            this.editId = f.jettraId();
            this.folio = f.folio();
            this.clienteId = f.clienteId();
            this.clienteNombre = f.clienteNombre();
            this.estado = f.estado();
            if (f.detalles() != null && !f.detalles().isEmpty()) {
                DetalleFacturaDto d = f.detalles().get(0);
                this.itemSku = d.sku();
                this.itemDescripcion = d.descripcion();
                this.itemCantidad = d.cantidad();
                this.itemPrecio = d.precioUnitario();
            }
        }
    }

    public void save() {
        if (itemDescripcion == null || itemDescripcion.isBlank() || itemCantidad <= 0 || itemPrecio < 0) {
            this.message = "Debe indicar descripción, cantidad > 0 y precio válido.";
            this.messageSeverity = "error";
            return;
        }
        DetalleFacturaDto item = DetalleFacturaDto.of(
                itemSku != null && !itemSku.isBlank() ? itemSku.trim() : "ITEM-01",
                itemDescripcion.trim(), itemCantidad, itemPrecio);
        double sub = item.subtotal();
        double tax = Math.round(sub * 0.07 * 100.0) / 100.0;
        double tot = Math.round((sub + tax) * 100.0) / 100.0;
        FacturaDto dto = new FacturaDto(editId, folio, clienteId, clienteNombre,
                LocalDate.now().toString(), estado, List.of(item), sub, tax, tot);

        if (editId == null || editId.isBlank()) {
            FacturaDto created = facturaService.create(dto);
            this.message = "Factura '" + created.folio() + "' emitida vía RestClient + JWT.";
        } else {
            FacturaDto updated = facturaService.update(editId, dto);
            this.message = "Factura '" + updated.folio() + "' actualizada vía RestClient + JWT.";
        }
        this.messageSeverity = "success";
        refreshList();
        openNew();
    }

    public void anular(FacturaDto f) {
        if (f != null && f.jettraId() != null) {
            facturaService.anular(f.jettraId());
            refreshList();
            this.message = "Factura '" + f.folio() + "' anulada.";
            this.messageSeverity = "warn";
        }
    }

    public void delete(FacturaDto f) {
        if (f != null && f.jettraId() != null) {
            facturaService.delete(f.jettraId());
            refreshList();
            this.message = "Factura '" + f.folio() + "' eliminada.";
            this.messageSeverity = "warn";
        }
    }

    public List<FacturaDto> getFilteredFacturas() {
        if (filterTerm == null || filterTerm.isBlank()) {
            return facturas;
        }
        String q = filterTerm.trim().toLowerCase();
        return facturas.stream()
                .filter(f -> (f.folio() != null && f.folio().toLowerCase().contains(q))
                        || (f.clienteNombre() != null && f.clienteNombre().toLowerCase().contains(q))
                        || (f.estado() != null && f.estado().toLowerCase().contains(q)))
                .toList();
    }

    public List<FacturaDto> getFacturas() { return facturas; }
    public String getEditId() { return editId; }
    public void setEditId(String editId) { this.editId = editId; }
    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getItemSku() { return itemSku; }
    public void setItemSku(String itemSku) { this.itemSku = itemSku; }
    public String getItemDescripcion() { return itemDescripcion; }
    public void setItemDescripcion(String itemDescripcion) { this.itemDescripcion = itemDescripcion; }
    public int getItemCantidad() { return itemCantidad; }
    public void setItemCantidad(int itemCantidad) { this.itemCantidad = itemCantidad; }
    public double getItemPrecio() { return itemPrecio; }
    public void setItemPrecio(double itemPrecio) { this.itemPrecio = itemPrecio; }
    public String getFilterTerm() { return filterTerm; }
    public void setFilterTerm(String filterTerm) { this.filterTerm = filterTerm; }
    public String getMessage() { return message; }
    public String getMessageSeverity() { return messageSeverity; }
    public String getBackendUrl() { return facturaService != null ? facturaService.resolveBackendUrl() : "http://localhost:8083"; }
}
