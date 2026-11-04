# Lambda Shield — Funcionalidades de negocio y flujos

## 1. Objetivo del negocio

Lambda Shield protege operaciones bancarias en tiempo real: cada transferencia se evalúa antes de ejecutarse, asigna un puntaje de riesgo (0–100) y decide si se aprueba, monitorea, retiene o bloquea. El sistema conserva el historial para auditoría y permite desactivar evaluaciones sin borrarlas.

## 2. Roles y casos de uso

| Rol | Casos de uso principales |
| :--- | :--- |
| CLIENTE | Consultar saldo, transferir con evaluación antifraude, ver sus operaciones y avisos de seguridad |
| ANALISTA | Monitorear evaluaciones, consultar reglas, generar reportes |
| ADMIN | Todo lo anterior, más simulador manual, gestión de usuarios y baja lógica de evaluaciones |

## 3. Flujos de negocio

### 3.1 Login

1. El usuario envía credenciales a `POST /login`.
2. `AuthService` valida contra `usuario_sistema` con BCrypt y resuelve el rol desde `usuario_rol`/`rol`.
3. Se renueva el identificador de sesión (`changeSessionId`).
4. Se guardan en sesión `usuario`, `nombreUsuario`, `rol` y `codigoCliente`.
5. `CLIENTE` redirige a `/operacion`; los demás a `/`.

### 3.2 Transferencia del cliente (portal)

1. El cliente completa el formulario (monto, categoría, país, destinatario). El servidor deriva hora, frecuencia, dispositivo e IP.
2. `POST /api/v1/evaluation` fuerza el `codigoCliente` de la sesión.
3. Se obtiene el promedio de gasto del cliente y el saldo de la cuenta principal.
4. El motor evalúa las ocho reglas y produce puntaje, nivel (`bajo/medio/alto/critico`) y decisión.
5. Si la operación es aprobada (bajo/medio): se valida el saldo, se descuenta el monto y se registran `saldo_anterior`/`saldo_posterior`.
6. Se persiste la transacción, la evaluación y cada factor aplicado.
7. El tipo de destinatario (nuevo/frecuente) se deriva del historial real del cliente.
8. La interfaz muestra recibo aprobado o preventivo, actualiza el saldo y prepende la fila al historial. Si hay saldo insuficiente, se rechaza con `400`.

### 3.3 Simulador del administrador

Igual que el portal, pero los factores se declaran manualmente y no se fuerza el cliente. Útil para probar el motor con valores arbitrarios.

### 3.4 Historial y baja lógica

1. `GET /api/v1/evaluation` lista las evaluaciones activas.
2. `PATCH /api/v1/evaluation/{id}` marca `es_activa = false`, registra `fecha_desactivacion` y deja la transacción en `REVERSADA`.
3. Si la operación estaba aprobada, se devuelve el monto a la cuenta.
4. `PATCH /api/v1/evaluation/{id}/activar` revierte la baja y restaura el estado de la transacción (con revalidación de saldo).
5. `GET /api/v1/evaluation?activas=false` incluye las desactivadas.

### 3.5 Dashboard y reportes

- El dashboard agrega saldo de cuentas activas, ingresos/egresos del periodo y variaciones frente al periodo anterior.
- La serie diaria y los últimos movimientos se calculan desde las transacciones reales.
- Los reportes permiten filtrar por fecha de evaluación, nivel y cliente, y exportar en CSV, Excel y PDF.

### 3.6 Gestión de usuarios (solo ADMIN)

- Alta, edición, cambio de clave, cambio de rol/estado y baja física.
- Protección del último administrador activo y de la cuenta propia.

## 4. Portal del cliente

- Muestra saldo y número de cuenta reales.
- Formulario de transferencia sin selector de tipo de destinatario: el sistema lo deriva.
- Historial solo de sus operaciones con estado diferenciado (procesada, monitoreo, retenida, bloqueada).
- Avisos de seguridad para operaciones de riesgo alto o crítico, con el detalle de las causas detectadas.
- Si una transferencia es bloqueada o retenida, el recibo explica los factores.

## 5. Entidades del dominio

| Entidad | Tabla | Descripción |
| :--- | :--- | :--- |
| `Cliente` | `cliente` | Datos del cliente y promedio de gasto |
| `Cuenta` | `cuenta` | Cuenta principal y saldo disponible |
| `Dispositivo` | `dispositivo` | Huella del dispositivo y si es conocido |
| `Beneficiario` | `beneficiario` | Destinatario y si es conocido |
| `Transaccion` | `transaccion` | Operación con montos, saldos y ubicación |
| `EvaluacionFraude` | `evaluacion_fraude` | Puntaje, nivel, decisión y estado activo |
| `EvaluacionRegla` | `evaluacion_regla` | Desglose de cada regla aplicada |
| `ReglaFraude` | `regla_fraude` | Catálogo de reglas y pesos |
| `UsuarioSistema` | `usuario_sistema` | Usuarios y hash BCrypt |
| `Rol` / `UsuarioRol` | `rol` / `usuario_rol` | Roles y asignaciones |
| `AlertaFraude`, `CasoFraude`, `HistorialCaso`, `Auditoria`, `CaracteristicaTransaccion` | — | Mapeadas al esquema; pendientes de servicio |
