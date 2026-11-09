# BackEndExample

Implementación de referencia para **Microservicios Backend Empresariales** sobre **JettraServer**, desarrollada en **Java 25+** con **Virtual Threads (Project Loom)**, **ZGC Generacional**, seguridad mediante **Tokens JWT Bearer (`@RolesAllowed`)**, base de datos embebida **JettraSecurityDB** y explorador interactivo **Swagger-UI** implementado con **JettraFlux**.

---

## 🚀 Características Principales

- **Microservicios REST Livianos:** Endpoints de clientes (`/api/v1/customers`), órdenes (`/api/v1/orders`) y telemetría (`/api/v1/system/info`) sin dependencias de servlets pesados ni Tomcat.
- **Seguridad Declarativa JWT Bearer:** Control granular de privilegios con anotaciones `@RolesAllowed({"ADMIN", "MANAGER", "USER"})` respaldadas por tokens emitidos por la instancia local.
- **Swagger-UI Reactivo Integrado (`/swagger-ui`):**
  - **Acreditación de Usuario:** Formulario de inicio de sesión directo con credenciales de JettraSecurityDB o inyección manual de tokens JWT Bearer.
  - **Administración de JettraSecurityDB:** Interfaz visual completa para consultar usuarios en tiempo real, crear nuevos usuarios, cambiar contraseñas, habilitar/deshabilitar cuentas, asignar y revocar roles, y registrar nuevos roles de sistema.
- **Ciclo de Vida Continuo:** El servidor permanece en ejecución continua de forma no bloqueante mediante Virtual Threads (`server.await()`).
- **Resolución Automática de Puertos:** Si el puerto configurado (8083) estuviera ocupado, el servidor detecta el siguiente puerto libre automáticamente.
- **Contenedorización Musl:** Imagen Docker basada en `bellsoft/liberica-runtime-container:jre-25-stream-musl` (< 90 MB) optimizada para Kubernetes y Docker Compose.

---

## 📋 Requisitos Previos

- **Java Development Kit (JDK):** Versión 25 o superior con preview activado (`--enable-preview`).
- **Apache Maven:** 3.9 o superior.
- **Docker & Docker Compose:** (Opcional, para ejecución en contenedores).

---

## 🛠️ Compilación y Empaquetado

Para compilar el proyecto y generar el **Uber JAR** ejecutable (Fat JAR):

```bash
# Desde el directorio JettraServerExamples/BackEndExample
mvn clean package -DskipTests
```

El artefacto generado se ubicará en:
`target/BackEndExample-1.0.0-SNAPSHOT-uber.jar`

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
     -jar target/BackEndExample-1.0.0-SNAPSHOT-uber.jar
```

### Opción B: Ejecutar mediante Maven

```bash
mvn exec:java
```

Por defecto, el servicio inicia en el puerto **8083** (configurable en `src/main/resources/META-INF/microprofile-config.properties` o mediante `-Dserver.port=8083`).

---

## 🌐 Endpoints y Rutas del Microservicio

Una vez iniciado el servidor:

| Método | Endpoint | Descripción | Seguridad / Roles |
| :--- | :--- | :--- | :--- |
| `GET` | `/swagger-ui` | Interfaz Swagger-UI interactiva con Acreditación y Gestión de Usuarios/Roles | Público |
| `GET` | `/openapi.json` | Especificación formal OpenAPI 3.0.0 | Público |
| `POST` | `/api/auth/login` | Inicia sesión y emite token JWT Bearer (`admin`/`admin`) | Público |
| `GET` | `/api/auth/me` | Obtiene el perfil del usuario autenticado | Bearer Token |
| `GET` | `/api/security/users` | Lista todos los usuarios de JettraSecurityDB | ADMIN, SECURITY_ADMIN |
| `POST` | `/api/security/users` | Registra nuevo usuario con credenciales y roles | ADMIN, SECURITY_ADMIN |
| `DELETE` | `/api/security/users` | Elimina un usuario de JettraSecurityDB | ADMIN, SECURITY_ADMIN |
| `POST` | `/api/security/users/role` | Asigna o revoca un rol (`action: add\|remove`) | ADMIN, SECURITY_ADMIN |
| `POST` | `/api/security/users/status`| Activa o desactiva la cuenta del usuario | ADMIN, SECURITY_ADMIN |
| `POST` | `/api/security/users/password`| Modifica la contraseña del usuario | ADMIN, SECURITY_ADMIN |
| `GET` | `/api/security/roles` | Lista los roles registrados en JettraSecurityDB | ADMIN, MANAGER, USER |
| `POST` | `/api/security/roles` | Registra un nuevo rol de sistema | ADMIN, SECURITY_ADMIN |
| `GET` | `/api/v1/customers` | Lista todos los clientes | ADMIN, MANAGER, USER |
| `GET` | `/api/v1/customers/{id}`| Obtiene detalle de cliente por ID | ADMIN, MANAGER, USER |
| `POST` | `/api/v1/customers` | Crea un nuevo cliente | ADMIN, MANAGER |
| `DELETE` | `/api/v1/customers/{id}`| Elimina un cliente | ADMIN |
| `GET` | `/api/v1/orders` | Lista todas las órdenes de compra | ADMIN, MANAGER, USER |
| `POST` | `/api/v1/orders` | Registra una nueva orden | ADMIN, MANAGER |
| `GET` | `/api/v1/system/info` | Telemetría del servidor y JVM | Público |
| `GET` | `/api/v1/system/ping` | Endpoint de sondeo y latencia | Público |
| `GET` | `/health` | Chequeo de salud MicroProfile Health | Público |

---

## 🐳 Ejecución con Docker

El proyecto incluye un [`Dockerfile`](Dockerfile) optimizado basado en **BellSoft Liberica Runtime Musl (JRE 25)** sobre Alpine Linux.

### 1. Construir la imagen Docker

Asegúrese de haber generado el Uber JAR previamente con `mvn clean package -DskipTests`:

```bash
docker build -t jettra/backend-example:1.0.0 .
```

### 2. Ejecutar el Contenedor Docker

```bash
docker run -d \
  --name backend-example \
  -p 8083:8083 \
  -v backend_example_data:/app/data \
  jettra/backend-example:1.0.0
```

### 3. Consultar Registros y Estado

```bash
# Ver logs en tiempo real
docker logs -f backend-example

# Verificar healthcheck
docker inspect --format='{{json .State.Health}}' backend-example
```

### 4. Detener y Eliminar el Contenedor

```bash
docker stop backend-example && docker rm backend-example
```

---

## 📦 Despliegue con Docker Compose

Para orquestar el microservicio con volúmenes persistentes y verificación automática de salud, utilice [`docker-compose.yml`](docker-compose.yml):

```bash
# Construir y levantar en segundo plano
docker compose up -d --build

# Monitorear logs
docker compose logs -f

# Detener el servicio
docker compose down
```

---

## ⚙️ Configuración (`microprofile-config.properties`)

La configuración se gestiona en [`src/main/resources/META-INF/microprofile-config.properties`](src/main/resources/META-INF/microprofile-config.properties):

```properties
# Puerto y host
server.port=8083
server.host=0.0.0.0

# Modo de Almacenamiento: JVM_RAM, DISK_MEMORY, OFF_HEAP
jettra.storage.mode=JVM_RAM

# Directorio de persistencia y base de seguridad JettraSecurityDB
jettra.server.data.dir=./data/jettra-backend
jettra.securitydb.path=./data/jettra-backend/securitydb

# Secret y Expiración JWT
jettra.jwt.secret=jettra_backend_super_secret_jwt_key_2026_enterprise
jettra.jwt.expiration.ms=86400000
```
Sobrescribible en tiempo de ejecución con variables de entorno: `SERVER_PORT`, `SERVER_HOST`, `JETTRA_STORAGE_MODE`, `JETTRA_SERVER_DATA_DIR`.

