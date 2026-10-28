-- =========================================================
-- Lambda Shield - migración para bases PostgreSQL existentes
-- =========================================================
-- Agrega las columnas de aplicación que no estaban en el
-- esquema original y prepara las columnas para que el modelo
-- JPA funcione también en H2/PostgreSQL.
--
-- Es idempotente: puede ejecutarse más de una vez.
-- =========================================================

BEGIN;

-- ---------------------------------------------------------
-- Cliente
-- ---------------------------------------------------------
ALTER TABLE cliente
    ADD COLUMN IF NOT EXISTS codigo_cliente VARCHAR(20);

UPDATE cliente
SET codigo_cliente = COALESCE(codigo_cliente, 'C-' || LPAD(id_cliente::text, 3, '0'))
WHERE codigo_cliente IS NULL;

ALTER TABLE cliente
    ALTER COLUMN codigo_cliente SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON kcu.constraint_schema = tc.constraint_schema
         AND kcu.constraint_name = tc.constraint_name
         AND kcu.table_name = tc.table_name
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'cliente'
          AND tc.constraint_type = 'UNIQUE'
          AND kcu.column_name = 'codigo_cliente'
    ) THEN
        ALTER TABLE cliente
            ADD CONSTRAINT uq_cliente_codigo_cliente UNIQUE (codigo_cliente);
    END IF;
END $$;

ALTER TABLE cliente
    ADD COLUMN IF NOT EXISTS promedio_gasto NUMERIC(18,2) NOT NULL DEFAULT 0;

ALTER TABLE cliente
    ADD COLUMN IF NOT EXISTS es_activo BOOLEAN NOT NULL DEFAULT TRUE;

-- ---------------------------------------------------------
-- Dispositivo
-- ---------------------------------------------------------
ALTER TABLE dispositivo
    ADD COLUMN IF NOT EXISTS direccion_ip VARCHAR(45);

-- ---------------------------------------------------------
-- Transacción
-- ---------------------------------------------------------
ALTER TABLE transaccion
    ALTER COLUMN direccion_ip TYPE VARCHAR(45)
    USING direccion_ip::text;

-- ---------------------------------------------------------
-- Evaluación
-- ---------------------------------------------------------
ALTER TABLE evaluacion_fraude
    ADD COLUMN IF NOT EXISTS es_activa BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE evaluacion_fraude
    ADD COLUMN IF NOT EXISTS fecha_desactivacion TIMESTAMPTZ;

-- ---------------------------------------------------------
-- Usuario del sistema
-- ---------------------------------------------------------
ALTER TABLE usuario_sistema
    ADD COLUMN IF NOT EXISTS id_cliente BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_attribute a
          ON a.attrelid = c.conrelid
         AND a.attnum = ANY (c.conkey)
        WHERE c.contype = 'f'
          AND c.conrelid = 'usuario_sistema'::regclass
          AND c.confrelid = 'cliente'::regclass
          AND a.attname = 'id_cliente'
    ) THEN
        ALTER TABLE usuario_sistema
            ADD CONSTRAINT fk_usuario_cliente
            FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente);
    END IF;
END $$;

-- ---------------------------------------------------------
-- Normalización de tipos para portabilidad H2/PostgreSQL
-- ---------------------------------------------------------
ALTER TABLE regla_fraude
    ALTER COLUMN parametros_json TYPE TEXT
    USING parametros_json::text;

ALTER TABLE evaluacion_regla
    ALTER COLUMN detalle_json TYPE TEXT
    USING detalle_json::text;

ALTER TABLE auditoria
    ALTER COLUMN datos_anteriores TYPE TEXT
    USING datos_anteriores::text;

ALTER TABLE auditoria
    ALTER COLUMN datos_nuevos TYPE TEXT
    USING datos_nuevos::text;

ALTER TABLE auditoria
    ALTER COLUMN direccion_ip TYPE VARCHAR(45)
    USING direccion_ip::text;

COMMIT;
