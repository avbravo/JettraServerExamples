package io.jettra.server.example.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entidad de dominio Cliente para consumo del microservicio BackEndExample
 * mediante Eclipse MicroProfile Rest Client y autenticación Jettra JWT.
 */
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String email;
    private String company;
    private String tier = "STANDARD";
    private boolean active = true;

    public Customer() {}

    public Customer(String name, String email, String company, String tier, boolean active) {
        this(null, name, email, company, tier, active);
    }

    public Customer(Long id, String name, String email, String company, String tier, boolean active) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.company = company;
        this.tier = (tier != null && !tier.isBlank()) ? tier : "STANDARD";
        this.active = active;
    }

    public static Customer parse(String idStr, String name, String email, String company, String tier, boolean active) {
        Long parsedId = null;
        if (idStr != null && !idStr.isBlank()) {
            try {
                parsedId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException ignored) {}
        }
        return new Customer(parsedId, name, email, company, tier, active);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = (tier != null && !tier.isBlank()) ? tier : "STANDARD";
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", company='" + company + '\'' +
                ", tier='" + tier + '\'' +
                ", active=" + active +
                '}';
    }
}
