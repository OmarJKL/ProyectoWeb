# Contexto
Este repo es "Lambda Shield", un sistema académico de detección de fraude bancario (Java 21, Spring Boot, Thymeleaf, arquitectura MVC: paquetes model, controller, view bajo com.lambdashield.fraude). Todo funciona en memoria (stores con datos semilla). Tengo una auditoría con problemas de seguridad, validación, consistencia con la especificación del README y pruebas. Necesito que los corrijas todos, en este orden.

# Reglas de trabajo
- Crea una rama fix/auditoria y haz un commit por tarea con mensaje claro.
- No cambies nada fuera del alcance de cada tarea y mantén el MVC actual y Java 21.
- Antes de editar, lee los archivos implicados. Si algo no coincide con lo que describo, dímelo antes de improvisar.
- Al terminar cada tarea ejecuta ./mvnw clean test. Al final, verifica también que ./mvnw spring-boot:run arranca.
- Al terminar, entrégame un resumen: archivos tocados, decisiones tomadas y cualquier cosa que no pudiste resolver.

# TAREA 1 · Proteger la API (crítico)
Problema: en controller/config/WebConfig.java se excluye /api/** del interceptor, así que todos los endpoints (incluido DELETE /api/v1/evaluation) funcionan sin sesión. Además, AuthInterceptor bloquea rutas que no existen (/api/v1/dashboard, /api/v1/reglas, /api/v1/reportes) en vez de las reales (/api/v1/metric, /api/v1/rule, /api/v1/report, /api/v1/evaluation).
Haz esto:
1. Quita /api/** de excludePathPatterns.
2. En AuthInterceptor, para rutas /api/** sin sesión responde 401 con JSON ({"error":"No autenticado"}) en vez de redirigir a /login. Para rutas de páginas mantén el redirect.
3. Define permisos por rol (ver Tarea 3):
   - CLIENTE: solo POST /api/v1/evaluation y sus propias páginas (/operacion). El resto de /api/** responde 403 JSON.
   - ANALISTA: lectura de todo (GET de evaluation, metric, rule, report/export y las páginas de admin). No puede hacer DELETE ni modificar nada.
   - ADMIN: acceso total, incluido DELETE /api/v1/evaluation.
4. Revisa static/js/*.js (auth.js, panel.js, transacciones.js, simulador.js, operacion.js): que los fetch sigan funcionando con la sesión y que ante un 401 redirijan a /login y ante un 403 muestren un mensaje.

# TAREA 2 · Credenciales y sesión
Problema: en model/store/UsuarioStore.java las claves están en texto plano y el README las publica.
1. Agrega la dependencia spring-security-crypto (solo el módulo de cifrado, no Spring Security completo) y guarda las claves con BCryptPasswordEncoder; compara con matches. Los usuarios semilla siguen siendo los mismos para pruebas.
2. En LoginController.postLogin, llama a request.changeSessionId() tras autenticar (evita session fixation).
3. Quita las contraseñas del README y deja una nota: "usuarios de prueba definidos en UsuarioStore".

# TAREA 3 · Separar roles ANALISTA y ADMIN
Hoy analista tiene rol ADMIN. Crea el rol ANALISTA, asígnalo a ese usuario y aplica los permisos de la Tarea 1. Revisa navbar.html y las plantillas para que el analista no vea botones de acciones destructivas (por ejemplo "limpiar historial").

# TAREA 4 · Fuga de datos en el portal del cliente
Problema: OperacionClienteController tiene fijo C-001, y si el cliente no tiene transacciones hace fallback a listarTodas(), mostrándole las de todos.
1. Agrega codigoCliente a UsuarioInfo (usuario, maria y cliente → C-001) y guárdalo en sesión al hacer login.
2. En /operacion usa el cliente de la sesión y filtra por ese cliente de forma exacta (si hace falta, agrega clienteCodigo a EvaluacionResponse y a EvaluacionStore). Elimina el fallback a todas las transacciones: si no tiene, lista vacía.
3. En POST /api/v1/evaluation, si el rol es CLIENTE, ignora el cliente del body y fuerza el código del cliente de la sesión.

# TAREA 5 · Validación de entrada y errores
Problema: ClienteStore.buscar(null) lanza NullPointerException (500) y no se valida nada: se aceptan montos negativos, horas inválidas, etc.
1. Haz ClienteStore.buscar null-safe.
2. Agrega spring-boot-starter-validation y anota TransaccionCrearRequest: @NotBlank en cliente, @Positive en monto, @Min(0) @Max(23) en hora, @Min(0) en operacionesUltimaHora, y @Pattern o validación equivalente para pais (local|regional|alto_riesgo), dispositivo (habitual|nuevo), ip (habitual|nueva) y beneficiario. Antes de fijar los valores permitidos de cada campo, mira qué envían simulador.js y operacion.js para no romper el frontend.
3. Usa @Valid en EvaluationApiController.evaluate.
4. Revisa GlobalExceptionHandler (hoy está en el paquete service): muévelo al paquete controller si corresponde y haz que devuelva 400 con JSON {"errores":[...]} para validaciones y 404/400 para cliente inexistente.

# TAREA 6 · Que los factores de riesgo no dependan solo del cliente
Problema: dispositivo, IP, hora y operaciones en la última hora llegan en el body, así que un cliente puede declararlo todo "habitual" y sacar score 0.
Implementa una versión mínima para el portal del cliente (el simulador del admin sigue aceptando valores manuales para pruebas):
1. hora: tomarla del reloj del servidor.
2. operacionesUltimaHora: calcularla desde EvaluacionStore contando las evaluaciones de ese cliente en la última hora (necesitarás guardar un LocalDateTime en EvaluacionResponse; mantén el formato de fecha para mostrar).
3. dispositivo e ip: compararlos contra un conjunto de valores conocidos por cliente en ClienteStore (por ejemplo, el User-Agent y request.getRemoteAddr()); si no está en el conjunto, es "nuevo"/"nueva".
Si algo de esto exige cambios grandes en el frontend, dime las opciones antes de hacerlo.

# TAREA 7 · Alinear las reglas con la especificación (README)
1. ReglaBeneficiario (RF008): hoy da 10 pts a cualquier beneficiario nuevo. Debe dar 0 si no se cumple nuevo && monto > umbral.
2. ReglaHorario (RF005): quita el tramo de 4 pts para horas ≥ 22; solo 00:00–04:59 puntúa (10 pts).
3. ReglaViajeImposible (RF007): elimina la rama de categorías casino/cripto (no es viaje imposible). Deja la regla como heurística simplificada (país + operaciones + IP) y documenta en el README que es una aproximación pendiente de geolocalización real.
4. ReglaMontoInusual (RF001): el código usa puntaje gradual por tramos; actualiza la tabla del README para describir esos tramos en vez de "monto > 5x → 25".
5. Unifica el texto de estado: para nivel "alto" usa "Retener / Alertar" en DeteccionFraudeService.determinarEstado y busca en templates/ y static/js/ cualquier comparación con "Revisión manual" para actualizarla.
6. Datos semilla de EvaluacionStore.inicializarDatosPrueba: cada puntaje debe ser igual a min(100, suma de factores) y coincidir con nivel/estado según los umbrales (0–29 bajo, 30–59 medio, 60–79 alto, 80–100 crítico). Hoy el caso #8 suma 60 pero tiene 65, y marca "regional" con 30 pts cuando la regla da 15. Corrige manteniendo cada caso en su nivel actual.

# TAREA 8 · Frontend (auditoría)
Revisa plantillas y JS buscando XSS: uso de th:utext, innerHTML o concatenación de HTML con datos que vienen del servidor o del usuario. Reemplaza por th:text / textContent donde aplique. Lista lo que encuentres aunque no sea grave.

# TAREA 9 · Pruebas
Agrega pruebas (usa el starter de test que corresponda a esta versión de Spring Boot):
- Cada regla (RF001–RF008): valores límite (por ejemplo, proporción 1.5/3.0/5.0 en RF001, 1/3/5/6 operaciones en RF006, hora 4/5/22 en RF005).
- DeteccionFraudeService: umbrales 29/30, 59/60, 79/80 y tope de 100.
- MockMvc: API sin sesión → 401; CLIENTE a endpoints de admin → 403; ANALISTA con DELETE → 403; ADMIN con DELETE → 204; request inválido → 400; /operacion no muestra transacciones de otros clientes.

# TAREA 10 · Limpieza del repo y documentación
1. .gitignore: agrega target/ y .idea/; ejecuta git rm -r --cached target .idea si están versionados.
2. pom.xml: verifica que la etiqueta del nombre sea <name> (no <n>), y corrige el README para que refleje la versión real de Spring Boot del pom y las dependencias reales (hoy dice Spring Boot 3 y Spring Data JPA, pero el pom usa otra versión y no incluye el starter de JPA).
3. README: actualiza permisos por rol, la tabla de reglas con lo cambiado y el catálogo de APIs con los códigos 401/403/400.

# Criterios de aceptación
- Sin sesión, ningún endpoint de /api/** responde datos.
- Un cliente solo ve y opera con su propia cuenta.
- Las pruebas nuevas pasan con ./mvnw clean test.
- README, código y datos semilla coinciden entre sí.