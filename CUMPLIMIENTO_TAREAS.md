# Informe de cumplimiento de tareas

**Proyecto:** Lambda Shield  
**Archivo de referencia:** `Instrucciones.MD`  
**Fecha de verificación:** 27/10/2026  
**Estado general:** **Cumple con las tareas 1 a 10**

---

## Resumen ejecutivo

Se revisó la implementación de las tareas 1 a 10 y se ejecutaron las verificaciones finales:

- `.\mvnw.cmd clean test` → **23 pruebas, 0 fallos, 0 errores**
- `.\mvnw.cmd spring-boot:run` → **arranque correcto** con PostgreSQL 18.6 en el puerto 8080
- Sin sesión, los endpoints `/api/**` responden **401 JSON**
- Los permisos por rol están implementados en el interceptor
- El portal del cliente solo muestra las operaciones del cliente autenticado
- La API valida los datos y devuelve errores JSON 400/404
- Las reglas y los datos semilla están alineados con los umbrales documentados
- El frontend no utiliza `th:utext` ni `innerHTML` con datos del servidor o del usuario

---

## Cumplimiento por tarea

### Tarea 1 · Proteger la API

**Estado: Cumple.**

- `WebConfig.java` ya no excluye `/api/**` del interceptor.
- `AuthInterceptor.java`:
  - Sin sesión en `/api/**` → `401` con `{"error":"No autenticado"}`.
  - CLIENTE: solo `POST /api/v1/evaluation`; el resto de `/api/**` → `403` JSON.
  - ANALISTA: solo lectura (`GET`) de `/api/**`.
  - ADMIN: acceso total, incluido `DELETE /api/v1/evaluation`.
- Páginas sin sesión redirigen a `/login`.
- Los fetch de `auth.js`, `panel.js`, `transacciones.js`, `simulador.js` y `operacion.js` usan `LSApi.fetch`:
  - `401` → redirección a `/login`.
  - `403` → aviso al usuario.
- Pruebas MockMvc verifican 401 sin sesión, 403 por rol y 204 en DELETE como ADMIN.

**Archivos principales:**
`controller/config/WebConfig.java`, `controller/config/AuthInterceptor.java`, `static/js/auth.js`.

---

### Tarea 2 · Credenciales y sesión

**Estado: Cumple.**

- `spring-security-crypto` está incluido en `pom.xml`.
- `UsuarioStore` almacena hashes BCrypt y valida con `passwordEncoder.matches(...)`.
- Los usuarios semilla se mantienen.
- `LoginController.postLogin` llama a `request.changeSessionId()` después de autenticar.
- El README no publica contraseñas y deja la nota: usuarios de prueba definidos en `UsuarioStore`.

**Archivos principales:**
`model/store/UsuarioStore.java`, `controller/view/LoginController.java`, `README.md`, `pom.xml`.

---

### Tarea 3 · Separar roles ANALISTA y ADMIN

**Estado: Cumple.**

- Existe el rol `ANALISTA`, asignado al usuario `analista`.
- El interceptor aplica los permisos definidos en la tarea 1.
- El navbar oculta el enlace del simulador para el analista.
- La tarjeta **Simulador** del dashboard solo se muestra a ADMIN.
- El botón **Reiniciar Historial** solo se muestra a ADMIN.
- `/simulador` y `/operacion` redirigen al inicio cuando los solicita un ANALISTA.

**Archivos principales:**
`controller/config/AuthInterceptor.java`, `templates/fragments/navbar.html`, `templates/dashboard.html`, `templates/transacciones.html`.

---

### Tarea 4 · Fuga de datos en el portal del cliente

**Estado: Cumple.**

- `UsuarioInfo` incluye `codigoCliente`.
- Al iniciar sesión se guarda `codigoCliente` en la sesión.
- Los usuarios `usuario`, `maria` y `cliente` tienen el código `C-001`.
- `/operacion` toma el cliente desde la sesión y filtra exactamente por ese código.
- Si no hay transacciones, se devuelve una lista vacía; ya no se listan las de todos.
- En `POST /api/v1/evaluation`, si el rol es CLIENTE, el servidor ignora el cliente del body y fuerza el código de la sesión.
- Las pruebas verifican que el portal no muestra transacciones de C-002 ni C-003.

**Archivos principales:**
`model/store/UsuarioStore.java`, `controller/view/LoginController.java`, `controller/view/OperacionClienteController.java`, `controller/api/EvaluationApiController.java`, `model/store/EvaluacionStore.java`.

---

### Tarea 5 · Validación de entrada y errores

**Estado: Cumple.**

- `ClienteStore.buscar(null)` es null-safe.
- `TransaccionCrearRequest` valida:
  - `cliente` con `@NotBlank`
  - `monto` con `@Positive`
  - `hora` con `@Min(0)` y `@Max(23)`
  - `operacionesUltimaHora` con `@Min(0)`
  - `pais` con `@Pattern`
  - `dispositivo` con `@Pattern`
  - `beneficiario` con `@Pattern`
  - `ip` con `@Pattern`
- `EvaluationApiController.evaluate` usa `@Valid`.
- `GlobalExceptionHandler` está en el paquete `controller`.
- Errores de validación → `400` con `{"errores":[...]}`.
- Cliente inexistente → `404` con `{"errores":[...]}`.
- Los valores permitidos coinciden con los enviados por `simulador.js` y `operacion.js`.

**Archivos principales:**
`model/store/ClienteStore.java`, `model/request/TransaccionCrearRequest.java`, `controller/api/EvaluationApiController.java`, `controller/GlobalExceptionHandler.java`.

---

### Tarea 6 · Factores de riesgo derivados en el servidor

**Estado: Cumple.**

Para el portal del cliente, `TransaccionService.crearYEvaluarCliente`:

- Toma la hora del reloj del servidor.
- Calcula `operacionesUltimaHora` desde `EvaluacionStore`, contando las evaluaciones del cliente en la última hora.
- Guarda `LocalDateTime` en `EvaluacionResponse` y conserva el formato de fecha para mostrar.
- Compara el `User-Agent` y la IP remota contra los valores conocidos de `ClienteStore`.
- Marca el dispositivo como `reconocido`/`nuevo` y la IP como `conocida`/`nueva`.

El simulador del ADMIN conserva los valores manuales para pruebas.

Además, en el portal del cliente ya no se muestran los selectores de dispositivo, hora, IP ni frecuencia. Se mantiene únicamente el país de la operación y se informa que los demás factores se verifican automáticamente desde la sesión.

**Archivos principales:**
`model/service/TransaccionService.java`, `model/store/EvaluacionStore.java`, `model/store/ClienteStore.java`, `model/response/EvaluacionResponse.java`, `templates/cliente/operacion.html`, `static/js/operacion.js`.

---

### Tarea 7 · Alinear las reglas con la especificación

**Estado: Cumple.**

- **RF008:** solo asigna puntos si el beneficiario es nuevo y el monto supera el umbral; en caso contrario, 0 puntos.
- **RF005:** solo puntúa la franja 00:00–04:59; se eliminó el tramo de 4 puntos para horas ≥ 22.
- **RF007:** se eliminó la rama de categorías casino/cripto. La regla usa una heurística simplificada de país, operaciones e IP, documentada como aproximación pendiente de geolocalización real.
- **RF001:** la tabla del README describe la escala gradual por proporción monto/promedio.
- **Estado “alto”:** se unificó como `Retener / Alertar` en `DeteccionFraudeService` y en las vistas/JS.
- **Datos semilla:** cada puntaje coincide con `min(100, suma de factores)` y con el nivel/estado según los umbrales:
  - 0–29: bajo
  - 30–59: medio
  - 60–79: alto
  - 80–100: crítico

**Archivos principales:**
`model/rule/ReglaBeneficiario.java`, `model/rule/ReglaHorario.java`, `model/rule/ReglaViajeImposible.java`, `model/rule/ReglaMontoInusual.java`, `model/service/DeteccionFraudeService.java`, `model/store/EvaluacionStore.java`, `README.md`.

---

### Tarea 8 · Revisión XSS del frontend

**Estado: Cumple.**

- No se encontraron usos de `th:utext`.
- No se encontraron usos de `innerHTML`, `outerHTML`, `insertAdjacentHTML` ni `document.write`.
- Las plantillas usan `th:text`.
- El JavaScript construye filas y mensajes con `textContent`, `createTextNode` y `append`.
- No se identificaron concatenaciones de HTML con datos del servidor o del usuario.

**Archivos revisados:**
`src/main/resources/templates/**`, `src/main/resources/static/js/*.js`.

---

### Tarea 9 · Pruebas

**Estado: Cumple.**

Se ejecutó:

```powershell
.\mvnw.cmd clean test
```

Resultado:

```text
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Cobertura de las pruebas:

- RF001–RF008: valores límite.
- `DeteccionFraudeService`: umbrales 29/30, 59/60, 79/80 y tope de 100.
- MockMvc:
  - API sin sesión → 401.
  - CLIENTE contra endpoints de administración → 403.
  - ANALISTA con DELETE → 403.
  - ADMIN con DELETE → 204.
  - Request inválido → 400.
  - Cliente inexistente → 404.
  - `/operacion` no muestra transacciones de otros clientes.

**Archivos principales:**
`src/test/java/com/lambdashield/fraude/model/rule/ReglasFraudeTest.java`, `src/test/java/com/lambdashield/fraude/model/service/DeteccionFraudeServiceTest.java`, `src/test/java/com/lambdashield/fraude/controller/SeguridadApiMockMvcTest.java`, `src/test/java/com/lambdashield/fraude/controller/view/OperacionClienteViewMockMvcTest.java`.

---

### Tarea 10 · Limpieza del repositorio y documentación

**Estado: Cumple en lo sustancial.**

- `.gitignore` incluye `target/` y `.idea/`.
- `pom.xml` usa correctamente `<name>Lambda Shield</name>`.
- El README refleja:
  - Java 21
  - Spring Boot 4.1.1
  - Dependencias reales, incluida `spring-boot-starter-data-jpa`
  - Permisos por rol
  - Tabla de reglas actualizada
  - Catálogo de APIs con códigos 401/403/400/404
- El README no contiene contraseñas.

**Nota:** el directorio no es un repositorio Git, por lo que no se creó la rama `fix/auditoria` ni commits por tarea, y no fue necesario ejecutar `git rm --cached`.

---

## Criterios de aceptación

| Criterio | Resultado |
|---|---|
| Sin sesión, ningún endpoint de `/api/**` responde datos | Cumple: 401 JSON |
| Un cliente solo ve y opera con su propia cuenta | Cumple: filtrado por código de sesión |
| Las pruebas nuevas pasan con `clean test` | Cumple: 23/23 |
| README, código y datos semilla coinciden | Cumple |

---

## Correcciones adicionales realizadas

1. **Dashboard del analista:** la tarjeta del simulador ahora solo se muestra a ADMIN.
2. **Portal del cliente:** se eliminaron los selectores de dispositivo, hora, IP y frecuencia; el servidor deriva esos valores.
3. **Hover del dashboard:** se eliminó el `transform` de las tarjetas de módulo. Ahora el hover solo aplica una sombra, sin cambiar el tamaño ni la posición.

---

## Verificación de arranque

```text
Database version: 18.6
Tomcat started on port 8080
Started FraudeApplication in 4.411 seconds
```

La aplicación arranca correctamente con la configuración actual de PostgreSQL.

---

## Conclusión

**Las tareas 1 a 10 de `Instrucciones.MD` están cumplidas.**  
Las pruebas automatizadas pasan, el arranque funciona y los criterios de aceptación fueron verificados.
