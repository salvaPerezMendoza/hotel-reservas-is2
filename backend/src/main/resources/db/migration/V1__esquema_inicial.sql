-- =====================================================================
-- Esquema inicial del sistema de reservas de hotel
-- TP Ingeniería de Software II - FIUBA - Grupo 11
-- =====================================================================

-- Necesaria para combinar igualdad (habitación) con superposición de
-- rangos de fechas en una misma restricción de exclusión.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------------
-- Usuarios (herencia de tabla única: huésped, recepcionista, administrador)
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id              BIGSERIAL PRIMARY KEY,
    rol             VARCHAR(20)  NOT NULL CHECK (rol IN ('HUESPED', 'RECEPCIONISTA', 'ADMINISTRADOR')),
    nombre          VARCHAR(80)  NOT NULL,
    apellido        VARCHAR(80)  NOT NULL,
    email           VARCHAR(160) NOT NULL UNIQUE,
    password_hash   VARCHAR(100),
    telefono        VARCHAR(40),
    documento       VARCHAR(30),
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE tokens_recuperacion (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token       VARCHAR(80)  NOT NULL UNIQUE,
    expira_en   TIMESTAMPTZ  NOT NULL,
    usado       BOOLEAN      NOT NULL DEFAULT FALSE
);

-- ---------------------------------------------------------------------
-- Catálogo: tipos de habitación, habitaciones, temporadas y tarifas
-- ---------------------------------------------------------------------
CREATE TABLE tipos_habitacion (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(80)    NOT NULL UNIQUE,
    descripcion  TEXT           NOT NULL DEFAULT '',
    capacidad    INT            NOT NULL CHECK (capacidad > 0),
    precio_base  NUMERIC(12, 2) NOT NULL CHECK (precio_base > 0),
    servicios    TEXT           NOT NULL DEFAULT '',
    imagen_url   TEXT,
    activo       BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE TABLE habitaciones (
    id        BIGSERIAL PRIMARY KEY,
    numero    VARCHAR(10) NOT NULL UNIQUE,
    piso      INT         NOT NULL,
    tipo_id   BIGINT      NOT NULL REFERENCES tipos_habitacion(id),
    estado    VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE'
              CHECK (estado IN ('DISPONIBLE', 'OCUPADA', 'LIMPIEZA', 'MANTENIMIENTO')),
    activa    BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE temporadas (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(80) NOT NULL,
    categoria    VARCHAR(10) NOT NULL CHECK (categoria IN ('ALTA', 'MEDIA', 'BAJA')),
    fecha_desde  DATE        NOT NULL,
    fecha_hasta  DATE        NOT NULL,
    CHECK (fecha_hasta >= fecha_desde),
    -- Dos temporadas no pueden pisarse: cada fecha tiene una sola categoría.
    CONSTRAINT temporadas_sin_superposicion
        EXCLUDE USING gist (daterange(fecha_desde, fecha_hasta, '[]') WITH &&)
);

CREATE TABLE tarifas (
    id            BIGSERIAL PRIMARY KEY,
    tipo_id       BIGINT         NOT NULL REFERENCES tipos_habitacion(id) ON DELETE CASCADE,
    categoria     VARCHAR(10)    NOT NULL CHECK (categoria IN ('ALTA', 'MEDIA', 'BAJA')),
    precio_noche  NUMERIC(12, 2) NOT NULL CHECK (precio_noche > 0),
    UNIQUE (tipo_id, categoria)
);

-- ---------------------------------------------------------------------
-- Configuración general del hotel (una sola fila)
-- ---------------------------------------------------------------------
CREATE TABLE configuracion_hotel (
    id                    INT          PRIMARY KEY CHECK (id = 1),
    nombre_hotel          VARCHAR(120) NOT NULL,
    politica_cancelacion  VARCHAR(20)  NOT NULL CHECK (politica_cancelacion IN ('FLEXIBLE', 'MODERADA', 'ESTRICTA')),
    porcentaje_sena       INT          NOT NULL CHECK (porcentaje_sena BETWEEN 0 AND 100),
    minutos_retencion     INT          NOT NULL CHECK (minutos_retencion > 0),
    hora_check_in         VARCHAR(5)   NOT NULL,
    hora_check_out        VARCHAR(5)   NOT NULL,
    estrategia_precio     VARCHAR(30)  NOT NULL CHECK (estrategia_precio IN ('POR_TEMPORADA', 'TEMPORADA_CON_DESCUENTO'))
);

INSERT INTO configuracion_hotel (id, nombre_hotel, politica_cancelacion, porcentaje_sena, minutos_retencion, hora_check_in, hora_check_out, estrategia_precio)
VALUES (1, 'Hotel Aurora', 'MODERADA', 30, 10, '14:00', '10:00', 'POR_TEMPORADA');

-- ---------------------------------------------------------------------
-- Reservas
-- ---------------------------------------------------------------------
CREATE TABLE reservas (
    id                    BIGSERIAL PRIMARY KEY,
    codigo                VARCHAR(12)    NOT NULL UNIQUE,
    huesped_id            BIGINT         NOT NULL REFERENCES usuarios(id),
    habitacion_id         BIGINT         NOT NULL REFERENCES habitaciones(id),
    tipo_id               BIGINT         NOT NULL REFERENCES tipos_habitacion(id),
    check_in              DATE           NOT NULL,
    check_out             DATE           NOT NULL,
    cantidad_huespedes    INT            NOT NULL CHECK (cantidad_huespedes > 0),
    precio_total          NUMERIC(12, 2) NOT NULL,
    estado                VARCHAR(20)    NOT NULL
                          CHECK (estado IN ('PENDIENTE_PAGO', 'CONFIRMADA', 'EN_CURSO', 'FINALIZADA', 'CANCELADA', 'EXPIRADA')),
    canal                 VARCHAR(15)    NOT NULL CHECK (canal IN ('ONLINE', 'RECEPCION')),
    retencion_hasta       TIMESTAMPTZ,
    observaciones         TEXT,
    creada_por_id         BIGINT         REFERENCES usuarios(id),
    creada_en             TIMESTAMPTZ    NOT NULL DEFAULT now(),
    cancelada_en          TIMESTAMPTZ,
    reembolso             NUMERIC(12, 2),
    check_in_real         TIMESTAMPTZ,
    check_out_real        TIMESTAMPTZ,
    recordatorio_enviado  BOOLEAN        NOT NULL DEFAULT FALSE,
    version               BIGINT         NOT NULL DEFAULT 0,
    CHECK (check_out > check_in),

    -- *** Garantía anti-overbooking (RF-08 / RNF-04) ***
    -- La base de datos rechaza dos reservas activas de la misma habitación
    -- cuyos rangos [check_in, check_out) se superpongan, aunque lleguen
    -- en el mismo instante desde dos usuarios distintos.
    CONSTRAINT reservas_sin_superposicion
        EXCLUDE USING gist (
            habitacion_id WITH =,
            daterange(check_in, check_out, '[)') WITH &&
        ) WHERE (estado IN ('PENDIENTE_PAGO', 'CONFIRMADA', 'EN_CURSO'))
);

CREATE INDEX idx_reservas_huesped ON reservas(huesped_id);
CREATE INDEX idx_reservas_fechas ON reservas(check_in, check_out);
CREATE INDEX idx_reservas_estado ON reservas(estado);

-- Bloqueos de habitaciones por mantenimiento (rango [desde, hasta) )
CREATE TABLE bloqueos (
    id             BIGSERIAL PRIMARY KEY,
    habitacion_id  BIGINT       NOT NULL REFERENCES habitaciones(id) ON DELETE CASCADE,
    desde          DATE         NOT NULL,
    hasta          DATE         NOT NULL,
    motivo         VARCHAR(200) NOT NULL,
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (hasta > desde)
);

-- ---------------------------------------------------------------------
-- Pagos y consumos
-- ---------------------------------------------------------------------
CREATE TABLE pagos (
    id                  BIGSERIAL PRIMARY KEY,
    reserva_id          BIGINT         NOT NULL REFERENCES reservas(id),
    monto               NUMERIC(12, 2) NOT NULL CHECK (monto > 0),
    medio               VARCHAR(20)    NOT NULL CHECK (medio IN ('ONLINE', 'EFECTIVO', 'TARJETA', 'TRANSFERENCIA')),
    concepto            VARCHAR(15)    NOT NULL CHECK (concepto IN ('SENA', 'TOTAL', 'SALDO', 'REINTEGRO')),
    estado              VARCHAR(15)    NOT NULL CHECK (estado IN ('APROBADO', 'RECHAZADO')),
    referencia          VARCHAR(40)    NOT NULL,
    fecha               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    registrado_por_id   BIGINT         REFERENCES usuarios(id)
);

CREATE INDEX idx_pagos_reserva ON pagos(reserva_id);
CREATE INDEX idx_pagos_fecha ON pagos(fecha);

CREATE TABLE consumos (
    id                  BIGSERIAL PRIMARY KEY,
    reserva_id          BIGINT         NOT NULL REFERENCES reservas(id),
    descripcion         VARCHAR(200)   NOT NULL,
    monto               NUMERIC(12, 2) NOT NULL CHECK (monto > 0),
    fecha               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    registrado_por_id   BIGINT         REFERENCES usuarios(id)
);

-- ---------------------------------------------------------------------
-- Auditoría de operaciones críticas (RNF-15)
-- ---------------------------------------------------------------------
CREATE TABLE auditoria (
    id         BIGSERIAL PRIMARY KEY,
    fecha      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    usuario    VARCHAR(160),
    accion     VARCHAR(60)  NOT NULL,
    entidad    VARCHAR(40)  NOT NULL,
    entidad_id VARCHAR(40),
    detalle    TEXT
);

CREATE INDEX idx_auditoria_fecha ON auditoria(fecha DESC);
