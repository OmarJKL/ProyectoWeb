# Lambda Shield

## Estado del proyecto

**Version:** Beta 1.1  
**Tipo:** Prototipo funcional de frontend  
**Estado de backend:** No implementado  
**Estado de conexion a base de datos:** No implementada

Lambda Shield es un prototipo web para evaluar operaciones bancarias con un motor de reglas antifraude. La Beta 1.1 permite simular transacciones, calcular un puntaje de riesgo, visualizar los factores que influyeron en la decision y consultar un historial local.

La aplicacion es actualmente una demostracion de interfaz y logica de negocio en el navegador. No procesa transacciones reales, no autentica usuarios contra un servidor y no envia datos a PostgreSQL ni MongoDB.

## Funcionalidades disponibles

- Inicio de sesion demostrativo con dos usuarios precargados.
- Proteccion de las paginas internas mediante `sessionStorage`.
- Panel general con resumen de movimientos, grafico y accesos a los modulos.
- Simulador de transacciones con ocho parametros de riesgo.
- Motor antifraude determinista basado en reglas y puntajes ponderados.
- Explicacion visual del resultado mediante puntaje, nivel, radar y detalle de factores.
- Generacion de trafico automatico para probar el motor.
- Historial de evaluaciones persistido en `localStorage`.
- Filtros por nivel de riesgo y reinicio del historial.
- Pagina informativa con la configuracion actual de las reglas.

## Requisitos

- Navegador moderno con soporte para JavaScript ES6, `sessionStorage` y `localStorage`.
- Conexion a internet para cargar Bootstrap, Bootstrap Icons, Chart.js y Google Fonts desde CDN.
- No se requiere Node.js, npm, Python ni un servidor backend para la funcionalidad actual.

## Ejecucion local

La forma recomendada es abrir el proyecto con una extension como **Live Server** de VS Code. Tambien puede servirse con cualquier servidor estatico.

1. Abrir la carpeta del proyecto en VS Code.
2. Iniciar un servidor estatico desde la raiz del repositorio.
3. Abrir `index.html` desde el navegador.
4. Si no existe una sesion activa, la aplicacion redirige al login.

Tambien se puede abrir directamente `frontend/html/login.html`, pero un servidor estatico evita diferencias de comportamiento entre navegadores al trabajar con archivos locales.

## Credenciales de demostracion

Las credenciales estan definidas en [frontend/js/verificacion.js](frontend/js/verificacion.js) y son solo para demostracion:

| Usuario | Clave |
| --- | --- |
| `admin` | `lambda2026` |
| `analista` | `fraude123` |

Estas credenciales no deben utilizarse en un entorno real. Actualmente se comparan en el cliente, por lo que cualquier persona puede inspeccionarlas desde el navegador.

## Arquitectura actual

```text
index.html                         Panel general en la raiz
frontend/
  html/
    login.html                     Login de demostracion
    simulador.html                 Formulario y resultado del motor
    transacciones.html             Historial y KPIs
    reglas.html                    Catalogo visual de reglas
  css/
    estilos.css                    Variables, componentes y responsive
  js/
    verificacion.js                Sesion y credenciales demo
    motorFraude.js                 Reglas y evaluacion de riesgo
    app.js                         Estado, DOM, historial y eventos
    panel.js                       Grafico y datos sinteticos del panel
  img/
    logo.ico                       Icono de la aplicacion
backend/
  .gitkeep                         Reserva de la estructura backend
  entity/
    PostgreSQL/BD-Desarrollo web.sql
                                    Modelo relacional propuesto
    MongoBD/info.md                 Indices propuestos
    MongoBD/mongodb_colecciones/    Fixtures de colecciones MongoDB
```

## Flujo funcional

```text
login.html
   |
   | sessionStorage: autenticacionLambdaShield = true
   v
index.html
   |
   +--> simulador.html --> motorFraude.js --> app.js --> localStorage
   |
   +--> transacciones.html <-----------------------------+
   |
   +--> reglas.html                                      |
   +-----------------------------------------------------+
```

1. `login.html` carga `verificacion.js` y valida las credenciales contra una lista local.
2. Al iniciar sesion se guarda una bandera en `sessionStorage` y se navega al panel.
3. `index.html` valida la sesion y muestra el dashboard.
4. Las paginas internas vuelven a validar la sesion antes de renderizarse.
5. El simulador normaliza los datos del formulario y ejecuta `MotorFraude`.
6. `app.js` agrega el resultado al historial y lo guarda en `localStorage`.
7. La pagina de transacciones lee el mismo estado local y renderiza la tabla, KPIs y filtros.

## Detalle de los modulos frontend

### `index.html`

Es el panel general del prototipo. Incluye:

- Resumen estatico de saldo, ingresos y egresos.
- Grafico de movimientos generado en el cliente.
- Tabla de movimientos recientes sinteticos.
- Navegacion hacia simulador, historial y reglas.
- Carga de `verificacion.js`, `motorFraude.js`, `app.js` y `panel.js`.

Los recursos del panel se resuelven desde la raiz mediante rutas `frontend/...`. Las paginas internas regresan al panel usando `../index.html`.

### `frontend/html/login.html`

Es la pantalla de acceso. No existe una API de login: `blIniciarSesion()` compara usuario y clave con constantes JavaScript. La sesion solo vive en la pestana actual mediante `sessionStorage`.

### `frontend/js/verificacion.js`

Contiene:

- `CLAVE_AUTENTICACION_BL`: nombre de la bandera de sesion.
- `USUARIOS_BL`: usuarios de demostracion.
- `blIniciarSesion()`: validacion local de credenciales.
- `blVerificarAutenticacion()`: guard de navegacion.
- `blCerrarSesion()`: elimina la sesion y vuelve al login.

No es un mecanismo de seguridad de produccion. La validacion debe trasladarse al backend antes de utilizar cuentas reales.

### `frontend/html/simulador.html`

Expone los datos de entrada que consume el motor:

- Titular y promedio historico.
- Monto.
- Categoria de comercio.
- Hora.
- Ubicacion.
- Dispositivo.
- Cantidad de operaciones en la ultima hora.
- Beneficiario.
- Direccion IP.

Muestra el puntaje sobre 100, el nivel de riesgo, una visualizacion tipo radar y el detalle de cada factor.

### `frontend/js/motorFraude.js`

Es la capa de dominio mas independiente del DOM. Define ocho reglas y la clase `MotorFraude`.

| Regla | Maximo |
| --- | ---: |
| Monto inusual | 20 |
| Ubicacion geografica | 15 |
| Horario | 10 |
| Velocidad de transacciones | 15 |
| Dispositivo | 10 |
| Categoria de comercio | 10 |
| Beneficiario | 10 |
| Direccion IP | 10 |
| **Total** | **100** |

El motor suma los puntajes de las reglas y limita el resultado a 100. La clasificacion actual es:

| Puntaje | Nivel | Decision visual |
| ---: | --- | --- |
| 0 - 29 | Bajo | Aprobada |
| 30 - 59 | Medio | Monitoreo |
| 60 - 79 | Alto | Revision manual |
| 80 - 100 | Critico | Bloqueada |

El contexto usado actualmente es minimo: el promedio del titular se obtiene de las opciones estaticas del formulario. No se consulta historial real, perfil de cliente, geolocalizacion, IP ni dispositivo.

### `frontend/js/app.js`

Es la capa de aplicacion compartida por simulador e historial. Sus responsabilidades son:

- Leer y guardar el estado de transacciones en `localStorage`.
- Evaluar y registrar operaciones.
- Renderizar el veredicto y el radar SVG.
- Renderizar la tabla detallada.
- Actualizar KPIs.
- Filtrar por nivel de riesgo.
- Generar trafico automatico cada 1.8 segundos.
- Mostrar alertas visuales cuando una operacion queda en nivel critico.
- Reiniciar el historial.
- Gestionar el cierre de sesion.

El estado persistido usa la clave `estadoBancoLambda` y contiene `transacciones` y `siguienteId`.

### `frontend/html/transacciones.html`

Muestra las evaluaciones almacenadas localmente. Presenta cuatro KPIs:

- Total de transacciones evaluadas.
- Operaciones bloqueadas.
- Operaciones en revision manual.
- Puntaje promedio.

La tabla es generada por `app.js`; no representa registros obtenidos desde una base de datos.

### `frontend/html/reglas.html`

Es una vista documental de las ocho reglas y sus pesos. Sirve como referencia visual de la configuracion del prototipo. La ejecucion real de las reglas esta en `motorFraude.js`.

### `frontend/js/panel.js`

Construye datos sinteticos de 365 dias para el grafico de ingresos y egresos de `index.html`, y ocho filas sinteticas para la tabla de movimientos. Estos datos se generan en memoria y no provienen del historial del simulador ni de una base de datos.

### `frontend/css/estilos.css`

Define el sistema visual de la aplicacion:

- Variables CSS de color, tipografia y riesgo.
- Navegacion, tarjetas, botones, formularios y tablas.
- Componentes del simulador, radar, anillo de puntaje y alertas.
- KPIs e interfaz del dashboard.
- Reglas responsive para pantallas menores.

Bootstrap, Bootstrap Icons y las tipografias se cargan externamente desde CDN en cada pagina.

## Persistencia actual

### `sessionStorage`

Se usa para la bandera de autenticacion `autenticacionLambdaShield`. Se pierde al cerrar la pestana o el contexto del navegador.

### `localStorage`

Se usa para `estadoBancoLambda`, con una estructura aproximada:

```json
{
  "transacciones": [
    {
      "id": "TR-00001",
      "fecha": "2026-09-09T12:00:00.000Z",
      "tr": {},
      "veredicto": {}
    }
  ],
  "siguienteId": 2
}
```

Este almacenamiento no tiene control de concurrencia, auditoria, validacion server-side, cifrado ni sincronizacion entre usuarios o dispositivos.

## Artefactos de backend preparados

### PostgreSQL

[backend/entity/PostgreSQL/BD-Desarrollo web.sql](backend/entity/PostgreSQL/BD-Desarrollo%20web.sql) contiene un modelo relacional propuesto con 18 tablas, entre ellas:

- `cliente`, `cuenta`, `beneficiario` y `dispositivo`.
- `transaccion` y `caracteristica_transaccion`.
- `regla_fraude`, `evaluacion_fraude` y `evaluacion_regla`.
- `alerta_fraude`, `caso_fraude` e `historial_caso`.
- `usuario_sistema`, `rol`, `usuario_rol` y `auditoria`.
- Catalogos de `canal` y `tipo_transaccion`.

El script define claves primarias, relaciones, restricciones basicas, tipos monetarios, JSONB para parametros/detalles y campos de auditoria. No se ejecuta automaticamente y no existe codigo de conexion desde el frontend.

### MongoDB

[backend/entity/MongoBD/info.md](backend/entity/MongoBD/info.md) documenta indices pendientes para cuatro colecciones. Los archivos JSON contienen fixtures de ejemplo para:

- `contexto_dispositivo`: huella, cliente, headers, navegador y geolocalizacion.
- `evento_transaccion`: eventos recibidos, payload, origen y timestamp.
- `log_integracion`: endpoint, correlacion, metadatos de request/response y latencia.
- `snapshot_decision`: features, reglas disparadas, metadata del modelo, score y explicacion.

Estos archivos son datos de referencia, no una base MongoDB activa. Ademas, `info.md` indica que faltan crear indices nuevos.

### Carpeta `backend`

Actualmente contiene `.gitkeep` y los artefactos de modelado. No hay API, servidor HTTP, controladores, servicios, repositorios, configuracion de secretos ni migraciones ejecutables.

## Limitaciones conocidas de Beta 1.1

- La autenticacion es simulada y las credenciales estan expuestas en el cliente.
- No hay backend ni endpoints HTTP.
- No hay conexion activa con PostgreSQL o MongoDB.
- Las transacciones no se almacenan de forma centralizada.
- Cada navegador mantiene su propio historial local.
- Los datos del dashboard son sinteticos y estan separados del historial antifraude.
- Las reglas no se administran desde la base de datos; estan codificadas en JavaScript.
- No hay roles, permisos, recuperacion de cuenta ni expiracion de sesion.
- No hay validacion de datos en servidor ni proteccion contra manipulacion del almacenamiento local.
- No hay pruebas automatizadas ni pipeline de integracion continua configurado.
- Los CDN externos son dependencias de ejecucion y requieren conectividad.
- El trafico automatico solo simula operaciones; no representa eventos bancarios reales.

## Hoja de ruta sugerida

### Beta 1.2: base tecnica

- Crear una API backend con autenticacion basada en sesiones seguras o tokens.
- Definir variables de entorno y gestion de secretos.
- Implementar endpoints para login, clientes, transacciones, evaluaciones e historial.
- Mover las credenciales fuera del navegador.
- Agregar validacion de esquemas y manejo uniforme de errores.

### Beta 1.3: persistencia e integracion

- Ejecutar y revisar el modelo PostgreSQL.
- Crear migraciones, datos semilla e indices necesarios.
- Integrar MongoDB para contexto de dispositivo, eventos, logs y snapshots.
- Persistir cada evaluacion y sus factores en la base de datos.
- Reemplazar el estado de `localStorage` por consultas a la API.

### Version candidata

- Gestionar usuarios, roles, alertas y casos de fraude.
- Incorporar auditoria y trazabilidad completa.
- Agregar pruebas unitarias del motor y pruebas de API.
- Proteger endpoints, validar permisos y aplicar controles de observabilidad.
- Separar configuracion de reglas de la logica de presentacion.
- Preparar despliegue, respaldo, monitoreo y documentacion operativa.

## Verificacion manual de la demo

1. Iniciar sesion con `admin` / `lambda2026`.
2. Entrar a **Simulador** y ejecutar la transaccion por defecto.
3. Cambiar ubicacion a **Alto Riesgo**, dispositivo a **Nuevo**, beneficiario a **Nuevo** e IP a **Nueva** para observar un nivel mas alto.
4. Abrir **Transacciones** y confirmar que la evaluacion aparece en la tabla.
5. Probar los filtros y **Reiniciar Historial**.
6. Volver al panel y cambiar los rangos del grafico.
7. Cerrar sesion y comprobar que las paginas protegidas redirigen al login.

## Nota de alcance

Este README describe el estado observado del repositorio en Beta 1.1. La presencia del esquema SQL y de fixtures MongoDB representa preparacion de arquitectura y contrato de datos, no una integracion terminada. Cualquier uso productivo requiere implementar el backend, la persistencia centralizada, la seguridad y las pruebas correspondientes.
