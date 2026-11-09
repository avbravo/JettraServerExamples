package io.jettra.server.example.aura;

import io.jettra.aura.ajax.AuraAjaxButton;
import io.jettra.aura.annotation.AuraPath;
import io.jettra.aura.components.AuraLabel;
import io.jettra.aura.core.AuraWebPage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Ejemplo de interactividad reactiva parcial vía AJAX en JettraAura.
 * Demuestra:
 * - AuraAjaxButton para procesar clics de usuario sin recargar la página.
 * - AuraAjaxRequestTarget para actualizar selectivamente componentes del DOM (setOutputMarkupId).
 * - Ejecución ligera y concurrente sobre Virtual Threads en el backend.
 */
@AuraPath("/aura/contador")
public class AuraCounterPage extends AuraTemplatePage {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private int counter = 0;
    private String log = "Listo para procesar eventos AJAX en JettraServer.";

    public AuraCounterPage() {
        setPageTitle("Contador Reactivo AJAX - JettraAura");

        // 1. Etiqueta del contador con MarkupId habilitado para actualizaciones parciales
        AuraLabel counterLabel = new AuraLabel("counterValue", () -> String.valueOf(counter));
        counterLabel.setOutputMarkupId(true);
        add(counterLabel);

        // 2. Registro de log de eventos
        AuraLabel logLabel = new AuraLabel("logMessage", () -> log);
        logLabel.setOutputMarkupId(true);
        add(logLabel);

        // 3. Botones AJAX reactivos
        add(new AuraAjaxButton("btnIncrement", target -> {
            counter++;
            log = "➕ Contador incrementado a " + counter + " a las " + LocalTime.now().format(TIME_FMT);
            target.add(counterLabel);
            target.add(logLabel);
        }));

        add(new AuraAjaxButton("btnDecrement", target -> {
            counter--;
            log = "➖ Contador decrementado a " + counter + " a las " + LocalTime.now().format(TIME_FMT);
            target.add(counterLabel);
            target.add(logLabel);
        }));

        add(new AuraAjaxButton("btnDouble", target -> {
            counter *= 2;
            log = "✖️ Contador duplicado a " + counter + " a las " + LocalTime.now().format(TIME_FMT);
            target.add(counterLabel);
            target.add(logLabel);
        }));

        add(new AuraAjaxButton("btnReset", target -> {
            counter = 0;
            log = "🔄 Contador reseteado a 0 a las " + LocalTime.now().format(TIME_FMT);
            target.add(counterLabel);
            target.add(logLabel);
        }));
    }

    public int getCounter() {
        return counter;
    }
}

