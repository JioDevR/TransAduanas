-- Active: 1790620186333@@127.0.0.1@3306@default
-- =============================================================================
-- SISTEMA SGA - SCRIPT DDL COMPLETO DE BASE DE DATOS
-- =============================================================================

CREATE DATABASE IF NOT EXISTS SGAdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE SGAdb;

SET FOREIGN_KEY_CHECKS = 0;

-- Limpieza preventiva de tablas existentes
DROP TABLE IF EXISTS cotizacion_pago;
DROP TABLE IF EXISTS documento_adjunto;
DROP TABLE IF EXISTS liquidacion_financiera;
DROP TABLE IF EXISTS despacho_transporte;
DROP TABLE IF EXISTS conductor;
DROP TABLE IF EXISTS unidad_transporte;
DROP TABLE IF EXISTS control_almacen;
DROP TABLE IF EXISTS expediente;
DROP TABLE IF EXISTS orden_servicio;
DROP TABLE IF EXISTS cotizacion;
DROP TABLE IF EXISTS catalogo_mercancia;
DROP TABLE IF EXISTS cliente;
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS sede;
DROP TABLE IF EXISTS rol;
DROP TABLE IF EXISTS catalogo_tipo_carga;
DROP TABLE IF EXISTS catalogo_puerto;

-- =============================================================================
-- 0. CATÁLOGOS AUXILIARES
-- =============================================================================

CREATE TABLE catalogo_puerto (
    -- Identificador y datos del puerto
    id_puerto INT NOT NULL AUTO_INCREMENT,
    codigo_puerto VARCHAR(20) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    pais VARCHAR(50) NOT NULL,
    tipo VARCHAR(30) NOT NULL DEFAULT 'MARITIMO',
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    -- Llave primaria y restricciones
    PRIMARY KEY (id_puerto),
    CONSTRAINT uq_puerto_codigo UNIQUE (codigo_puerto)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE catalogo_tipo_carga (
    -- Identificador y datos del tipo de carga
    id_tipo_carga INT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    -- Llave primaria y restricciones
    PRIMARY KEY (id_tipo_carga),
    CONSTRAINT uq_tipo_carga_codigo UNIQUE (codigo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE catalogo_mercancia (
    -- Identificador y clasificación arancelaria
    id_mercancia INT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    categoria VARCHAR(80) NOT NULL,

    -- Regulación y tributación aduanera
    requiere_permiso BOOLEAN NOT NULL DEFAULT FALSE,
    entidad_reguladora VARCHAR(80) NULL,
    arancel_base_pct DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    -- Llave primaria y restricciones
    PRIMARY KEY (id_mercancia),
    CONSTRAINT uq_mercancia_codigo UNIQUE (codigo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 1. MÓDULO DE SEGURIDAD Y ACCESO TI
-- =============================================================================

CREATE TABLE rol (
    -- Identificador y datos del rol
    id_rol INT NOT NULL AUTO_INCREMENT,
    nombre_rol VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255) NULL,
    estado_activo BOOLEAN NOT NULL DEFAULT TRUE,

    -- Llave primaria y restricciones
    PRIMARY KEY (id_rol),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre_rol)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE sede (
    -- Identificador y datos de ubicación
    id_sede INT NOT NULL AUTO_INCREMENT,
    codigo_sede VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    tipo VARCHAR(50) NOT NULL DEFAULT 'SEDE_CENTRAL',
    direccion VARCHAR(255) NOT NULL,
    distrito VARCHAR(100) NOT NULL,
    telefono VARCHAR(50) NULL,
    estado VARCHAR(50) NOT NULL DEFAULT 'OPERATIVO',

    -- Llave primaria y restricciones
    PRIMARY KEY (id_sede),
    CONSTRAINT uq_sede_codigo UNIQUE (codigo_sede)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE usuario (
    -- Identificador y relaciones organizacionales
    id_usuario INT NOT NULL AUTO_INCREMENT,
    id_rol INT NOT NULL,
    id_sede INT NULL,

    -- Credenciales de acceso
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    -- Información personal y contacto
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    correo_institucional VARCHAR(100) NOT NULL,
    tipo_documento VARCHAR(10) NOT NULL DEFAULT 'DNI',
    numero_documento VARCHAR(20) NULL,
    telefono VARCHAR(20) NULL,

    -- Perfil operativo y SUNAT
    cargo_titulo VARCHAR(100) NULL,
    codigo_sunat VARCHAR(50) NULL,

    -- Auditoría y estado
    ultimo_acceso DATETIME NULL,
    estado_activo BOOLEAN NOT NULL DEFAULT TRUE,

    -- Llave primaria y restricciones
    PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_username UNIQUE (username),
    CONSTRAINT uq_usuario_correo UNIQUE (correo_institucional),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_usuario_sede FOREIGN KEY (id_sede) REFERENCES sede (id_sede) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 2. MÓDULO DE MARKETING Y COMERCIAL (VENTAS & CLIENTES)
-- =============================================================================

CREATE TABLE cliente (
    -- Identificador y datos comerciales de la empresa
    id_cliente INT NOT NULL AUTO_INCREMENT,
    tipo_documento VARCHAR(10) NOT NULL DEFAULT 'RUC',
    numero_documento VARCHAR(15) NOT NULL,
    razon_social VARCHAR(150) NOT NULL,
    contacto_representante VARCHAR(100) NULL,
    direccion_fiscal VARCHAR(200) NULL,
    correo_contacto VARCHAR(100) NOT NULL,
    telefono_contacto VARCHAR(20) NULL,

    -- Control y estado comercial
    es_prospecto BOOLEAN NOT NULL DEFAULT TRUE,

    -- Llave primaria y restricciones
    PRIMARY KEY (id_cliente),
    CONSTRAINT uq_cliente_num_doc UNIQUE (numero_documento)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE cotizacion (
    -- Identificador y relaciones comerciales
    id_cotizacion INT NOT NULL AUTO_INCREMENT,
    codigo_cotizacion VARCHAR(20) NOT NULL,
    id_cliente INT NOT NULL,
    id_usuario_comercial INT NOT NULL,
    id_mercancia INT NULL,
    id_tipo_carga INT NULL,

    -- Operación, régimen y ruta logística
    regimen_aduanero VARCHAR(50) NOT NULL DEFAULT 'IMPORTACION_DEFINITIVA',
    id_puerto_envio INT NULL,
    id_puerto_destino INT NULL,
    servicio_transporte BOOLEAN NOT NULL DEFAULT FALSE,
    origen_departamento VARCHAR(60) NULL,
    origen_provincia VARCHAR(60) NULL,
    destino_departamento VARCHAR(60) NULL,
    destino_provincia VARCHAR(60) NULL,
    costo_transporte_interno DECIMAL(12, 2) NOT NULL DEFAULT 0.00,

    -- Costos y liquidación estimada (USD)
    valor_fob DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    monto_flete DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    monto_seguro_estimado DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    monto_honorarios DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tasa_advalorem DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    tasa_percepcion DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    monto_tributos DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_cotizado DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    validez_dias INT NOT NULL DEFAULT 15,
    fecha_emision DATE NOT NULL,

    -- Revisión técnica del agente aduanero (Paso 2)
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    id_agente_revisor INT NULL,
    fecha_revision DATETIME NULL,
    motivo_observacion TEXT NULL,
    observaciones TEXT NULL,

    -- Control financiero y anticipos (Paso 2.5: Pre-Expediente)
    estado_pago VARCHAR(25) NOT NULL DEFAULT 'SIN_PAGO',
    total_abonado DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    saldo_pendiente DECIMAL(12, 2) NOT NULL DEFAULT 0.00,

    -- Llave primaria y restricciones
    PRIMARY KEY (id_cotizacion),
    CONSTRAINT uq_cotizacion_codigo UNIQUE (codigo_cotizacion),
    CONSTRAINT fk_cotizacion_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_cotizacion_comercial FOREIGN KEY (id_usuario_comercial) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_cotizacion_revisor FOREIGN KEY (id_agente_revisor) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_cotizacion_mercancia FOREIGN KEY (id_mercancia) REFERENCES catalogo_mercancia (id_mercancia) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_cotizacion_tipo_carga FOREIGN KEY (id_tipo_carga) REFERENCES catalogo_tipo_carga (id_tipo_carga) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_cotizacion_puerto_envio FOREIGN KEY (id_puerto_envio) REFERENCES catalogo_puerto (id_puerto) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_cotizacion_puerto_destino FOREIGN KEY (id_puerto_destino) REFERENCES catalogo_puerto (id_puerto) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE cotizacion_pago (
    -- Identificador y cotización asociada
    id_pago INT NOT NULL AUTO_INCREMENT,
    id_cotizacion INT NOT NULL,
    codigo_pago VARCHAR(30) NOT NULL,

    -- Detalle del abono y conversión monetaria
    tipo_abono VARCHAR(30) NOT NULL DEFAULT 'ADELANTO',
    monto_abonado DECIMAL(12, 2) NOT NULL,
    moneda VARCHAR(5) NOT NULL DEFAULT 'USD',
    tipo_cambio DECIMAL(8, 4) NOT NULL DEFAULT 3.7500,
    monto_equivalente_pen DECIMAL(12, 2) NOT NULL,

    -- Información bancaria y comprobante
    metodo_pago VARCHAR(50) NOT NULL,
    banco_origen VARCHAR(80) NULL,
    banco_destino VARCHAR(80) NOT NULL,
    numero_operacion VARCHAR(60) NOT NULL,
    fecha_operacion DATE NOT NULL,
    id_documento_comprobante INT NULL,

    -- Validación financiera y contable
    estado_pago VARCHAR(25) NOT NULL DEFAULT 'PENDIENTE_REVISION',
    id_usuario_finanzas INT NULL,
    fecha_validacion DATETIME NULL,
    observacion_finanzas TEXT NULL,

    -- Auditoría de registro
    id_usuario_registro INT NOT NULL,
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Llave primaria y restricciones
    PRIMARY KEY (id_pago),
    CONSTRAINT uq_cotizacion_pago_codigo UNIQUE (codigo_pago),
    CONSTRAINT fk_pago_cotizacion FOREIGN KEY (id_cotizacion) REFERENCES cotizacion (id_cotizacion) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_pago_usuario_finanzas FOREIGN KEY (id_usuario_finanzas) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_pago_usuario_registro FOREIGN KEY (id_usuario_registro) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_pago_doc_comprobante FOREIGN KEY (id_documento_comprobante) REFERENCES documento_adjunto (id_documento) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 3. MÓDULO DE OPERACIONES ADUANERAS (EXPEDIENTES - IMPORT / EXPORT)
-- =============================================================================

CREATE TABLE expediente (
    -- Identificador y relaciones principales
    id_expediente INT NOT NULL AUTO_INCREMENT,
    codigo_expediente VARCHAR(20) NOT NULL,
    id_cotizacion INT NOT NULL,
    id_cliente INT NOT NULL,
    id_mercancia INT NULL,
    id_tipo_carga INT NULL,

    -- Asignación a agente aduanero responsable (Paso 3)
    id_agente_aduanero INT NOT NULL,
    fecha_asignacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Contacto exterior (Import: Proveedor en origen | Export: Comprador en destino)
    contacto_exterior_nombre VARCHAR(150) NULL,
    contacto_exterior_telefono VARCHAR(30) NULL,
    contacto_exterior_correo VARCHAR(100) NULL,

    -- Fechas de tránsito internacional
    fecha_zarpe_etd DATE NULL,
    fecha_llegada_eta DATE NULL,
    fecha_arribo_aduana DATETIME NULL,

    -- Trámite aduanero SUNAT y canales (Pasos 4, 5 y 6)
    numero_dam VARCHAR(30) NULL,
    canal_sunat VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_asignacion_canal DATETIME NULL,

    -- Notificación e inspección de canales (Naranja / Rojo - Paso 6)
    notificado_agente BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_notificacion DATETIME NULL,
    detalle_inspeccion TEXT NULL,
    fecha_levante_autorizacion DATETIME NULL,

    -- Carga, contenedor y nave
    numero_contenedor VARCHAR(30) NULL,
    precinto_aduanero VARCHAR(50) NULL,
    nave_viaje VARCHAR(100) NULL,

    -- Ciclo de vida y estado del expediente
    estado_expediente VARCHAR(30) NOT NULL DEFAULT 'ASIGNADO',
    fecha_apertura DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre DATETIME NULL,
    observaciones TEXT NULL,

    -- Llave primaria y restricciones
    PRIMARY KEY (id_expediente),
    CONSTRAINT uq_expediente_codigo UNIQUE (codigo_expediente),
    CONSTRAINT uq_expediente_cotizacion UNIQUE (id_cotizacion),
    CONSTRAINT uq_expediente_dam UNIQUE (numero_dam),
    CONSTRAINT fk_expediente_cotizacion FOREIGN KEY (id_cotizacion) REFERENCES cotizacion (id_cotizacion) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_expediente_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_expediente_agente FOREIGN KEY (id_agente_aduanero) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_expediente_mercancia FOREIGN KEY (id_mercancia) REFERENCES catalogo_mercancia (id_mercancia) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_expediente_tipo_carga FOREIGN KEY (id_tipo_carga) REFERENCES catalogo_tipo_carga (id_tipo_carga) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 4. MÓDULO DE ALMACÉN Y DEPÓSITOS TEMPORALES
-- =============================================================================

CREATE TABLE control_almacen (
    -- Identificador y expediente vinculado
    id_control_almacen INT NOT NULL AUTO_INCREMENT,
    id_expediente INT NOT NULL,

    -- Depósito temporal y ubicación
    deposito_temporal VARCHAR(100) NOT NULL,
    ubicacion_bahia VARCHAR(50) NULL DEFAULT 'Patio General',
    tipo_ingreso VARCHAR(30) NOT NULL DEFAULT 'DIRECTO_VERDE',
    finalidad_almacenaje VARCHAR(30) NOT NULL DEFAULT 'INGRESO_IMPORTACION',

    -- Control de tiempos, pesaje y precintos
    fecha_ingreso DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dias_libres_almacen INT NOT NULL DEFAULT 15,
    fecha_limite_retiro DATE NOT NULL,
    cantidad_bultos INT NOT NULL DEFAULT 0,
    peso_bruto_kg DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    ticket_pesaje_vgm VARCHAR(50) NULL,
    precinto_conforme BOOLEAN NOT NULL DEFAULT TRUE,

    -- Autorización de retiro y estado de custodia
    autorizado_movimiento BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_salida DATETIME NULL,
    estado_almacen VARCHAR(30) NOT NULL DEFAULT 'EN_CUSTODIA',
    observaciones TEXT NULL,

    -- Llave primaria y restricciones
    PRIMARY KEY (id_control_almacen),
    CONSTRAINT uq_almacen_expediente UNIQUE (id_expediente),
    CONSTRAINT fk_almacen_expediente FOREIGN KEY (id_expediente) REFERENCES expediente (id_expediente) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 5. MÓDULO DE TRANSPORTE Y DISTRIBUCIÓN LOCAL (3NF)
-- =============================================================================

CREATE TABLE conductor (
    -- Identificador y vinculación de usuario
    id_conductor INT NOT NULL AUTO_INCREMENT,
    id_usuario INT NULL,

    -- Información personal y contacto
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    numero_documento VARCHAR(15) NULL,
    telefono VARCHAR(20) NULL,

    -- Licencia de conducir profesional
    tipo_licencia VARCHAR(10) NOT NULL DEFAULT 'A-IIIc',
    numero_licencia VARCHAR(20) NOT NULL,
    fecha_vencimiento_licencia DATE NULL,

    -- Disponibilidad y estado operativo
    estado_operativo VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    estado_activo BOOLEAN NOT NULL DEFAULT TRUE,

    -- Llave primaria y restricciones
    PRIMARY KEY (id_conductor),
    CONSTRAINT uq_conductor_usuario UNIQUE (id_usuario),
    CONSTRAINT uq_conductor_licencia UNIQUE (numero_licencia),
    CONSTRAINT fk_conductor_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE unidad_transporte (
    -- Identificador y datos del vehículo de carga
    id_unidad INT NOT NULL AUTO_INCREMENT,
    placa_tracto VARCHAR(15) NOT NULL,
    placa_carreta VARCHAR(15) NULL,
    marca_modelo VARCHAR(100) NOT NULL,
    ano_fabricacion INT NULL,
    tipo_vehiculo VARCHAR(50) NOT NULL DEFAULT 'TRACTO_PLATAFORMA',
    capacidad_carga_kg DECIMAL(10, 2) NOT NULL DEFAULT 30000.00,

    -- Certificaciones técnicas y kilometraje
    soat_vencimiento DATE NULL,
    citv_vencimiento DATE NULL,
    kilometraje_actual INT NOT NULL DEFAULT 0,

    -- Disponibilidad y estado operativo
    estado_operativo VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    estado_activo BOOLEAN NOT NULL DEFAULT TRUE,

    -- Llave primaria y restricciones
    PRIMARY KEY (id_unidad),
    CONSTRAINT uq_unidad_tracto UNIQUE (placa_tracto)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE despacho_transporte (
    -- Identificador y tracking de traslado
    id_despacho_transporte INT NOT NULL AUTO_INCREMENT,
    id_expediente INT NOT NULL,
    codigo_tracking VARCHAR(30) NOT NULL,
    tipo_traslado VARCHAR(30) NOT NULL DEFAULT 'ENTREGA_IMPORTACION',

    -- Recursos asignados normalizados (3NF)
    id_conductor INT NULL,
    id_unidad INT NULL,

    -- Documentación y ruta de traslado
    guia_remision_transporte VARCHAR(30) NULL,
    id_documento_remision INT NULL,
    direccion_partida VARCHAR(255) NOT NULL,
    direccion_llegada VARCHAR(255) NOT NULL,
    distrito_llegada VARCHAR(100) NOT NULL,

    -- Datos del receptor de la carga
    contacto_recepcion VARCHAR(100) NULL,
    receptor_nombre VARCHAR(120) NULL,
    receptor_dni VARCHAR(15) NULL,

    -- Fechas y control de traslado
    fecha_programada DATETIME NOT NULL,
    fecha_salida_ruta DATETIME NULL,
    fecha_entrega_conforme DATETIME NULL,

    -- Incidencias y estado del traslado
    estado_transporte VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_ASIGNACION',
    detalle_incidencia TEXT NULL,
    observaciones TEXT NULL,

    -- Llave primaria y restricciones
    PRIMARY KEY (id_despacho_transporte),
    CONSTRAINT uq_transporte_expediente UNIQUE (id_expediente),
    CONSTRAINT uq_transporte_tracking UNIQUE (codigo_tracking),
    CONSTRAINT fk_transporte_expediente FOREIGN KEY (id_expediente) REFERENCES expediente (id_expediente) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_transporte_conductor FOREIGN KEY (id_conductor) REFERENCES conductor (id_conductor) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_transporte_unidad FOREIGN KEY (id_unidad) REFERENCES unidad_transporte (id_unidad) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_despacho_doc_remision FOREIGN KEY (id_documento_remision) REFERENCES documento_adjunto (id_documento) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 6. MÓDULO DE FINANZAS Y LIQUIDACIÓN
-- =============================================================================

CREATE TABLE liquidacion_financiera (
    -- Identificador, expediente y responsable
    id_liquidacion INT NOT NULL AUTO_INCREMENT,
    codigo_liquidacion VARCHAR(30) NOT NULL,
    id_expediente INT NOT NULL,
    id_usuario_liquidador INT NULL,

    -- Desglose de cobranza consolidada
    total_tributos_sunat DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    gastos_almacen_puerto DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    flete_transporte_local DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    honorarios_agencia DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    subtotal_imponible DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    igv_monto DECIMAL(12, 2) NOT NULL DEFAULT 0.00,

    -- Detracciones SPOT y totales finales
    aplica_detraccion BOOLEAN NOT NULL DEFAULT TRUE,
    porcentaje_detraccion DECIMAL(5, 2) NOT NULL DEFAULT 12.00,
    monto_detraccion DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_general_pen DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_general_usd DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tipo_cambio DECIMAL(8, 4) NOT NULL DEFAULT 3.7500,

    -- Cobranza, facturación y conciliación bancaria
    estado_pago VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    numero_factura VARCHAR(30) NULL,
    id_documento_factura INT NULL,
    metodo_pago VARCHAR(50) NULL,
    banco_destino VARCHAR(80) NULL,
    numero_operacion VARCHAR(50) NULL,
    id_documento_voucher INT NULL,
    fecha_emision DATE NOT NULL,
    fecha_pago DATETIME NULL,
    observaciones TEXT NULL,

    -- Llave primaria y restricciones
    PRIMARY KEY (id_liquidacion),
    CONSTRAINT uq_liquidacion_codigo UNIQUE (codigo_liquidacion),
    CONSTRAINT uq_liquidacion_expediente UNIQUE (id_expediente),
    CONSTRAINT fk_liquidacion_expediente FOREIGN KEY (id_expediente) REFERENCES expediente (id_expediente) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_liquidacion_usuario FOREIGN KEY (id_usuario_liquidador) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_liquidacion_doc_factura FOREIGN KEY (id_documento_factura) REFERENCES documento_adjunto (id_documento) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_liquidacion_doc_voucher FOREIGN KEY (id_documento_voucher) REFERENCES documento_adjunto (id_documento) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 7. MÓDULO DE GESTIÓN DOCUMENTAL
-- =============================================================================

CREATE TABLE documento_adjunto (
    -- Identificador del documento
    id_documento INT NOT NULL AUTO_INCREMENT,

    -- Metadatos y almacenamiento del archivo físico
    nombre_original VARCHAR(255) NOT NULL,
    nombre_almacenado VARCHAR(255) NOT NULL,
    formato_mime VARCHAR(120) NOT NULL,
    extension VARCHAR(20) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    ruta_almacenamiento VARCHAR(500) NOT NULL,

    -- Clasificación documental
    categoria VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    descripcion VARCHAR(255) NULL,

    -- Asociación polimórfica a entidades de negocio
    entidad_tipo VARCHAR(40) NOT NULL DEFAULT 'GENERAL',
    id_cotizacion INT NULL,
    id_expediente INT NULL,
    id_cliente INT NULL,

    -- Auditoría y estado
    id_usuario_creador INT NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo TINYINT(1) NOT NULL DEFAULT 1,

    -- Llave primaria y restricciones
    PRIMARY KEY (id_documento),
    CONSTRAINT uq_doc_nombre_almacenado UNIQUE (nombre_almacenado),
    CONSTRAINT fk_doc_cotizacion FOREIGN KEY (id_cotizacion) REFERENCES cotizacion (id_cotizacion) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_doc_expediente FOREIGN KEY (id_expediente) REFERENCES expediente (id_expediente) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_doc_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_doc_usuario FOREIGN KEY (id_usuario_creador) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 8. ÍNDICES COMPLEMENTARIOS
-- =============================================================================

-- Catálogos y Seguridad TI
CREATE INDEX idx_puerto_estado ON catalogo_puerto (estado);
CREATE INDEX idx_tipo_carga_estado ON catalogo_tipo_carga (estado);
CREATE INDEX idx_mercancia_estado ON catalogo_mercancia (estado);
CREATE INDEX idx_mercancia_categoria ON catalogo_mercancia (categoria);
CREATE INDEX idx_rol_estado ON rol (estado_activo);
CREATE INDEX idx_sede_estado ON sede (estado);
CREATE INDEX idx_usuario_rol ON usuario (id_rol);
CREATE INDEX idx_usuario_sede ON usuario (id_sede);
CREATE INDEX idx_usuario_estado ON usuario (estado_activo);

-- Marketing y Comercial (Cotizaciones y Pagos)
CREATE INDEX idx_cotizacion_cliente ON cotizacion (id_cliente);
CREATE INDEX idx_cotizacion_comercial ON cotizacion (id_usuario_comercial);
CREATE INDEX idx_cotizacion_puerto_envio ON cotizacion (id_puerto_envio);
CREATE INDEX idx_cotizacion_puerto_destino ON cotizacion (id_puerto_destino);
CREATE INDEX idx_cotizacion_revisor ON cotizacion (id_agente_revisor);
CREATE INDEX idx_cotizacion_mercancia ON cotizacion (id_mercancia);
CREATE INDEX idx_cotizacion_estado ON cotizacion (estado);
CREATE INDEX idx_cotizacion_estado_pago ON cotizacion (estado_pago);
CREATE INDEX idx_pago_cotizacion ON cotizacion_pago (id_cotizacion);
CREATE INDEX idx_pago_estado ON cotizacion_pago (estado_pago);
CREATE INDEX idx_pago_cotizacion_estado ON cotizacion_pago (id_cotizacion, estado_pago);
CREATE INDEX idx_pago_fecha ON cotizacion_pago (fecha_operacion);

-- Operaciones Aduaneras (Expedientes)
CREATE INDEX idx_expediente_cliente ON expediente (id_cliente);
CREATE INDEX idx_expediente_agente ON expediente (id_agente_aduanero);
CREATE INDEX idx_expediente_mercancia ON expediente (id_mercancia);
CREATE INDEX idx_expediente_canal ON expediente (canal_sunat);
CREATE INDEX idx_expediente_estado ON expediente (estado_expediente);

-- Almacén y Depósitos Temporales
CREATE INDEX idx_almacen_estado ON control_almacen (estado_almacen);

-- Transporte y Distribución Local
CREATE INDEX idx_conductor_licencia ON conductor (numero_licencia);
CREATE INDEX idx_conductor_usuario ON conductor (id_usuario);
CREATE INDEX idx_conductor_estado_operativo ON conductor (estado_operativo);
CREATE INDEX idx_unidad_placa ON unidad_transporte (placa_tracto);
CREATE INDEX idx_unidad_vencimientos ON unidad_transporte (soat_vencimiento, citv_vencimiento);
CREATE INDEX idx_unidad_estado ON unidad_transporte (estado_operativo);
CREATE INDEX idx_transporte_estado ON despacho_transporte (estado_transporte);

-- Finanzas y Liquidación
CREATE INDEX idx_liquidacion_estado_pago ON liquidacion_financiera (estado_pago);
CREATE INDEX idx_liquidacion_liquidador ON liquidacion_financiera (id_usuario_liquidador);

-- Gestión Documental
CREATE INDEX idx_doc_entidad ON documento_adjunto (entidad_tipo, id_cotizacion, id_expediente, id_cliente);
CREATE INDEX idx_doc_categoria ON documento_adjunto (categoria);
CREATE INDEX idx_doc_activo ON documento_adjunto (activo);

-- Rehabilitación de validación de claves foráneas
SET FOREIGN_KEY_CHECKS = 1;