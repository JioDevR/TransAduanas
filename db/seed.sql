-- =============================================================================
-- SISTEMA SGA - DATOS INICIALES MÍNIMOS (SOLO SISTEMA Y ADMIN)
-- =============================================================================
-- Base de Datos: SGAdb
-- Contraseña del usuario administrador: Password123!
-- Hash BCrypt: $2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee
-- =============================================================================

USE SGAdb;

SET FOREIGN_KEY_CHECKS = 0;

-- Limpieza previa en orden inverso de dependencias para asegurar idempotencia
DELETE FROM liquidacion_financiera;
DELETE FROM despacho_transporte;
DELETE FROM control_almacen;
DELETE FROM documento_adjunto;
DELETE FROM expediente;
DELETE FROM cotizacion_pago;
DELETE FROM cotizacion;
DELETE FROM cliente;
DELETE FROM usuario;
DELETE FROM conductor;
DELETE FROM unidad_transporte;
DELETE FROM catalogo_mercancia;
DELETE FROM catalogo_tipo_carga;
DELETE FROM catalogo_puerto;
DELETE FROM sede;
DELETE FROM rol;

-- =============================================================================
-- 1. ROLES DEL SISTEMA (SEGURIDAD Y CONTROL RBAC)
-- =============================================================================
INSERT INTO rol (id_rol, nombre_rol, descripcion, estado_activo) VALUES
(1, 'ADMINISTRADOR_TI', 'Administrador de TI', TRUE),
(2, 'GERENTE_GENERAL', 'Gerente General', TRUE),
(3, 'AGENTE_COMERCIAL', 'Agente Comercial', TRUE),
(4, 'SUPERVISOR_ADUANERO', 'Supervisor Aduanero', TRUE),
(5, 'AGENTE_ADUANERO', 'Agente Aduanero', TRUE),
(6, 'SUPERVISOR_TRANSPORTE', 'Supervisor de Transporte', TRUE),
(7, 'CONTADOR', 'Contador', TRUE),
(8, 'ASESOR_LEGAL', 'Asesor Legal', TRUE);

-- =============================================================================
-- 2. SEDE ÚNICA INICIAL DE PRUEBA
-- =============================================================================
INSERT INTO sede (id_sede, codigo_sede, nombre, tipo, direccion, distrito, telefono, estado) VALUES
(1, 'SED-PRUEBA', 'Prueba', 'SEDE_CENTRAL', 'Dirección de Prueba', 'Callao', '+51 (1) 000-0000', 'OPERATIVO');

-- =============================================================================
-- 3. CATÁLOGO DE PUERTOS: PUERTO ÚNICO DE PRUEBA
-- =============================================================================
INSERT INTO catalogo_puerto (id_puerto, codigo_puerto, nombre, pais, tipo, estado) VALUES
(1, 'PRUEBA', 'Prueba', 'Perú', 'MARITIMO', 'ACTIVO');

-- =============================================================================
-- 4. CATÁLOGO DE TIPOS DE CARGA: TIPO ÚNICO DE PRUEBA
-- =============================================================================
INSERT INTO catalogo_tipo_carga (id_tipo_carga, codigo, nombre, estado) VALUES
(1, 'PRUEBA', 'Prueba', 'ACTIVO');

-- =============================================================================
-- 5. USUARIO ÚNICO: ADMINISTRADOR TI
-- =============================================================================
INSERT INTO usuario (
    id_usuario,
    id_rol,
    id_sede,
    username,
    password_hash,
    nombres,
    apellidos,
    correo_institucional,
    tipo_documento,
    numero_documento,
    telefono,
    cargo_titulo,
    codigo_sunat,
    ultimo_acceso,
    estado_activo
) VALUES (
    1,
    1,
    1,
    'admin',
    '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee',
    'Administrador',
    'Sistema',
    'admin@sga.pe',
    'DNI',
    '00000000',
    '+51 999 999 999',
    'Administrador de TI',
    'ADM-001',
    NOW(),
    TRUE
);

SET FOREIGN_KEY_CHECKS = 1;