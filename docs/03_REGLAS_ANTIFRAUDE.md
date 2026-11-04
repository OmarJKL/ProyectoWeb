# Lambda Shield — Motor antifraude

## 1. Cómo se evalúa una operación

Cada transferencia pasa por ocho reglas (RF001–RF008). Cada una devuelve un factor con un puntaje parcial y un máximo. El puntaje total es la suma de los parciales, limitada a 100. El nivel y la decisión se derivan del total.

| Puntaje | Nivel | Decisión | Acción |
| :---: | :--- | :--- | :--- |
| 0–29 | Bajo | Aprobar | Se ejecuta la transferencia |
| 30–59 | Medio | Monitorear | Se ejecuta con seguimiento |
| 60–79 | Alto | Retener / Alertar | Se pausa para revisión |
| 80–100 | Crítico | Bloquear | Se detiene y se genera alerta |

## 2. Reglas

### RF001 — Monto inusual (peso 25)

Compara el monto de la operación con el promedio de gasto del cliente (`promedio_gasto`).

| Proporción monto/promedio | Puntaje |
| :--- | :---: |
| ≤ 1.5x | 0 |
| > 1.5x y ≤ 3.0x | 5–15 (gradual) |
| > 3.0x y < 5.0x | 15–22 (gradual) |
| ≥ 5.0x | 25 |

Ejemplo: un cliente con promedio de S/ 180 que envía S/ 900 (5x) recibe 25 pts.

### RF002 — Dispositivo nuevo (peso 15)

Si el dispositivo no está en el conjunto de dispositivos conocidos del cliente: 15 pts. Si es habitual: 0.

### RF003 — IP nueva (peso 10)

Si la IP no está registrada previamente para el cliente: 10 pts. Si es habitual: 0.

### RF004 — País nuevo (peso 30)

| Ubicación | Puntaje |
| :--- | :---: |
| local (Perú) | 0 |
| regional (Chile, Colombia, Ecuador) | 15 |
| alto_riesgo (otro) | 30 |

### RF005 — Horario inusual (peso 10)

Operaciones entre 00:00 y 04:59: 10 pts. Resto del día: 0.

### RF006 — Alta frecuencia (peso 25)

Gradual según el número de operaciones en la última hora:

| Operaciones en la última hora | Puntaje |
| :--- | :---: |
| 0–1 | 0 |
| 2–3 | 5–12 (gradual) |
| 4–5 | 15–20 (gradual) |
| ≥ 6 | 25 |

### RF007 — Viaje imposible (peso 40)

Heurística simplificada (país + frecuencia + IP):

- 40 pts si el país es regional o de alto riesgo y hay ≥ 2 operaciones en la última hora.
- 28 pts si el país es de alto riesgo y la IP es nueva.
- 0 en caso contrario.

Es una aproximación pendiente de geolocalización real.

### RF008 — Beneficiario nuevo con monto alto (peso 20)

20 pts si el destinatario es nuevo **y** el monto supera el umbral (`promedio * 1.5`, mínimo 500). Beneficiario frecuente o monto por debajo del umbral: 0.

## 3. Derivación de factores en el servidor

Para el portal del cliente los factores no se declaran: se derivan para evitar que el cliente se auto-califique con score 0.

- **Hora**: del reloj del servidor.
- **Frecuencia**: conteo de evaluaciones activas del cliente en la última hora (PostgreSQL).
- **Dispositivo**: se compara el User-Agent con los dispositivos conocidos; si no está, es nuevo.
- **IP**: se compara con dispositivos y transacciones previas; si no está, es nueva.
- **Beneficiario**: se deriva del historial real del cliente por nombre del destinatario.
- **Monto, país y categoría**: vienen del formulario (declarados por el cliente).

## 4. Persistencia de la evaluación

Cada evaluación guarda:
- La transacción (monto, saldos, IP, país, canal, dispositivo, beneficiario).
- La evaluación (puntaje, nivel, decisión, modelo, fecha, duración).
- Cada factor aplicado en `evaluacion_regla` (si se disparó, valor detectado, peso aplicado).

Las evaluaciones y transacciones no se borran: se marcan como inactivas (`es_activa = false`) y la transacción pasa a `REVERSADA`.

## 5. Ejemplo de evaluación

Cliente con promedio de gasto S/ 180.00, dispositivo nuevo, IP nueva, país regional, hora 02:00, 3 operaciones en la última hora, beneficiario nuevo, monto S/ 900.00:

| Regla | Puntaje | Motivo |
| :--- | :---: | :--- |
| RF001 Monto inusual | 25 | Monto 5.0x excede el umbral crítico (>5x promedio_30d) |
| RF002 Dispositivo nuevo | 15 | Dispositivo no reconocido sin historial previo |
| RF003 IP nueva | 10 | Conexión desde una IP no registrada |
| RF004 País nuevo | 15 | Operación desde región (CL, CO, EC) |
| RF005 Horario inusual | 10 | Operación entre 00:00 y 04:59 |
| RF006 Alta frecuencia | 12 | 3 operaciones en la última hora |
| RF007 Viaje imposible | 40 | Heurística país + frecuencia |
| RF008 Beneficiario nuevo + monto alto | 20 | Nuevo destinatario con monto elevado |

**Total: 100 (limitado al tope) → Crítico → Bloqueada.**

En una operación normal (monto S/ 150, dispositivo habitual, IP conocida, país local, hora 14:00, 0 operaciones previas, beneficiario recurrente) el total sería 0 → Bajo → Aprobada, y el saldo se descuenta de la cuenta.
