-- =============================================================================
-- SISTEMA SGA - DATOS INICIALES SEMILLA (SEED DEFINITIVO)
-- =============================================================================
-- Base de Datos: SGAdb
-- Contraseña para usuarios semilla: Password123!
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

ALTER TABLE sede AUTO_INCREMENT = 1;
ALTER TABLE catalogo_puerto AUTO_INCREMENT = 1;
ALTER TABLE catalogo_tipo_carga AUTO_INCREMENT = 1;
ALTER TABLE catalogo_mercancia AUTO_INCREMENT = 1;
ALTER TABLE usuario AUTO_INCREMENT = 1;

-- =============================================================================
-- 1. SEDES DEL SISTEMA (CATÁLOGO MAESTRO DE SEDES Y SUCURSALES)
-- =============================================================================
INSERT INTO sede (id_sede, codigo_sede, nombre, tipo, direccion, distrito, telefono, estado) VALUES
(1, 'SED-CORP-01', 'Sede Central Corporativa', 'SEDE_CENTRAL', 'Av. José Pardo N° 835, Piso 11, Edificio Empresarial Pardo Central', 'Miraflores', '(01) 640-8200', 'OPERATIVO'),
(2, 'SED-VENT-01', 'Sucursal Comercial San Isidro', 'AGENCIA', 'Av. Canaval y Moreyra N° 452, Local Comercial 102', 'San Isidro', '(01) 640-8205', 'OPERATIVO'),
(3, 'SED-CALL-MAR', 'Sede Operativa Marítima y Patio TMS', 'TERMINAL', 'Av. Néstor Gambetta N° 4280, Km 7.5, Zona Industrial Mi Perú', 'Callao', '(01) 640-8210', 'OPERATIVO'),
(4, 'SED-CALL-AER', 'Sede Operativa Aérea', 'TERMINAL', 'Av. Elmer Faucett s/n, Centro Aéreo Comercial (CAC), Módulo E, Of. 204', 'Callao', '(01) 640-8220', 'OPERATIVO'),
(5, 'SED-PAIT-01', 'Sede Regional Norte Paita', 'SUCURSAL', 'Carretera Sullana - Paita Km 56.5, Mz. D, Lote 3, Zona Industrial Las Lomas', 'Paita', '(073) 53-1940', 'OPERATIVO'),
(6, 'SED-MATA-01', 'Sede Regional Sur Matarani', 'SUCURSAL', 'Av. Industrial N° 210, Zona de Extensión Portuaria Islay', 'Islay', '(054) 48-7130', 'OPERATIVO');

-- =============================================================================
-- 2. CATÁLOGO DE PUERTOS
-- =============================================================================
INSERT INTO catalogo_puerto (id_puerto, codigo_puerto, nombre, pais, tipo, estado) VALUES
(1, 'PECLL', 'Puerto del Callao', 'Perú', 'MARITIMO', 'ACTIVO'),
(2, 'PEPAI', 'Puerto de Paita', 'Perú', 'MARITIMO', 'ACTIVO'),
(3, 'PEMAT', 'Puerto de Matarani', 'Perú', 'MARITIMO', 'ACTIVO'),
(4, 'PEAQP', 'Aeropuerto Internacional Jorge Chávez', 'Perú', 'AEREO', 'ACTIVO');

-- =============================================================================
-- 3. CATÁLOGO DE TIPOS DE CARGA
-- =============================================================================
INSERT INTO catalogo_tipo_carga (id_tipo_carga, codigo, nombre, estado) VALUES
(1, 'CARGA_GENERAL', 'Carga General Suelta / Paletizada', 'ACTIVO'),
(2, 'CARGA_REFRIGERADA', 'Carga Perecible Refrigerada', 'ACTIVO'),
(3, 'CARGA_PELIGROSA_IMO', 'Carga Peligrosa Regulada IMO', 'ACTIVO'),
(4, 'CARGA_GRANEL', 'Carga a Granel Sólido / Líquido', 'ACTIVO');

-- =============================================================================
-- 4. CATÁLOGO DE MERCANCÍAS Y SUBPARTIDAS ARANCELARIAS (NANDINA)
-- =============================================================================
INSERT INTO catalogo_mercancia (id_mercancia, codigo, nombre, categoria, requiere_permiso, entidad_reguladora, arancel_base_pct, estado) VALUES
(1, '8517.13.00.00', 'Smartphone / Teléfono inteligente', 'Tecnología', TRUE, 'MTC', 0.00, 'ACTIVO'),
(2, '8471.30.00.00', 'Computadora portátil / Laptop', 'Tecnología', FALSE, NULL, 0.00, 'ACTIVO'),
(3, '8471.30.00.10', 'Tablet con conectividad móvil', 'Tecnología', TRUE, 'MTC', 0.00, 'ACTIVO'),
(4, '8517.62.20.00', 'Reloj inteligente / Smartwatch', 'Tecnología', TRUE, 'MTC', 0.00, 'ACTIVO'),
(5, '8528.72.00.00', 'Televisor Smart TV', 'Tecnología', FALSE, NULL, 0.00, 'ACTIVO'),
(6, '9504.50.00.00', 'Consola de videojuegos', 'Entretenimiento', FALSE, NULL, 0.00, 'ACTIVO'),
(7, '8703.22.90.00', 'Automóvil nuevo a gasolina (1000 a 1500 cc)', 'Vehículos', FALSE, NULL, 6.00, 'ACTIVO'),
(8, '8703.23.90.10', 'Automóvil nuevo a gasolina (1500 a 3000 cc)', 'Vehículos', FALSE, NULL, 6.00, 'ACTIVO'),
(9, '8703.80.00.00', 'Automóvil nuevo 100% eléctrico', 'Vehículos', FALSE, NULL, 0.00, 'ACTIVO'),
(10, '8703.40.00.00', 'Automóvil nuevo híbrido gasolina-eléctrico', 'Vehículos', FALSE, NULL, 6.00, 'ACTIVO'),
(11, '8703.23.90.90', 'Automóvil usado a gasolina (hasta 2 años)', 'Vehículos', TRUE, 'MTC / CITV', 6.00, 'ACTIVO'),
(12, '8711.20.00.00', 'Motocicleta nueva a gasolina (50 a 250 cc)', 'Vehículos', FALSE, NULL, 6.00, 'ACTIVO'),
(13, '8704.31.10.10', 'Camioneta Pick-up nueva (bien de capital)', 'Vehículos', FALSE, NULL, 6.00, 'ACTIVO'),
(14, '8712.00.00.00', 'Bicicleta convencional sin motor', 'Transporte', FALSE, NULL, 11.00, 'ACTIVO'),
(15, '9503.00.99.00', 'Juguetes diversos de plástico y muñecos', 'Juguetería', TRUE, 'DIGESA', 6.00, 'ACTIVO'),
(16, '9504.90.90.00', 'Juegos de mesa y cartas', 'Juguetería', FALSE, NULL, 0.00, 'ACTIVO'),
(17, '6404.11.00.00', 'Calzado deportivo / Zapatillas', 'Calzado', FALSE, NULL, 11.00, 'ACTIVO'),
(18, '6403.59.00.00', 'Zapatos con suela y capellada de cuero', 'Calzado', FALSE, NULL, 11.00, 'ACTIVO'),
(19, '6109.10.00.30', 'Camisetas y polos de algodón', 'Confecciones', FALSE, NULL, 11.00, 'ACTIVO'),
(20, '6203.42.00.00', 'Pantalones y jeans de algodón', 'Confecciones', FALSE, NULL, 11.00, 'ACTIVO'),
(21, '3004.90.29.00', 'Medicamentos para uso humano dosificados', 'Farmacia', TRUE, 'DIGEMID', 0.00, 'ACTIVO'),
(22, '3304.99.00.00', 'Productos cosméticos y cuidado de la piel', 'Cosmética', TRUE, 'DIGEMID', 6.00, 'ACTIVO'),
(23, '2106.90.79.00', 'Suplementos alimenticios y vitaminas', 'Nutrición', TRUE, 'DIGESA', 6.00, 'ACTIVO'),
(24, '2208.30.00.00', 'Whisky y licores destilados', 'Licores', TRUE, 'DIGESA', 6.00, 'ACTIVO'),
(25, '2203.00.00.00', 'Cerveza de malta', 'Bebidas', TRUE, 'DIGESA', 6.00, 'ACTIVO'),
(26, '2402.20.20.00', 'Cigarrillos de tabaco rubio', 'Tabaco', TRUE, 'MINSA / DIGESA', 6.00, 'ACTIVO'),
(27, '1905.31.00.00', 'Galletas dulces y snacks procesados', 'Alimentos', TRUE, 'DIGESA', 6.00, 'ACTIVO'),
(28, '0808.10.00.00', 'Manzanas frescas', 'Agropecuario', TRUE, 'SENASA', 6.00, 'ACTIVO'),
(29, '8418.10.19.00', 'Refrigerador doméstico combinado', 'Electrohogar', FALSE, NULL, 0.00, 'ACTIVO'),
(30, '8450.11.00.00', 'Lavadora automática de ropa', 'Electrohogar', FALSE, NULL, 0.00, 'ACTIVO'),
(31, '9403.60.00.00', 'Muebles de madera para el hogar', 'Hogar', FALSE, NULL, 11.00, 'ACTIVO'),
(32, '4011.10.00.00', 'Neumáticos nuevos para automóviles', 'Autopartes', FALSE, NULL, 6.00, 'ACTIVO'),
(33, '9302.00.00.00', 'Armas de fuego de uso civil (pistolas/revólveres)', 'Seguridad', TRUE, 'SUCAMEC', 6.00, 'ACTIVO');

-- =============================================================================
-- 5. USUARIOS DEL SISTEMA (ROLES OPERATIVOS SGA)
-- =============================================================================
-- Contraseña para todos los usuarios: Password123!
-- Hash BCrypt: $2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee
INSERT INTO usuario (
    id_usuario, rol, id_sede, username, password_hash, nombres, apellidos, correo_institucional,
    tipo_documento, numero_documento, telefono, cargo_titulo, codigo_sunat, ultimo_acceso, estado_activo
) VALUES
(1, 'ADMINISTRADOR_TI', 1, 'admin', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Administrador', 'Sistema TI', 'admin@transaduanas.pe', 'DNI', '00000001', '+51 999 000 001', 'Administrador de TI', 'TI-001', NOW(), TRUE),
(2, 'GERENTE_GENERAL', 1, 'gerente', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Carlos', 'Mendoza Valdivia', 'gerencia@transaduanas.pe', 'DNI', '00000002', '+51 999 000 002', 'Gerente General', 'GG-001', NOW(), TRUE),
(3, 'AGENTE_COMERCIAL', 2, 'comercial', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Sofía', 'Paredes Romero', 'ventas.sanisidro@transaduanas.pe', 'DNI', '00000003', '+51 999 000 003', 'Agente Comercial', 'COM-001', NOW(), TRUE),
(4, 'SUPERVISOR_ADUANERO', 3, 'supervisor', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Martín', 'Sotomayor Vega', 'operaciones.maritimo@transaduanas.pe', 'DNI', '00000004', '+51 999 000 004', 'Supervisor Aduanero', 'SUP-001', NOW(), TRUE),
(5, 'AGENTE_ADUANERO', 4, 'agente', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Jorge', 'Herrera Díaz', 'operaciones.aereo@transaduanas.pe', 'DNI', '00000005', '+51 999 000 005', 'Agente Aduanero', 'AGE-001', NOW(), TRUE),
(6, 'SUPERVISOR_TRANSPORTE', 3, 'transporte', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Raúl', 'Cárdenas Meléndez', 'transporte@transaduanas.pe', 'DNI', '00000006', '+51 999 000 006', 'Supervisor de Transporte', 'TRA-001', NOW(), TRUE),
(7, 'CONTADOR', 1, 'contador', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Elena', 'Morales Benítez', 'finanzas@transaduanas.pe', 'DNI', '00000007', '+51 999 000 007', 'Contador', 'CON-001', NOW(), TRUE),
(8, 'ASESOR_LEGAL', 1, 'legal', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Fernando', 'Arana Silva', 'legal@transaduanas.pe', 'DNI', '00000008', '+51 999 000 008', 'Asesor Legal', 'LEG-001', NOW(), TRUE);

SET FOREIGN_KEY_CHECKS = 1;