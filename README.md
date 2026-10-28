# 🛡️ LAMBDA SHIELD · Sistema de Detección de Fraude Bancario

> **Plataforma web bancaria y motor heurístico de prevención de fraude en tiempo real.**  
> Desarrollado con **Java 21 LTS**, **Spring Boot 4.1.1**, **Thymeleaf**, **Bootstrap 5** y **JavaScript Vanilla**.

---

## 📋 Descripción del Proyecto

**Lambda Shield** es una solución integral orientada a la banca digital que combina una interfaz de operaciones financieras seguras para clientes con un avanzado centro de control y monitoreo para analistas y administradores antifraude.

El núcleo de la solución implementa un **motor de reglas de scoring ponderado** alineado estrictamente con las especificaciones técnicas del documento de arquitectura (Sección 18), evaluando cada transacción frente a múltiples factores de riesgo contextuales y de comportamiento.

---

## 🏛️ Arquitectura del Sistema (MVC Estricto)

El proyecto sigue formalmente el patrón arquitectónico **Modelo - Vista - Controlador (MVC)** estructurado en 3 capas de paquetes bien delimitadas en Java:

```text
src/main/java/com/lambdashield/fraude/
│
├── FraudeApplication.java            # Punto de entrada de la aplicación Spring Boot
│
├── model/                            # ◄── CAPA MODELO (Datos, reglas y lógica de negocio)
│   ├── entity/                       # Clases de dominio JPA (Cliente, Cuenta, Transacción, etc.)
│   ├── enums/                        # Tipos enumerados fijos (NivelRiesgo, EstadoTransaccion, etc.)
│   ├── request/                      # DTOs de entrada enviados por el cliente/usuario
│   ├── response/                     # DTOs estructurados de salida hacia la vista o API
│   ├── store/                        # Repositorios en memoria thread-safe con datos semilla
│   ├── rule/                         # Motor antifraude: 8 reglas heurísticas (RF001–RF008)
│   ├── service/                      # Lógica del dominio (evaluación, conciliación, reportes)
│   └── repository/                   # Interfaces Spring Data JPA preparadas para persistencia
│
├── controller/                       # ◄── CAPA CONTROLADOR (Recepción y ruteo de peticiones)
│   ├── view/                         # Controladores MVC que renderizan vistas HTML con Thymeleaf
│   ├── api/                          # Controladores REST que exponen servicios web en formato JSON
│   └── config/                       # Interceptores de sesión web y configuración de rutas
│
└── view/                             # ◄── CAPA VISTA (Documentación de la capa de presentación)
    └── ViewLayer.java                # Enlace documental hacia las plantillas Thymeleaf
```

> **Nota sobre la Vista:** Las plantillas HTML de la vista son procesadas por Thymeleaf y se ubican por convención del framework en `src/main/resources/templates/`.

---

## ✨ Características Principales

### 👤 1. Portal del Cliente (Banca en Línea)
* **Transferencias Bancarias Protegidas:** Formulario guiado para transferencias locales e interbancarias.
* **Evaluación en Tiempo Real:** Validación preventiva instantánea contra el motor antifraude.
* **Historial Dinámico de Operaciones:** Registro visual de transferencias procesadas con badges de estado y actualización automática de saldo contable.

### 🛡️ 2. Panel del Administrador y Analista Antifraude
* **Panorama Financiero Interactivo:** Métricas de saldo, ingresos y egresos con gráfico dinámico (*Chart.js*) filtrable por periodos (`7d`, `15d`, `1m`, `3m`, `1a`).
* **Historial Reciente Sincronizado:** Tabla de movimientos vinculada en tiempo real a las transacciones evaluadas por el sistema.
* **Simulador de Transacciones con Radar SVG:** Entorno de pruebas para simular tráfico manual y automático, con desglose de factores de riesgo e indicador gráfico radial.
* **Catálogo de Reglas y Scoring:** Matriz explicativa con las 8 reglas oficiales (RF001 a RF008), pesos iniciales y umbrales de decisión.
* **Centro de Reportes y Auditoría:** Resumen ejecutivo de capital prevenido, tasas de bloqueo y exportación de datos a formato **CSV**.

---

## ⚙️ Motor de Reglas Antifraude (Punto 18 de Arquitectura)

Cada transacción es evaluada por un conjunto desacoplado de 8 reglas (`com.lambdashield.fraude.model.rule.*`):

| Código | Regla | Condición de Disparo | Peso Inicial |
| :--- | :--- | :--- | :---: |
| **RF001** | Monto inusual | Gradual según `monto / promedio_30d`: `≤1.5x` = 0; `>1.5x y ≤3x` = 5–15; `>3x y <5x` = 15–22; `≥5x` = 25 | **25 pts** |
| **RF002** | Dispositivo nuevo | `es_dispositivo_nuevo = true` | **15 pts** |
| **RF003** | IP nueva | `es_ip_nueva = true` | **10 pts** |
| **RF004** | País nuevo | `es_pais_nuevo = true` (jurisdicción atípica) | **30 pts** |
| **RF005** | Horario inusual | `es_horario_inusual = true` (00:00 – 04:59) | **10 pts** |
| **RF006** | Alta frecuencia | Gradual según `cantidad_transacciones_1h`: `≤1` = 0; `2–3` = 5–12; `4–5` = 15–20; `>5` = 25 | **25 pts** |
| **RF007** | Viaje imposible | Heurística país + operaciones + IP (aproximación; sin geolocalización real) | **40 pts** |
| **RF008** | Beneficiario nuevo + monto alto | `nuevo && monto > umbral` (0 pts si no se cumple) | **20 pts** |

> **Nota RF007:** La regla de viaje imposible es una heurística simplificada basada en país, cantidad de operaciones e IP. No calcula distancia/tiempo real; queda pendiente integrar geolocalización para una validación física precisa.

### 🎯 Escala de Scoring y Decisiones
* **0 – 29 pts**: **BAJO** $\rightarrow$ `APROBAR` (Operación legítima procesada de inmediato).
* **30 – 59 pts**: **MEDIO** $\rightarrow$ `MONITOREAR` (Aprobada con trazabilidad reforzada).
* **60 – 79 pts**: **ALTO** $\rightarrow$ `RETENER / ALERTAR` (Pausa temporal para validación).
* **80 – 100 pts**: **CRÍTICO** $\rightarrow$ `BLOQUEAR / ALERTA CRÍTICA` (Detención preventiva inmediata).
* *Normalización:* La sumatoria acumulada trunca automáticamente al tope de **100 puntos**.

---

## 🔐 Credenciales y Permisos por Rol

> **Nota:** usuarios de prueba definidos en `UsuarioStore`. Las contraseñas no se publican en la documentación y se almacenan cifradas con BCrypt.

| Rol | Usuario | Páginas permitidas | API permitida |
| :--- | :--- | :--- | :--- |
| **Administrador** | `admin` | Todas: Inicio, Simulador, Transacciones, Reglas, Reportes y Operación. | Todos los métodos de `/api/**`, incluido `DELETE /api/v1/evaluation`. |
| **Analista** | `analista` | Inicio, Transacciones, Reglas y Reportes. `/operacion` y `/simulador` redirigen a `/`. | Solo lectura (`GET`) de `/api/**`. `POST` y `DELETE` responden `403`. |
| **Cliente** | `usuario` *(o `maria`, `cliente`)* | Solo `/operacion`. El resto de páginas redirige a `/operacion`. | Solo `POST /api/v1/evaluation`; el resto de `/api/**` responde `403`. |

**Reglas de sesión y autorización:**
* Sin sesión, cualquier endpoint `/api/**` responde `401` con `{"error":"No autenticado"}`.
* Sin sesión, las páginas protegidas redirigen a `/login`.
* Si el rol no tiene permiso, la API responde `403` con `{"error":"No autorizado"}`.
* En el portal del cliente, el código de cliente se toma de la sesión; el `cliente` enviado en el body no puede suplantar a otro titular.
* El analista es de solo lectura: no ve el simulador ni ejecuta acciones destructivas como limpiar el historial.

---

## 🛠️ Requisitos Previos

* **Java Development Kit (JDK):** Versión **21 LTS** o superior instalada.
  * Para comprobar: `java -version`
* **PostgreSQL:** El proyecto configura la conexión en `src/main/resources/application.properties` y usa `spring.jpa.hibernate.ddl-auto=validate`. Crea la base `lambda_shield` (script de referencia en `backend/entity/PostgreSQL/BD-Desarrollo web.sql`) y ajusta las credenciales antes de arrancar.
* **Git:** Para clonar el repositorio.
* **Navegador web moderno:** Google Chrome, Microsoft Edge, Firefox, Brave, etc.
* *Nota:* **No es necesario instalar Maven por separado.** El proyecto incluye el Maven Wrapper (`mvnw` / `mvnw.cmd`).
* *Nota sobre pruebas:* `./mvnw clean test` no requiere PostgreSQL; el perfil de test usa los stores en memoria.

---

## 🚀 Cómo Clonar y Ejecutar el Proyecto

### 1. Clonar el repositorio
Abre una terminal y ejecuta:
```bash
git clone https://github.com/TU_USUARIO/TU_REPOSITORIO.git
cd TU_REPOSITORIO
```

### 2. Ejecutar la aplicación

#### En Windows (PowerShell o CMD):
```powershell
.\mvnw.cmd spring-boot:run
```

#### En Linux o macOS:
```bash
chmod +x mvnw
./mvnw spring-boot:run
```

### 3. Acceder en el navegador
Una vez que en la consola aparezca `Started FraudeApplication in ... seconds`:
* Abre tu navegador e ingresa a: **[http://localhost:8080](http://localhost:8080)**
* Se mostrará la pantalla de inicio de sesión. Inicia con las credenciales de administrador o cliente indicadas arriba.

---

## 🧪 Pruebas Automatizadas

El proyecto incluye pruebas unitarias de las 8 reglas, pruebas de umbrales del motor y pruebas MockMvc de autenticación, autorización, validación y aislamiento del portal del cliente.

#### En Windows:
```powershell
.\mvnw.cmd clean test
```

#### En Linux o macOS:
```bash
./mvnw clean test
```

> Las pruebas no requieren PostgreSQL: usan los stores en memoria y un perfil de test que excluye la autoconfiguración de DataSource/JPA.

---

## 💻 Ejecución desde Entornos de Desarrollo (IDEs)

### IntelliJ IDEA (Recomendado):
1. Selecciona **File $\rightarrow$ Open...** y elige la carpeta del proyecto donde se encuentra el `pom.xml`.
2. Espera a que IntelliJ sincronice las dependencias de Maven.
3. Asegúrate de tener seleccionado el **JDK 21** en *Project Structure* (`Ctrl + Alt + Shift + S`).
4. Abre `src/main/java/com/lambdashield/fraude/FraudeApplication.java` y haz clic en el botón verde **Run** (o presiona `Shift + F10`).

### Visual Studio Code:
1. Abre la carpeta del proyecto en VS Code.
2. Instala la extensión **Extension Pack for Java** y **Spring Boot Extension Pack**.
3. Presiona `F5` para iniciar en modo depuración o haz clic en **Run** sobre `FraudeApplication.java`.

---

## 🌐 Catálogo de APIs REST (Clean Endpoints)

Todas las APIs siguen el estándar RESTful con responsabilidad única y versionado `/api/v1/`:

| Método | Endpoint | Descripción / Función | Roles permitidos | Códigos relevantes |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/evaluation` | Evalúa una transacción contra las 8 reglas y guarda el resultado. | `ADMIN`, `CLIENTE` (forzado a su propio código) | `200`, `400`, `401`, `403`, `404` |
| `GET` | `/api/v1/evaluation` | Obtiene el historial completo de evaluaciones registradas. | `ADMIN`, `ANALISTA` | `200`, `401`, `403` |
| `DELETE` | `/api/v1/evaluation` | Limpia el historial de evaluaciones en memoria. | `ADMIN` | `204`, `401`, `403` |
| `GET` | `/api/v1/metric` | Retorna los KPIs consolidados (Total, Aprobadas, Monitoreo, Bloqueadas, Score Promedio). | `ADMIN`, `ANALISTA` | `200`, `401`, `403` |
| `GET` | `/api/v1/metric/summary?days={n}` | Retorna el balance contable (`Saldo = Base + Ingresos - Egresos`) del periodo. | `ADMIN`, `ANALISTA` | `200`, `401`, `403` |
| `GET` | `/api/v1/metric/movement?days={n}` | Provee la serie temporal de ingresos/egresos para el gráfico *Chart.js*. | `ADMIN`, `ANALISTA` | `200`, `401`, `403` |
| `GET` | `/api/v1/metric/recent?days={n}` | Devuelve los movimientos recientes sincronizados para la tabla del dashboard. | `ADMIN`, `ANALISTA` | `200`, `401`, `403` |
| `GET` | `/api/v1/rule` | Devuelve la lista canónica de reglas antifraude con sus pesos y condiciones. | `ADMIN`, `ANALISTA` | `200`, `401`, `403` |
| `GET` | `/api/v1/report/export` | Genera y descarga el reporte de auditoría en formato **CSV**. | `ADMIN`, `ANALISTA` | `200`, `401`, `403` |

**Códigos de error de la API:**
* `401` — `{"error":"No autenticado"}`: no hay sesión válida.
* `403` — `{"error":"No autorizado"}`: el rol autenticado no tiene permiso para el método/recurso.
* `400` — `{"errores":[...]}`: el body no cumple las validaciones (`monto`, `hora`, `pais`, `dispositivo`, `beneficiario`, `ip`, etc.).
* `404` — `{"errores":[...]}`: el cliente indicado no existe.

---

## 👥 Tecnologías Utilizadas

* **Lenguaje:** Java 21 LTS
* **Framework:** Spring Boot 4.1.1 (Spring Web MVC, Spring Data JPA, Bean Validation)
* **Motor de Plantillas:** Thymeleaf
* **Seguridad:** Spring Security Crypto (BCrypt) y autorización por interceptor de sesión.
* **Persistencia:** PostgreSQL (driver en runtime) con entidades y repositorios JPA. La lógica funcional de la demo usa stores en memoria.
* **Frontend:** HTML5 semántico, CSS3 personalizado, Bootstrap 5.3, Bootstrap Icons
* **Gráficos & Visualización:** Chart.js 4.4, SVG dinámico
* **Pruebas:** JUnit 5, MockMvc y AssertJ (`spring-boot-starter-test`, `spring-boot-starter-webmvc-test`)
* **Utilidades:** Lombok (opcional) y Spring Boot DevTools (runtime)
* **Gestor de Construcción:** Apache Maven Wrapper

---

© 2026 **Lambda Shield** · Proyecto Académico de Detección de Fraude Bancario.