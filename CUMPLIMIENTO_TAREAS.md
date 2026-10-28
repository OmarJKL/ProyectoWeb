# Informe de cumplimiento de tareas

**Proyecto:** Lambda Shield  
**Archivo de referencia:** `Instrucciones.MD`  
**Última verificación:** 28/10/2026  
**Estado general:** tareas 1 a 10 cumplidas y persistencia migrada a PostgreSQL.

---

## Resumen ejecutivo

- `.\mvnw.cmd clean test` -> **24 pruebas, 0 fallos, 0 errores**.
- `.\mvnw.cmd spring-boot:run` -> arranque correcto con PostgreSQL 18.6 en el puerto 8080.
- Los usuarios, clientes y evaluaciones se leen y escriben en PostgreSQL mediante JPA.
- Se eliminaron los stores en memoria (`EvaluacionStore`, `ClienteStore`, `UsuarioStore`).
- La baja de evaluaciones es lógica mediante `PATCH`; no existe endpoint `DELETE`.
- Las evaluaciones desactivadas permanecen en la base con `es_activa = false` y su transacción pasa a `REVERSADA`.
- El portal del cliente solo muestra las operaciones del cliente autenticado.
- La API valida los datos y devuelve errores JSON 400/404/405.
- Las pruebas de integración usan H2 en modo PostgreSQL y no requieren una instancia real.

---

## Persistencia PostgreSQL

### Entidades y repositorios

La aplicación utiliza las entidades JPA existentes y sus repositorios Spring Data:

- `UsuarioSistema`, `Rol`, `UsuarioRol` -> autenticación y roles.
- `Cliente`, `Cuenta`, `Dispositivo` -> datos del cliente y factores de riesgo.
- `Transaccion`, `EvaluacionFraude`, `EvaluacionRegla` -> evaluación y desglose de reglas.
- `ReglaFraude`, `Canal`, `TipoTransaccion` -> catálogos.

`AuthService` valida las credenciales contra `usuario_sistema` con BCrypt y resuelve el rol desde `usuario_rol`/`rol`. `ClienteService` obtiene clientes, cuentas, dispositivos e IP conocidas. `TransaccionService` persiste la transacción, la evaluación y cada regla aplicada.

### Cambios de esquema

Se añadieron las columnas necesarias para la aplicación:

- `cliente.codigo_cliente`, `cliente.promedio_gasto`, `cliente.es_activo`
- `dispositivo.direccion_ip`
- `evaluacion_fraude.es_activa`, `evaluacion_fraude.fecha_desactivacion`
- `usuario_sistema.id_cliente`

También se normalizaron columnas `jsonb`/`inet` a `TEXT`/`VARCHAR` para que el mismo modelo funcione en PostgreSQL y H2.

El esquema canónico actualizado está en `backend/entity/PostgreSQL/BD-Desarrollo web.sql`.
La migración para una base existente está en `backend/entity/PostgreSQL/migracion_soft_delete_postgresql.sql`.

### Carga inicial idempotente

`DataInitializer` crea, solo si no existen:

- roles ADMIN, ANALISTA y CLIENTE;
- usuarios de demostración con contraseñas BCrypt;
- clientes C-001, C-002 y C-003 con cuentas y dispositivos;
- catálogos de canales y tipos de transacción;
- las ocho reglas RF001-RF008;
- diez evaluaciones de demostración.

Al reiniciar la aplicación no se duplican los catálogos ni las evaluaciones.

---

## Baja lógica mediante PATCH

### Endpoint

```http
PATCH /api/v1/evaluation/{id}
```

Comportamiento:

1. Busca una evaluación activa por su identificador interno.
2. Marca `es_activa = false`.
3. Registra `fecha_desactivacion`.
4. Cambia la transacción asociada a `REVERSADA`.
5. Devuelve `204 No Content`.

La fila nunca se elimina físicamente. Las consultas de historial, dashboard, reportes y KPIs filtran por `es_activa = true`.

### Interfaz

En `transacciones.html` se eliminó el botón **Reiniciar Historial**. En su lugar, cada fila muestra **Desactivar** únicamente para ADMIN. `transacciones.js` invoca el `PATCH` y recarga la vista.

### DELETE bloqueado

- No existe `@DeleteMapping` en la aplicación.
- `DELETE /api/v1/evaluation` responde `405 Method Not Allowed`.
- `AuthInterceptor` rechaza cualquier `DELETE` sobre `/api/**` antes de autorizar por rol.

---

## Cumplimiento por tarea

### Tarea 1 - Proteger la API

**Cumple.**

- `/api/**` está cubierto por `AuthInterceptor`.
- Sin sesión -> `401` JSON.
- CLIENTE -> solo `POST /api/v1/evaluation`.
- ANALISTA -> solo `GET`.
- ADMIN -> todos los métodos permitidos, incluido PATCH.
- DELETE no se permite para ningún rol.

### Tarea 2 - Credenciales y sesión

**Cumple.**

- `AuthService` autentica contra PostgreSQL con BCrypt.
- `LoginController` renueva el identificador de sesión después de autenticar.
- El README no publica contraseñas.
- La nota de la pantalla de login referencia la tabla `usuario_sistema`.

### Tarea 3 - Separar ANALISTA y ADMIN

**Cumple.**

- El rol ANALISTA es de solo lectura.
- No ve el simulador ni el portal del cliente.
- No puede ejecutar PATCH.
- ADMIN conserva el acceso completo y la acción de desactivación.

### Tarea 4 - Aislamiento del portal del cliente

**Cumple.**

- `codigoCliente` se guarda en la sesión al iniciar sesión.
- `/operacion` consulta solo las evaluaciones de ese código.
- Si no hay código en sesión, devuelve una lista vacía.
- `POST /api/v1/evaluation` fuerza el cliente de la sesión para el rol CLIENTE.

### Tarea 5 - Validación y errores

**Cumple.**

- `TransaccionCrearRequest` aplica Bean Validation.
- `@Valid` está presente en el controlador.
- Errores de validación -> `400` con `{"errores":[...]}`.
- Cliente inexistente -> `404`.
- Evaluación inexistente o inactiva -> `404`.
- Método no permitido -> `405`.

### Tarea 6 - Factores derivados en el servidor

**Cumple.**

Para el portal del cliente, `TransaccionService.crearYEvaluarCliente`:

- toma la hora del servidor;
- cuenta las evaluaciones activas de la última hora desde PostgreSQL;
- compara el User-Agent con los dispositivos conocidos del cliente;
- compara la IP remota con dispositivos y transacciones previas;
- persiste el dispositivo nuevo y sus datos de uso.

El simulador de ADMIN mantiene los valores manuales para pruebas.

### Tarea 7 - Reglas alineadas

**Cumple.**

- RF001: escala gradual por monto/promedio.
- RF005: solo 00:00-04:59.
- RF007: heurística de país, frecuencia e IP.
- RF008: solo puntúa beneficiario nuevo con monto alto.
- Puntaje total limitado a 100.
- Niveles: 0-29 bajo, 30-59 medio, 60-79 alto, 80-100 crítico.

Las reglas se cargan desde `regla_fraude` y se aplican mediante el mismo motor `DeteccionFraudeService`.

### Tarea 8 - Revisión XSS

**Cumple.**

- No se usan `th:utext`, `innerHTML`, `outerHTML`, `insertAdjacentHTML` ni `document.write`.
- Las plantillas usan `th:text`.
- El JavaScript construye contenido con `textContent` y `append`.

### Tarea 9 - Pruebas

**Cumple.**

Comando ejecutado:

```powershell
.\mvnw.cmd clean test
```

Resultado:

```text
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Las pruebas cubren reglas, umbrales, seguridad, validación, persistencia, soft delete, rechazo de DELETE y aislamiento del portal del cliente.

### Tarea 10 - Limpieza y documentación

**Cumple.**

- `.gitignore` incluye `target/` y `.idea/`.
- El README describe Java 21, Spring Boot 4.1.1, PostgreSQL, H2 para pruebas y la API PATCH.
- `CUMPLIMIENTO_TAREAS.md` refleja el estado real.
- El directorio no es un repositorio Git, por lo que no se crearon ramas ni commits.

---

## Verificación funcional realizada

### Arranque con PostgreSQL

```text
Database version: 18.6
Tomcat started on port 8080
Started FraudeApplication in 4.59 seconds
```

### Datos iniciales

```text
cliente: 3
usuario_sistema: 5
rol: 3
usuario_rol: 5
transaccion: 10
evaluacion_fraude: 10
evaluacion_regla: 80
regla_fraude: 8
dispositivo: 11
```

### Flujo PATCH verificado

1. Se creó una evaluación de prueba con `POST /api/v1/evaluation` (id interno 11).
2. `PATCH /api/v1/evaluation/11` respondió `204`.
3. La fila permaneció en `evaluacion_fraude` con `es_activa = false`.
4. La transacción 11 quedó en estado `REVERSADA`.
5. `GET /api/v1/evaluation` devolvió 10 evaluaciones activas (la 11 ya no aparece).
6. La vista `/transacciones` y el dashboard tampoco mostraron la evaluación desactivada.
7. Tras reiniciar la aplicación, la fila inactiva siguió existiendo y los datos iniciales no se duplicaron.

### Seguridad verificada

```text
DELETE /api/v1/evaluation/11 -> 405
PATCH /api/v1/evaluation/99999 -> 404
```

---

## Conclusión

La aplicación conserva la estructura MVC, las vistas, la validación, los roles y el motor antifraude, pero ahora usa PostgreSQL como fuente de verdad. Las operaciones de baja son lógicas mediante `PATCH`, no existe borrado físico desde la API y las pruebas automatizadas pasan en un entorno H2 aislado.
