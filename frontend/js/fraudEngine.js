/**
 * ==========================================================================
 * LAMBDA SHIELD · fraudEngine.js
 * --------------------------------------------------------------------------
 * Motor de detección de fraude basado en REGLAS PONDERADAS.
 *
 * Por qué reglas y no "una IA mágica": en un caso real, el primer filtro
 * de un banco casi siempre es un motor determinista y auditable (puedes
 * explicarle a un regulador o a un cliente EXACTAMENTE por qué se bloqueó
 * una operación). Un modelo de ML se apila encima de esto en producción,
 * pero la base explicable siempre va primero. Por eso cada regla devuelve
 * no solo un puntaje, sino el "porqué".
 *
 * Cada regla:
 *   - recibe la transacción + un "contexto" (historial simulado del cliente)
 *   - devuelve { score, max, reason } con score entre 0 y max
 *
 * El score total es la suma de los scores individuales (tope 100).
 * Este archivo NO toca el DOM: es lógica pura, testeable de forma aislada.
 * ==========================================================================
 */

const FraudRules = (() => {

    /* ---- Utilidad: interpola linealmente un valor entre [inMin, inMax]
       hacia [outMin, outMax], con clamp en los extremos. Se usa mucho
       para convertir "una señal cruda" en "puntos de riesgo". ---- */
    function scale(value, inMin, inMax, outMin, outMax) {
        if (inMax === inMin) return outMin;
        const t = (value - inMin) / (inMax - inMin);
        const clamped = Math.max(0, Math.min(1, t));
        return outMin + clamped * (outMax - outMin);
    }

    /* =========================================================
       REGLA 1 — Monto inusual respecto al comportamiento habitual
       Peso máximo: 25 pts
       Lógica: compara el monto contra el promedio histórico del
       titular. Cuanto más se aleja hacia arriba, más sospechoso.
       ========================================================= */
    function reglaMonto(tx, ctx) {
        const MAX = 25;
        const promedio = ctx.promedioCliente || 100;
        const ratio = tx.monto / promedio;

        let score = 0;
        let reason;

        if (ratio <= 1.5) {
            score = 0;
            reason = `Monto S/ ${tx.monto.toFixed(2)} dentro del rango habitual (prom. S/ ${promedio.toFixed(2)}).`;
        } else if (ratio <= 3) {
            score = scale(ratio, 1.5, 3, 6, 14);
            reason = `Monto ${ratio.toFixed(1)}x por encima del promedio del titular.`;
        } else {
            score = scale(ratio, 3, 8, 14, MAX);
            reason = `Monto ${ratio.toFixed(1)}x por encima del promedio: fuerte desviación de patrón.`;
        }

        return { id: 'monto', label: 'Monto inusual', score: Math.round(score), max: MAX, reason };
    }

    /* =========================================================
       REGLA 2 — Ubicación geográfica
       Peso máximo: 20 pts
       Lógica: transacciones fuera del país habitual, y sobre todo
       en jurisdicciones de alto riesgo (paraísos fiscales, países
       con alta tasa de fraude reportada), suman puntos.
       ========================================================= */
    function reglaUbicacion(tx) {
        const MAX = 20;
        const tabla = {
            local:        { score: 0,  reason: 'Operación realizada en el país habitual del titular.' },
            regional:     { score: 10, reason: 'Operación desde un país vecino no habitual para este titular.' },
            alto_riesgo:  { score: 20, reason: 'Operación desde una jurisdicción marcada como de alto riesgo.' },
        };
        const r = tabla[tx.pais] || tabla.local;
        return { id: 'ubicacion', label: 'Ubicación geográfica', score: r.score, max: MAX, reason: r.reason };
    }

    /* =========================================================
       REGLA 3 — Horario inusual
       Peso máximo: 15 pts
       Lógica: la franja de madrugada (00:00–05:00) concentra
       estadísticamente más fraude con tarjeta/clonación que el
       resto del día.
       ========================================================= */
    function reglaHorario(tx) {
        const MAX = 15;
        const hora = tx.hora; // 0-23

        let score = 0, reason;
        if (hora >= 0 && hora < 5) {
            score = MAX;
            reason = `Operación a las ${String(hora).padStart(2, '0')}:00, en horario de madrugada (00:00–05:00).`;
        } else if (hora >= 22) {
            score = 6;
            reason = `Operación nocturna (${String(hora).padStart(2, '0')}:00), fuera del horario típico.`;
        } else {
            score = 0;
            reason = 'Operación dentro del horario habitual de actividad.';
        }
        return { id: 'horario', label: 'Horario de la operación', score, max: MAX, reason };
    }

    /* =========================================================
       REGLA 4 — Velocidad / frecuencia de transacciones
       Peso máximo: 20 pts
       Lógica: múltiples operaciones en poco tiempo es la firma
       clásica de una tarjeta comprometida ("card testing").
       ========================================================= */
    function reglaVelocidad(tx) {
        const MAX = 20;
        const n = tx.operacionesUltimaHora;

        let score, reason;
        if (n <= 1) {
            score = 0;
            reason = 'Frecuencia normal: una sola operación en la última hora.';
        } else if (n <= 3) {
            score = scale(n, 2, 3, 5, 10);
            reason = `${n} operaciones en la última hora: frecuencia algo elevada.`;
        } else {
            score = scale(n, 4, 12, 12, MAX);
            reason = `${n} operaciones en la última hora: patrón típico de "card testing".`;
        }
        return { id: 'velocidad', label: 'Velocidad de transacciones', score: Math.round(score), max: MAX, reason };
    }

    /* =========================================================
       REGLA 5 — Dispositivo / huella no reconocida
       Peso máximo: 10 pts
       Lógica: un dispositivo nuevo no es fraude por sí solo, pero
       combinado con otras señales (monto alto, ubicación rara)
       incrementa fuertemente la sospecha.
       ========================================================= */
    function reglaDispositivo(tx) {
        const MAX = 10;
        if (tx.dispositivo === 'nuevo') {
            return { id: 'dispositivo', label: 'Dispositivo / huella digital', score: MAX, max: MAX, reason: 'Dispositivo sin historial previo para este titular.' };
        }
        return { id: 'dispositivo', label: 'Dispositivo / huella digital', score: 0, max: MAX, reason: 'Dispositivo reconocido y previamente asociado al titular.' };
    }

    /* =========================================================
       REGLA 6 — Categoría de comercio de alto riesgo
       Peso máximo: 10 pts
       Lógica: ciertos rubros (casinos, cripto) tienen tasas base
       de fraude / lavado más altas y se penalizan directamente.
       ========================================================= */
    function reglaCategoria(tx) {
        const MAX = 10;
        const riesgoAlto = ['casino', 'cripto'];
        if (riesgoAlto.includes(tx.categoria)) {
            return { id: 'categoria', label: 'Categoría de comercio', score: MAX, max: MAX, reason: `Rubro "${tx.categoria}" clasificado como de alto riesgo.` };
        }
        return { id: 'categoria', label: 'Categoría de comercio', score: 0, max: MAX, reason: `Rubro "${tx.categoria}" sin riesgo elevado asociado.` };
    }

    return {
        all: [reglaMonto, reglaUbicacion, reglaHorario, reglaVelocidad, reglaDispositivo, reglaCategoria],
        scale,
    };
})();


class FraudEngine {

    /**
     * @param {Function[]} rules - lista de funciones regla(tx, ctx) => {score,max,reason}
     * @param {Object} thresholds - umbrales de clasificación de nivel de riesgo
     */
    constructor(rules = FraudRules.all, thresholds = { bajo: 30, medio: 60, alto: 80 }) {
        this.rules = rules;
        this.thresholds = thresholds;
    }

    /**
     * Clasifica un score numérico (0-100) en un nivel de riesgo.
     */
    nivelDeScore(score) {
        if (score < this.thresholds.bajo)  return 'bajo';
        if (score < this.thresholds.medio) return 'medio';
        if (score < this.thresholds.alto)  return 'alto';
        return 'critico';
    }

    /**
     * Ejecuta todas las reglas sobre una transacción y devuelve
     * el veredicto completo: score total, nivel, y el detalle
     * (factores) que lo explica.
     *
     * @param {Object} tx  - transacción normalizada
     * @param {Object} ctx - contexto/historial del cliente
     */
    evaluate(tx, ctx = {}) {
        const factores = this.rules.map(regla => regla(tx, ctx));

        const scoreTotal = Math.min(
            100,
            factores.reduce((sum, f) => sum + f.score, 0)
        );

        const nivel = this.nivelDeScore(scoreTotal);

        return {
            score: scoreTotal,
            nivel,
            bloqueada: nivel === 'critico',
            enRevision: nivel === 'alto',
            factores,
        };
    }
}
