package io.jettra.example.microservices.service;

import io.jettra.example.microservices.model.Cliente;
import io.jettra.example.microservices.model.DetalleFactura;
import io.jettra.example.microservices.model.Factura;
import io.jettra.example.microservices.repository.ClienteRepository;
import io.jettra.example.microservices.repository.FacturaRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Capa de servicio de negocio para la facturación electrónica sobre {@code example_factura_db}.
 * Calcula subtotales, impuesto ITBMS/IVA (7%) y totales, genera folios y controla anulaciones.
 */
public class FacturaService {

    private static final double TAX_RATE = 0.07;
    private final FacturaRepository facturaRepository;
    private final ClienteRepository clienteRepository;
    private final AtomicLong folioSeq = new AtomicLong(1000);

    public FacturaService(FacturaRepository facturaRepository, ClienteRepository clienteRepository) {
        this.facturaRepository = facturaRepository;
        this.clienteRepository = clienteRepository;
    }

    public void seedDefaultsIfEmpty() {
        if (facturaRepository.count() == 0) {
            List<Cliente> clientes = clienteRepository.findAll();
            String cliId = clientes.isEmpty() ? "CLI-DEFAULT" : clientes.get(0).jettraId();
            String cliName = clientes.isEmpty() ? "Acme Corporation S.A." : clientes.get(0).nombre();
            emitirFactura(new Factura(
                    null,
                    "FAC-2026-0001",
                    cliId,
                    cliName,
                    LocalDate.now().toString(),
                    "EMITIDA",
                    List.of(
                            DetalleFactura.of("SRV-JETTRA-01", "Licencia JettraStore Cluster Enterprise", 1, 1200.00),
                            DetalleFactura.of("SUP-24X7", "Soporte 24x7 Clúster Raft", 2, 150.00)
                    ),
                    0.0, 0.0, 0.0
            ));
        }
    }

    public List<Factura> findAll() {
        return facturaRepository.findAll();
    }

    public Optional<Factura> findById(String id) {
        return Optional.ofNullable(facturaRepository.findById(id));
    }

    public List<Factura> findByClienteId(String clienteId) {
        return facturaRepository.findByClienteId(clienteId);
    }

    public List<Factura> findByEstado(String estado) {
        return facturaRepository.findByEstado(estado);
    }

    public Factura emitirFactura(Factura entrada) {
        if (entrada == null) {
            throw new IllegalArgumentException("La factura es obligatoria");
        }
        if (entrada.detalles() == null || entrada.detalles().isEmpty()) {
            throw new IllegalArgumentException("La factura debe contener al menos un ítem en detalles");
        }
        List<DetalleFactura> normalized = new ArrayList<>();
        double subtotal = 0.0;
        for (DetalleFactura d : entrada.detalles()) {
            int qty = Math.max(1, d.cantidad());
            double unit = Math.max(0.0, d.precioUnitario());
            DetalleFactura norm = DetalleFactura.of(d.sku(), d.descripcion(), qty, unit);
            normalized.add(norm);
            subtotal += norm.subtotal();
        }
        subtotal = round2(subtotal);
        double impuesto = round2(subtotal * TAX_RATE);
        double total = round2(subtotal + impuesto);

        String folio = (entrada.folio() != null && !entrada.folio().isBlank())
                ? entrada.folio().trim()
                : "FAC-2026-" + folioSeq.incrementAndGet();
        String fecha = (entrada.fechaEmision() != null && !entrada.fechaEmision().isBlank())
                ? entrada.fechaEmision().trim()
                : LocalDate.now().toString();
        String estado = (entrada.estado() != null && !entrada.estado().isBlank())
                ? entrada.estado().trim().toUpperCase()
                : "EMITIDA";

        String clienteNombre = entrada.clienteNombre();
        if ((clienteNombre == null || clienteNombre.isBlank()) && entrada.clienteId() != null) {
            Cliente c = clienteRepository.findById(entrada.clienteId());
            if (c != null) {
                clienteNombre = c.nombre();
            }
        }

        Factura calculada = new Factura(
                entrada.jettraId(),
                folio,
                entrada.clienteId(),
                clienteNombre != null ? clienteNombre : "Consumidor Final",
                fecha,
                estado,
                normalized,
                subtotal,
                impuesto,
                total
        );
        return facturaRepository.save(calculada);
    }

    public Factura actualizarFactura(String id, Factura entrada) {
        Factura existente = facturaRepository.findById(id);
        if (existente == null) {
            throw new IllegalArgumentException("Factura no encontrada: " + id);
        }
        Factura recalculada = new Factura(
                id,
                (entrada.folio() != null && !entrada.folio().isBlank()) ? entrada.folio() : existente.folio(),
                (entrada.clienteId() != null && !entrada.clienteId().isBlank()) ? entrada.clienteId() : existente.clienteId(),
                (entrada.clienteNombre() != null && !entrada.clienteNombre().isBlank()) ? entrada.clienteNombre() : existente.clienteNombre(),
                (entrada.fechaEmision() != null && !entrada.fechaEmision().isBlank()) ? entrada.fechaEmision() : existente.fechaEmision(),
                (entrada.estado() != null && !entrada.estado().isBlank()) ? entrada.estado().toUpperCase() : existente.estado(),
                (entrada.detalles() != null && !entrada.detalles().isEmpty()) ? entrada.detalles() : existente.detalles(),
                entrada.subtotal() > 0 ? entrada.subtotal() : existente.subtotal(),
                entrada.impuesto() > 0 ? entrada.impuesto() : existente.impuesto(),
                entrada.total() > 0 ? entrada.total() : existente.total()
        );
        return facturaRepository.update(id, recalculada);
    }

    public Optional<Factura> anularFactura(String id) {
        Factura existente = facturaRepository.findById(id);
        if (existente == null) {
            return Optional.empty();
        }
        return Optional.of(facturaRepository.update(id, existente.withEstado("ANULADA")));
    }

    public boolean eliminarFactura(String id) {
        return facturaRepository.deleteById(id);
    }

    public long count() {
        return facturaRepository.count();
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
