package io.jettra.server.example.flux;

import io.jettra.flux.core.FluxView;
import io.jettra.flux.core.Modifier;
import io.jettra.flux.core.Widget;
import io.jettra.flux.theme.Themes;
import io.jettra.flux.widgets.Badge;
import io.jettra.flux.widgets.Column;
import io.jettra.flux.widgets.Header;
import io.jettra.flux.widgets.Link;
import io.jettra.flux.widgets.Paragraph;
import io.jettra.flux.widgets.RawHtml;
import io.jettra.flux.widgets.Row;

/**
 * Vista de demostración interactiva con sincronización en tiempo real vía JettraFlux.
 */
public class InteractiveCounterFluxView extends FluxView {

    private final int count;

    public InteractiveCounterFluxView() {
        this(42);
    }

    public InteractiveCounterFluxView(int count) {
        this.count = count;
    }

    @Override
    public Widget build() {
        Widget headerLeft = Column.of(
                Header.of(2, "🎛️ Demostración Interactiva JettraFlux")
                        .modifier(new Modifier().style("margin: 0; font-size: 1.25rem; color: var(--text-main);")),
                Paragraph.of("Control de estado reactivo bidireccional cliente-servidor en tiempo real")
                        .modifier(new Modifier().style("margin: 0.25rem 0 0 0; font-size: 0.85rem; color: var(--text-muted);"))
        );

        Widget stateBadge = Badge.warning("State Reactive Component")
                .modifier(new Modifier().style("font-weight: 700; padding: 0.35rem 0.75rem; border-radius: 6px; font-size: 0.8rem;"));

        Widget headerRow = Row.of(headerLeft, stateBadge)
                .modifier(new Modifier().style("display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border-subtle); padding-bottom: 1rem; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 0.5rem; width: 100%;"));

        Widget counterPanel = RawHtml.of("""
            <div style="text-align: center; padding: 2.5rem 1rem; background: var(--bg-subtle); border-radius: 12px; border: 1px solid var(--border-subtle); margin-bottom: 1.5rem;">
              <div style="font-size: 0.9rem; text-transform: uppercase; color: var(--text-muted); font-weight: 700; letter-spacing: 0.05em;">Estado Actual del Componente Flux</div>
              <div id="fluxCounterValue" style="font-size: 4rem; font-weight: 800; color: var(--primary); margin: 0.75rem 0; transition: transform 0.15s ease;">%d</div>
              <p style="color: var(--text-muted); font-size: 0.875rem; max-width: 460px; margin: 0 auto 1.5rem auto;">Los botones a continuación disparan eventos reactivos que mutan el estado instantáneamente sin recarga de página.</p>
              <div style="display: inline-flex; gap: 0.75rem; flex-wrap: wrap; justify-content: center;">
                <button type="button" class="app-btn-primary" style="font-size: 1.05rem; padding: 0.6rem 1.25rem;" onclick="changeCounter(1)">➕ Incrementar (+1)</button>
                <button type="button" class="app-btn-secondary" style="font-size: 1.05rem; padding: 0.6rem 1.25rem;" onclick="changeCounter(-1)">➖ Decrementar (-1)</button>
                <button type="button" class="app-btn-secondary" style="font-size: 1.05rem; padding: 0.6rem 1.25rem; color: var(--danger); border-color: var(--danger);" onclick="resetCounter()">🔄 Reiniciar (0)</button>
              </div>
            </div>
            <script>
              let currentCount = %d;
              function changeCounter(delta) {
                currentCount += delta;
                const el = document.getElementById('fluxCounterValue');
                if (el) {
                  el.textContent = currentCount;
                  el.style.transform = 'scale(1.1)';
                  setTimeout(() => el.style.transform = 'scale(1.0)', 150);
                }
              }
              function resetCounter() {
                currentCount = 0;
                const el = document.getElementById('fluxCounterValue');
                if (el) el.textContent = '0';
              }
            </script>
            """.formatted(count, count));

        Widget linkCatalog = Link.of("/flux/catalogo", "Ir al Catálogo Reactivo (Flux)")
                .modifier(new Modifier().cssClass("app-btn-secondary").style("text-decoration:none;"));

        return Column.of(headerRow, counterPanel, Row.of(linkCatalog))
                .modifier(new Modifier().style("width: 100%; display: flex; flex-direction: column; gap: 0.5rem;"));
    }

    public String renderSectionHtml() {
        return build().render(Themes.Core());
    }
}
