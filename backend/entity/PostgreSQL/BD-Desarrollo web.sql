-- =========================================================
-- 1. TABLA CLIENTE
-- =========================================================

CREATE TABLE cliente (
    id_cliente BIGINT PRIMARY KEY,
    tipo_documento VARCHAR(20) NOT NULL,
    numero_documento VARCHAR(30) NOT NULL,
    nombres VARCHAR(100),
    apellido_paterno VARCHAR(100),
    apellido_materno VARCHAR(100),
    fecha_nacimiento DATE,
    correo_electronico VARCHAR(150),
    num VARCHAR(30)
);


-- =========================================================
-- 2. TABLA CUENTA
-- =========================================================

CREATE TABLE cuenta (
    id_cuenta BIGINT PRIMARY KEY,
    id_cliente BIGINT NOT NULL,
    numero_cuenta VARCHAR(34) NOT NULL UNIQUE,
    tipo_cuenta VARCHAR(30) NOT NULL,
    codigo_moneda CHAR(3) NOT NULL,
    saldo_disponible NUMERIC(18,2) NOT NULL,
    estado_cuenta VARCHAR(20) NOT NULL,
    fecha_apertura DATE NOT NULL,
    fecha_cierre DATE,
    fecha_creacion TIMESTAMPTZ NOT NULL,

    FOREIGN KEY (id_cliente)
        REFERENCES cliente(id_cliente)
);


-- =========================================================
-- 3. TABLA BENEFICIARIO
-- =========================================================

CREATE TABLE beneficiario (
    id_beneficiario BIGINT PRIMARY KEY,
    id_cliente BIGINT NOT NULL,
    nombre_beneficiario VARCHAR(150) NOT NULL,
    entidad_financiera VARCHAR(120),
    numero_cuenta_destino VARCHAR(50),
    codigo_pais CHAR(2),
    es_conocido BOOLEAN NOT NULL,
    fecha_primer_uso TIMESTAMPTZ,
    fecha_ultimo_uso TIMESTAMPTZ,
    es_activo BOOLEAN NOT NULL,

    FOREIGN KEY (id_cliente)
        REFERENCES cliente(id_cliente)
);


-- =========================================================
-- 4. TABLA CANAL
-- =========================================================

CREATE TABLE canal (
    id_canal SMALLINT PRIMARY KEY,
    codigo_canal VARCHAR(20) NOT NULL UNIQUE,
    nombre_canal VARCHAR(100) NOT NULL,
    es_activo BOOLEAN
);


-- =========================================================
-- 5. TABLA TIPO TRANSACCION
-- =========================================================

CREATE TABLE tipo_transaccion (
    id_tipo_transaccion SMALLINT PRIMARY KEY,
    codigo_tipo VARCHAR(40) NOT NULL UNIQUE,
    nombre_tipo VARCHAR(120) NOT NULL,
    naturaleza VARCHAR(10) NOT NULL,
    es_activo BOOLEAN NOT NULL
);


-- =========================================================
-- 6. TABLA DISPOSITIVO
-- =========================================================

CREATE TABLE dispositivo (
    id_dispositivo BIGINT PRIMARY KEY,
    id_cliente BIGINT NOT NULL,
    huella_dispositivo VARCHAR(255) UNIQUE,
    tipo_dispositivo VARCHAR(30),
    sistema_operativo VARCHAR(50),
    version_sistema VARCHAR(30),
    navegador VARCHAR(50),
    es_conocido BOOLEAN NOT NULL,
    fecha_primer_uso TIMESTAMPTZ,
    fecha_ultimo_uso TIMESTAMPTZ,

    FOREIGN KEY (id_cliente)
        REFERENCES cliente(id_cliente)
);


-- =========================================================
-- 7. TABLA TRANSACCION
-- =========================================================

CREATE TABLE transaccion (
    id_transaccion BIGINT PRIMARY KEY,
    codigo_transaccion UUID NOT NULL UNIQUE,

    id_cuenta BIGINT NOT NULL,
    id_beneficiario BIGINT,
    id_tipo_transaccion SMALLINT NOT NULL,
    id_canal SMALLINT NOT NULL,
    id_dispositivo BIGINT,

    monto_transaccion NUMERIC(18,2) NOT NULL
        CHECK (monto_transaccion > 0),

    codigo_moneda CHAR(3) NOT NULL,
    fecha_hora_transaccion TIMESTAMPTZ NOT NULL,

    saldo_anterior NUMERIC(18,2),
    saldo_posterior NUMERIC(18,2),

    direccion_ip INET,
    codigo_pais CHAR(2),
    ciudad VARCHAR(100),
    latitud NUMERIC(9,6),
    longitud NUMERIC(9,6),

    estado_transaccion VARCHAR(20) NOT NULL,
    fecha_registro TIMESTAMPTZ NOT NULL,

    FOREIGN KEY (id_cuenta)
        REFERENCES cuenta(id_cuenta),

    FOREIGN KEY (id_beneficiario)
        REFERENCES beneficiario(id_beneficiario),

    FOREIGN KEY (id_tipo_transaccion)
        REFERENCES tipo_transaccion(id_tipo_transaccion),

    FOREIGN KEY (id_canal)
        REFERENCES canal(id_canal),

    FOREIGN KEY (id_dispositivo)
        REFERENCES dispositivo(id_dispositivo)
);


-- =========================================================
-- 8. TABLA CARACTERISTICA TRANSACCION
-- =========================================================

CREATE TABLE caracteristica_transaccion (
    id_transaccion BIGINT PRIMARY KEY,

    monto_promedio_30d NUMERIC(18,2),
    monto_maximo_30d NUMERIC(18,2),

    cantidad_transacciones_10m INTEGER NOT NULL,
    cantidad_transacciones_1h INTEGER NOT NULL,
    cantidad_transacciones_24h INTEGER NOT NULL,
    cantidad_destinatarios_24h INTEGER NOT NULL,

    minutos_desde_ultima_transaccion INTEGER,
    distancia_ultima_transaccion_km NUMERIC(10,2),

    es_dispositivo_nuevo BOOLEAN NOT NULL,
    es_ip_nueva BOOLEAN NOT NULL,
    es_pais_nuevo BOOLEAN NOT NULL,
    es_ciudad_nueva BOOLEAN NOT NULL,
    es_beneficiario_nuevo BOOLEAN NOT NULL,
    es_horario_inusual BOOLEAN NOT NULL,
    es_monto_inusual BOOLEAN NOT NULL,
    es_velocidad_inusual BOOLEAN NOT NULL,

    fecha_calculo TIMESTAMPTZ NOT NULL,

    FOREIGN KEY (id_transaccion)
        REFERENCES transaccion(id_transaccion)
);


-- =========================================================
-- 9. TABLA REGLA FRAUDE
-- =========================================================

CREATE TABLE regla_fraude (
    id_regla INTEGER PRIMARY KEY,
    codigo_regla VARCHAR(30) NOT NULL UNIQUE,
    nombre_regla VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500),
    nivel_riesgo VARCHAR(20) NOT NULL,
    peso NUMERIC(5,2) NOT NULL,
    parametros_json JSONB,
    version_regla INTEGER NOT NULL,
    es_activa BOOLEAN NOT NULL,
    fecha_inicio_vigencia TIMESTAMPTZ NOT NULL,
    fecha_fin_vigencia TIMESTAMPTZ
);


-- =========================================================
-- 10. TABLA EVALUACION FRAUDE
-- =========================================================

CREATE TABLE evaluacion_fraude (
    id_evaluacion BIGINT PRIMARY KEY,
    id_transaccion BIGINT NOT NULL,

    fraude_score NUMERIC(5,2) NOT NULL
        CHECK (fraude_score >= 0 AND fraude_score <= 100),

    nivel_riesgo VARCHAR(20) NOT NULL,
    decision VARCHAR(20) NOT NULL,
    es_posible_fraude BOOLEAN NOT NULL,

    modelo_utilizado VARCHAR(40),
    version_modelo VARCHAR(40),
    fecha_evaluacion TIMESTAMPTZ NOT NULL,
    duracion_ms INTEGER,

    FOREIGN KEY (id_transaccion)
        REFERENCES transaccion(id_transaccion)
);


-- =========================================================
-- 11. TABLA EVALUACION REGLA
-- =========================================================

CREATE TABLE evaluacion_regla (
    id_evaluacion_regla BIGINT PRIMARY KEY,

    id_evaluacion BIGINT NOT NULL,
    id_regla INTEGER NOT NULL,

    se_disparo BOOLEAN NOT NULL,
    valor_detectado VARCHAR(250),
    peso_aplicado NUMERIC(5,2) NOT NULL,
    detalle_json JSONB,

    FOREIGN KEY (id_evaluacion)
        REFERENCES evaluacion_fraude(id_evaluacion),

    FOREIGN KEY (id_regla)
        REFERENCES regla_fraude(id_regla)
);


-- =========================================================
-- 12. TABLA USUARIO SISTEMA
-- =========================================================

CREATE TABLE usuario_sistema (
    id_usuario BIGINT PRIMARY KEY,
    nombre_usuario VARCHAR(80) NOT NULL UNIQUE,
    nombres VARCHAR(120) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    hash_password VARCHAR(255) NOT NULL,
    estado_usuario VARCHAR(20) NOT NULL,
    ultimo_acceso TIMESTAMPTZ,
    fecha_creacion TIMESTAMPTZ NOT NULL
);


-- =========================================================
-- 13. TABLA ALERTA FRAUDE
-- =========================================================

CREATE TABLE alerta_fraude (
    id_alerta BIGINT PRIMARY KEY,
    codigo_alerta UUID NOT NULL UNIQUE,

    id_transaccion BIGINT NOT NULL,
    id_evaluacion BIGINT NOT NULL,
    id_usuario_asignado BIGINT,

    tipo_alerta VARCHAR(50) NOT NULL,
    nivel_prioridad VARCHAR(20) NOT NULL,
    descripcion VARCHAR(500),
    estado_alerta VARCHAR(30) NOT NULL,

    fecha_generacion TIMESTAMPTZ NOT NULL,
    fecha_revision TIMESTAMPTZ,

    FOREIGN KEY (id_transaccion)
        REFERENCES transaccion(id_transaccion),

    FOREIGN KEY (id_evaluacion)
        REFERENCES evaluacion_fraude(id_evaluacion),

    FOREIGN KEY (id_usuario_asignado)
        REFERENCES usuario_sistema(id_usuario)
);


-- =========================================================
-- 14. TABLA CASO FRAUDE
-- =========================================================

CREATE TABLE caso_fraude (
    id_caso BIGINT PRIMARY KEY,
    codigo_caso UUID NOT NULL UNIQUE,

    id_alerta BIGINT NOT NULL UNIQUE,
    id_usuario_responsable BIGINT,

    estado_caso VARCHAR(30) NOT NULL,
    resultado_caso VARCHAR(30),
    comentario_cierre TEXT,

    fecha_apertura TIMESTAMPTZ NOT NULL,
    fecha_cierre TIMESTAMPTZ,

    FOREIGN KEY (id_alerta)
        REFERENCES alerta_fraude(id_alerta),

    FOREIGN KEY (id_usuario_responsable)
        REFERENCES usuario_sistema(id_usuario)
);


-- =========================================================
-- 15. TABLA HISTORIAL CASO
-- =========================================================

CREATE TABLE historial_caso (
    id_historial BIGINT PRIMARY KEY,

    id_caso BIGINT NOT NULL,
    id_usuario BIGINT,

    tipo_evento VARCHAR(40) NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo VARCHAR(30),
    comentario TEXT,

    fecha_evento TIMESTAMPTZ NOT NULL,

    FOREIGN KEY (id_caso)
        REFERENCES caso_fraude(id_caso),

    FOREIGN KEY (id_usuario)
        REFERENCES usuario_sistema(id_usuario)
);


-- =========================================================
-- 16. TABLA ROL
-- =========================================================

CREATE TABLE rol (
    id_rol SMALLINT PRIMARY KEY,
    codigo_rol VARCHAR(40) NOT NULL UNIQUE,
    nombre_rol VARCHAR(100) NOT NULL,
    descripcion VARCHAR(250),
    es_activo BOOLEAN NOT NULL
);


-- =========================================================
-- 17. TABLA USUARIO ROL
-- =========================================================

CREATE TABLE usuario_rol (
    id_usuario BIGINT NOT NULL,
    id_rol SMALLINT NOT NULL,

    fecha_asignacion TIMESTAMPTZ NOT NULL,

    PRIMARY KEY (id_usuario, id_rol),

    FOREIGN KEY (id_usuario)
        REFERENCES usuario_sistema(id_usuario),

    FOREIGN KEY (id_rol)
        REFERENCES rol(id_rol)
);


-- =========================================================
-- 18. TABLA AUDITORIA
-- =========================================================

CREATE TABLE auditoria (
    id_auditoria BIGINT PRIMARY KEY,

    id_usuario BIGINT,

    entidad VARCHAR(80) NOT NULL,
    id_entidad VARCHAR(80) NOT NULL,
    accion VARCHAR(40) NOT NULL,

    datos_anteriores JSONB,
    datos_nuevos JSONB,

    direccion_ip INET,
    fecha_evento TIMESTAMPTZ NOT NULL,

    FOREIGN KEY (id_usuario)
        REFERENCES usuario_sistema(id_usuario)
);