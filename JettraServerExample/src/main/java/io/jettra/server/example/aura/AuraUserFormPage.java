package io.jettra.server.example.aura;

import io.jettra.aura.annotation.AuraPath;
import io.jettra.aura.components.AuraButton;
import io.jettra.aura.components.AuraCheckBox;
import io.jettra.aura.components.AuraDropDownChoice;
import io.jettra.aura.components.AuraFeedbackPanel;
import io.jettra.aura.components.AuraForm;
import io.jettra.aura.components.AuraLabel;
import io.jettra.aura.components.AuraTextArea;
import io.jettra.aura.components.AuraTextField;
import io.jettra.aura.core.AuraWebPage;
import io.jettra.aura.model.CompoundPropertyModel;
import java.io.Serializable;
import java.util.List;

/**
 * Ejemplo de formulario moderno con JettraAura.
 * Demuestra:
 * - CompoundPropertyModel para binding transparente de POJOs por aura:id.
 * - Validación y manejo de estados booleanos con AuraCheckBox.
 * - Selectores con AuraDropDownChoice.
 * - Áreas de texto con AuraTextArea.
 * - FeedbackPanel con mensajes automáticos de éxito/error.
 */
@AuraPath("/aura/formulario")
public class AuraUserFormPage extends AuraTemplatePage {

    public static class UserFormData implements Serializable {
        private String username = "jettraAdmin";
        private String email = "admin@jettra.io";
        private String role = "ADMIN";
        private String department = "Ingeniería";
        private boolean active = true;
        private boolean notificationsEnabled = true;
        private String notes = "Usuario administrador con privilegios de supervisión de clúster.";

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public boolean isNotificationsEnabled() { return notificationsEnabled; }
        public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    private final UserFormData formData = new UserFormData();

    public AuraUserFormPage() {
        setPageTitle("Formulario Reactivo - JettraAura");

        // 1. Mensajes de retroalimentación
        add(new AuraFeedbackPanel("feedback"));

        // 2. Formulario con CompoundPropertyModel
        AuraForm<UserFormData> form = new AuraForm<>("userForm", CompoundPropertyModel.of(formData), () -> {
            if (formData.getUsername() == null || formData.getUsername().trim().length() < 3) {
                error("El nombre de usuario debe contener al menos 3 caracteres.");
                return;
            }
            if (formData.getEmail() == null || !formData.getEmail().contains("@")) {
                error("Debe proporcionar una dirección de correo electrónico válida.");
                return;
            }

            success("Usuario '" + formData.getUsername() + "' (" + formData.getRole() + ") actualizado correctamente.");
        });

        form.add(new AuraTextField<>("username"));
        form.add(new AuraTextField<>("email"));
        form.add(new AuraDropDownChoice<String>("role", List.of("ADMIN", "OPERATOR", "USER", "AUDITOR")));
        form.add(new AuraDropDownChoice<String>("department", List.of("Ingeniería", "Operaciones", "Seguridad", "Finanzas")));
        form.add(new AuraCheckBox("active"));
        form.add(new AuraCheckBox("notificationsEnabled"));
        form.add(new AuraTextArea("notes"));
        form.add(new AuraButton("btnSaveUser"));
        add(form);

        // 3. Etiquetas de Vista Previa del Modelo
        add(new AuraLabel("previewUsername", () -> formData.getUsername()));
        add(new AuraLabel("previewEmail", () -> formData.getEmail()));
        add(new AuraLabel("previewRole", () -> formData.getRole()));
        add(new AuraLabel("previewDepartment", () -> formData.getDepartment()));
        add(new AuraLabel("previewStatus", () -> formData.isActive() ? "✅ Activo" : "❌ Inactivo"));
    }

    public UserFormData getFormData() {
        return formData;
    }
}
