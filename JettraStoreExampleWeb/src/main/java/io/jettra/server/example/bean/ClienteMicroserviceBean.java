package io.jettra.server.example.bean;

import io.jettra.server.example.model.ClienteDto;
import io.jettra.server.example.service.ClienteMicroserviceClientService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Managed Bean Jakarta Faces / CDI para operaciones CRUD de Clientes ({@code example_factura_db})
 * consumiendo {@code /api/v1/clientes} (@RestRepository) vía MicroProfile RestClient + JWT.
 */
@Named("clienteMsBean")
@SessionScoped
public class ClienteMicroserviceBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ClienteMicroserviceClientService clienteService;

    private List<ClienteDto> clientes = new ArrayList<>();
    private String editId;
    private String ruc = "";
    private String nombre = "";
    private String email = "";
    private String categoria = "ENTERPRISE";
    private boolean activo = true;
    private String filterTerm = "";
    private String message;
    private String messageSeverity = "info";

    @PostConstruct
    public void init() {
        if (clienteService == null) {
            clienteService = new ClienteMicroserviceClientService();
        }
        refreshList();
    }

    public void setClienteService(ClienteMicroserviceClientService clienteService) {
        this.clienteService = clienteService;
    }

    public void refreshList() {
        if (clienteService != null) {
            this.clientes = clienteService.listAll();
        }
    }

    public void openNew() {
        this.editId = null;
        this.ruc = "";
        this.nombre = "";
        this.email = "";
        this.categoria = "ENTERPRISE";
        this.activo = true;
        this.message = null;
    }

    public void edit(ClienteDto c) {
        if (c != null) {
            this.editId = c.jettraId();
            this.ruc = c.ruc();
            this.nombre = c.nombre();
            this.email = c.email();
            this.categoria = c.categoria();
            this.activo = c.activo();
        }
    }

    public void save() {
        if (nombre == null || nombre.isBlank() || ruc == null || ruc.isBlank() || email == null || !email.contains("@")) {
            this.message = "RUC, nombre y email válido son obligatorios.";
            this.messageSeverity = "error";
            return;
        }
        ClienteDto dto = new ClienteDto(editId, ruc.trim(), nombre.trim(), email.trim(), categoria, activo);
        if (editId == null || editId.isBlank()) {
            ClienteDto created = clienteService.create(dto);
            this.message = "Cliente '" + created.nombre() + "' registrado vía RestClient + JWT.";
        } else {
            ClienteDto updated = clienteService.update(editId, dto);
            this.message = "Cliente '" + updated.nombre() + "' actualizado vía RestClient + JWT.";
        }
        this.messageSeverity = "success";
        refreshList();
        openNew();
    }

    public void delete(ClienteDto c) {
        if (c != null && c.jettraId() != null) {
            clienteService.delete(c.jettraId());
            refreshList();
            this.message = "Cliente '" + c.nombre() + "' eliminado.";
            this.messageSeverity = "warn";
        }
    }

    public List<ClienteDto> getFilteredClientes() {
        if (filterTerm == null || filterTerm.isBlank()) return clientes;
        String q = filterTerm.trim().toLowerCase();
        return clientes.stream()
                .filter(c -> (c.nombre() != null && c.nombre().toLowerCase().contains(q))
                        || (c.ruc() != null && c.ruc().toLowerCase().contains(q))
                        || (c.email() != null && c.email().toLowerCase().contains(q))
                        || (c.categoria() != null && c.categoria().toLowerCase().contains(q)))
                .toList();
    }

    public List<ClienteDto> getClientes() { return clientes; }
    public String getEditId() { return editId; }
    public void setEditId(String editId) { this.editId = editId; }
    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public String getFilterTerm() { return filterTerm; }
    public void setFilterTerm(String filterTerm) { this.filterTerm = filterTerm; }
    public String getMessage() { return message; }
    public String getMessageSeverity() { return messageSeverity; }
    public String getBackendUrl() { return clienteService != null ? clienteService.resolveBackendUrl() : "http://localhost:8083"; }
}
