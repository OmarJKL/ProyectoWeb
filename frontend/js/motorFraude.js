
const ReglasFraude = (() => {

    /* ---- Utilidad: interpola linealmente un valor entre [minIn, maxIn]
       hacia [minOut, maxOut], con límite (clamp) en los extremos. Se usa mucho
       para convertir "una señal cruda" en "puntos de riesgo". ---- */
    function escalar(valor, minIn, maxIn, minOut, maxOut) {
        if (maxIn === minIn) return minOut;
        const t = (valor - minIn) / (maxIn - minIn);
        const limitado = Math.max(0, Math.min(1, t));
        return minOut + limitado * (maxOut - minOut);
    }

    function reglaMonto(tr, contexto) {
        const MAX = 20;
        const promedio = contexto.promedioCliente || 100;
        const proporcion = tr.monto / promedio;

        let puntaje = 0;
        let motivo;

        if (proporcion <= 1.5) {
            puntaje = 0;
            motivo = `Monto S/ ${tr.monto.toFixed(2)} dentro del rango habitual (prom. S/ ${promedio.toFixed(2)}).`;
        } else if (proporcion <= 3) {
            puntaje = escalar(proporcion, 1.5, 3, 5, 12);
            motivo = `Monto ${proporcion.toFixed(1)}x por encima del promedio del titular.`;
        } else {
            puntaje = escalar(proporcion, 3, 8, 12, MAX);
            motivo = `Monto ${proporcion.toFixed(1)}x por encima del promedio: fuerte desviación de patrón.`;
        }

        return { id: 'monto', etiqueta: 'Monto inusual', puntaje: Math.round(puntaje), max: MAX, motivo };
    }

    function reglaUbicacion(tr) {
        const MAX = 15;
        const tabla = {
            local:        { puntaje: 0,  motivo: 'Operación realizada en el país habitual del titular.' },
            regional:     { puntaje: 8,  motivo: 'Operación desde un país vecino no habitual para este titular.' },
            alto_riesgo:  { puntaje: MAX, motivo: 'Operación desde una jurisdicción marcada como de alto riesgo.' },
        };
        const r = tabla[tr.pais] || tabla.local;
        return { id: 'ubicacion', etiqueta: 'Ubicación geográfica', puntaje: r.puntaje, max: MAX, motivo: r.motivo };
    }


    function reglaHorario(tr) {
        const MAX = 10;
        const hora = tr.hora; // 0-23

        let puntaje = 0, motivo;
        if (hora >= 0 && hora < 5) {
            puntaje = MAX;
            motivo = `Operación a las ${String(hora).padStart(2, '0')}:00, en horario de madrugada (00:00–05:00).`;
        } else if (hora >= 22) {
            puntaje = 4;
            motivo = `Operación nocturna (${String(hora).padStart(2, '0')}:00), fuera del horario típico.`;
        } else {
            puntaje = 0;
            motivo = 'Operación dentro del horario habitual de actividad.';
        }
        return { id: 'horario', etiqueta: 'Horario de la operación', puntaje, max: MAX, motivo };
    }

    function reglaVelocidad(tr) {
        const MAX = 15;
        const n = tr.operacionesUltimaHora;

        let puntaje, motivo;
        if (n <= 1) {
            puntaje = 0;
            motivo = 'Frecuencia normal: una sola operación en la última hora.';
        } else if (n <= 3) {
            puntaje = escalar(n, 2, 3, 4, 8);
            motivo = `${n} operaciones en la última hora: frecuencia algo elevada.`;
        } else {
            puntaje = escalar(n, 4, 12, 8, MAX);
            motivo = `${n} operaciones en la última hora: patrón típico de "card testing".`;
        }
        return { id: 'velocidad', etiqueta: 'Velocidad de transacciones', puntaje: Math.round(puntaje), max: MAX, motivo };
    }

    function reglaDispositivo(tr) {
        const MAX = 10;
        if (tr.dispositivo === 'nuevo') {
            return { id: 'dispositivo', etiqueta: 'Dispositivo / huella digital', puntaje: MAX, max: MAX, motivo: 'Dispositivo sin historial previo para este titular.' };
        }
        return { id: 'dispositivo', etiqueta: 'Dispositivo / huella digital', puntaje: 0, max: MAX, motivo: 'Dispositivo reconocido y previamente asociado al titular.' };
    }

   
    function reglaCategoria(tr) {
        const MAX = 10;
        const riesgoAlto = ['casino', 'cripto'];
        if (riesgoAlto.includes(tr.categoria)) {
            return { id: 'categoria', etiqueta: 'Categoría de comercio', puntaje: MAX, max: MAX, motivo: `Rubro "${tr.categoria}" clasificado como de alto riesgo.` };
        }
        return { id: 'categoria', etiqueta: 'Categoría de comercio', puntaje: 0, max: MAX, motivo: `Rubro "${tr.categoria}" sin riesgo elevado asociado.` };
    }

   
    function reglaBeneficiario(tr) {
        const MAX = 10;
        if (tr.beneficiario === 'nuevo') {
            return { id: 'beneficiario', etiqueta: 'Tipo de beneficiario', puntaje: MAX, max: MAX, motivo: 'Beneficiario nuevo o sin historial de transacciones previas.' };
        }
        return { id: 'beneficiario', etiqueta: 'Tipo de beneficiario', puntaje: 0, max: MAX, motivo: 'Beneficiario recurrente y validado.' };
    }

    
    function reglaIp(tr) {
        const MAX = 10;
        if (tr.ip === 'nueva') {
            return { id: 'ip', etiqueta: 'Dirección IP', puntaje: MAX, max: MAX, motivo: 'Conexión desde una dirección IP no registrada previamente.' };
        }
        return { id: 'ip', etiqueta: 'Dirección IP', puntaje: 0, max: MAX, motivo: 'Conexión desde una dirección IP habitual e histórica.' };
    }

    return {
        todas: [reglaMonto, reglaUbicacion, reglaHorario, reglaVelocidad, reglaDispositivo, reglaCategoria, reglaBeneficiario, reglaIp],
        escalar,
    };
})();


class MotorFraude {

    /**
     * @param {Function[]} reglas - lista de funciones regla(tr, contexto) => {puntaje,max,motivo}
     * @param {Object} umbrales - límites de clasificación de nivel de riesgo
     */
    constructor(reglas = ReglasFraude.todas, umbrales = { bajo: 30, medio: 60, alto: 80 }) {
        this.reglas = reglas;
        this.umbrales = umbrales;
    }

    /**
     * Clasifica un puntaje numérico (0-100) en un nivel de riesgo.
     */
    nivelDePuntaje(puntaje) {
        if (puntaje < this.umbrales.bajo)  return 'bajo';
        if (puntaje < this.umbrales.medio) return 'medio';
        if (puntaje < this.umbrales.alto)  return 'alto';
        return 'critico';
    }

    /**
     * Ejecuta todas las reglas sobre una transacción y devuelve
     * el veredicto completo: puntaje total, nivel, y el detalle
     * (factores) que lo explica.
     *
     * @param {Object} tr  - transacción normalizada
     * @param {Object} contexto - contexto/historial del cliente
     */
    evaluar(tr, contexto = {}) {
        const factores = this.reglas.map(regla => regla(tr, contexto));

        const puntajeTotal = Math.min(
            100,
            factores.reduce((suma, f) => suma + f.puntaje, 0)
        );

        const nivel = this.nivelDePuntaje(puntajeTotal);

        return {
            puntaje: puntajeTotal,
            nivel,
            bloqueada: nivel === 'critico',
            enRevision: nivel === 'alto',
            factores,
        };
    }
}
