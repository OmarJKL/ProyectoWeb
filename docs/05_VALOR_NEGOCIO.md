# Lambda Shield — Valor de negocio

## 1. Problema que resuelve

El fraude bancario en canales digitales genera pérdidas directas (dinero transferido a cuentas fraudulentas), costos de recuperación, multas regulatorias y pérdida de confianza del cliente. La detección manual o tardía multiplica el daño. Lambda Shield evalúa cada transferencia **antes** de ejecutarla y bloquea o retiene las operaciones sospechosas, reduciendo el fraude en tiempo real.

## 2. Propuesta de valor

| Para quién | Qué obtiene |
| :--- | :--- |
| Cliente | Transferencias protegidas automáticamente, sin pasos adicionales; transparencia sobre el estado de cada operación y avisos cuando algo se retiene o bloquea |
| Analista | Panel de monitoreo con KPIs, historial filtrable, detalle de factores por operación y reportes exportables |
| Administrador | Control total: simulador de escenarios, gestión de usuarios y reglas, baja lógica auditada |
| Banco | Reducción de pérdidas por fraude, trazabilidad completa, cumplimiento normativo, datos listos para auditoría |

## 3. Capacidades clave

- **Evaluación en tiempo real**: cada transferencia se puntúa en milisegundos antes de ejecutarse.
- **Motor de ocho reglas**: monto inusual, dispositivo nuevo, IP nueva, país nuevo, horario inusual, alta frecuencia, viaje imposible y beneficiario nuevo con monto alto.
- **Decisión automática**: aprobar, monitorear, retener o bloquear según el puntaje.
- **Persistencia completa**: cada evaluación guarda la transacción, el desglose de reglas y los saldos.
- **Baja lógica**: las operaciones desactivadas no se borran; quedan auditables y son reversibles.
- **Portal del cliente seguro**: el cliente no puede declarar sus propios factores; hora, frecuencia, dispositivo e IP se derivan en el servidor.
- **Aislamiento por rol**: un cliente solo ve sus propias operaciones.

## 4. KPIs y reportes

- Total de operaciones, aprobadas, en monitoreo/revisión y bloqueadas.
- Puntaje promedio y tasa de bloqueo.
- Monto en riesgo prevenido / retenido.
- Volumen total evaluado.
- Exportación filtrada en CSV, Excel y PDF por fecha, nivel y cliente.

## 5. Beneficio operativo

- **Menos fraude ejecutado**: las operaciones de riesgo crítico se bloquean antes de mover dinero.
- **Menos falsos positivos costosos**: los niveles medio y alto se monitorean sin detener la operación.
- **Menos carga manual**: el motor automatiza lo que antes requería revisión caso por caso.
- **Auditoría lista**: cada evaluación queda con su desglose y las bajas son reversibles y fechadas.
- **Escalable**: la arquitectura por capas permite cambiar reglas, agregar canales o integrar nuevos factores sin reescribir el flujo.

## 6. Riesgos y limitaciones actuales

- La heurística de viaje imposible (RF007) es una aproximación sin geolocalización real.
- Los saldos y movimientos del dashboard provienen de la base; las variaciones son porcentuales por periodo.
- Las alertas y casos (investigación formal) están modelados en el esquema pero aún sin flujo de trabajo dedicado.
- No hay integración con sistemas de notificación (correo/SMS) para alertar al cliente.
- La protección de la API se basa en sesión HTTP; para una API pública convendría tokenización (JWT) y CSRF.

## 7. Próximos pasos sugeridos

1. Flujo de investigación de alertas y casos (asignación, resolución, cierre).
2. Auditoría de acciones (quién desactivó/reactivó qué y cuándo).
3. Geolocalización real para RF007 y parámetros de reglas editables.
4. Notificaciones al cliente cuando una operación se retiene o bloquea.
5. Migraciones versionadas (Flyway/Liquibase) y CI/CD.
