# Lambda Shield - Sistema de Detección de Fraude Bancario

> Plataforma web bancaria y motor heurístico de prevención de fraude en tiempo real.
> Desarrollada con **Java 21 LTS**, **Spring Boot 4.1.1**, **Spring Data JPA**, **Thymeleaf**, **Bootstrap 5** y **JavaScript Vanilla**.

---

## Descripción del proyecto

**Lambda Shield** combina una interfaz de operaciones financieras para clientes con un centro de control para analistas y administradores antifraude.

El núcleo implementa un **motor de reglas de scoring ponderado** (RF001-RF008) alineado con la especificación de arquitectura. Cada evaluación se guarda en PostgreSQL junto con su transacción, cliente, cuenta, dispositivo, beneficiario y desglose de reglas.

---

## Arquitectura (N capas)

```text
frontend/src/main/resources/
├── templates/           # Vistas Thymeleaf
└── static/              # CSS, JavaScript e imágenes

backend/src/main/
├── java/com/lambdashield/fraude/
│   ├── presentation/              # Capa de presentación (HTTP y vistas)
│   │   ├── api/rest/              # Controladores REST (/api/v1/**)
│   │   ├── web/                   # Controladores MVC/Thymeleaf
│   │   ├── dto/                   # Contratos JSON de entrada y salida
│   │   ├── mapper/                # Traduce modelos de aplicación a DTO y etiquetas visuales
│   │   ├── export/                # Serialización CSV de reportes
│   │   ├── security/              # Interceptor y política de autorización por rol
│   │   └── exception/             # Traducción de errores a HTTP/vistas
│   ├── application/               # Capa de aplicación (casos de uso)
│   │   ├── command/               # Comandos de entrada de los casos de uso
│   │   ├── model/                 # Modelos de resultado independientes de la interfaz
│   │   ├── mapper/                # Entidades del dominio -> modelos de aplicación
│   │   └── service/               # Casos de uso, consultas y coordinación
│   ├── domain/                    # Capa de dominio (sin dependencias hacia otras capas)
│   │   ├── entity/                # Entidades JPA del modelo de negocio
│   │   ├── enums/                 # Estados y tipos del dominio
│   │   ├── fraud/                 # Motor antifraude: factores y reglas
│   │   ├── device/                # Huella de dispositivo
│   │   └── customer/              # Reglas de presentación del nombre del cliente
│   ├── repository/                # Persistencia: contratos Spring Data JPA
│   └── config/                    # Composición y configuración (raíz de dependencias)
│       └── seed/                  # Sembradores demo idempotentes por responsabilidad
└── resources/                     # Configuración del backend y scripts SQL

backend/src/test/                  # Pruebas unitarias, de integración y de arquitectura
pom.xml                            # Build Maven del backend y recursos frontend
```

Flujo de dependencias: `presentation` -> `application` -> `domain`, con `repository` como persistencia usada por `application`. `config` es la raíz de composición y es la única capa que puede conocer a todas las demás. Los casos de uso devuelven modelos de `application/model` y nunca DTO HTTP; `presentation/mapper` añade las etiquetas visuales (estado, icono, color) y formatea fechas. Los sembradores demo se separan por catálogos, clientes, usuarios y evaluaciones; `DataInitializer` sólo coordina su orden.

Las dependencias entre capas están verificadas por pruebas de arquitectura con **ArchUnit** (`backend/src/test/java/com/lambdashield/fraude/architecture/ArquitecturaCapasTest.java`). Si una dependencia prohibida se introduce, `mvnw test` falla.

Reglas verificadas:

| Regla | Descripción |
| :--- | :--- |
| Capas | `presentation` sólo lo usa `config`; `application` sólo lo usan `presentation` y `config`; `repository` sólo lo usan `application` y `config`; `domain` sólo lo usan `application`, `repository` y `config`. |
| Dominio puro | `domain` no depende de Spring. |
| Aplicación sin web | `application` no depende de `jakarta.servlet` ni de `org.springframework.web`/`ui`. |
| Controladores | Todo `@RestController`/`@Controller` vive en `presentation` y no accede a `repository`. |
| Sin ciclos | Los módulos de primer nivel no forman dependencias circulares. |

Decisiones explícitas:

- **Entidades JPA en el dominio:** `domain/entity` usa `jakarta.persistence` y Lombok. Es un compromiso aceptado para no duplicar el modelo; las reglas de negocio puras viven en `domain/fraud`, sin anotaciones de persistencia.
- **Acceso a repositorios desde la aplicación:** los casos de uso consultan `repository` directamente. No se introducen puertos de persistencia porque el proyecto usa Spring Data JPA como única fuente de datos.
- **Entidades sin casos de uso:** `AlertaFraude`, `Auditoria`, `CasoFraude`, `HistorialCaso` y `CaracteristicaTransaccion` están mapeadas al esquema pero aún no tienen servicios que las usen. Se conservan para que `ddl-auto=validate` siga coincidiendo con la base de datos.

El proyecto conserva una aplicación Spring MVC/Thymeleaf: las vistas y recursos pertenecen al frontend, pero Maven los incorpora al artefacto del backend para servirlos. REST y páginas MVC tienen paquetes separados; los controladores MVC no son endpoints REST.

**No existen stores en memoria.** Usuarios, clientes y evaluaciones se leen y escriben mediante JPA en PostgreSQL.

### Aplicación de SOLID

- **Responsabilidad única:** consultas, métricas, generación de CSV, resolución de dispositivos, autorización y carga demo están en componentes separados.
- **Abierto/cerrado e inversión de dependencias:** el motor consume `ReglaEvaluable`; agregar una regla no requiere editar el flujo de evaluación.
- **Sustitución e interfaces pequeñas:** cada regla implementa el mismo contrato de una sola operación y puede probarse de forma aislada.
- **Separación de capas:** el controlador adapta el DTO HTTP a `EvaluarTransaccionCommand`; las reglas usan `TransaccionEvaluable` y `FactorEvaluado`, sin depender de DTO web ni de Spring.

Los controladores no contienen lógica de negocio. Las entidades JPA usan getters/setters explícitos en vez de `@Data`, evitando `equals`, `hashCode` y `toString` generados sobre relaciones bidireccionales.

> **Nota:** las cifras del dashboard (saldo, ingresos, egresos, variaciones, serie diaria y movimientos recientes) provienen de PostgreSQL: saldo de las cuentas activas y movimientos según la naturaleza (débito/crédito) de cada transacción. Las variaciones comparan el periodo elegido con el inmediatamente anterior.

---

## Persistencia

La aplicación usa las tablas existentes del esquema PostgreSQL:

- `usuario_sistema`, `rol`, `usuario_rol`
- `cliente`, `cuenta`, `dispositivo`, `beneficiario`
- `transaccion`, `evaluacion_fraude`, `evaluacion_regla`
- `regla_fraude`, `canal`, `tipo_transaccion`
- tablas auxiliares de alertas, casos y auditoría

El esquema canónico está en `backend/src/main/resources/db/postgresql/BD-Desarrollo web.sql`.
Los cambios de migración aplicados a una base existente están en `backend/src/main/resources/db/postgresql/migracion_soft_delete_postgresql.sql`.

### Verificación del mapeo JPA contra PostgreSQL

`spring.jpa.hibernate.ddl-auto=validate` obliga a Hibernate a comparar cada entidad con las columnas reales al arrancar. Para verificar el esquema sin levantar el servidor web:

```powershell
# Con las credenciales de .env no hace falta exportar nada
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.main.web-application-type=none"
```

Si una columna, tabla o tipo no coincide, el arranque falla con `Schema validation: ...` y el mensaje indica la tabla y la columna exactas. Una base creada con la versión anterior del script debe recibir antes `migracion_soft_delete_postgresql.sql`; la migración es idempotente y añade `cliente.codigo_cliente`, `cliente.promedio_gasto`, `cliente.es_activo`, `dispositivo.direccion_ip`, `evaluacion_fraude.es_activa`, `evaluacion_fraude.fecha_desactivacion`, `usuario_sistema.id_cliente`, normaliza los tipos `jsonb`/`inet` a `TEXT`/`VARCHAR(45)` y quita la secuencia `IDENTITY` de `caracteristica_transaccion.id_transaccion`, cuya clave se comparte con `transaccion` mediante `@MapsId`.

Las 18 tablas del esquema (`cliente`, `cuenta`, `beneficiario`, `canal`, `tipo_transaccion`, `dispositivo`, `transaccion`, `caracteristica_transaccion`, `regla_fraude`, `evaluacion_fraude`, `evaluacion_regla`, `usuario_sistema`, `alerta_fraude`, `caso_fraude`, `historial_caso`, `rol`, `usuario_rol`, `auditoria`) tienen entidad en `domain/entity`.

`DataInitializer` carga de forma idempotente:

- roles y usuarios de demostración (contraseñas cifradas con BCrypt)
- clientes, cuentas y dispositivos conocidos
- catálogos de canales y tipos de transacción
- las ocho reglas antifraude
- diez evaluaciones de ejemplo

La carga no duplica datos al reiniciar la aplicación.

### Baja lógica (sin DELETE)

Las evaluaciones no se eliminan físicamente:

- `PATCH /api/v1/evaluation/{id}` marca `evaluacion_fraude.es_activa = false`
- registra `fecha_desactivacion`
- cambia la transacción asociada a `REVERSADA`
- la fila permanece en la base de datos y desaparece de las listas activas

La operación es reversible:

- `PATCH /api/v1/evaluation/{id}/activar` vuelve `es_activa` a `true`, borra `fecha_desactivacion` y la transacción recupera el estado que le corresponde según su nivel de riesgo (`bajo/medio` → `APROBADA`, `alto` → `RETENIDA`, `crítico` → `BLOQUEADA`).
- El dinero es simétrico: al desactivar una operación aprobada se devuelve el monto a la cuenta, y al reactivarla se descuenta otra vez (con la misma validación de saldo).
- `GET /api/v1/evaluation?activas=false` incluye también las desactivadas (cada registro trae el flag `activa`).

No existe un endpoint `DELETE` sobre evaluaciones. Las peticiones `DELETE` a `/api/v1/evaluation` responden `405`. La única excepción es la gestión de usuarios (`DELETE /api/v1/user/{id}`), reservada a ADMIN.

En la vista de historial, el administrador dispone de los botones **Desactivar** y **Reactivar** por fila, ambos con un modal de confirmación. El filtro **Desactivadas** muestra las que están dadas de baja.

---

## Características principales

### Portal del cliente

- Transferencias protegidas con evaluación antifraude en tiempo real.
- Factores de dispositivo, IP, horario y frecuencia derivados en el servidor.
- **Tipo de destinatario derivado del historial**: el cliente no declara si el destinatario es nuevo o frecuente; se determina por las transferencias previas al mismo nombre (la primera vez es nuevo y a partir de la segunda, frecuente).
- **Saldo real y persistente**: las operaciones aprobadas descuentan el saldo de la cuenta y registran `saldo_anterior`/`saldo_posterior`; si el saldo no alcanza, la operación se rechaza con `400 Saldo insuficiente`. Las operaciones retenidas o bloqueadas no mueven dinero.
- Historial limitado a las operaciones del cliente autenticado.
- Estados diferenciados por operación: procesada, en monitoreo, retenida en revisión o bloqueada.
- Avisos de seguridad con las operaciones de riesgo alto o crítico y el detalle de las causas detectadas en cada rechazo.

### Panel administrativo y analista

- Dashboard con cifras reales: saldo de cuentas activas, ingresos/egresos, variaciones frente al periodo anterior, serie diaria y últimos movimientos.
- Historial de evaluaciones con datos reales, filtros por nivel, monto y fecha, y vista de desactivadas.
- Simulador manual de transacciones con desglose de factores.
- Catálogo de reglas leído desde `regla_fraude`.
- Reportes con filtros por fecha, nivel y cliente, y exportación en CSV, Excel y PDF.
- Desactivación lógica de evaluaciones por parte de ADMIN.
- Gestión de usuarios (solo ADMIN): alta, edición, cambio de clave, cambio de rol/estado y baja física, con protección del último administrador.

---

## Motor de reglas antifraude

| Código | Regla | Condición de disparo | Peso |
| :--- | :--- | :--- | :---: |
| **RF001** | Monto inusual | Gradual según `monto / promedio`: `<=1.5x` = 0; `>1.5x y <=3x` = 5-15; `>3x y <5x` = 15-22; `>=5x` = 25 | 25 |
| **RF002** | Dispositivo nuevo | `es_dispositivo_nuevo = true` | 15 |
| **RF003** | IP nueva | `es_ip_nueva = true` | 10 |
| **RF004** | País nuevo | `es_pais_nuevo = true` | 30 |
| **RF005** | Horario inusual | Operación entre 00:00 y 04:59 | 10 |
| **RF006** | Alta frecuencia | Gradual según operaciones en la última hora | 25 |
| **RF007** | Viaje imposible | Heurística de país + operaciones + IP | 40 |
| **RF008** | Beneficiario nuevo con monto alto | `nuevo && monto > umbral` | 20 |

Escala de decisión:

- **0-29:** BAJO - aprobar
- **30-59:** MEDIO - monitorear
- **60-79:** ALTO - retener / alertar
- **80-100:** CRÍTICO - bloquear / alerta crítica

La suma se limita a 100 puntos.

---

## Roles y permisos

Las credenciales se validan contra `usuario_sistema` usando BCrypt. Los roles se resuelven desde `usuario_rol` y `rol`.

| Rol | Usuario | Páginas | API |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` | Todas | Todos los métodos permitidos, incluido `PATCH /api/v1/evaluation/{id}`, la gestión de `/api/v1/user` y su `DELETE` |
| **ANALISTA** | `analista` | Inicio, transacciones, reglas y reportes | Solo `GET /api/**`, excepto `/api/v1/user` (reservado a ADMIN) |
| **CLIENTE** | `usuario`, `maria`, `cliente` | Solo `/operacion` | Solo `POST /api/v1/evaluation` para su propio cliente |

Reglas de sesión:

- Sin sesión, `/api/**` responde `401` con `{"error":"No autenticado"}`.
- Sin sesión, las páginas protegidas redirigen a `/login`.
- Si el rol no tiene permiso, la API responde `403` con `{"error":"No autorizado"}`.
- El portal del cliente usa el `codigoCliente` de la sesión y no confía en el cliente enviado en el body.
- `DELETE` solo está permitido en `/api/v1/user/{id}` (gestión de usuarios, solo ADMIN). En el resto de la API responde `405`.

Las contraseñas de demostración no se publican en esta documentación; están definidas en `config/seed/UsuarioDemoSeeder` y se almacenan cifradas en la base de datos.

---

## Requisitos

- **JDK 21 LTS** o superior.
- **PostgreSQL** con la base `lambda_shield` creada.
- Maven Wrapper incluido (`mvnw.cmd` / `mvnw`).
- Navegador moderno.

Si el `java` por defecto del sistema es anterior a 21, apunta `JAVA_HOME` a un JDK 21+ antes de compilar, porque `pom.xml` usa `<release>21</release>`.

La conexión se configura en `backend/src/main/resources/application.properties`. Por defecto usa:

```text
jdbc:postgresql://localhost:5432/lambda_shield
```

El usuario, la contraseña y el resto de parámetros de conexión se configuran en el archivo **`.env`** de la raíz del repositorio:

```text
DB_USER=postgres
DB_PASSWORD=postgres
#DB_HOST=localhost
#DB_PORT=5432
#DB_NAME=lambda_shield
```

Spring Boot lo carga al arrancar mediante `spring.config.import=optional:file:./.env[.properties]` en `application.properties`. Si el archivo no existe, se usan los valores por defecto de la tabla. Las variables de entorno reales (`DB_USER`, `DB_PASSWORD`, ...) tienen prioridad sobre `.env`, así que en producción basta con exportarlas sin tocar el archivo.

`.env` está en `.gitignore`: no se sube al repositorio. Para crear uno nuevo, copia la plantilla `.env.example`.

Antes de arrancar, aplica `backend/src/main/resources/db/postgresql/BD-Desarrollo web.sql` en una base nueva o `backend/src/main/resources/db/postgresql/migracion_soft_delete_postgresql.sql` en una base existente. El runtime usa `spring.jpa.hibernate.ddl-auto=validate`.

---

## Ejecución

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

Abre `http://localhost:8080` e inicia sesión con uno de los usuarios de demostración.

---

## Pruebas

```powershell
.\mvnw.cmd clean test
```

Las pruebas usan **H2 en modo PostgreSQL**, con `ddl-auto=create-drop`, por lo que **no requieren una instancia de PostgreSQL**. Son 81 pruebas en diez grupos:

- `domain.fraud.rule.ReglasFraudeTest`: valores límite de las ocho reglas;
- `application.service.DeteccionFraudeServiceTest`: umbrales del motor de detección;
- `presentation.api.rest.SeguridadApiMockMvcTest`: autenticación, autorización por rol, baja lógica con `PATCH`, reactivación, listado con desactivadas, rechazo de `DELETE` y aislamiento del portal del cliente;
- `presentation.api.rest.ValidacionApiMockMvcTest`: validación de cada campo y parámetro de la API, JSON malformado, tipos incorrectos y `Content-Type` no soportado;
- `presentation.api.rest.UsuarioApiMockMvcTest`: CRUD de usuarios, unicidad, cambio de clave, protección del último administrador y permisos;
- `presentation.api.rest.ReporteApiMockMvcTest`: exportación CSV/Excel/PDF, filtros por fecha, nivel y cliente, y validación del formato;
- `presentation.api.rest.DashboardApiMockMvcTest`: cifras reales del resumen, serie diaria y movimientos recientes;
- `presentation.api.rest.PortalClienteApiMockMvcTest`: tipo de destinatario derivado del historial, descuento de saldo, rechazo por saldo insuficiente y bloqueos sin movimiento de dinero;
- `presentation.api.rest.SaludMockMvcTest`: `/actuator/health` público y sin detalles; el resto de Actuator no expuesto;
- `architecture.ArquitecturaCapasTest`: las seis reglas de ArchUnit (capas, dominio sin Spring, aplicación sin web, controladores en `presentation`, sin acceso a repositorios desde controladores y sin ciclos).

Si una carpeta está sincronizada con OneDrive, `clean` puede fallar por bloqueos de archivos; basta con `.\mvnw.cmd test`.

---

## API REST

| Método | Endpoint | Descripción | Roles | Códigos |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/evaluation` | Evalúa y persiste una transacción | ADMIN, CLIENTE (forzado a su propio código) | 200, 400, 401, 403, 404, 415 |
| `GET` | `/api/v1/evaluation` | Lista evaluaciones activas (`?activas=false` incluye las desactivadas) | ADMIN, ANALISTA | 200, 401, 403 |
| `PATCH` | `/api/v1/evaluation/{id}` | Desactiva una evaluación (baja lógica) | ADMIN | 204, 400, 401, 403, 404 |
| `PATCH` | `/api/v1/evaluation/{id}/activar` | Reactiva una evaluación dada de baja | ADMIN | 204, 400, 401, 403, 404 |
| `GET` | `/api/v1/metric` | KPIs consolidados | ADMIN, ANALISTA | 200, 401, 403 |
| `GET` | `/api/v1/metric/summary?days={n}` | Balance contable del periodo | ADMIN, ANALISTA | 200, 400, 401, 403 |
| `GET` | `/api/v1/metric/movement?days={n}` | Serie temporal para el gráfico | ADMIN, ANALISTA | 200, 400, 401, 403 |
| `GET` | `/api/v1/metric/recent?days={n}` | Movimientos recientes | ADMIN, ANALISTA | 200, 400, 401, 403 |
| `GET` | `/api/v1/rule` | Reglas activas desde la base de datos | ADMIN, ANALISTA | 200, 401, 403 |
| `GET` | `/api/v1/report/export?formato={csv\|xlsx\|pdf}` | Reporte con filtros `desde`, `hasta`, `nivel`, `cliente` | ADMIN, ANALISTA | 200, 400, 401, 403 |
| `GET` | `/api/v1/user` | Lista de usuarios del sistema | ADMIN | 200, 401, 403 |
| `GET` | `/api/v1/user/{id}` | Usuario por identificador | ADMIN | 200, 400, 401, 403, 404 |
| `POST` | `/api/v1/user` | Alta de usuario (rol, clave BCrypt y cliente asociado) | ADMIN | 201, 400, 401, 403 |
| `PATCH` | `/api/v1/user/{id}` | Actualiza nombres, correo, estado, rol y cliente | ADMIN | 200, 400, 401, 403, 404 |
| `PATCH` | `/api/v1/user/{id}/clave` | Cambia la clave del usuario | ADMIN | 204, 400, 401, 403, 404 |
| `DELETE` | `/api/v1/user/{id}` | Baja física de una cuenta y sus roles | ADMIN | 204, 400, 401, 403, 404 |

Validaciones de entrada:

| Endpoint | Campo / parámetro | Regla |
| :--- | :--- | :--- |
| `POST /api/v1/evaluation` | `cliente` | Obligatorio; 1-20 letras, números o guiones |
| | `monto` | Mayor que cero; hasta 16 enteros y 2 decimales |
| | `categoria` | Opcional; letras y `_`, máximo 40 caracteres (vacío = `otros`) |
| | `hora` | 0-23 |
| | `pais`, `dispositivo`, `ip` | Obligatorios; sólo los valores permitidos del simulador |
| | `beneficiario` | Opcional (`recurrente`/`nuevo`); el portal del cliente lo deriva del historial |
| | `beneficiarioNombre` | Opcional; hasta 150 caracteres. Identifica al destinatario y con él se deriva el tipo |
| | `operacionesUltimaHora` | 0-1000 |
| `PATCH /api/v1/evaluation/{id}` | `id` | Número entero positivo |
| `GET /api/v1/metric/*` | `days` | 1-365 (rango máximo del dashboard) |

Códigos de error relevantes:

- `400`: validación fallida, JSON malformado, cuerpo ausente, parámetro con tipo incorrecto o parámetro obligatorio ausente. Siempre responde `{"errores":[...]}`.
- `401`: sesión ausente.
- `403`: rol sin permiso.
- `404`: cliente o evaluación inexistente.
- `405`: método no permitido, por ejemplo `DELETE`.
- `415`: `Content-Type` distinto de `application/json`.

### Páginas MVC (fuera de la API)

Rutas Thymeleaf servidas por `presentation/web`. No son endpoints REST: devuelven HTML, no JSON.

| Ruta | Controlador | Roles | Descripción |
| :--- | :--- | :--- | :--- |
| `/` | `DashboardController` | ADMIN, ANALISTA | Panel general con resumen y movimientos |
| `/login`, `/logout` | `LoginController` | Públicas | Autenticación y cierre de sesión |
| `/operacion` | `OperacionClienteController` | CLIENTE | Banca en línea del cliente autenticado |
| `/transacciones` | `TransaccionViewController` | ADMIN, ANALISTA | Historial de evaluaciones y KPIs |
| `/reglas` | `ReglaViewController` | ADMIN, ANALISTA | Catálogo de reglas del motor |
| `/reportes` | `ReporteController` | ADMIN, ANALISTA | Reportes con filtros y exportación CSV/Excel/PDF |
| `/simulador` | `SimuladorViewController` | ADMIN | Simulador manual de transacciones |
| `/usuarios` | `UsuarioViewController` | ADMIN | Gestión de usuarios (CRUD) |

Un ANALISTA que intente abrir `/operacion`, `/simulador` o `/usuarios` se redirige a `/`; un CLIENTE que intente cualquier página distinta de `/operacion` se redirige a `/operacion`. La política vive en `presentation/security/AuthorizationPolicy`.

---

## Codificación de archivos

Todo el código y los recursos son **UTF-8 sin BOM**, con finales de línea CRLF en Windows (`.gitattributes` mantiene `mvnw` en LF). El `pom.xml` fija `project.build.sourceEncoding=UTF-8` y `project.reporting.outputEncoding=UTF-8`, de modo que `javac` interpreta los acentos igual con JDK 17 o superior, sin depender del *default charset* de la máquina. En tiempo de ejecución, `server.servlet.encoding.charset=UTF-8` con `force=true` garantiza que las respuestas HTTP declaren UTF-8.

---

## Operación y calidad

**Base de datos local con Docker** (opcional, en lugar de instalar PostgreSQL):

```powershell
docker compose up -d
```

`docker-compose.yml` levanta PostgreSQL 18 con las variables de `.env` y aplica el esquema canónico solo la primera vez, cuando el volumen está vacío. Para reaplicarlo: `docker compose down -v`. Si ya tienes PostgreSQL en el puerto 5432, define `DB_PORT=5433` en `.env`.

**Salud de la aplicación:** `GET /actuator/health` responde `{"status":"UP"}` sin sesión y sin detalles de la base de datos. El resto de endpoints de Actuator no están expuestos.

**Cobertura:** `.\mvnw.cmd test` genera el reporte JaCoCo en `target/site/jacoco/index.html`.

**Integración continua:** `.github/workflows/ci.yml` ejecuta las pruebas con JDK 21 en cada push a `main` y en cada pull request, y publica el reporte de cobertura como artefacto.

---

## Documentación del proyecto

- `README.md` — este documento: arquitectura, API, páginas, esquema y pruebas.
- `docs/` — documentación técnica detallada (arquitectura, funcionalidades, reglas, consultas JPA, valor de negocio).
- `CUMPLIMIENTO_TAREAS.md` — informe de cumplimiento por tarea (seguridad, sesión, roles, aislamiento, persistencia y baja lógica).
- `Instrucciones.MD` — consigna original del trabajo.

---

## Tecnologías

- Java 21 LTS
- Spring Boot 4.1.1 (Web MVC, Data JPA, Validation)
- Thymeleaf
- Spring Security Crypto (BCrypt)
- PostgreSQL 18
- H2 (solo pruebas)
- Bootstrap 5.3, Bootstrap Icons, Chart.js
- JUnit 5, MockMvc, AssertJ
- Maven Wrapper

---

© 2026 **Lambda Shield** - Proyecto académico de detección de fraude bancario.
