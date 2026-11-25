# WebExample

Aplicación empresarial full-stack de referencia para **JettraServer**, desarrollada en **Java 25+** con soporte nativo para **Project Loom (Virtual Threads)**, **Jakarta EE 11** (CDI 4.1, Faces 4.1 / PrimeFaces 16, Servlet 6.1, JPA 3.2), interfaces reactivas **JettraFlux**, **Hamburger Menu interactivo** y explorador **Swagger-UI (OpenAPI 3.0.0)** con acreditación de seguridad y administración de **JettraSecurityDB**.

---

## 🚀 Características Principales

- **Arquitectura Web Híbrida:** Vistas Facelets (`.xhtml`) con PrimeFaces, vistas reactivas en Java puro con **JettraFlux** y páginas orientadas a plantillas `.html`/`.xhtml` con **JettraAura** (variante optimizada y simplificada de JettraFlux).
- **Comunicación MicroProfile Rest Client 3.0 & Autenticación Jettra JWT:**
  - Cliente REST declarativo con anotaciones estándar de MicroProfile (`@RegisterRestClient`, `@Path`, `@HeaderParam`).
  - Interconexión protegida y transparente con el microservicio `BackEndExample` mediante tokens Bearer HMAC-SHA256 (`JettraJwtAuthManager`).
  - Interfaz CRUD completa (`Customer`) disponible de manera simultánea en **Jakarta Faces**, **JettraFlux** y **JettraAura**.
- **Suite JettraAura (Variante JettraFlux para HTML y Jakarta Faces XHTML):**
  - **Asociación 1:1 HTML/XHTML $\leftrightarrow$ Java:** Marcado `.html` y `.xhtml` limpio con vinculación por `aura:id` o `flux:id` e interoperabilidad directa con `JettraFlux` (`AuraFluxWidget`, `toFluxWidget()`, `AuraFacesBridge`).
  - **Catálogo Dinámico (`AuraListView` + `AuraModal`):** Iteración sobre colecciones JPA con operaciones completas CRUD y tres diálogos interactivos (`AuraModal`) para **Registrar Datos**, **Editar Producto** y **Confirmar Eliminación**.
  - **Formularios con `CompoundPropertyModel`:** Vinculación directa de POJOs por `aura:id` / `flux:id`.
  - **Control de Eventos y Reactividad Quirúrgica con AJAX:** Eventos tipados (`AuraEvent`, `AuraEventListener`, `@AuraAction`) y actualización granular del DOM sin recargas de página completas sobre Virtual Threads de Project Loom.
- **Hamburger Menu Responsivo:** Navegación optimizada para escritorios, portátiles, tablets y móviles con colapso fluido y modo *off-canvas*.
- **Swagger-UI Interactivo (`/swagger-ui`):**
  - **Acreditación JWT:** Inicio de sesión directo o token Bearer manual contra `JettraSecurityDB`.
  - **Administración de JettraSecurityDB:** Sección interactiva para consultar, registrar, activar/desactivar usuarios, asignar o revocar roles, modificar contraseñas y crear roles en tiempo real.
- **Persistencia Tri-Modal:** Conmutable entre `JVM_RAM`, `DISK_MEMORY` y `OFF_HEAP` (Project Panama FFM).
- **Alineación MicroProfile:** Configuración unificada mediante `microprofile-config.properties`.
- **Contenedorización Ultraligera:** Basada en `bellsoft/liberica-runtime-container:jre-25-stream-musl` (< 90 MB).

---

## 📋 Requisitos Previos

- **Java Development Kit (JDK):** Versión 25 o superior con preview activado (`--enable-preview`).
- **Apache Maven:** 3.9 o superior.
- **Docker & Docker Compose:** (Opcional, para ejecución en contenedores).

---

## 🛠️ Compilación y Empaquetado

Para compilar el proyecto y generar el **Uber JAR** ejecutable (Fat JAR):

```bash
# Desde el directorio JettraServerExamples/WebExample
mvn clean package -DskipTests
```

El artefacto generado se ubicará en:
`target/WebExample-1.0.0-SNAPSHOT-uber.jar`

Para ejecutar las pruebas automatizadas con **JettraTest** (100% libre de JUnit):
```bash
mvn test
```

---

## 💻 Ejecución Local

### Opción A: Ejecutar mediante el Uber JAR (Recomendado)

```bash
java --enable-preview \
     -XX:+UnlockExperimentalVMOptions \
     -XX:+UseCompactObjectHeaders \
     -XX:+UseContainerSupport \
     -XX:+UseZGC \
     -Xms256m -Xmx1g \
     -jar target/WebExample-1.0.0-SNAPSHOT-uber.jar
```

### Opción B: Ejecutar mediante Maven

```bash
mvn exec:java
```

Por defecto, la aplicación inicia en el puerto **8080** (configurable en `src/main/resources/META-INF/microprofile-config.properties` o mediante el parámetro `-Dserver.port=8080`).

---

## 🌐 URLs y Servicios Disponibles

Una vez iniciado el servidor, acceda a los siguientes recursos:

| Recurso | URL | Descripción / Credenciales |
| :--- | :--- | :--- |
| **Login PrimeFaces** | `http://localhost:8080/login.xhtml` | Usuario: `admin` \| Clave: `admin` |
| **Dashboard Principal** | `http://localhost:8080/dashboard.xhtml` | Panel central con telemetría y menú hamburguesa |
| **Catálogo JPA** | `http://localhost:8080/productos.xhtml` | Gestión CRUD de catálogo con persistencia JPA |
| **Clientes Faces (MP-Client)** | `http://localhost:8080/clientes.xhtml` | CRUD de Clientes PrimeFaces con MicroProfile Rest Client + JWT |
| **Swagger-UI OpenAPI** | `http://localhost:8080/swagger-ui` | Explorador interactivo de APIs con panel de Acreditación y Gestión de Usuarios/Roles |
| **OpenAPI Spec JSON** | `http://localhost:8080/openapi.json` | Contrato formal OpenAPI 3.0.0 |
| **Vistas JettraFlux** | `http://localhost:8080/flux/customers` | CRUD de Clientes Reactivo (Java 25 puro sin StringBuilder) |
| | `http://localhost:8080/flux/storage-monitor` | Monitor de memoria y persistencia reactiva |
| | `http://localhost:8080/flux/telemetry` | Telemetría del servidor y métricas Loom |
| | `http://localhost:8080/flux/counter` | Componente interactivo reactivo |
| | `http://localhost:8080/flux/catalogo` | Catálogo renderizado por JettraFlux |
| **Suite JettraAura** | `http://localhost:8080/aura/demo` | Portal central de JettraAura con métricas en vivo |
| | `http://localhost:8080/aura/customers` | CRUD Clientes Aura con modales (`AuraModal`) y AJAX |
| | `http://localhost:8080/aura/catalogo` | Catálogo iterativo (`AuraListView`) con alta rápida |
| | `http://localhost:8080/aura/formulario` | Formulario POJO con `CompoundPropertyModel` |
| | `http://localhost:8080/aura/contador` | Reactividad parcial quirúrgica con AJAX en Loom |
| **Health & Métricas** | `http://localhost:8080/health` | Health Check (MicroProfile Health 4.0) |
| | `http://localhost:8080/metrics` | Métricas JVM, almacenamiento y uptime |

---

## 🐳 Ejecución con Docker

El proyecto incluye un [`Dockerfile`](Dockerfile) optimizado basado en **BellSoft Liberica Runtime Musl (JRE 25)** sobre Alpine Linux.

### 1. Construir la imagen Docker

Asegúrese de haber empaquetado el proyecto previamente con `mvn clean package -DskipTests`:

```bash
docker build -t jettra/web-example:1.0.0 .
```

### 2. Ejecutar el Contenedor Docker

```bash
docker run -d \
  --name web-example \
  -p 8080:8080 \
  -v web_example_data:/app/data \
  jettra/web-example:1.0.0
```

### 3. Consultar Registros y Estado

```bash
# Ver logs en tiempo real
docker logs -f web-example

# Inspeccionar estado de salud
docker inspect --format='{{json .State.Health}}' web-example
```

### 4. Detener y Eliminar el Contenedor

```bash
docker stop web-example && docker rm web-example
```

---

## 📦 Despliegue con Docker Compose

Para orquestar el contenedor con volúmenes persistentes y verificación de salud automática, utilice [`docker-compose.yml`](docker-compose.yml):

```bash
# Iniciar el servicio en segundo plano (construye automáticamente si no existe)
docker compose up -d --build

# Seguir los registros del contenedor
docker compose logs -f

# Detener y remover servicios manteniendo los datos
docker compose down
```

---

## ⚙️ Configuración (`microprofile-config.properties`)

La configuración se administra de forma estándar según **Eclipse MicroProfile Config 3.1** en [`src/main/resources/META-INF/microprofile-config.properties`](src/main/resources/META-INF/microprofile-config.properties). Las variables pueden sobrescribirse mediante variables de entorno del sistema o en Docker:

```properties
# Puerto y host de escucha
server.port=8080
server.host=0.0.0.0

# Modo de Persistencia: JVM_RAM, DISK_MEMORY, OFF_HEAP
jettra.storage.mode=JVM_RAM

# Directorio de datos y JettraSecurityDB
jettra.server.data.dir=./data/jettra-server-example
jettra.securitydb.path=./data/jettra-server-example/securitydb

# Parámetros del token JWT
jettra.jwt.secret=jettra_server_example_secret_key_2026_super_secure
jettra.jwt.expiration.ms=86400000

# ==============================================================================
# Eclipse MicroProfile Rest Client 3.0 Configuration
# ==============================================================================
customer-client/mp-rest/url=http://localhost:8081
customer-client/mp-rest/scope=jakarta.enterprise.context.ApplicationScoped
customer-client/mp-rest/connectTimeout=5000
customer-client/mp-rest/readTimeout=5000

# Integración con BackEndExample
backend.api.url=http://localhost:8081
backend.api.auth.user=admin
backend.api.auth.password=admin
backend.jwt.secret=jettra_backend_super_secret_jwt_key_2026
```

---

## 👥 Integración Eclipse MicroProfile Rest Client & Autenticación Jettra JWT

`WebExample` se comunica con el microservicio de clientes en `BackEndExample` (puerto `8081`) utilizando **Eclipse MicroProfile Rest Client 3.0** y autenticación basada en **Jettra JWT**.

### 1. Interfaz Declarativa `CustomerRestClient`
Anotada con el estándar MicroProfile:
```java
@RegisterRestClient(configKey = "customer-client", baseUri = "http://localhost:8081")
@Path("/api/v1/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface CustomerRestClient {

    @GET
    List<Customer> listCustomers(@HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{id}")
    Customer getCustomer(@HeaderParam("Authorization") String authorization, @PathParam("id") Long id);

    @POST
    Customer createCustomer(@HeaderParam("Authorization") String authorization, Customer customer);

    @DELETE
    @Path("/{id}")
    void deleteCustomer(@HeaderParam("Authorization") String authorization, @PathParam("id") Long id);
}
```

### 2. Autenticación Criptográfica con `JettraJwtAuthManager`
Adquiere y administra tokens Bearer válidos comunicándose con `/api/auth/login` de `BackEndExample` (credenciales `admin`/`admin`), o generando tokens HMAC-SHA256 directos de alta seguridad con la clave compartida `backend.jwt.secret`.

### 3. Implementaciones de Interfaz CRUD en los 3 Motores Frontend
El mismo microservicio se visualiza y administra en tres tecnologías distintas:

1. **Jakarta Faces 4.0 / PrimeFaces 16 (`/clientes.xhtml`):**
   - Respaldo mediante `CustomerBean` (`@SessionScoped`).
   - Tabla interactiva con columnas ordenables, filtros, badges de estado y botones de acción.
   - Diálogos modales para **Registrar Nuevo Cliente**, **Editar Cliente** y **Confirmar Eliminación**.
2. **JettraFlux Reactivo (`/flux/customers`):**
   - Construido 100% en Java 25 puro **sin utilizar `StringBuilder`**.
   - Tarjetas KPI en tiempo real (Total Clientes, Clientes Activos, Endpoint Backend).
   - Formulario de alta rápida e iteración sobre widgets de tabla reactiva.
3. **JettraAura con Diálogos `AuraModal` (`/aura/customers`):**
   - Vinculación 1:1 entre `CustomerAuraPage.java` y `CustomerAuraPage.html`.
   - Diálogos modales dinámicos (`registerCustomerModal`, `editCustomerModal`, `deleteCustomerModal`).
   - Envío asíncrono con `AuraAjaxButton` y actualización granular del DOM en hilos virtuales Loom.


