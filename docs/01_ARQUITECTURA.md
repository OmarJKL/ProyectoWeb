# Lambda Shield — Arquitectura detallada

## 1. Visión general

Lambda Shield es una aplicación bancaria de detección de fraude en tiempo real. Combina un portal de banca en línea para clientes con un panel de control para analistas y administradores, y un motor heurístico de reglas (RF001–RF008) que puntúa cada transferencia entre 0 y 100.

- **Stack:** Java 21 LTS, Spring Boot 4.1.1, Spring Data JPA, Thymeleaf, Bootstrap 5, PostgreSQL 18 (H2 en modo PostgreSQL para pruebas).
- **Patrón:** arquitectura N capas con separación física de frontend y backend sobre un único artefacto Maven.
- **Persistencia:** JPA con `ddl-auto=validate` (el esquema se aplica manualmente y Hibernate comprueba el mapeo al arrancar).

## 2. Estructura del repositorio

```
frontend/src/main/resources/
├── templates/           # Vistas Thymeleaf (dashboard, transacciones, reportes, operacion, simulador, usuarios, login)
├── static/              # CSS, JavaScript e imágenes
│   ├── js/              # auth.js, panel.js, transacciones.js, simulador.js, operacion.js, reportes.js, usuarios.js
│   └── css/estilos.css  # Sistema de diseño (variables, componentes)
backend/src/main/
├── java/com/lambdashield/fraude/
│   ├── presentation/    # Capa de presentación (HTTP y vistas)
│   │   ├── api/rest/    # Controladores REST (/api/v1/**)
│   │   ├── web/         # Controladores MVC (Thymeleaf)
│   │   ├── dto/         # Contratos JSON de entrada y salida
│   │   ├── mapper/      # Traduce modelos de aplicación a DTO y etiquetas visuales
│   │   ├── export/      # Serialización CSV, Excel y PDF
│   │   ├── security/    # Interceptor y política de autorización por rol
│   │   └── exception/   # Traducción de errores a HTTP/vistas
│   ├── application/     # Capa de aplicación (casos de uso)
│   │   ├── command/     # Comandos de entrada
│   │   ├── model/       # Modelos de resultado independientes de la interfaz
│   │   ├── mapper/      # Entidades JPA -> modelos de aplicación
│   │   └── service/     # Casos de uso, consultas y coordinación
│   ├── domain/          # Capa de dominio (sin dependencias hacia otras capas)
│   │   ├── entity/      # Entidades JPA del modelo de negocio
│   │   ├── enums/       # Estados y tipos del dominio
│   │   ├── fraud/       # Motor antifraude: factores, reglas evaluables
│   │   ├── device/      # Huella de dispositivo
│   │   └── customer/    # Reglas de presentación del nombre del cliente
│   ├── repository/      # Persistencia: contratos Spring Data JPA
│   └── config/          # Composición y configuración (raíz de dependencias)
│       └── seed/        # Sembradores demo idempotentes por responsabilidad
└── resources/           # application.properties y scripts SQL
backend/src/test/         # Pruebas unitarias, de integración y de arquitectura
```

## 3. Capas y reglas de dependencia

El flujo es unidireccional: `presentation → application → domain`, con `repository` como persistencia usada por `application`. `config` es la raíz de composición y la única que puede conocer a todas las demás.

<|message|> Capa | Qué vive ahí | Qué no puede hacer |
| :--- | :--- | :--- |
| `presentation` | Controladores REST/MVC, DTO, mappers HTTP, exportadores | No accede a `repository` ni a `domain` directamente |
| `application` | Casos de uso, comandos, modelos de resultado, mappers de entidad | No depende de `jakarta.servlet`, `org.springframework.web` ni de `config` |
| `domain` | Entidades JPA, enums, reglas puras (`ReglaEvaluable`) | Sin dependencias de Spring |
| `repository` | Contratos Spring Data JPA | Solo la usa `application` y `config` |
| `config` | Composición, sembradores, demo | Es la raíz de dependencias |

Las reglas están verificadas automáticamente por **ArchUnit** en `ArquitecturaCapasTest` (6 reglas): orden de capas, dominio sin Spring, aplicación sin web, controladores en `presentation`, controladores sin acceso a repositorios y sin ciclos entre módulos.

## 4. Separación REST vs no REST

- **REST** (`presentation/api/rest`): `/api/v1/evaluation`, `/api/v1/metric`, `/api/v1/rule`, `/api/v1/report`, `/api/v1/user`. Devuelven JSON.
- **MVC** (`presentation/web`): `/`, `/login`, `/logout`, `/operacion`, `/transacciones`, `/reglas`, `/reportes`, `/simulador`, `/usuarios`. Devuelven HTML Thymeleaf.
- El interceptor `AuthInterceptor` y la política `AuthorizationPolicy` distinguen ambos mundos: sin sesión, `/api/**` responde `401` JSON y las páginas redirigen a `/login`.

## 5. Autorización por rol

| Rol | Páginas | API |
| :--- | :--- | :--- |
| ADMIN | Todas | Todos los métodos, incluidos PATCH de evaluaciones, CRUD de usuarios y su DELETE |
| ANALISTA | Inicio, transacciones, reglas, reportes | Solo GET de `/api/**`, excepto `/api/v1/user` (reservado a ADMIN) |
| CLIENTE | Solo `/operacion` | Solo POST `/api/v1/evaluation` para su propio cliente |

`DELETE` está bloqueado en toda la API excepto `/api/v1/user/{id}` (gestión de usuarios, solo ADMIN). Las evaluaciones usan baja lógica con `PATCH`.

## 6. Seguridad

- Credenciales en BCrypt sobre `usuario_sistema`; comparación con `matches`.
- `request.changeSessionId()` tras autenticar (evita session fixation).
- `@Valid` y Bean Validation en `TransaccionCrearRequest` y en los DTO de usuarios.
- `GlobalExceptionHandler` traduce validaciones a `400 {"errores":[...]}`, cliente inexistente a `404`, método no permitido a `405`, tipo de contenido a `415`.
- El cliente no puede declarar factores de riesgo: hora, frecuencia, dispositivo e IP se derivan en el servidor.
- El portal del cliente usa el `codigoCliente` de la sesión y nunca el del body.
