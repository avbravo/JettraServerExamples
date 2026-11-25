package io.jettra.server.example.bean;

import io.jettra.server.example.model.Customer;
import io.jettra.server.example.service.CustomerClientService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Managed Bean Jakarta Faces / CDI para la gestión CRUD de Clientes.
 * Se comunica mediante Eclipse MicroProfile Rest Client con BackEndExample.
 */
@Named("customerBean")
@SessionScoped
public class CustomerBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private CustomerClientService customerService;

    private List<Customer> customers = new ArrayList<>();
    private Customer customer = new Customer();
    private String filterTerm = "";
    private String message = null;
    private String messageSeverity = "info";

    @PostConstruct
    public void init() {
        if (customerService == null) {
            customerService = new CustomerClientService();
        }
        refreshList();
    }

    public void refreshList() {
        if (customerService != null) {
            this.customers = customerService.listAll();
        }
    }

    public void openNew() {
        this.customer = new Customer();
        this.message = null;
    }

    public void edit(Customer c) {
        if (c != null) {
            this.customer = new Customer(c.getId(), c.getName(), c.getEmail(), c.getCompany(), c.getTier(), c.isActive());
        }
    }

    public void save() {
        if (customer != null && customer.getName() != null && !customer.getName().isBlank()) {
            customerService.save(customer);
            refreshList();
            this.message = "Cliente '" + customer.getName() + "' guardado exitosamente vía MicroProfile Rest Client.";
            this.messageSeverity = "success";
            this.customer = new Customer();
        } else {
            this.message = "El nombre del cliente es obligatorio.";
            this.messageSeverity = "error";
        }
    }

    public void delete(Customer c) {
        if (c != null && c.getId() != null) {
            customerService.delete(c.getId());
            refreshList();
            this.message = "Cliente eliminado con éxito.";
            this.messageSeverity = "warn";
        }
    }

    public List<Customer> getFilteredCustomers() {
        if (filterTerm == null || filterTerm.isBlank()) {
            return customers;
        }
        String term = filterTerm.trim().toLowerCase();
        return customers.stream()
                .filter(c -> (c.getName() != null && c.getName().toLowerCase().contains(term))
                        || (c.getEmail() != null && c.getEmail().toLowerCase().contains(term))
                        || (c.getCompany() != null && c.getCompany().toLowerCase().contains(term))
                        || (c.getTier() != null && c.getTier().toLowerCase().contains(term)))
                .collect(Collectors.toList());
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getFilterTerm() {
        return filterTerm;
    }

    public void setFilterTerm(String filterTerm) {
        this.filterTerm = filterTerm;
    }

    public String getMessage() {
        return message;
    }

    public String getMessageSeverity() {
        return messageSeverity;
    }

    public String getBackendUrl() {
        return customerService != null ? customerService.resolveBackendUrl() : "http://localhost:8081";
    }

    public String getJwtTokenPreview() {
        if (customerService == null) return "No disponible";
        String token = customerService.getActiveJwtToken();
        if (token == null) return "No autenticado";
        if (token.length() > 35) {
            return token.substring(0, 30) + "...";
        }
        return token;
    }
}

