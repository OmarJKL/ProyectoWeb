# 🛡️ LAMBDA SHIELD · Sistema de Detección de Fraude Bancario

> **Plataforma web bancaria y motor heurístico de prevención de fraude en tiempo real.**  
> Desarrollado con **Java 21 LTS**, **Spring Boot 3**, **Thymeleaf**, **Bootstrap 5** y **JavaScript Vanilla**.

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
| **RF001** | Monto inusual | `monto > 5 x promedio_30d` | **25 pts** |
| **RF002** | Dispositivo nuevo | `es_dispositivo_nuevo = true` | **15 pts** |
| **RF003** | IP nueva | `es_ip_nueva = true` | **10 pts** |
| **RF004** | País nuevo | `es_pais_nuevo = true` (jurisdicción atípica) | **30 pts** |
| **RF005** | Horario inusual | `es_horario_inusual = true` (00:00 – 05:00) | **10 pts** |
| **RF006** | Alta frecuencia | `cantidad_transacciones_1h > 5` | **25 pts** |
| **RF007** | Viaje imposible | `distancia / tiempo excede umbral físico` | **40 pts** |
| **RF008** | Beneficiario nuevo + monto alto | `nuevo && monto > umbral` | **20 pts** |

### 🎯 Escala de Scoring y Decisiones
* **0 – 29 pts**: **BAJO** $\rightarrow$ `APROBAR` (Operación legítima procesada de inmediato).
* **30 – 59 pts**: **MEDIO** $\rightarrow$ `MONITOREAR` (Aprobada con trazabilidad reforzada).
* **60 – 79 pts**: **ALTO** $\rightarrow$ `RETENER / ALERTAR` (Pausa temporal para validación).
* **80 – 100 pts**: **CRÍTICO** $\rightarrow$ `BLOQUEAR / ALERTA CRÍTICA` (Detención preventiva inmediata).
* *Normalización:* La sumatoria acumulada trunca automáticamente al tope de **100 puntos**.

---

## 🔐 Credenciales de Acceso para Pruebas

| Rol | Usuario | Contraseña | Vistas y Accesos Disponibles |
| :--- | :--- | :--- | :--- |
| **Administrador** | `admin` | `lambda2026` | Inicio, Simulador, Transacciones, Motor de Reglas, Reportes |
| **Analista** | `analista` | `fraude123` | Monitoreo general, auditoría e investigación |
| **Cliente Estándar** | `usuario` *(o `maria`)* | `cliente123` *(o `123456`)* | Banca en Línea, realizar transferencias, saldo personal |

---

## 🛠️ Requisitos Previos

* **Java Development Kit (JDK):** Versión **21 LTS** o superior instalada.
  * Para comprobar: `java -version`
* **Git:** Para clonar el repositorio.
* **Navegador web moderno:** Google Chrome, Microsoft Edge, Firefox, Brave, etc.
* *Nota:* **No es necesario instalar Maven por separado.** El proyecto incluye el Maven Wrapper (`mvnw` / `mvnw.cmd`).

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

| Método | Endpoint | Descripción / Función |
| :--- | :--- | :--- |
| `POST` | `/api/v1/evaluation` | Evalúa una transacción contra las 8 reglas y guarda el resultado. |
| `GET` | `/api/v1/evaluation` | Obtiene el historial completo de evaluaciones registradas. |
| `DELETE` | `/api/v1/evaluation` | Limpia el historial de evaluaciones en memoria. |
| `GET` | `/api/v1/metric` | Retorna los KPIs consolidados (Total, Aprobadas, Monitoreo, Bloqueadas, Score Promedio). |
| `GET` | `/api/v1/metric/summary?days={n}` | Retorna el balance contable (`Saldo = Base + Ingresos - Egresos`) del periodo. |
| `GET` | `/api/v1/metric/movement?days={n}` | Provee la serie temporal de ingresos/egresos para el gráfico *Chart.js*. |
| `GET` | `/api/v1/metric/recent?days={n}` | Devuelve los movimientos recientes sincronizados para la tabla del dashboard. |
| `GET` | `/api/v1/rule` | Devuelve la lista canónica de reglas antifraude con sus pesos y condiciones. |
| `GET` | `/api/v1/report/export` | Genera y descarga el reporte de auditoría en formato **CSV**. |

---

## 👥 Tecnologías Utilizadas

* **Lenguaje:** Java 21 LTS
* **Framework:** Spring Boot 3.x (Spring Web, Spring MVC, Spring Data JPA, Bean Validation)
* **Motor de Plantillas:** Thymeleaf
* **Frontend:** HTML5 semántico, CSS3 personalizado, Bootstrap 5.3, Bootstrap Icons
* **Gráficos & Visualización:** Chart.js 4.4, SVG dinámico
* **Gestor de Construcción:** Apache Maven Wrapper

---

© 2026 **Lambda Shield** · Proyecto Académico de Detección de Fraude Bancario.