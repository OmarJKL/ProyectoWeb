# Lambda Shield - Sistema de Detección de Fraude Bancario

> Plataforma web bancaria y motor heurístico de prevención de fraude en tiempo real.
> Desarrollada con **Java 21 LTS**, **Spring Boot 4.1.1**, **Spring Data JPA**, **Thymeleaf**, **Bootstrap 5** y **JavaScript Vanilla**.

---

## Descripción del proyecto

**Lambda Shield** combina una interfaz de operaciones financieras para clientes con un centro de control para analistas y administradores antifraude.

El núcleo implementa un **motor de reglas de scoring ponderado** (RF001-RF008) alineado con la especificación de arquitectura. Cada evaluación se guarda en PostgreSQL junto con su transacción, cliente, cuenta, dispositivo, beneficiario y desglose de reglas.

---

## Arquitectura (MVC)

```text
src/main/java/com/lambdashield/fraude/
├── FraudeApplication.java
├── model/
│   ├── entity/          # Entidades JPA (Cliente, Transaccion, EvaluacionFraude, etc.)
│   ├── enums/           # NivelRiesgo, EstadoTransaccion, DecisionFraude, etc.
│   ├── request/         # DTOs de entrada
│   ├── response/        # DTOs de salida
│   ├── rule/            # Ocho reglas heurísticas RF001-RF008
│   ├── repository/      # Repositorios Spring Data JPA
│   └── service/         # Autenticación, evaluaciones, clientes, métricas y reportes
├── controller/
│   ├── view/            # Controladores MVC (Thymeleaf)
│   ├── api/             # Controladores REST
│   └── config/          # Interceptor de sesión, BCrypt y carga inicial de datos
└── view/
```

Las plantillas están en `src/main/resources/templates/` y los recursos web en `src/main/resources/static/`.

**No existen stores en memoria.** Usuarios, clientes y evaluaciones se leen y escriben mediante JPA en PostgreSQL.

---

## Persistencia

La aplicación usa las tablas existentes del esquema PostgreSQL:

- `usuario_sistema`, `rol`, `usuario_rol`
- `cliente`, `cuenta`, `dispositivo`, `beneficiario`
- `transaccion`, `evaluacion_fraude`, `evaluacion_regla`
- `regla_fraude`, `canal`, `tipo_transaccion`
- tablas auxiliares de alertas, casos y auditoría

El esquema canónico está en `backend/entity/PostgreSQL/BD-Desarrollo web.sql`.
Los cambios de migración aplicados a una base existente están en `backend/entity/PostgreSQL/migracion_soft_delete_postgresql.sql`.

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

No existe un endpoint `DELETE`. Las peticiones `DELETE` a `/api/v1/evaluation` responden `405`.

En la vista de historial, el administrador dispone del botón **Desactivar** por fila. El antiguo botón **Reiniciar Historial** fue eliminado.

---

## Características principales

### Portal del cliente

- Transferencias protegidas con evaluación antifraude en tiempo real.
- Factores de dispositivo, IP, horario y frecuencia derivados en el servidor.
- Historial limitado a las operaciones del cliente autenticado.
- Saldo y número de cuenta obtenidos desde PostgreSQL.

### Panel administrativo y analista

- Dashboard con métricas de saldo, ingresos y egresos.
- Historial de evaluaciones activas con filtros por nivel.
- Simulador manual de transacciones con desglose de factores.
- Catálogo de reglas leído desde `regla_fraude`.
- Reportes y exportación CSV.
- Desactivación lógica de evaluaciones por parte de ADMIN.

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
| **ADMIN** | `admin` | Todas | Todos los métodos permitidos, incluido `PATCH /api/v1/evaluation/{id}` |
| **ANALISTA** | `analista` | Inicio, transacciones, reglas y reportes | Solo `GET /api/**` |
| **CLIENTE** | `usuario`, `maria`, `cliente` | Solo `/operacion` | Solo `POST /api/v1/evaluation` para su propio cliente |

Reglas de sesión:

- Sin sesión, `/api/**` responde `401` con `{"error":"No autenticado"}`.
- Sin sesión, las páginas protegidas redirigen a `/login`.
- Si el rol no tiene permiso, la API responde `403` con `{"error":"No autorizado"}`.
- El portal del cliente usa el `codigoCliente` de la sesión y no confía en el cliente enviado en el body.
- `DELETE` no está permitido en ninguna ruta de la API.

Las contraseñas de demostración no se publican en esta documentación; están definidas en `DataInitializer` y se almacenan cifradas en la base de datos.

---

## Requisitos

- **JDK 21 LTS** o superior.
- **PostgreSQL** con la base `lambda_shield` creada.
- Maven Wrapper incluido (`mvnw.cmd` / `mvnw`).
- Navegador moderno.

La conexión se configura en `src/main/resources/application.properties`. Por defecto usa:

```text
jdbc:postgresql://localhost:5432/lambda_shield
```

Antes de arrancar, aplica `backend/entity/PostgreSQL/BD-Desarrollo web.sql` en una base nueva o `backend/entity/PostgreSQL/migracion_soft_delete_postgresql.sql` en una base existente. El runtime usa `spring.jpa.hibernate.ddl-auto=validate`.

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

Las pruebas usan **H2 en modo PostgreSQL**, con `ddl-auto=create-drop`, por lo que **no requieren una instancia de PostgreSQL**. Incluyen:

- valores límite de las ocho reglas;
- umbrales del motor de detección;
- autenticación y autorización por rol;
- validación de requests;
- baja lógica con `PATCH`;
- rechazo de `DELETE`;
- aislamiento del portal del cliente.

---

## API REST

| Método | Endpoint | Descripción | Roles | Códigos |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/evaluation` | Evalúa y persiste una transacción | ADMIN, CLIENTE (forzado a su propio código) | 200, 400, 401, 403, 404 |
| `GET` | `/api/v1/evaluation` | Lista evaluaciones activas | ADMIN, ANALISTA | 200, 401, 403 |
| `PATCH` | `/api/v1/evaluation/{id}` | Desactiva una evaluación (baja lógica) | ADMIN | 204, 401, 403, 404 |
| `GET` | `/api/v1/metric` | KPIs consolidados | ADMIN, ANALISTA | 200, 401, 403 |
| `GET` | `/api/v1/metric/summary?days={n}` | Balance contable del periodo | ADMIN, ANALISTA | 200, 401, 403 |
| `GET` | `/api/v1/metric/movement?days={n}` | Serie temporal para el gráfico | ADMIN, ANALISTA | 200, 401, 403 |
| `GET` | `/api/v1/metric/recent?days={n}` | Movimientos recientes | ADMIN, ANALISTA | 200, 401, 403 |
| `GET` | `/api/v1/rule` | Reglas activas desde la base de datos | ADMIN, ANALISTA | 200, 401, 403 |
| `GET` | `/api/v1/report/export` | Reporte CSV | ADMIN, ANALISTA | 200, 401, 403 |

Códigos de error relevantes:

- `400`: body inválido, con `{"errores":[...]}`.
- `401`: sesión ausente.
- `403`: rol sin permiso.
- `404`: cliente o evaluación inexistente.
- `405`: método no permitido, por ejemplo `DELETE`.

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
