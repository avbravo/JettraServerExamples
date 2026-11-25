# JettraStoreExampleMicroservices (`RF-23`)

Proyecto de microservicios en **Java 25+** (anteriormente `BackEndExample`) integrado directamente con el clúster **JettraStore** mediante **`JettraStoreDriver`**, trabajando sobre la base de datos **`example_factura_db`** e incorporando generación en tiempo de compilación de recursos REST con **`@RestRepository`** (`JettraRest`).

## Arquitectura
- **Configuración de clúster:** `src/main/resources/cluster.config` + `ClusterConfigLoader` (`JettraClientConfig` / `JettraClient`).
- **Modelos inmutables (Java `record`):** `Cliente`, `DetalleFactura`, `Factura`.
- **Repositorios sobre `example_factura_db`:**
  - `ClienteRepository` (`@Repository` + `@RestRepository`) -> genera `ClienteRest` en tiempo de compilación (`/api/v1/clientes`).
  - `FacturaRepository` (`@Repository` + `@RestRepository`) -> genera `FacturaRest` en tiempo de compilación (`/api/v1/facturas-auto`).
  - Implementaciones persistentes con `JettraStoreDriver`: `JettraClienteRepository` (bucket `Clientes`) y `JettraFacturaRepository` (bucket `Facturas`).
- **Servicios de negocio:** `ClienteService`, `FacturaService` (cálculo de subtotales, impuesto 7%, folios y anulaciones).
- **Endpoints & Seguridad:** JWT Bearer (`POST /api/auth/login`), `@PermitAll` / `@RolesAllowed` / `@Secured`, `/api/v1/facturas`, `/api/v1/clientes`, `/api/v1/facturas-auto`, `/api/v1/system/ping`, `/api/v1/system/info`, `/swagger-ui`.

## Compilación y pruebas (`JettraTest`)
```bash
mvn clean verify
```
