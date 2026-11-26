-- Active: 1790620186333@@127.0.0.1@3306@default
-- =============================================================================
-- SISTEMA SGA - SCRIPT DDL COMPLETO DE BASE DE DATOS (NORMALIZADO 3FN - 6FN)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS SGAdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE SGAdb;

SET FOREIGN_KEY_CHECKS = 0;

-- Limpieza preventiva de tablas existentes
DROP TABLE IF EXISTS expediente_solicitud_transporte;
DROP TABLE IF EXISTS expediente_conformidad_entrega;
DROP TABLE IF EXISTS expediente_transmision_naranja;
DROP TABLE IF EXISTS expediente_inspeccion_rojo;
DROP TABLE IF EXISTS expediente_canal_sunat;
DROP TABLE IF EXISTS expediente_transito_internacional;
DROP TABLE IF EXISTS expediente_contacto_exterior;
DROP TABLE IF EXISTS expediente_evento;

DROP TABLE IF EXISTS comprobante_factura;
DROP TABLE IF EXISTS cotizacion_pago;
DROP TABLE IF EXISTS notificacion_cliente;
DROP TABLE IF EXISTS usuario_cliente;
DROP TABLE IF EXISTS documento_adjunto;
DROP TABLE IF EXISTS liquidacion_financiera;
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

DROP TABLE IF EXISTS catalogo_provincia;
DROP TABLE IF EXISTS catalogo_departamento;
DROP TABLE IF EXISTS catalogo_categoria_documento;
DROP TABLE IF EXISTS catalogo_motivo_observacion;
DROP TABLE IF EXISTS catalogo_entidad_reguladora;
DROP TABLE IF EXISTS catalogo_categoria_mercancia;
DROP TABLE IF EXISTS catalogo_metodo_pago;
DROP TABLE IF EXISTS catalogo_cuenta_recaudadora;
DROP TABLE IF EXISTS catalogo_banco;
DROP TABLE IF EXISTS auditoria_evento;

-- =============================================================================
-- 0. CATÁLOGOS AUXILIARES Y MAESTROS 3FN
-- =============================================================================

CREATE TABLE catalogo_puerto (
    id_puerto INT NOT NULL AUTO_INCREMENT,
    codigo_puerto VARCHAR(20) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    pais VARCHAR(50) NOT NULL,
    tipo VARCHAR(30) NOT NULL DEFAULT 'MARITIMO',
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    PRIMARY KEY (id_puerto),
    CONSTRAINT uq_puerto_codigo UNIQUE (codigo_puerto)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE catalogo_tipo_carga (
    id_tipo_carga INT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255) NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    PRIMARY KEY (id_tipo_carga),
    CONSTRAINT uq_tipo_carga_codigo UNIQUE (codigo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE catalogo_categoria_mercancia (
    id_categoria INT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(80) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    PRIMARY KEY (id_categoria),
    CONSTRAINT uq_cat_mercancia_codigo UNIQUE (codigo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE catalogo_entidad_reguladora (
    id_entidad INT NOT NULL AUTO_INCREMENT,
    sigla VARCHAR(50) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    PRIMARY KEY (id_entidad),
    CONSTRAINT uq_entidad_reg_sigla UNIQUE (sigla)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE catalogo_mercancia (
    id_mercancia INT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    id_categoria INT NULL,
    categoria VARCHAR(80) NOT NULL,
    requiere_permiso BOOLEAN NOT NULL DEFAULT FALSE,
    id_entidad_reguladora INT NULL,
    entidad_reguladora VARCHAR(80) NULL,
    arancel_base_pct DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    PRIMARY KEY (id_mercancia),
    CONSTRAINT uq_mercancia_codigo UNIQUE (codigo),
    CONSTRAINT fk_mercancia_categoria FOREIGN KEY (id_categoria) REFERENCES catalogo_categoria_mercancia (id_categoria) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_mercancia_entidad_reg FOREIGN KEY (id_entidad_reguladora) REFERENCES catalogo_entidad_reguladora (id_entidad) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE catalogo_banco (
    id_banco INT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(40) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    aplica_origen BOOLEAN NOT NULL DEFAULT TRUE,
    aplica_destino BOOLEAN NOT NULL DEFAULT TRUE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    PRIMARY KEY (id_banco),
    CONSTRAINT uq_banco_codigo UNIQUE (codigo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE catalogo_cuenta_recaudadora (
    id_cuenta INT NOT NULL AUTO_INCREMENT,
    id_banco INT NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    moneda VARCHAR(5) NOT NULL DEFAULT 'USD',
    numero_cuenta VARCHAR(50) NOT NULL,
    cci VARCHAR(50) NULL,
    aplica_portal_b2b BOOLEAN NOT NULL DEFAULT TRUE,
    aplica_sga BOOLEAN NOT NULL DEFAULT TRUE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    PRIMARY KEY (id_cuenta),
    CONSTRAINT uq_cuenta_codigo UNIQUE (codigo),
    CONSTRAINT fk_cuenta_banco FOREIGN KEY (id_banco) REFERENCES catalogo_banco (id_banco) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE catalogo_metodo_pago (
    id_metodo_pago INT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    aplica_cotizacion BOOLEAN NOT NULL DEFAULT TRUE,
    aplica_liquidacion BOOLEAN NOT NULL DEFAULT TRUE,
    aplica_portal_b2b BOOLEAN NOT NULL DEFAULT TRUE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    PRIMARY KEY (id_metodo_pago),
    CONSTRAINT uq_metodo_pago_codigo UNIQUE (codigo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE catalogo_motivo_observacion (
    id_motivo INT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(50) NOT NULL,
    descripcion VARCHAR(200) NOT NULL,
    modulo VARCHAR(30) NOT NULL DEFAULT 'ABONO',
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    PRIMARY KEY (id_motivo),
    CONSTRAINT uq_motivo_obs_codigo UNIQUE (codigo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE catalogo_categoria_documento (
    id_categoria_documento INT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    PRIMARY KEY (id_categoria_documento),
    CONSTRAINT uq_cat_doc_codigo UNIQUE (codigo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE catalogo_departamento (
    id_departamento INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(60) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    PRIMARY KEY (id_departamento),
    CONSTRAINT uq_departamento_nombre UNIQUE (nombre)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE catalogo_provincia (
    id_provincia INT NOT NULL AUTO_INCREMENT,
    id_departamento INT NOT NULL,
    nombre VARCHAR(60) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    PRIMARY KEY (id_provincia),
    CONSTRAINT uq_provincia_dep_nombre UNIQUE (id_departamento, nombre),
    CONSTRAINT fk_provincia_departamento FOREIGN KEY (id_departamento) REFERENCES catalogo_departamento (id_departamento) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 1. MÓDULO DE SEGURIDAD Y ACCESO TI
-- =============================================================================

CREATE TABLE sede (
    id_sede INT NOT NULL AUTO_INCREMENT,
    codigo_sede VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    tipo VARCHAR(50) NOT NULL DEFAULT 'SEDE_CENTRAL',
    direccion VARCHAR(255) NOT NULL,
    distrito VARCHAR(100) NOT NULL,
    telefono VARCHAR(50) NULL,
    estado VARCHAR(50) NOT NULL DEFAULT 'OPERATIVO',
    PRIMARY KEY (id_sede),
    CONSTRAINT uq_sede_codigo UNIQUE (codigo_sede)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE usuario (
    id_usuario INT NOT NULL AUTO_INCREMENT,
    rol VARCHAR(50) NOT NULL,
    id_sede INT NULL,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    correo_institucional VARCHAR(100) NOT NULL,
    tipo_documento VARCHAR(10) NOT NULL DEFAULT 'DNI',
    numero_documento VARCHAR(20) NULL,
    telefono VARCHAR(20) NULL,
    cargo_titulo VARCHAR(100) NULL,
    codigo_sunat VARCHAR(50) NULL,
    ultimo_acceso DATETIME NULL,
    estado_activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_username UNIQUE (username),
    CONSTRAINT uq_usuario_correo UNIQUE (correo_institucional),
    CONSTRAINT fk_usuario_sede FOREIGN KEY (id_sede) REFERENCES sede (id_sede) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 2. MÓDULO DE MARKETING Y COMERCIAL (VENTAS & CLIENTES)
-- =============================================================================

CREATE TABLE cliente (
    id_cliente INT NOT NULL AUTO_INCREMENT,
    tipo_documento VARCHAR(10) NOT NULL DEFAULT 'RUC',
    numero_documento VARCHAR(15) NOT NULL,
    razon_social VARCHAR(150) NOT NULL,
    contacto_representante VARCHAR(100) NULL,
    direccion_fiscal VARCHAR(200) NULL,
    correo_contacto VARCHAR(100) NOT NULL,
    telefono_contacto VARCHAR(20) NULL,
    estado_cliente VARCHAR(30) NOT NULL DEFAULT 'PROSPECTO',
    estado_activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id_cliente),
    CONSTRAINT uq_cliente_num_doc UNIQUE (numero_documento)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE usuario_cliente (
    id_usuario_cliente INT NOT NULL AUTO_INCREMENT,
    id_cliente INT NOT NULL,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    estado_activo BOOLEAN NOT NULL DEFAULT TRUE,
    debe_cambiar_password BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_acceso DATETIME NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario_cliente),
    CONSTRAINT uq_usrcli_cliente UNIQUE (id_cliente),
    CONSTRAINT uq_usrcli_username UNIQUE (username),
    CONSTRAINT fk_usrcli_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE notificacion_cliente (
    id_notificacion INT NOT NULL AUTO_INCREMENT,
    id_cliente INT NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    mensaje TEXT NOT NULL,
    tipo VARCHAR(50) NOT NULL DEFAULT 'SISTEMA',
    leido BOOLEAN NOT NULL DEFAULT FALSE,
    enlace VARCHAR(255) NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_notificacion),
    CONSTRAINT fk_notifcli_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE cotizacion (
    id_cotizacion INT NOT NULL AUTO_INCREMENT,
    codigo_cotizacion VARCHAR(20) NOT NULL,
    id_cliente INT NOT NULL,
    id_usuario_comercial INT NULL,
    id_mercancia INT NULL,
    id_tipo_carga INT NULL,
    regimen_aduanero VARCHAR(50) NOT NULL DEFAULT 'IMPORTACION_DEFINITIVA',
    id_puerto_envio INT NULL,
    id_puerto_destino INT NULL,
    servicio_transporte BOOLEAN NOT NULL DEFAULT FALSE,
    id_provincia_origen INT NULL,
    origen_departamento VARCHAR(60) NULL,
    origen_provincia VARCHAR(60) NULL,
    id_provincia_destino INT NULL,
    destino_departamento VARCHAR(60) NULL,
    destino_provincia VARCHAR(60) NULL,
    costo_transporte_interno DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
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
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    id_agente_revisor INT NULL,
    fecha_revision DATETIME NULL,
    motivo_observacion TEXT NULL,
    observaciones TEXT NULL,
    estado_pago VARCHAR(25) NOT NULL DEFAULT 'SIN_PAGO',
    total_abonado DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    saldo_pendiente DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id_cotizacion),
    CONSTRAINT uq_cotizacion_codigo UNIQUE (codigo_cotizacion),
    CONSTRAINT fk_cotizacion_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_cotizacion_comercial FOREIGN KEY (id_usuario_comercial) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_cotizacion_revisor FOREIGN KEY (id_agente_revisor) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_cotizacion_mercancia FOREIGN KEY (id_mercancia) REFERENCES catalogo_mercancia (id_mercancia) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_cotizacion_tipo_carga FOREIGN KEY (id_tipo_carga) REFERENCES catalogo_tipo_carga (id_tipo_carga) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_cotizacion_puerto_envio FOREIGN KEY (id_puerto_envio) REFERENCES catalogo_puerto (id_puerto) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_cotizacion_puerto_destino FOREIGN KEY (id_puerto_destino) REFERENCES catalogo_puerto (id_puerto) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_cotizacion_prov_origen FOREIGN KEY (id_provincia_origen) REFERENCES catalogo_provincia (id_provincia) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_cotizacion_prov_destino FOREIGN KEY (id_provincia_destino) REFERENCES catalogo_provincia (id_provincia) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE cotizacion_pago (
    id_pago INT NOT NULL AUTO_INCREMENT,
    id_cotizacion INT NOT NULL,
    codigo_pago VARCHAR(30) NOT NULL,
    tipo_abono VARCHAR(30) NOT NULL DEFAULT 'ADELANTO',
    monto_abonado DECIMAL(12, 2) NOT NULL,
    moneda VARCHAR(5) NOT NULL DEFAULT 'USD',
    tipo_cambio DECIMAL(8, 4) NOT NULL DEFAULT 3.7500,
    monto_equivalente_pen DECIMAL(12, 2) NOT NULL,
    id_metodo_pago INT NULL,
    metodo_pago VARCHAR(50) NOT NULL,
    id_banco_origen INT NULL,
    banco_origen VARCHAR(80) NULL,
    id_banco_destino INT NULL,
    id_cuenta_recaudadora INT NULL,
    banco_destino VARCHAR(80) NOT NULL,
    numero_operacion VARCHAR(60) NOT NULL,
    fecha_operacion DATE NOT NULL,
    id_documento_comprobante INT NULL,
    estado_pago VARCHAR(25) NOT NULL DEFAULT 'PENDIENTE_REVISION',
    id_usuario_finanzas INT NULL,
    fecha_validacion DATETIME NULL,
    observacion_finanzas TEXT NULL,
    id_usuario_registro INT NULL,
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_pago),
    CONSTRAINT uq_cotizacion_pago_codigo UNIQUE (codigo_pago),
    CONSTRAINT fk_pago_cotizacion FOREIGN KEY (id_cotizacion) REFERENCES cotizacion (id_cotizacion) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_pago_metodo FOREIGN KEY (id_metodo_pago) REFERENCES catalogo_metodo_pago (id_metodo_pago) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_pago_banco_origen FOREIGN KEY (id_banco_origen) REFERENCES catalogo_banco (id_banco) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_pago_banco_destino FOREIGN KEY (id_banco_destino) REFERENCES catalogo_banco (id_banco) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_pago_cuenta_recaudadora FOREIGN KEY (id_cuenta_recaudadora) REFERENCES catalogo_cuenta_recaudadora (id_cuenta) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_pago_usuario_finanzas FOREIGN KEY (id_usuario_finanzas) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_pago_usuario_registro FOREIGN KEY (id_usuario_registro) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_pago_doc_comprobante FOREIGN KEY (id_documento_comprobante) REFERENCES documento_adjunto (id_documento) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 3. MÓDULO DE OPERACIONES ADUANERAS (EXPEDIENTE NÚCLEO + SATÉLITES 6FN)
-- =============================================================================

CREATE TABLE expediente (
    id_expediente INT NOT NULL AUTO_INCREMENT,
    codigo_expediente VARCHAR(20) NOT NULL,
    id_cotizacion INT NOT NULL,
    id_cliente INT NOT NULL,
    id_mercancia INT NULL,
    id_tipo_carga INT NULL,
    id_agente_aduanero INT NULL,
    fecha_asignacion DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    en_espera_transito BOOLEAN NOT NULL DEFAULT FALSE,
    en_revision_aduana BOOLEAN NOT NULL DEFAULT FALSE,
    estado_expediente VARCHAR(30) NOT NULL DEFAULT 'ASIGNADO',
    fecha_apertura DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre DATETIME NULL,
    observaciones TEXT NULL,
    PRIMARY KEY (id_expediente),
    CONSTRAINT uq_expediente_codigo UNIQUE (codigo_expediente),
    CONSTRAINT uq_expediente_cotizacion UNIQUE (id_cotizacion),
    CONSTRAINT fk_expediente_cotizacion FOREIGN KEY (id_cotizacion) REFERENCES cotizacion (id_cotizacion) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_expediente_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_expediente_agente FOREIGN KEY (id_agente_aduanero) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_expediente_mercancia FOREIGN KEY (id_mercancia) REFERENCES catalogo_mercancia (id_mercancia) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_expediente_tipo_carga FOREIGN KEY (id_tipo_carga) REFERENCES catalogo_tipo_carga (id_tipo_carga) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE expediente_contacto_exterior (
    id_expediente INT NOT NULL,
    contacto_exterior_nombre VARCHAR(150) NULL,
    contacto_exterior_telefono VARCHAR(30) NULL,
    contacto_exterior_correo VARCHAR(100) NULL,
    verificacion_origen_ok BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_verificacion_origen DATETIME NULL,
    PRIMARY KEY (id_expediente),
    CONSTRAINT fk_exp_contacto_expediente FOREIGN KEY (id_expediente) REFERENCES expediente (id_expediente) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE expediente_transito_internacional (
    id_expediente INT NOT NULL,
    fecha_zarpe_etd DATE NULL,
    fecha_llegada_eta DATE NULL,
    fecha_arribo_aduana DATETIME NULL,
    numero_contenedor VARCHAR(30) NULL,
    precinto_aduanero VARCHAR(50) NULL,
    nave_viaje VARCHAR(100) NULL,
    tipo_transporte_internacional VARCHAR(30) NULL,
    numero_vuelo VARCHAR(60) NULL,
    PRIMARY KEY (id_expediente),
    CONSTRAINT fk_exp_transito_expediente FOREIGN KEY (id_expediente) REFERENCES expediente (id_expediente) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE expediente_canal_sunat (
    id_expediente INT NOT NULL,
    numero_dam VARCHAR(30) NULL,
    canal_sunat VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_asignacion_canal DATETIME NULL,
    notificado_agente BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_notificacion DATETIME NULL,
    fecha_levante_autorizacion DATETIME NULL,
    PRIMARY KEY (id_expediente),
    CONSTRAINT uq_expediente_dam UNIQUE (numero_dam),
    CONSTRAINT fk_exp_canal_expediente FOREIGN KEY (id_expediente) REFERENCES expediente (id_expediente) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE expediente_inspeccion_rojo (
    id_expediente INT NOT NULL,
    cuadrilla_solicitada BOOLEAN NOT NULL DEFAULT FALSE,
    cuadrilla_detalle VARCHAR(200) NULL,
    fecha_cita_inspeccion DATETIME NULL,
    codigo_cita_inspeccion VARCHAR(50) NULL,
    detalle_inspeccion TEXT NULL,
    revision_rojo_finalizada BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id_expediente),
    CONSTRAINT fk_exp_rojo_expediente FOREIGN KEY (id_expediente) REFERENCES expediente (id_expediente) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE expediente_transmision_naranja (
    id_expediente INT NOT NULL,
    envio_sunat_simulado_ok BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_transmision_sunat VARCHAR(60) NULL,
    fecha_envio_sunat DATETIME NULL,
    PRIMARY KEY (id_expediente),
    CONSTRAINT fk_exp_naranja_expediente FOREIGN KEY (id_expediente) REFERENCES expediente (id_expediente) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE expediente_conformidad_entrega (
    id_expediente INT NOT NULL,
    id_documento_conformidad_destino INT NULL,
    entregado_almacen_verde BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_entrega_almacen_verde DATETIME NULL,
    informe_conformidad_texto TEXT NULL,
    fecha_informe_conformidad DATETIME NULL,
    receptor_conformidad_nombre VARCHAR(150) NULL,
    receptor_conformidad_dni VARCHAR(30) NULL,
    id_documento_informe_conformidad INT NULL,
    PRIMARY KEY (id_expediente),
    CONSTRAINT fk_exp_conf_expediente FOREIGN KEY (id_expediente) REFERENCES expediente (id_expediente) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE expediente_solicitud_transporte (
    id_expediente INT NOT NULL,
    transporte_solicitado BOOLEAN NOT NULL DEFAULT FALSE,
    transporte_tipo_traslado VARCHAR(30) NULL,
    transporte_direccion_partida VARCHAR(255) NULL,
    transporte_direccion_llegada VARCHAR(255) NULL,
    transporte_distrito_llegada VARCHAR(100) NULL,
    transporte_contacto_recepcion VARCHAR(100) NULL,
    transporte_fecha_programada DATETIME NULL,
    transporte_observaciones TEXT NULL,
    transporte_fecha_solicitud DATETIME NULL,
    PRIMARY KEY (id_expediente),
    CONSTRAINT fk_exp_transp_expediente FOREIGN KEY (id_expediente) REFERENCES expediente (id_expediente) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE expediente_evento (
    id_evento INT NOT NULL AUTO_INCREMENT,
    id_expediente INT NOT NULL,
    id_usuario INT NULL,
    codigo_estado VARCHAR(60) NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT NULL,
    regimen VARCHAR(30) NULL,
    usuario_nombre VARCHAR(150) NULL,
    fecha_evento DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_evento),
    CONSTRAINT fk_exp_evento_expediente FOREIGN KEY (id_expediente) REFERENCES expediente (id_expediente) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_exp_evento_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 4. MÓDULO DE ALMACÉN Y DEPÓSITOS TEMPORALES
-- =============================================================================

CREATE TABLE control_almacen (
    id_control_almacen INT NOT NULL AUTO_INCREMENT,
    id_expediente INT NOT NULL,
    deposito_temporal VARCHAR(100) NOT NULL,
    ubicacion_bahia VARCHAR(50) NULL DEFAULT 'Patio General',
    tipo_ingreso VARCHAR(30) NOT NULL DEFAULT 'DIRECTO_VERDE',
    finalidad_almacenaje VARCHAR(30) NOT NULL DEFAULT 'INGRESO_IMPORTACION',
    fecha_ingreso DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dias_libres_almacen INT NOT NULL DEFAULT 15,
    fecha_limite_retiro DATE NOT NULL,
    cantidad_bultos INT NOT NULL DEFAULT 0,
    peso_bruto_kg DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    ticket_pesaje_vgm VARCHAR(50) NULL,
    precinto_conforme BOOLEAN NOT NULL DEFAULT TRUE,
    autorizado_movimiento BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_salida DATETIME NULL,
    estado_almacen VARCHAR(30) NOT NULL DEFAULT 'EN_CUSTODIA',
    observaciones TEXT NULL,
    PRIMARY KEY (id_control_almacen),
    CONSTRAINT uq_almacen_expediente UNIQUE (id_expediente),
    CONSTRAINT fk_almacen_expediente FOREIGN KEY (id_expediente) REFERENCES expediente (id_expediente) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 5. MÓDULO DE FINANZAS Y LIQUIDACIÓN
-- =============================================================================

CREATE TABLE liquidacion_financiera (
    id_liquidacion INT NOT NULL AUTO_INCREMENT,
    codigo_liquidacion VARCHAR(30) NOT NULL,
    id_expediente INT NOT NULL,
    id_usuario_liquidador INT NULL,
    total_tributos_sunat DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    gastos_almacen_puerto DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    flete_transporte_local DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    honorarios_agencia DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    subtotal_imponible DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    igv_monto DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    aplica_detraccion BOOLEAN NOT NULL DEFAULT TRUE,
    porcentaje_detraccion DECIMAL(5, 2) NOT NULL DEFAULT 12.00,
    monto_detraccion DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_general_pen DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_general_usd DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tipo_cambio DECIMAL(8, 4) NOT NULL DEFAULT 3.7500,
    estado_pago VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    numero_factura VARCHAR(30) NULL,
    id_documento_factura INT NULL,
    id_metodo_pago INT NULL,
    metodo_pago VARCHAR(50) NULL,
    id_banco_destino INT NULL,
    banco_destino VARCHAR(80) NULL,
    numero_operacion VARCHAR(50) NULL,
    id_documento_voucher INT NULL,
    fecha_emision DATE NOT NULL,
    fecha_pago DATETIME NULL,
    observaciones TEXT NULL,
    PRIMARY KEY (id_liquidacion),
    CONSTRAINT uq_liquidacion_codigo UNIQUE (codigo_liquidacion),
    CONSTRAINT uq_liquidacion_expediente UNIQUE (id_expediente),
    CONSTRAINT fk_liquidacion_expediente FOREIGN KEY (id_expediente) REFERENCES expediente (id_expediente) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_liquidacion_usuario FOREIGN KEY (id_usuario_liquidador) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_liquidacion_metodo FOREIGN KEY (id_metodo_pago) REFERENCES catalogo_metodo_pago (id_metodo_pago) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_liquidacion_banco_dest FOREIGN KEY (id_banco_destino) REFERENCES catalogo_banco (id_banco) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_liquidacion_doc_factura FOREIGN KEY (id_documento_factura) REFERENCES documento_adjunto (id_documento) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_liquidacion_doc_voucher FOREIGN KEY (id_documento_voucher) REFERENCES documento_adjunto (id_documento) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE comprobante_factura (
    id_comprobante INT NOT NULL AUTO_INCREMENT,
    tipo_comprobante VARCHAR(20) NOT NULL DEFAULT 'FACTURA',
    serie VARCHAR(10) NOT NULL,
    correlativo INT NOT NULL,
    numero_completo VARCHAR(25) NOT NULL,
    origen_tipo VARCHAR(30) NOT NULL,
    id_pago_cotizacion INT NULL,
    id_liquidacion INT NULL,
    id_cliente INT NOT NULL,
    cliente_razon_social VARCHAR(200) NOT NULL,
    cliente_numero_documento VARCHAR(25) NOT NULL,
    cliente_direccion VARCHAR(255) NULL,
    codigo_referencia VARCHAR(60) NOT NULL,
    concepto TEXT NOT NULL,
    moneda VARCHAR(5) NOT NULL DEFAULT 'PEN',
    tipo_cambio DECIMAL(8, 4) NOT NULL DEFAULT 3.7500,
    inafecto DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    subtotal_gravado DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    igv_monto DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_comprobante DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_equivalente_pen DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    estado VARCHAR(20) NOT NULL DEFAULT 'EMITIDA',
    fecha_emision DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario_emisor INT NULL,
    observaciones TEXT NULL,
    PRIMARY KEY (id_comprobante),
    CONSTRAINT uq_comprobante_numero UNIQUE (numero_completo),
    CONSTRAINT fk_comprobante_pago_cot FOREIGN KEY (id_pago_cotizacion) REFERENCES cotizacion_pago (id_pago) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_comprobante_liquidacion FOREIGN KEY (id_liquidacion) REFERENCES liquidacion_financiera (id_liquidacion) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_comprobante_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_comprobante_usuario FOREIGN KEY (id_usuario_emisor) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 7. MÓDULO DE GESTIÓN DOCUMENTAL
-- =============================================================================

CREATE TABLE documento_adjunto (
    id_documento INT NOT NULL AUTO_INCREMENT,
    nombre_original VARCHAR(255) NOT NULL,
    nombre_almacenado VARCHAR(255) NOT NULL,
    formato_mime VARCHAR(120) NOT NULL,
    extension VARCHAR(20) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    ruta_almacenamiento VARCHAR(500) NOT NULL,
    id_categoria_documento INT NULL,
    categoria VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    descripcion VARCHAR(255) NULL,
    entidad_tipo VARCHAR(40) NOT NULL DEFAULT 'GENERAL',
    id_cotizacion INT NULL,
    id_expediente INT NULL,
    id_cliente INT NULL,
    id_usuario_creador INT NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id_documento),
    CONSTRAINT uq_doc_nombre_almacenado UNIQUE (nombre_almacenado),
    CONSTRAINT fk_doc_cat_doc FOREIGN KEY (id_categoria_documento) REFERENCES catalogo_categoria_documento (id_categoria_documento) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_doc_cotizacion FOREIGN KEY (id_cotizacion) REFERENCES cotizacion (id_cotizacion) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_doc_expediente FOREIGN KEY (id_expediente) REFERENCES expediente (id_expediente) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_doc_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_doc_usuario FOREIGN KEY (id_usuario_creador) REFERENCES usuario (id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- 8. ÍNDICES COMPLEMENTARIOS
-- =============================================================================

CREATE INDEX idx_puerto_estado ON catalogo_puerto (estado);
CREATE INDEX idx_tipo_carga_estado ON catalogo_tipo_carga (estado);
CREATE INDEX idx_mercancia_estado ON catalogo_mercancia (estado);
CREATE INDEX idx_mercancia_categoria ON catalogo_mercancia (categoria);
CREATE INDEX idx_mercancia_id_cat ON catalogo_mercancia (id_categoria);
CREATE INDEX idx_banco_estado ON catalogo_banco (estado);
CREATE INDEX idx_cuenta_rec_estado ON catalogo_cuenta_recaudadora (estado, moneda);
CREATE INDEX idx_metodo_pago_estado ON catalogo_metodo_pago (estado);
CREATE INDEX idx_motivo_obs_modulo ON catalogo_motivo_observacion (modulo, estado);
CREATE INDEX idx_cat_doc_estado ON catalogo_categoria_documento (estado);
CREATE INDEX idx_provincia_dep ON catalogo_provincia (id_departamento);

CREATE INDEX idx_sede_estado ON sede (estado);
CREATE INDEX idx_usuario_rol ON usuario (rol);
CREATE INDEX idx_usuario_sede ON usuario (id_sede);
CREATE INDEX idx_usuario_estado ON usuario (estado_activo);

CREATE INDEX idx_cliente_estado ON cliente (estado_cliente);
CREATE INDEX idx_usrcli_empresa ON usuario_cliente (id_cliente);
CREATE INDEX idx_usrcli_estado ON usuario_cliente (estado_activo);

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

CREATE INDEX idx_expediente_cliente ON expediente (id_cliente);
CREATE INDEX idx_expediente_agente ON expediente (id_agente_aduanero);
CREATE INDEX idx_expediente_mercancia ON expediente (id_mercancia);
CREATE INDEX idx_expediente_estado ON expediente (estado_expediente);
CREATE INDEX idx_exp_canal_sunat ON expediente_canal_sunat (canal_sunat);

CREATE INDEX idx_almacen_estado ON control_almacen (estado_almacen);
CREATE INDEX idx_liquidacion_estado_pago ON liquidacion_financiera (estado_pago);
CREATE INDEX idx_liquidacion_liquidador ON liquidacion_financiera (id_usuario_liquidador);

CREATE INDEX idx_doc_entidad ON documento_adjunto (entidad_tipo, id_cotizacion, id_expediente, id_cliente);
CREATE INDEX idx_doc_categoria ON documento_adjunto (categoria);
CREATE INDEX idx_doc_activo ON documento_adjunto (activo);

CREATE INDEX idx_notifcli_cliente ON notificacion_cliente (id_cliente);
CREATE INDEX idx_notifcli_leido ON notificacion_cliente (id_cliente, leido);

-- =============================================================================
-- 9. AUDITORÍA DE EVENTOS TI Y SEGURIDAD
-- =============================================================================

CREATE TABLE auditoria_evento (
    id_evento BIGINT NOT NULL AUTO_INCREMENT,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actor VARCHAR(80) NOT NULL,
    rol_actor VARCHAR(50) NULL,
    modulo VARCHAR(50) NOT NULL,
    accion VARCHAR(60) NOT NULL,
    entidad VARCHAR(60) NULL,
    id_entidad VARCHAR(50) NULL,
    detalle VARCHAR(500) NULL,
    ip_origen VARCHAR(64) NULL,
    resultado VARCHAR(20) NOT NULL DEFAULT 'EXITO',
    PRIMARY KEY (id_evento)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_auditoria_fecha ON auditoria_evento (fecha);
CREATE INDEX idx_auditoria_actor ON auditoria_evento (usuario_actor);
CREATE INDEX idx_auditoria_modulo ON auditoria_evento (modulo);
CREATE INDEX idx_auditoria_resultado ON auditoria_evento (resultado);

SET FOREIGN_KEY_CHECKS = 1;
