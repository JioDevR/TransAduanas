-- =============================================================================
-- SISTEMA SGA - DATOS INICIALES SEMILLA (SEED DEFINITIVO)
-- =============================================================================
-- Base de Datos: SGAdb
-- Contraseña para usuarios semilla: Password123!
-- Hash BCrypt: $2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee
-- =============================================================================

USE SGAdb;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;

-- Limpieza previa en orden inverso de dependencias para asegurar idempotencia
DELETE FROM liquidacion_financiera;
DELETE FROM despacho_transporte;
DELETE FROM control_almacen;
DELETE FROM documento_adjunto;
DELETE FROM expediente;
DELETE FROM cotizacion_pago;
DELETE FROM cotizacion;
DELETE FROM usuario_cliente;
DELETE FROM cliente;
DELETE FROM usuario WHERE rol != 'ADMINISTRADOR_TI' AND username != 'admin';
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
ALTER TABLE usuario AUTO_INCREMENT = 2;
ALTER TABLE usuario_cliente AUTO_INCREMENT = 1;
ALTER TABLE cliente AUTO_INCREMENT = 1;

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
-- 2. CATÁLOGO DE PUERTOS (CATÁLOGO MAESTRO DE PUERTOS COMERCIALES Y TERMINALES)
-- =============================================================================
INSERT INTO catalogo_puerto (id_puerto, codigo_puerto, nombre, pais, tipo, estado) VALUES

    -- Puertos Nacionales (Perú)
    (1, 'PECAL-DPW', 'Callao - Muelle Sur (DP World)', 'Perú', 'MARITIMO', 'ACTIVO'),
    (2, 'PECAL-APM', 'Callao - Muelle Norte (APM Terminals)', 'Perú', 'MARITIMO', 'ACTIVO'),
    (3, 'PECHY', 'Megapuerto de Chancay', 'Perú', 'MARITIMO', 'ACTIVO'),
    (4, 'PEPAI', 'Puerto de Paita', 'Perú', 'MARITIMO', 'ACTIVO'),
    (5, 'PEMAT', 'Puerto de Matarani', 'Perú', 'MARITIMO', 'ACTIVO'),
    (6, 'PESAL', 'Puerto de Salaverry', 'Perú', 'MARITIMO', 'ACTIVO'),
    (7, 'PEPIS', 'Puerto General San Martín (Pisco)', 'Perú', 'MARITIMO', 'ACTIVO'),
    (8, 'PEILO', 'Puerto de Ilo', 'Perú', 'MARITIMO', 'ACTIVO'),
    (9, 'PECHM', 'Puerto de Chimbote', 'Perú', 'MARITIMO', 'ACTIVO'),
    (10, 'PEYUR', 'Puerto Fluvial de Yurimaguas', 'Perú', 'FLUVIAL', 'ACTIVO'),
    (11, 'PEAQP', 'Aeropuerto Internacional Jorge Chávez', 'Perú', 'AEREO', 'ACTIVO'),

    -- Puertos Internacionales - Asia
    (12, 'CNSHA', 'Puerto de Shanghái', 'China', 'MARITIMO', 'ACTIVO'),
    (13, 'CNNGB', 'Puerto de Ningbo-Zhoushan', 'China', 'MARITIMO', 'ACTIVO'),
    (14, 'CNSZX', 'Puerto de Shenzhen (Yantian/Shekou)', 'China', 'MARITIMO', 'ACTIVO'),
    (15, 'CNTAO', 'Puerto de Qingdao', 'China', 'MARITIMO', 'ACTIVO'),
    (16, 'KRPUS', 'Puerto de Busan', 'Corea del Sur', 'MARITIMO', 'ACTIVO'),
    (17, 'JPYOK', 'Puerto de Yokohama', 'Japón', 'MARITIMO', 'ACTIVO'),
    (18, 'SGSIN', 'Puerto de Singapur', 'Singapur', 'MARITIMO', 'ACTIVO'),
    (19, 'HKHKG', 'Puerto de Hong Kong', 'Hong Kong', 'MARITIMO', 'ACTIVO'),

    -- Puertos Internacionales - Norteamérica
    (20, 'USLAX', 'Puerto de Los Ángeles', 'Estados Unidos', 'MARITIMO', 'ACTIVO'),
    (21, 'USLGB', 'Puerto de Long Beach', 'Estados Unidos', 'MARITIMO', 'ACTIVO'),
    (22, 'USPHL', 'Puerto de Filadelfia (Delaware River)', 'Estados Unidos', 'MARITIMO', 'ACTIVO'),
    (23, 'USHOU', 'Puerto de Houston', 'Estados Unidos', 'MARITIMO', 'ACTIVO'),
    (24, 'USMIA', 'Puerto de Miami', 'Estados Unidos', 'MARITIMO', 'ACTIVO'),
    (25, 'MXZLO', 'Puerto de Manzanillo', 'México', 'MARITIMO', 'ACTIVO'),
    (26, 'CAVAN', 'Puerto de Vancouver', 'Canadá', 'MARITIMO', 'ACTIVO'),

    -- Puertos Internacionales - Europa
    (27, 'NLRTM', 'Puerto de Róterdam', 'Países Bajos', 'MARITIMO', 'ACTIVO'),
    (28, 'BEANR', 'Puerto de Amberes-Brujas', 'Bélgica', 'MARITIMO', 'ACTIVO'),
    (29, 'DEHAM', 'Puerto de Hamburgo', 'Alemania', 'MARITIMO', 'ACTIVO'),
    (30, 'ESALG', 'Puerto de Algeciras', 'España', 'MARITIMO', 'ACTIVO'),
    (31, 'ESVLC', 'Puerto de Valencia', 'España', 'MARITIMO', 'ACTIVO'),
    (32, 'GBFXT', 'Puerto de Felixstowe', 'Reino Unido', 'MARITIMO', 'ACTIVO'),

    -- Puertos Internacionales - Sudamérica y Centroamérica
    (33, 'PABLB', 'Puerto de Balboa', 'Panamá', 'MARITIMO', 'ACTIVO'),
    (34, 'PAMIT', 'Manzanillo International Terminal', 'Panamá', 'MARITIMO', 'ACTIVO'),
    (35, 'COBUN', 'Puerto de Buenaventura', 'Colombia', 'MARITIMO', 'ACTIVO'),
    (36, 'ECGYE', 'Puerto de Guayaquil (Contecon)', 'Ecuador', 'MARITIMO', 'ACTIVO'),
    (37, 'CLSAI', 'Puerto de San Antonio', 'Chile', 'MARITIMO', 'ACTIVO'),
    (38, 'CLVAP', 'Puerto de Valparaíso', 'Chile', 'MARITIMO', 'ACTIVO'),
    (39, 'BRSSZ', 'Puerto de Santos', 'Brasil', 'MARITIMO', 'ACTIVO'),

    -- Aeropuertos Internacionales - Asia
    (40, 'CNPVG', 'Aeropuerto Internacional de Shanghái-Pudong', 'China', 'AEREO', 'ACTIVO'),
    (41, 'CNNGB-AIR', 'Aeropuerto Internacional de Ningbo-Lishe', 'China', 'AEREO', 'ACTIVO'),
    (42, 'CNSZX-AIR', 'Aeropuerto Internacional de Shenzhen-Bao''an', 'China', 'AEREO', 'ACTIVO'),
    (43, 'CNTAO-AIR', 'Aeropuerto Internacional de Qingdao-Jiaodong', 'China', 'AEREO', 'ACTIVO'),
    (44, 'KRICN', 'Aeropuerto Internacional de Seúl-Incheon', 'Corea del Sur', 'AEREO', 'ACTIVO'),
    (45, 'KRPUS-AIR', 'Aeropuerto Internacional de Gimhae (Busan)', 'Corea del Sur', 'AEREO', 'ACTIVO'),
    (46, 'JPNRT', 'Aeropuerto Internacional de Tokio-Narita', 'Japón', 'AEREO', 'ACTIVO'),
    (47, 'JPHND', 'Aeropuerto Internacional de Tokio-Haneda', 'Japón', 'AEREO', 'ACTIVO'),
    (48, 'JPKIX', 'Aeropuerto Internacional de Kansai (Osaka)', 'Japón', 'AEREO', 'ACTIVO'),
    (49, 'SGSIN-AIR', 'Aeropuerto Internacional de Singapur-Changi', 'Singapur', 'AEREO', 'ACTIVO'),
    (50, 'HKHKG-AIR', 'Aeropuerto Internacional de Hong Kong (Chek Lap Kok)', 'Hong Kong', 'AEREO', 'ACTIVO'),

    -- Aeropuertos Internacionales - Norteamérica
    (51, 'USLAX-AIR', 'Aeropuerto Internacional de Los Ángeles (LAX)', 'Estados Unidos', 'AEREO', 'ACTIVO'),
    (52, 'USLGB-AIR', 'Aeropuerto de Long Beach', 'Estados Unidos', 'AEREO', 'ACTIVO'),
    (53, 'USPHL-AIR', 'Aeropuerto Internacional de Filadelfia', 'Estados Unidos', 'AEREO', 'ACTIVO'),
    (54, 'USIAH', 'Aeropuerto Intercontinental George Bush (Houston)', 'Estados Unidos', 'AEREO', 'ACTIVO'),
    (55, 'USMIA-AIR', 'Aeropuerto Internacional de Miami (MIA)', 'Estados Unidos', 'AEREO', 'ACTIVO'),
    (56, 'USORD', 'Aeropuerto Internacional O''Hare (Chicago)', 'Estados Unidos', 'AEREO', 'ACTIVO'),
    (57, 'USJFK', 'Aeropuerto Internacional John F. Kennedy (Nueva York)', 'Estados Unidos', 'AEREO', 'ACTIVO'),
    (58, 'MXZLO-AIR', 'Aeropuerto Internacional Playa de Oro (Manzanillo)', 'México', 'AEREO', 'ACTIVO'),
    (59, 'MXMEX', 'Aeropuerto Internacional de la Ciudad de México (AICM)', 'México', 'AEREO', 'ACTIVO'),
    (60, 'CAYVR', 'Aeropuerto Internacional de Vancouver', 'Canadá', 'AEREO', 'ACTIVO'),

    -- Aeropuertos Internacionales - Europa
    (61, 'NLAMS', 'Aeropuerto de Ámsterdam-Schiphol', 'Países Bajos', 'AEREO', 'ACTIVO'),
    (62, 'NLRTM-AIR', 'Aeropuerto de Róterdam-La Haya', 'Países Bajos', 'AEREO', 'ACTIVO'),
    (63, 'BEBRU', 'Aeropuerto de Bruselas-Zaventem', 'Bélgica', 'AEREO', 'ACTIVO'),
    (64, 'DEFRA', 'Aeropuerto de Fráncfort del Meno', 'Alemania', 'AEREO', 'ACTIVO'),
    (65, 'DEHAM-AIR', 'Aeropuerto de Hamburgo', 'Alemania', 'AEREO', 'ACTIVO'),
    (66, 'ESMAD', 'Aeropuerto Adolfo Suárez Madrid-Barajas', 'España', 'AEREO', 'ACTIVO'),
    (67, 'ESBCN', 'Aeropuerto Josep Tarradellas Barcelona-El Prat', 'España', 'AEREO', 'ACTIVO'),
    (68, 'ESVLC-AIR', 'Aeropuerto de Valencia-Manises', 'España', 'AEREO', 'ACTIVO'),
    (69, 'ESAGP', 'Aeropuerto de Málaga-Costa del Sol', 'España', 'AEREO', 'ACTIVO'),
    (70, 'GBLHR', 'Aeropuerto de Londres-Heathrow', 'Reino Unido', 'AEREO', 'ACTIVO'),
    (71, 'GBSTN', 'Aeropuerto de Londres-Stansted', 'Reino Unido', 'AEREO', 'ACTIVO'),

    -- Aeropuertos Internacionales - Sudamérica y Centroamérica
    (72, 'PAPTY', 'Aeropuerto Internacional de Tocumen (Ciudad de Panamá)', 'Panamá', 'AEREO', 'ACTIVO'),
    (73, 'PAONX', 'Aeropuerto Internacional Enrique Adolfo Jiménez (Colón)', 'Panamá', 'AEREO', 'ACTIVO'),
    (74, 'COBOG', 'Aeropuerto Internacional El Dorado (Bogotá)', 'Colombia', 'AEREO', 'ACTIVO'),
    (75, 'COCLO', 'Aeropuerto Internacional Alfonso Bonilla Aragón (Cali)', 'Colombia', 'AEREO', 'ACTIVO'),
    (76, 'ECGYE-AIR', 'Aeropuerto Internacional José Joaquín de Olmedo (Guayaquil)', 'Ecuador', 'AEREO', 'ACTIVO'),
    (77, 'ECUIO', 'Aeropuerto Internacional Mariscal Sucre (Quito)', 'Ecuador', 'AEREO', 'ACTIVO'),
    (78, 'CLSCL', 'Aeropuerto Internacional Arturo Merino Benítez (Santiago)', 'Chile', 'AEREO', 'ACTIVO'),
    (79, 'BRGRU', 'Aeropuerto Internacional de São Paulo-Guarulhos', 'Brasil', 'AEREO', 'ACTIVO'),
    (80, 'BRVCP', 'Aeropuerto Internacional de Viracopos-Campinas', 'Brasil', 'AEREO', 'ACTIVO');

-- =============================================================================
-- 3. CATÁLOGO DE TIPOS DE CARGA
-- =============================================================================
INSERT INTO catalogo_tipo_carga (id_tipo_carga, codigo, nombre, estado) VALUES

    -- 1. Carga en Contenedores (La más frecuente en DP World / APM Terminals)
    (1, 'CARGA_FCL', 'Carga Contenedorizada Completa (FCL / Dry & HC)', 'ACTIVO'),
    (2, 'CARGA_LCL', 'Carga Consolidada / Carga Suelta en Contenedor (LCL)', 'ACTIVO'),

    -- 2. Carga con Temperatura Controlada (Agroexportación / Farmacéutica)
    (3, 'CARGA_REEFER', 'Carga Perecible Refrigerada (Contenedor Reefer)', 'ACTIVO'),

    -- 3. Vehículos y Carga sobre Ruedas (Autos, camiones, maquinaria)
    (4, 'CARGA_RO_RO', 'Carga Rodante / Vehicular (Ro-Ro - Autos y Maquinaria)', 'ACTIVO'),

    -- 4. Grandes Volúmenes y Proyectos Mineros / Industriales
    (5, 'CARGA_PROYECTO', 'Carga Proyecto / Sobredimensionada (Breakbulk / OOG)', 'ACTIVO'),
    (6, 'CARGA_GRANEL_SOLIDO', 'Carga a Granel Solido (Minerales, Granos, Fertilizantes)', 'ACTIVO'),
    (7, 'CARGA_GRANEL_LIQUIDO', 'Carga a Granel Liquido (Quimicos, Aceites, Combustibles)', 'ACTIVO'),

    -- 5. Cargas Reguladas y de Alta Seguridad
    (8, 'CARGA_PELIGROSA_IMO', 'Carga Peligrosa Clasificada IMO (Materiales Peligrosos)', 'ACTIVO'),
    (9, 'CARGA_VALORADA', 'Carga Valiosa / Alta Tecnologia (Requiere Custodia / Escolta)', 'ACTIVO');

-- =============================================================================
-- 4. CATÁLOGO DE MERCANCÍAS Y SUBPARTIDAS ARANCELARIAS (NANDINA)
-- =============================================================================
INSERT INTO catalogo_mercancia (id_mercancia, codigo, nombre, categoria, requiere_permiso, entidad_reguladora, arancel_base_pct, estado) VALUES
    (1, '8517.13.00.00', 'Smartphone / Telefono inteligente', 'Tecnologia', TRUE, 'MTC', 0.00, 'ACTIVO'),
    (2, '8471.30.00.00', 'Computadora portatil / Laptop', 'Tecnologia', FALSE, NULL, 0.00, 'ACTIVO'),
    (3, '8471.30.00.10', 'Tablet con conectividad movil', 'Tecnologia', TRUE, 'MTC', 0.00, 'ACTIVO'),
    (4, '8517.62.20.00', 'Reloj inteligente / Smartwatch', 'Tecnologia', TRUE, 'MTC', 0.00, 'ACTIVO'),
    (5, '8528.72.00.00', 'Televisor Smart TV', 'Tecnologia', FALSE, NULL, 0.00, 'ACTIVO'),
    (6, '9504.50.00.00', 'Consola de videojuegos', 'Entretenimiento', FALSE, NULL, 0.00, 'ACTIVO'),
    (7, '8703.22.90.00', 'Automovil nuevo a gasolina (1000 a 1500 cc)', 'Vehiculos', FALSE, NULL, 6.00, 'ACTIVO'),
    (8, '8703.23.90.10', 'Automovil nuevo a gasolina (1500 a 3000 cc)', 'Vehiculos', FALSE, NULL, 6.00, 'ACTIVO'),
    (9, '8703.80.00.00', 'Automovil nuevo 100% electrico', 'Vehiculos', FALSE, NULL, 0.00, 'ACTIVO'),
    (10, '8703.40.00.00', 'Automovil nuevo hibrido gasolina-electrico', 'Vehiculos', FALSE, NULL, 6.00, 'ACTIVO'),
    (11, '8703.23.90.90', 'Automovil usado a gasolina (hasta 2 anos)', 'Vehiculos', TRUE, 'MTC / CITV', 6.00, 'ACTIVO'),
    (12, '8711.20.00.00', 'Motocicleta nueva a gasolina (50 a 250 cc)', 'Vehiculos', FALSE, NULL, 6.00, 'ACTIVO'),
    (13, '8704.31.10.10', 'Camioneta Pick-up nueva (bien de capital)', 'Vehiculos', FALSE, NULL, 6.00, 'ACTIVO'),
    (14, '8712.00.00.00', 'Bicicleta convencional sin motor', 'Transporte', FALSE, NULL, 11.00, 'ACTIVO'),
    (15, '9503.00.99.00', 'Juguetes diversos de plastico y munecos', 'Jugueteria', TRUE, 'DIGESA', 6.00, 'ACTIVO'),
    (16, '9504.90.90.00', 'Juegos de mesa y cartas', 'Jugueteria', FALSE, NULL, 0.00, 'ACTIVO'),
    (17, '6404.11.00.00', 'Calzado deportivo / Zapatillas', 'Calzado', FALSE, NULL, 11.00, 'ACTIVO'),
    (18, '6403.59.00.00', 'Zapatos con suela y capellada de cuero', 'Calzado', FALSE, NULL, 11.00, 'ACTIVO'),
    (19, '6109.10.00.30', 'Camisetas y polos de algodon', 'Confecciones', FALSE, NULL, 11.00, 'ACTIVO'),
    (20, '6203.42.00.00', 'Pantalones y jeans de algodon', 'Confecciones', FALSE, NULL, 11.00, 'ACTIVO'),
    (21, '3004.90.29.00', 'Medicamentos para uso humano dosificados', 'Farmacia', TRUE, 'DIGEMID', 0.00, 'ACTIVO'),
    (22, '3304.99.00.00', 'Productos cosmeticos y cuidado de la piel', 'Cosmetica', TRUE, 'DIGEMID', 6.00, 'ACTIVO'),
    (23, '2106.90.79.00', 'Suplementos alimenticios y vitaminas', 'Nutricion', TRUE, 'DIGESA', 6.00, 'ACTIVO'),
    (24, '2208.30.00.00', 'Whisky y licores destilados', 'Licores', TRUE, 'DIGESA', 6.00, 'ACTIVO'),
    (25, '2203.00.00.00', 'Cerveza de malta', 'Bebidas', TRUE, 'DIGESA', 6.00, 'ACTIVO'),
    (26, '2402.20.20.00', 'Cigarrillos de tabaco rubio', 'Tabaco', TRUE, 'MINSA / DIGESA', 6.00, 'ACTIVO'),
    (27, '1905.31.00.00', 'Galletas dulces y snacks procesados', 'Alimentos', TRUE, 'DIGESA', 6.00, 'ACTIVO'),
    (28, '0808.10.00.00', 'Manzanas frescas', 'Agropecuario', TRUE, 'SENASA', 6.00, 'ACTIVO'),
    (29, '8418.10.19.00', 'Refrigerador domestico combinado', 'Electrohogar', FALSE, NULL, 0.00, 'ACTIVO'),
    (30, '8450.11.00.00', 'Lavadora automatica de ropa', 'Electrohogar', FALSE, NULL, 0.00, 'ACTIVO'),
    (31, '9403.60.00.00', 'Muebles de madera para el hogar', 'Hogar', FALSE, NULL, 11.00, 'ACTIVO'),
    (32, '4011.10.00.00', 'Neumaticos nuevos para automoviles', 'Autopartes', FALSE, NULL, 6.00, 'ACTIVO'),
    (33, '9302.00.00.00', 'Armas de fuego de uso civil (pistolas/revolveres)', 'Seguridad', TRUE, 'SUCAMEC', 6.00, 'ACTIVO'),
    (34, '0810.40.00.00', 'Arandanos frescos (Cranberries / Blueberries)', 'Agroexportacion', TRUE, 'SENASA', 0.00, 'ACTIVO'),
    (35, '0806.10.00.00', 'Uvas frescas de mesa (Red Globe / Crimson)', 'Agroexportacion', TRUE, 'SENASA', 0.00, 'ACTIVO'),
    (36, '0804.40.00.00', 'Paltas / Aguacates frescos (Variedad Hass)', 'Agroexportacion', TRUE, 'SENASA', 0.00, 'ACTIVO'),
    (37, '0709.20.00.00', 'Esparragos frescos o refrigerados', 'Agroexportacion', TRUE, 'SENASA', 0.00, 'ACTIVO'),
    (38, '0901.11.90.00', 'Cafe en grano verde sin tostar ni descafeinar', 'Agroexportacion', TRUE, 'SENASA', 0.00, 'ACTIVO'),
    (39, '0307.43.00.00', 'Calamar gigante / Pota congelada entera o anillas', 'Pesca', TRUE, 'SANIPES', 0.00, 'ACTIVO'),
    (40, '0306.17.90.00', 'Langostinos y camarones congelados', 'Pesca', TRUE, 'SANIPES', 0.00, 'ACTIVO'),
    (41, '1604.14.10.00', 'Conservas de atun en lomos o trozos en aceite', 'Alimentos', TRUE, 'SANIPES', 6.00, 'ACTIVO'),
    (42, '7318.15.90.00', 'Pernos, tornillos y tuercas de acero galvanizado', 'Ferreteria', FALSE, NULL, 0.00, 'ACTIVO'),
    (43, '6804.22.00.00', 'Discos de corte abrasivos para amoladoras', 'Ferreteria', FALSE, NULL, 6.00, 'ACTIVO'),
    (44, '8204.11.00.00', 'Juegos de llaves de mano combinadas y dados', 'Ferreteria', FALSE, NULL, 0.00, 'ACTIVO'),
    (45, '8716.80.10.00', 'Carretillas manuales de construccion y carga', 'Ferreteria', FALSE, NULL, 11.00, 'ACTIVO'),
    (46, '8429.52.00.00', 'Excavadoras mecanicas de pala frontal y oruga', 'Maquinaria', FALSE, NULL, 0.00, 'ACTIVO'),
    (47, '8474.20.00.00', 'Maquinas chancadoras y molinos para minerales', 'Maquinaria', FALSE, NULL, 0.00, 'ACTIVO'),
    (48, '8502.13.10.00', 'Grupos electrogenos diesel de gran potencia', 'Equipamiento', FALSE, NULL, 0.00, 'ACTIVO'),
    (49, '2807.00.10.00', 'Acido sulfurico industrial (Sustancia Fiscalizada)', 'Quimicos / IMO', TRUE, 'SUCAMEC / SUNAT IQBF', 0.00, 'ACTIVO'),
    (50, '2903.71.00.00', 'Gases refrigerantes clorofluorados (Cuidado Ozono)', 'Quimicos', TRUE, 'PRODUCE', 0.00, 'ACTIVO'),
    (51, '9506.32.00.00', 'Madera para construccion / Vigas laminadas', 'Madera', FALSE, NULL, 0.00, 'ACTIVO'),
    (52, '9403.80.00.00', 'Muebles de madera y ratan para exteriores', 'Hogar', FALSE, NULL, 0.00, 'ACTIVO');

-- =============================================================================
-- 5. USUARIOS DEL SISTEMA (ROLES OPERATIVOS SGA)
-- =============================================================================
-- Nota: El usuario 'admin' (ADMINISTRADOR_TI) no se incluye en esta semilla ya que es
-- aprovisionado automáticamente por el sistema al iniciar (DataInitializer).
-- Contraseña para todos los usuarios semilla: Password123!
-- Hash BCrypt: $2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee
INSERT INTO usuario (id_usuario, rol, id_sede, username, password_hash, nombres, apellidos, correo_institucional, tipo_documento, numero_documento, telefono, cargo_titulo, codigo_sunat, ultimo_acceso, estado_activo) VALUES
    (2, 'GERENTE_GENERAL', 1, 'gerente', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Carlos', 'Mendoza Valdivia', 'gerencia@transaduanas.pe', 'DNI', '00000002', '+51 999 000 002', 'Gerente General', 'GG-001', NOW(), TRUE),
    (3, 'ASESOR_LEGAL', 1, 'legal', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Fernando', 'Arana Silva', 'legal@transaduanas.pe', 'DNI', '10234567', '+51 987 112 001', 'Asesor Legal', 'LEG-001', NOW(), TRUE),
    (4, 'ASESOR_LEGAL', 2, 'mcamacho', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Mariana', 'Camacho Ruiz', 'm.camacho@transaduanas.pe', 'DNI', '10345678', '+51 987 112 002', 'Asesor Legal', 'LEG-002', NOW(), TRUE),
    (5, 'ASESOR_LEGAL', 5, 'jvilchez', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Javier', 'Vilchez Navarro', 'j.vilchez@transaduanas.pe', 'DNI', '10456789', '+51 987 112 003', 'Asesor Legal', 'LEG-003', NOW(), TRUE),
    (6, 'ASESOR_LEGAL', 6, 'cportugal', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Claudia', 'Portugal Yabar', 'c.portugal@transaduanas.pe', 'DNI', '10567890', '+51 987 112 004', 'Asesor Legal', 'LEG-004', NOW(), TRUE),
    (7, 'CONTADOR', 1, 'contador', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Elena', 'Morales Benitez', 'finanzas@transaduanas.pe', 'DNI', '20123456', '+51 987 223 001', 'Contador', 'CON-001', NOW(), TRUE),
    (8, 'CONTADOR', 2, 'rsalazar', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Ricardo', 'Salazar Huaman', 'r.salazar@transaduanas.pe', 'DNI', '20234567', '+51 987 223 002', 'Contador', 'CON-002', NOW(), TRUE),
    (9, 'CONTADOR', 3, 'gmontoya', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Gisela', 'Montoya Cordova', 'g.montoya@transaduanas.pe', 'DNI', '20345678', '+51 987 223 003', 'Contador', 'CON-003', NOW(), TRUE),
    (10, 'CONTADOR', 5, 'lcornejo', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Luis', 'Cornejo Barrientos', 'l.cornejo@transaduanas.pe', 'DNI', '20456789', '+51 987 223 004', 'Contador', 'CON-004', NOW(), TRUE),
    (11, 'CONTADOR', 6, 'vzamora', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Valeria', 'Zamora Caceres', 'v.zamora@transaduanas.pe', 'DNI', '20567890', '+51 987 223 005', 'Contador', 'CON-005', NOW(), TRUE),
    (12, 'AGENTE_COMERCIAL', 2, 'comercial', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Sofia', 'Paredes Romero', 'ventas.sanisidro@transaduanas.pe', 'DNI', '30123456', '+51 987 334 001', 'Agente Comercial', 'COM-001', NOW(), TRUE),
    (13, 'AGENTE_COMERCIAL', 1, 'dcastillo', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Diego', 'Castillo Alva', 'd.castillo@transaduanas.pe', 'DNI', '30234567', '+51 987 334 002', 'Agente Comercial', 'COM-002', NOW(), TRUE),
    (14, 'AGENTE_COMERCIAL', 1, 'nvalverde', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Natalia', 'Valverde Castro', 'n.valverde@transaduanas.pe', 'DNI', '30345678', '+51 987 334 003', 'Agente Comercial', 'COM-003', NOW(), TRUE),
    (15, 'AGENTE_COMERCIAL', 2, 'mrodriguez', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Mateo', 'Rodriguez Chumpitaz', 'm.rodriguez@transaduanas.pe', 'DNI', '30456789', '+51 987 334 004', 'Agente Comercial', 'COM-004', NOW(), TRUE),
    (16, 'AGENTE_COMERCIAL', 2, 'pquispe', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Paola', 'Quispe Mamani', 'p.quispe@transaduanas.pe', 'DNI', '30567890', '+51 987 334 005', 'Agente Comercial', 'COM-005', NOW(), TRUE),
    (17, 'AGENTE_COMERCIAL', 3, 'aguzman', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Alvaro', 'Guzman Barreda', 'a.guzman@transaduanas.pe', 'DNI', '30678901', '+51 987 334 006', 'Agente Comercial', 'COM-006', NOW(), TRUE),
    (18, 'AGENTE_COMERCIAL', 3, 'btorres', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Brenda', 'Torres Llosa', 'b.torres@transaduanas.pe', 'DNI', '30789012', '+51 987 334 007', 'Agente Comercial', 'COM-007', NOW(), TRUE),
    (19, 'AGENTE_COMERCIAL', 4, 'jchavez', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Julio', 'Chavez Becerra', 'j.chavez@transaduanas.pe', 'DNI', '30890123', '+51 987 334 008', 'Agente Comercial', 'COM-008', NOW(), TRUE),
    (20, 'AGENTE_COMERCIAL', 4, 'kdelgado', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Karla', 'Delgado Ponce', 'k.delgado@transaduanas.pe', 'DNI', '30901234', '+51 987 334 009', 'Agente Comercial', 'COM-009', NOW(), TRUE),
    (21, 'AGENTE_COMERCIAL', 5, 'hzeta', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Hector', 'Zeta Seminario', 'h.zeta@transaduanas.pe', 'DNI', '31012345', '+51 987 334 010', 'Agente Comercial', 'COM-010', NOW(), TRUE),
    (22, 'AGENTE_COMERCIAL', 5, 'mmorante', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Monica', 'Morante Purizaca', 'm.morante@transaduanas.pe', 'DNI', '31123456', '+51 987 334 011', 'Agente Comercial', 'COM-011', NOW(), TRUE),
    (23, 'AGENTE_COMERCIAL', 6, 'ggaldos', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Gonzalo', 'Galdos Vera', 'g.galdos@transaduanas.pe', 'DNI', '31234567', '+51 987 334 012', 'Agente Comercial', 'COM-012', NOW(), TRUE),
    (24, 'SUPERVISOR_ADUANERO', 3, 'supervisor', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Martin', 'Sotomayor Vega', 'operaciones.maritimo@transaduanas.pe', 'DNI', '40123456', '+51 987 445 001', 'Supervisor Aduanero', 'SUP-001', NOW(), TRUE),
    (25, 'SUPERVISOR_ADUANERO', 3, 'rcabrera', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Renzo', 'Cabrera Farfan', 'r.cabrera@transaduanas.pe', 'DNI', '40234567', '+51 987 445 002', 'Supervisor Aduanero', 'SUP-002', NOW(), TRUE),
    (26, 'SUPERVISOR_ADUANERO', 3, 'apena', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Andrea', 'Pena Quintana', 'a.pena@transaduanas.pe', 'DNI', '40345678', '+51 987 445 003', 'Supervisor Aduanero', 'SUP-003', NOW(), TRUE),
    (27, 'SUPERVISOR_ADUANERO', 4, 'dmiranda', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Daniel', 'Miranda Bravo', 'd.miranda@transaduanas.pe', 'DNI', '40456789', '+51 987 445 004', 'Supervisor Aduanero', 'SUP-004', NOW(), TRUE),
    (28, 'SUPERVISOR_ADUANERO', 4, 'sllanos', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Silvia', 'Llanos Palacios', 's.llanos@transaduanas.pe', 'DNI', '40567890', '+51 987 445 005', 'Supervisor Aduanero', 'SUP-005', NOW(), TRUE),
    (29, 'SUPERVISOR_ADUANERO', 5, 'eojeda', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Edgar', 'Ojeda Garrido', 'e.ojeda@transaduanas.pe', 'DNI', '40678901', '+51 987 445 006', 'Supervisor Aduanero', 'SUP-006', NOW(), TRUE),
    (30, 'SUPERVISOR_ADUANERO', 5, 'ctineo', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Carmen', 'Tineo Carrasco', 'c.tineo@transaduanas.pe', 'DNI', '40789012', '+51 987 445 007', 'Supervisor Aduanero', 'SUP-007', NOW(), TRUE),
    (31, 'SUPERVISOR_ADUANERO', 6, 'mzegarra', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Manuel', 'Zegarra Valdivia', 'm.zegarra@transaduanas.pe', 'DNI', '40890123', '+51 987 445 008', 'Supervisor Aduanero', 'SUP-008', NOW(), TRUE),
    (32, 'AGENTE_ADUANERO', 4, 'agente', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Jorge', 'Herrera Diaz', 'operaciones.aereo@transaduanas.pe', 'DNI', '50123456', '+51 987 556 001', 'Agente Aduanero', 'AGE-001', NOW(), TRUE),
    (33, 'AGENTE_ADUANERO', 3, 'fbalbuena', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Franco', 'Balbuena Otero', 'f.balbuena@transaduanas.pe', 'DNI', '50234567', '+51 987 556 002', 'Agente Aduanero', 'AGE-002', NOW(), TRUE),
    (34, 'AGENTE_ADUANERO', 3, 'lmejia', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Lucia', 'Mejia Ramos', 'l.mejia@transaduanas.pe', 'DNI', '50345678', '+51 987 556 003', 'Agente Aduanero', 'AGE-003', NOW(), TRUE),
    (35, 'AGENTE_ADUANERO', 3, 'ccordova', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Cesar', 'Cordova Nunez', 'c.cordova@transaduanas.pe', 'DNI', '50456789', '+51 987 556 004', 'Agente Aduanero', 'AGE-004', NOW(), TRUE),
    (36, 'AGENTE_ADUANERO', 3, 'rtapia', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Rocio', 'Tapia Calderon', 'r.tapia@transaduanas.pe', 'DNI', '50567890', '+51 987 556 005', 'Agente Aduanero', 'AGE-005', NOW(), TRUE),
    (37, 'AGENTE_ADUANERO', 3, 'jalarcon', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Joel', 'Alarcon Suclupe', 'j.alarcon@transaduanas.pe', 'DNI', '50678901', '+51 987 556 006', 'Agente Aduanero', 'AGE-006', NOW(), TRUE),
    (38, 'AGENTE_ADUANERO', 3, 'pflores', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Patricia', 'Flores Yupanqui', 'p.flores@transaduanas.pe', 'DNI', '50789012', '+51 987 556 007', 'Agente Aduanero', 'AGE-007', NOW(), TRUE),
    (39, 'AGENTE_ADUANERO', 3, 'gsuarez', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Gustavo', 'Suarez Carreno', 'g.suarez@transaduanas.pe', 'DNI', '50890123', '+51 987 556 008', 'Agente Aduanero', 'AGE-008', NOW(), TRUE),
    (40, 'AGENTE_ADUANERO', 4, 'mmendoza', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Melanie', 'Mendoza Huaman', 'm.mendoza@transaduanas.pe', 'DNI', '50901234', '+51 987 556 009', 'Agente Aduanero', 'AGE-009', NOW(), TRUE),
    (41, 'AGENTE_ADUANERO', 4, 'asalazar', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Adrian', 'Salazar Vidal', 'a.salazar@transaduanas.pe', 'DNI', '51012345', '+51 987 556 010', 'Agente Aduanero', 'AGE-010', NOW(), TRUE),
    (42, 'AGENTE_ADUANERO', 4, 'verazo', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Vanessa', 'Erazo Luque', 'v.erazo@transaduanas.pe', 'DNI', '51123456', '+51 987 556 011', 'Agente Aduanero', 'AGE-011', NOW(), TRUE),
    (43, 'AGENTE_ADUANERO', 4, 'carias', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Christian', 'Arias Zavaleta', 'c.arias@transaduanas.pe', 'DNI', '51234567', '+51 987 556 012', 'Agente Aduanero', 'AGE-012', NOW(), TRUE),
    (44, 'AGENTE_ADUANERO', 4, 'dgutierrez', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Diana', 'Gutierrez Pino', 'd.gutierrez@transaduanas.pe', 'DNI', '51345678', '+51 987 556 013', 'Agente Aduanero', 'AGE-013', NOW(), TRUE),
    (45, 'AGENTE_ADUANERO', 5, 'jpaz', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Jonathan', 'Paz Farfan', 'j.paz@transaduanas.pe', 'DNI', '51456789', '+51 987 556 014', 'Agente Aduanero', 'AGE-015', NOW(), TRUE),
    (46, 'AGENTE_ADUANERO', 5, 'ssandoval', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Sara', 'Sandoval Yarleque', 's.sandoval@transaduanas.pe', 'DNI', '51567890', '+51 987 556 016', 'Agente Aduanero', 'AGE-016', NOW(), TRUE),
    (47, 'AGENTE_ADUANERO', 5, 'mchunga', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Moises', 'Chunga Ipanaque', 'm.chunga@transaduanas.pe', 'DNI', '51678901', '+51 987 556 017', 'Agente Aduanero', 'AGE-017', NOW(), TRUE),
    (48, 'AGENTE_ADUANERO', 5, 'ymendoza', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Yessenia', 'Mendoza Albujar', 'y.mendoza@transaduanas.pe', 'DNI', '51789012', '+51 987 556 018', 'Agente Aduanero', 'AGE-018', NOW(), TRUE),
    (49, 'AGENTE_ADUANERO', 6, 'rcondori', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Raul', 'Condori Mamani', 'r.condori@transaduanas.pe', 'DNI', '51890123', '+51 987 556 019', 'Agente Aduanero', 'AGE-019', NOW(), TRUE),
    (50, 'AGENTE_ADUANERO', 6, 'mquispe', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Miriam', 'Quispe Apaza', 'm.quispea@transaduanas.pe', 'DNI', '51901234', '+51 987 556 020', 'Agente Aduanero', 'AGE-020', NOW(), TRUE),
    (51, 'AGENTE_ADUANERO', 6, 'fllanos', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Fernando', 'Llanos Coaguila', 'f.llanos@transaduanas.pe', 'DNI', '52012345', '+51 987 556 021', 'Agente Aduanero', 'AGE-021', NOW(), TRUE),
    (52, 'AGENTE_ADUANERO', 6, 'jchoque', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Juana', 'Choque Huayta', 'j.choque@transaduanas.pe', 'DNI', '52123456', '+51 987 556 022', 'Agente Aduanero', 'AGE-022', NOW(), TRUE),
    (53, 'AGENTE_ADUANERO', 6, 'aaguilar', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Alex', 'Aguilar Cornejo', 'a.aguilar@transaduanas.pe', 'DNI', '52234567', '+51 987 556 023', 'Agente Aduanero', 'AGE-023', NOW(), TRUE),
    (54, 'SUPERVISOR_TRANSPORTE', 3, 'transporte', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Raul', 'Cardenas Melendez', 'transporte@transaduanas.pe', 'DNI', '60123456', '+51 987 667 001', 'Supervisor de Transporte', 'TRA-001', NOW(), TRUE),
    (55, 'SUPERVISOR_TRANSPORTE', 3, 'ebravor', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Enrique', 'Bravo Rojas', 'e.bravo@transaduanas.pe', 'DNI', '60234567', '+51 987 667 002', 'Supervisor de Transporte', 'TRA-002', NOW(), TRUE),
    (56, 'SUPERVISOR_TRANSPORTE', 3, 'wgalarza', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Wilmer', 'Galarza Silva', 'w.galarza@transaduanas.pe', 'DNI', '60345678', '+51 987 667 003', 'Supervisor de Transporte', 'TRA-003', NOW(), TRUE),
    (57, 'SUPERVISOR_TRANSPORTE', 3, 'osanchez', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Oscar', 'Sanchez Huamanchumo', 'o.sanchez@transaduanas.pe', 'DNI', '60456789', '+51 987 667 004', 'Supervisor de Transporte', 'TRA-004', NOW(), TRUE),
    (58, 'SUPERVISOR_TRANSPORTE', 4, 'fparedes', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Felix', 'Paredes Valles', 'f.paredes@transaduanas.pe', 'DNI', '60567890', '+51 987 667 005', 'Supervisor de Transporte', 'TRA-005', NOW(), TRUE),
    (59, 'SUPERVISOR_TRANSPORTE', 4, 'jcastaneda', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Juan', 'Castaneda Luna', 'j.castaneda@transaduanas.pe', 'DNI', '60678901', '+51 987 667 006', 'Supervisor de Transporte', 'TRA-006', NOW(), TRUE),
    (60, 'SUPERVISOR_TRANSPORTE', 4, 'mmendozaa', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Mario', 'Mendoza Ayala', 'm.mendozaa@transaduanas.pe', 'DNI', '60789012', '+51 987 667 007', 'Supervisor de Transporte', 'TRA-007', NOW(), TRUE),
    (61, 'SUPERVISOR_TRANSPORTE', 5, 'wvillegas', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Walter', 'Villegas Bancayan', 'w.villegas@transaduanas.pe', 'DNI', '60890123', '+51 987 667 008', 'Supervisor de Transporte', 'TRA-008', NOW(), TRUE),
    (62, 'SUPERVISOR_TRANSPORTE', 5, 'gchiroque', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Guillermo', 'Chiroque Pasache', 'g.chiroque@transaduanas.pe', 'DNI', '60901234', '+51 987 667 009', 'Supervisor de Transporte', 'TRA-009', NOW(), TRUE),
    (63, 'SUPERVISOR_TRANSPORTE', 5, 'anavarro', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Armando', 'Navarro Dioses', 'a.navarro@transaduanas.pe', 'DNI', '61012345', '+51 987 667 010', 'Supervisor de Transporte', 'TRA-010', NOW(), TRUE),
    (64, 'SUPERVISOR_TRANSPORTE', 6, 'jcalisaya', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Jose', 'Calisaya Pari', 'j.calisaya@transaduanas.pe', 'DNI', '61123456', '+51 987 667 011', 'Supervisor de Transporte', 'TRA-011', NOW(), TRUE),
    (65, 'SUPERVISOR_TRANSPORTE', 6, 'mquispeq', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Marco', 'Quispe Quispe', 'm.quispeq@transaduanas.pe', 'DNI', '61234567', '+51 987 667 012', 'Supervisor de Transporte', 'TRA-012', NOW(), TRUE);

-- =============================================================================
-- 6. CLIENTES DEL SISTEMA (EMPRESAS IMPORTADORAS Y EXPORTADORAS)
-- =============================================================================
INSERT INTO cliente (id_cliente, tipo_documento, numero_documento, razon_social, contacto_representante, direccion_fiscal, correo_contacto, telefono_contacto, estado_cliente, estado_activo) VALUES
    (1, 'RUC', '20370146994', 'Camposol S.A.', 'Guillermo Lozano Benites', 'Carretera Panamericana Norte Km 497, Chao', 'import.export@camposol.com.pe', '(044) 60-7000', 'CLIENTE', TRUE),
    (2, 'RUC', '20440058771', 'Danper Trujillo S.A.C.', 'Rosario Bazan Cabellos', 'Carretera Industrial Mz. L Lote 5, Trujillo', 'comercioexterior@danper.com', '(044) 25-1030', 'CLIENTE', TRUE),
    (3, 'RUC', '20132397774', 'Agricola Cerro Prieto S.A.', 'Alfonso Bustamante Canny', 'Av. San Borja Sur 526, San Borja, Lima', 'operaciones@acp.com.pe', '(01) 475-2200', 'CLIENTE', TRUE),
    (4, 'RUC', '20297052841', 'Viru S.A.', 'Yoselyn Maldonado Silva', 'Av. Manuel Bandeira 780, San Isidro, Lima', 'logistica@viru.com.pe', '(01) 618-5000', 'CLIENTE', TRUE),
    (5, 'RUC', '20480392110', 'Sociedad Agricola Drokasa S.A. (Agrokasa)', 'Carlos Arrese Arbaiza', 'Av. Consorcio 110, La Victoria, Lima', 'aduanas@agrokasa.pe', '(01) 224-5500', 'CLIENTE', TRUE),
    (6, 'RUC', '20452369882', 'Complejo Agroindustrial Beta S.A.', 'Lionel Arce Orbegozo', 'Av. Manuel Olguin 325, Of. 802, Surco, Lima', 'embarques@beta.com.pe', '(01) 611-3800', 'CLIENTE', TRUE),
    (7, 'RUC', '20509631456', 'Agricola Don Ricardo S.A.C.', 'Ricardo Briceno Villena', 'Fundo Santa Margarita s/n, La Tinguina, Ica', 'ventas@donricardo.com', '(056) 40-2010', 'CLIENTE', TRUE),
    (8, 'RUC', '20601248993', 'Exportadora Agricola Organica del Norte S.A.C.', 'Manuel Farfan Seminario', 'Carretera Sullana - Tambogrande Km 8.5, Piura', 'mfarfan@agroorganica.pe', '(073) 30-8812', 'PROSPECTO', TRUE),
    (9, 'RUC', '20554128963', 'Agroberries Peru S.A.C.', 'Felipe Valenzuela Rivas', 'Av. Primavera 120, Of. 504, Chacarilla, Surco, Lima', 'contacto.peru@agroberries.cl', '(01) 719-4450', 'CLIENTE', TRUE),
    (10, 'RUC', '20603347891', 'Frutas y Especias del Huallaga E.I.R.L.', 'Segundo Huaman Tanchiva', 'Jr. San Martin 420, Tarapoto, San Martin', 'ventas@frutashuallaga.pe', '(042) 52-2311', 'PROSPECTO', TRUE),
    (11, 'RUC', '20100971772', 'Tecnologica de Alimentos S.A. (TASA)', 'Gonzalo de Romana', 'Av. Manuel Olguin 501, Piso 5, Surco, Lima', 'aduanas@tasa.com.pe', '(01) 611-1400', 'CLIENTE', TRUE),
    (12, 'RUC', '20100070970', 'Pesquera Diamante S.A.', 'Fernando Brescia Moreyra', 'Av. Republica de Panama 2577, La Victoria, Lima', 'comex@diamante.com.pe', '(01) 513-2000', 'CLIENTE', TRUE),
    (13, 'RUC', '20297052922', 'Pesquera Exalmar S.A.A.', 'Victor Matta Curotto', 'Av. Victor Andres Belaunde 214, San Isidro, Lima', 'logistica@exalmar.com.pe', '(01) 441-4420', 'CLIENTE', TRUE),
    (14, 'RUC', '20100152356', 'Pesquera Hayduk S.A.', 'Walter Martinez Moreno', 'Av. Manuel Olguin 211, Piso 7, Surco, Lima', 'hayduk@hayduk.com.pe', '(01) 211-3400', 'CLIENTE', TRUE),
    (15, 'RUC', '20484019219', 'Inversiones Prisco S.A.C. (Mar Frio)', 'Eduardo Osterling Letts', 'Carretera a Paita Km 3.5, Zona Industrial, Paita', 'operaciones@prisco.com.pe', '(073) 21-1250', 'CLIENTE', TRUE),
    (16, 'RUC', '20512896541', 'Seafrost S.A.C.', 'Antonio Bologna Campodonico', 'Carretera Paita - Sullana Km 1.5, Paita, Piura', 'exportaciones@seafrost.com.pe', '(073) 53-2000', 'CLIENTE', TRUE),
    (17, 'RUC', '20528963214', 'Altamar Foods Peru S.R.L.', 'Juan Carlos Benavides', 'Av. Nestor Gambetta Km 14.5, Callao', 'info@altamarfoods.pe', '(01) 577-4320', 'CLIENTE', TRUE),
    (18, 'RUC', '20600124851', 'Acuacultura Marina del Sur S.A.C.', 'Roberto Zegarra Ballon', 'Calle Muelle Fiscal s/n, Islay, Matarani, Arequipa', 'rzegarra@acuasur.pe', '(054) 48-7320', 'PROSPECTO', TRUE),
    (19, 'RUC', '20604512398', 'Marinas y Conservas Chimbote S.A.C.', 'Enrique Cardenas Gil', 'Av. Los Pescadores Mz. A Lote 4, Chimbote, Ancash', 'comercial@marinaschimbote.pe', '(043) 35-1890', 'CLIENTE', TRUE),
    (20, 'RUC', '20546321894', 'Langostinera del Pacifico Tumbes E.I.R.L.', 'Raul Dioses Carrillo', 'Carretera Panamericana Norte Km 1260, Tumbes', 'rdioses@langostinosdelpacifico.pe', '(072) 52-4411', 'PROSPECTO', TRUE),
    (21, 'RUC', '20300263578', 'Samsung Electronics Latinoamerica Peru S.A.C.', 'Hoon Kang', 'Av. Canaval y Moreyra 380, Piso 18, San Isidro, Lima', 'importaciones.peru@samsung.com', '(01) 710-8000', 'CLIENTE', TRUE),
    (22, 'RUC', '20511894520', 'LG Electronics Peru S.A.', 'Ki Wook Kim', 'Av. Republica de Colombia 791, Piso 12, San Isidro, Lima', 'logistica.lg@lge.com', '(01) 618-2000', 'CLIENTE', TRUE),
    (23, 'RUC', '20501874211', 'Lenovo Peru S.R.L.', 'Andres Becerra Moreno', 'Av. Rivera Navarrete 501, Piso 15, San Isidro, Lima', 'comex.peru@lenovo.com', '(01) 616-5200', 'CLIENTE', TRUE),
    (24, 'RUC', '20556214875', 'Xiaomi Peru S.A.C.', 'Wei Chen', 'Av. Jorge Chavez 184, Of. 601, Miraflores, Lima', 'customs.peru@xiaomi.com', '(01) 712-9800', 'CLIENTE', TRUE),
    (25, 'RUC', '20504128965', 'Deltron Computadoras S.A.C.', 'Nestor Quispe Zevallos', 'Av. Manuel Bandeira 241, San Martin de Porres, Lima', 'compras.exterior@deltron.com.pe', '(01) 415-0100', 'CLIENTE', TRUE),
    (26, 'RUC', '20508932145', 'Ingram Micro Peru S.A.C.', 'Fernando Zevallos Ortiz', 'Av. Argentina 2060, Callao', 'logistica.ingram@ingrammicro.com', '(01) 614-4200', 'CLIENTE', TRUE),
    (27, 'RUC', '20541239874', 'Intcomex del Peru S.A.C.', 'Luis Alfredo Gomez', 'Av. Elmer Faucett 1950, Callao', 'aduanas.intcomex@intcomex.com', '(01) 716-5000', 'CLIENTE', TRUE),
    (28, 'RUC', '20602356981', 'Innova Tech Soluciones Globales S.A.C.', 'Marcos Barrientos Vega', 'Calle Schell 319, Of. 402, Miraflores, Lima', 'mbarrientos@innovatech.pe', '(01) 445-1289', 'PROSPECTO', TRUE),
    (29, 'RUC', '20605891234', 'SmartHome Peru Import S.A.C.', 'Brenda Caceres Wong', 'Av. Conquistadores 620, San Isidro, Lima', 'bcaceres@smarthomeperu.com', '(01) 421-8890', 'PROSPECTO', TRUE),
    (30, 'RUC', '20608974512', 'CyberGaming Hardware del Peru E.I.R.L.', 'Kevin Alarcon Torres', 'Av. Wilson 1240, Galeria Cyberplaza Tienda 210, Lima', 'ventas@cybergaming.pe', '(01) 424-6632', 'PROSPECTO', TRUE),
    (31, 'RUC', '20330790762', 'Compania Minera Antamina S.A.', 'Victor Gobitz Colchado', 'Av. El Derby 055, Torre 1, Piso 8, Surco, Lima', 'supplychain@antamina.com', '(01) 217-3000', 'CLIENTE', TRUE),
    (32, 'RUC', '20100142989', 'Southern Peru Copper Corporation', 'Oscar Gonzalez Rocha', 'Av. Caminos del Inca 171, Chacarilla, Surco, Lima', 'compras.import@southernperu.com.pe', '(01) 512-0440', 'CLIENTE', TRUE),
    (33, 'RUC', '20100055237', 'Ferreyros S.A.', 'Gonzaga Ferrand Aspillaga', 'Jr. Cristobal de Peralta Norte 820, Surco, Lima', 'aduanas@ferreyros.com.pe', '(01) 626-4000', 'CLIENTE', TRUE),
    (34, 'RUC', '20100115663', 'Komatsu-Mitsui Maquinarias Peru S.A.', 'Julio Molina Barreda', 'Av. Nestor Gambetta Km 3.5, Callao', 'importaciones@kmmp.com.pe', '(01) 615-8000', 'CLIENTE', TRUE),
    (35, 'RUC', '20508541296', 'Atlas Copco Peruana S.A.', 'Patricio Cabezas Silva', 'Calle Los Calderos 185, Urb. El Vulcano, Ate, Lima', 'logistica.peru@atlascopco.com', '(01) 411-6100', 'CLIENTE', TRUE),
    (36, 'RUC', '20100085497', 'Epiroc Peru S.A.', 'Angel Tobar Morales', 'Av. Nicolas Ayllon 2634, Ate, Lima', 'customs.peru@epiroc.com', '(01) 205-3000', 'CLIENTE', TRUE),
    (37, 'RUC', '20521489632', 'Metso Outotec Peru S.A.C.', 'Renato Silva Manrique', 'Av. Paseo de la Republica 3245, San Isidro, Lima', 'aduanas.metso@mogroup.com', '(01) 313-4000', 'CLIENTE', TRUE),
    (38, 'RUC', '20601548792', 'Perforaciones & Taladros Andinos S.A.C.', 'Guillermo Holguin Prado', 'Av. Evitamiento Km 5, Parque Industrial, Arequipa', 'gholguin@perforacionesandinas.pe', '(054) 44-5511', 'PROSPECTO', TRUE),
    (39, 'RUC', '20603412589', 'Maquinarias Mineras del Sur E.I.R.L.', 'Hernan Mamani Choque', 'Av. Independencia 810, Cercado, Arequipa', 'hmamani@maquimin.pe', '(054) 28-9040', 'PROSPECTO', TRUE),
    (40, 'RUC', '20607845123', 'Generadores y Motores Industriales S.A.C.', 'Esteban Rospigliosi Ruiz', 'Av. Colonial 2480, Bellavista, Callao', 'erospigliosi@genmotores.pe', '(01) 451-9920', 'PROSPECTO', TRUE),
    (41, 'RUC', '20100038812', 'Toyota del Peru S.A.', 'Toshiro Nishimura', 'Av. Santo Toribio 173, Via Central 125, San Isidro, Lima', 'aduanas@toyotaperu.com.pe', '(01) 611-1000', 'CLIENTE', TRUE),
    (42, 'RUC', '20384562145', 'Derco Peru S.A.', 'Claudio Echeverria Gomez', 'Av. Manuel Olguin 325, Piso 11, Surco, Lima', 'comex@derco.pe', '(01) 713-5000', 'CLIENTE', TRUE),
    (43, 'RUC', '20100124816', 'Divemotor S.A.', 'Peter Gremler', 'Av. Canada 1160, La Victoria, Lima', 'importaciones@divemotor.com.pe', '(01) 712-2000', 'CLIENTE', TRUE),
    (44, 'RUC', '20100236541', 'Volvo Peru S.A.', 'Jorgen Sjostedt', 'Carretera Panamericana Sur Km 23.8, Lurin, Lima', 'aduanas.volvo@volvo.com', '(01) 317-1111', 'CLIENTE', TRUE),
    (45, 'RUC', '20501487923', 'Goodyear del Peru S.A.', 'Carlos Perez-Albela', 'Av. Argentina 6030, Carmen de la Legua, Callao', 'aduanas.peru@goodyear.com', '(01) 517-3100', 'CLIENTE', TRUE),
    (46, 'RUC', '20514896325', 'Continental Tire Andina Sucursal Peru', 'Santiago Delgado Mora', 'Av. Canaval y Moreyra 452, San Isidro, Lima', 'importaciones@continental-tires.pe', '(01) 610-8500', 'CLIENTE', TRUE),
    (47, 'RUC', '20536985214', 'Frenosa S.A.', 'Eduardo Echecopar Florez', 'Av. Maquinarias 2150, Cercado de Lima, Lima', 'export@frenosa.com.pe', '(01) 336-7000', 'CLIENTE', TRUE),
    (48, 'RUC', '20601478523', 'Distribuidora de Repuestos Automotrices del Centro S.A.C.', 'Jorge Villacorta Poma', 'Av. Mariscal Castilla 1420, El Tambo, Huancayo', 'jvillacorta@repuestoscentro.pe', '(064) 24-8833', 'PROSPECTO', TRUE),
    (49, 'RUC', '20604578912', 'Llantas y Baterias del Norte E.I.R.L.', 'Fidel Chiroque Purizaca', 'Av. Sanchez Cerro 1080, Piura', 'fchiroque@llantasdelnorte.pe', '(073) 32-1590', 'PROSPECTO', TRUE),
    (50, 'RUC', '20609874125', 'Mundial Car Accesorios & Tuning S.A.C.', 'Cesar Melendez Canto', 'Av. Iquitos 840, La Victoria, Lima', 'ventas@mundialcar.pe', '(01) 431-2570', 'PROSPECTO', TRUE),
    (51, 'RUC', '20100017491', 'Quimica Suiza S.A.C.', 'Gianfranco Ferrari', 'Av. Republica de Panama 2577, La Victoria, Lima', 'comex@quimicasuiza.com.pe', '(01) 211-4000', 'CLIENTE', TRUE),
    (52, 'RUC', '20100062446', 'Laboratorios Medifarma S.A.', 'Fernando Liendo Seminario', 'Jr. Ecuador 787, Cercado de Lima, Lima', 'aduanas@medifarma.com.pe', '(01) 330-3500', 'CLIENTE', TRUE),
    (53, 'RUC', '20100035473', 'Laboratorios AC Farma S.A.', 'Arnulfo Carrion Cardenas', 'Calle Los Plateros 152, El Artesano, Ate, Lima', 'comercioexterior@acfarma.com.pe', '(01) 748-2000', 'CLIENTE', TRUE),
    (54, 'RUC', '20100138281', 'Bayer S.A.', 'Mathias Schipper', 'Av. Paseo de la Republica 3074, San Isidro, Lima', 'aduanas.peru@bayer.com', '(01) 211-3800', 'CLIENTE', TRUE),
    (55, 'RUC', '20100057281', 'Pfizer S.A.', 'Christopher Ariyan', 'Av. Javier Prado Este 444, Torre B, Piso 15, San Isidro, Lima', 'logistica.peru@pfizer.com', '(01) 615-2300', 'CLIENTE', TRUE),
    (56, 'RUC', '20100108871', 'Belcorp (Cetco S.A.)', 'Eduardo Belmont Anderson', 'Av. Pardo y Aliaga 652, Piso 12, San Isidro, Lima', 'comex@belcorp.biz', '(01) 211-3600', 'CLIENTE', TRUE),
    (57, 'RUC', '20502145896', 'Natura Cosmeticos S.A.', 'Daniella de Luca', 'Av. Faustino Sanchez Carrion 615, Jesus Maria, Lima', 'importaciones@natura.net', '(01) 611-1200', 'CLIENTE', TRUE),
    (58, 'RUC', '20601985421', 'Biomedica & Diagnostico Clinico del Peru S.A.C.', 'Dr. Andres Valencia Otero', 'Av. Brasil 1450, Of. 502, Jesus Maria, Lima', 'avalencia@biomedicaperu.pe', '(01) 460-7714', 'PROSPECTO', TRUE),
    (59, 'RUC', '20603874512', 'Drogueria Farmavida Express E.I.R.L.', 'Dra. Luzmila Benavente Rios', 'Jr. Huancavelica 450, Trujillo, La Libertad', 'lbenavente@farmavida.pe', '(044) 20-3345', 'PROSPECTO', TRUE),
    (60, 'RUC', '20607412589', 'Insumos Odontologicos del Sur S.A.C.', 'Dr. Christian Apaza Nunez', 'Calle Mercaderes 210, Cercado, Arequipa', 'capaza@insumosdentalsur.pe', '(054) 22-8119', 'PROSPECTO', TRUE),
    (61, 'RUC', '20100055238', 'Alicorp S.A.A.', 'Alfredo Perez Gubbins', 'Av. Argentina 4793, Carmen de la Legua, Callao', 'comex@alicorp.com.pe', '(01) 315-0800', 'CLIENTE', TRUE),
    (62, 'RUC', '20100100101', 'Gloria S.A.', 'Claudio Rodriguez Huaco', 'Av. Republica de Panama 2461, Santa Catalina, La Victoria, Lima', 'aduanas@gloria.com.pe', '(01) 470-7170', 'CLIENTE', TRUE),
    (63, 'RUC', '20100030595', 'Union de Cervecerias Peruanas Backus y Johnston S.A.A.', 'Sergio Rincon', 'Av. Nicolas Ayllon 3986, Ate, Lima', 'supplychain@backus.com.pe', '(01) 311-3000', 'CLIENTE', TRUE),
    (64, 'RUC', '20100070971', 'Molinera del Centro S.A.', 'Ernesto Nicolini de la Riva', 'Av. Enrique Meiggs 230, Chimbote, Ancash', 'comercioexterior@molineracentro.com.pe', '(043) 32-1400', 'CLIENTE', TRUE),
    (65, 'RUC', '20100010721', 'Supermercados Peruanos S.A.', 'Juan Carlos Vallejo', 'Calle Morelli 181, San Borja, Lima', 'importaciones.retail@spsa.pe', '(01) 618-8000', 'CLIENTE', TRUE),
    (66, 'RUC', '20100070972', 'Cencosud Retail Peru S.A.', 'Alfredo Mastrokalos', 'Calle Augusto Angulo 130, Miraflores, Lima', 'aduanas@cencosud.com.pe', '(01) 613-8888', 'CLIENTE', TRUE),
    (67, 'RUC', '20100057282', 'Tiendas Por Departamento Ripley S.A.', 'Eduardo Daly', 'Av. Las Begonias 540, San Isidro, Lima', 'comex@ripley.com.pe', '(01) 610-4000', 'CLIENTE', TRUE),
    (68, 'RUC', '20601239854', 'Importadora Gourmet Los Andes S.A.C.', 'Gonzalo Benavides Larco', 'Av. La Encalada 1240, Surco, Lima', 'gbenavides@gourmetandes.pe', '(01) 436-5520', 'PROSPECTO', TRUE),
    (69, 'RUC', '20604125896', 'Distribuidora de Licores & Vinos del Plata S.A.C.', 'Mariana Ocampo Cisneros', 'Av. Reducto 820, Miraflores, Lima', 'mocampo@vinosdelplata.pe', '(01) 446-9930', 'PROSPECTO', TRUE),
    (70, 'RUC', '20608521478', 'Snacks Saludables y Frutos Secos del Valle E.I.R.L.', 'Fabiola Cardenas Prado', 'Urb. San Andres Mz. C Lote 12, Trujillo', 'fcardenas@snacksvalle.pe', '(044) 29-1140', 'PROSPECTO', TRUE),
    (71, 'RUC', '20100010724', 'Corporacion Aceros Arequipa S.A.', 'Tulio Silgado Consiglieri', 'Av. Enrique Canaval y Moreyra 340, Piso 17, San Isidro, Lima', 'aduanas@aasa.com.pe', '(01) 517-1800', 'CLIENTE', TRUE),
    (72, 'RUC', '20100142990', 'Empresa Siderurgica del Peru S.A.A. (Siderperu)', 'Richard Cervantes', 'Av. Santiago de Surco 3245, Surco, Lima', 'comex@sider.com.pe', '(01) 618-6800', 'CLIENTE', TRUE),
    (73, 'RUC', '20100115664', 'Maestro Peru S.A. / Sodimac Peru S.A.', 'Wilhelm Ramberg', 'Av. Javier Prado Este 4200, Surco, Lima', 'aduanas@falabella.com.pe', '(01) 615-6000', 'CLIENTE', TRUE),
    (74, 'RUC', '20100062447', 'Promart Homecenter (Homecenters Peruanos S.A.)', 'Manuel de la Rosa', 'Av. Aviacion 2405, Piso 6, San Borja, Lima', 'comercioexterior@promart.pe', '(01) 619-4000', 'CLIENTE', TRUE),
    (75, 'RUC', '20100035474', 'San Lorenzo Ceramicas S.A.C.', 'Antoni Rovira', 'Av. Industrial 1010, Urb. Las Praderas, Lurin, Lima', 'export@sanlorenzo.com.pe', '(01) 614-7400', 'CLIENTE', TRUE),
    (76, 'RUC', '20501487924', 'Pavco Wavin Peru S.A.', 'Carlos Gonzalez Morla', 'Calle Uno Mz. B Lote 1, Parque Industrial El Asesor, Ate, Lima', 'logistica@pavco.com.pe', '(01) 319-3200', 'CLIENTE', TRUE),
    (77, 'RUC', '20601478524', 'Importaciones Ferreteras del Pacifico S.A.C.', 'Victor Hugo Zevallos', 'Av. Argentina 1840, Cercado de Lima, Lima', 'vzevallos@ferrepacifico.pe', '(01) 336-8810', 'PROSPECTO', TRUE),
    (78, 'RUC', '20603412590', 'Estructuras & Andamios Industriales S.A.C.', 'Ing. Cesar Montero Paredes', 'Av. Nestor Gambetta Km 8.5, Callao', 'cmontero@andamiosperu.pe', '(01) 577-6210', 'PROSPECTO', TRUE),
    (79, 'RUC', '20605891235', 'Maderas & Enchapados del Oriente E.I.R.L.', 'German Pinedo Vasquez', 'Av. Centenario Km 4.2, Pucallpa, Ucayali', 'gpinedo@maderaoriente.pe', '(061) 57-4422', 'PROSPECTO', TRUE),
    (80, 'RUC', '20609874126', 'Vidrios & Aluminios Arquitectonicos del Sur S.A.C.', 'Eduardo Caceres Valdivia', 'Parque Industrial Mz. G Lote 8, Cerro Colorado, Arequipa', 'ecaceres@vidriosur.pe', '(054) 44-8920', 'PROSPECTO', TRUE);

-- =============================================================================

-- =============================================================================
-- USUARIOS DEL PORTAL B2B (1 Cuenta por cada Empresa Cliente)
-- Hash BCrypt para Password123!: $2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee
-- =============================================================================
INSERT INTO usuario_cliente (id_usuario_cliente, id_cliente, username, password_hash, estado_activo, debe_cambiar_password, ultimo_acceso) VALUES
    (1, 1, '20370146994', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (2, 2, '20440058771', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (3, 3, '20132397774', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (4, 4, '20297052841', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (5, 5, '20480392110', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (6, 6, '20452369882', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (7, 7, '20509631456', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (8, 8, '20601248993', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (9, 9, '20554128963', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (10, 10, '20603347891', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (11, 11, '20100971772', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (12, 12, '20100070970', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (13, 13, '20297052922', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (14, 14, '20100152356', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (15, 15, '20484019219', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (16, 16, '20512896541', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (17, 17, '20528963214', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (18, 18, '20600124851', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (19, 19, '20604512398', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (20, 20, '20546321894', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (21, 21, '20300263578', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (22, 22, '20511894520', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (23, 23, '20501874211', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (24, 24, '20556214875', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (25, 25, '20504128965', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (26, 26, '20508932145', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (27, 27, '20541239874', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (28, 28, '20602356981', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (29, 29, '20605891234', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (30, 30, '20608974512', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (31, 31, '20330790762', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (32, 32, '20100142989', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (33, 33, '20100055237', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (34, 34, '20100115663', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (35, 35, '20508541296', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (36, 36, '20100085497', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (37, 37, '20521489632', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (38, 38, '20601548792', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (39, 39, '20603412589', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (40, 40, '20607845123', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (41, 41, '20100038812', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (42, 42, '20384562145', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (43, 43, '20100124816', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (44, 44, '20100236541', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (45, 45, '20501487923', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (46, 46, '20514896325', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (47, 47, '20536985214', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (48, 48, '20601478523', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (49, 49, '20604578912', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (50, 50, '20609874125', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (51, 51, '20100017491', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (52, 52, '20100062446', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (53, 53, '20100035473', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (54, 54, '20100138281', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (55, 55, '20100057281', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (56, 56, '20100108871', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (57, 57, '20502145896', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (58, 58, '20601985421', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (59, 59, '20603874512', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (60, 60, '20607412589', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (61, 61, '20100055238', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (62, 62, '20100100101', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (63, 63, '20100030595', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (64, 64, '20100070971', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (65, 65, '20100010721', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (66, 66, '20100070972', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (67, 67, '20100057282', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (68, 68, '20601239854', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (69, 69, '20604125896', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (70, 70, '20608521478', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (71, 71, '20100010724', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (72, 72, '20100142990', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (73, 73, '20100115664', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (74, 74, '20100062447', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (75, 75, '20100035474', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (76, 76, '20501487924', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (77, 77, '20601478524', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (78, 78, '20603412590', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (79, 79, '20605891235', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL),
    (80, 80, '20609874126', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', TRUE, FALSE, NULL);

SET FOREIGN_KEY_CHECKS = 1;
