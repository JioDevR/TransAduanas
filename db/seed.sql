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
-- Contraseña para todos los usuarios: Password123!
-- Hash BCrypt: $2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee
INSERT INTO usuario (id_usuario, rol, id_sede, username, password_hash, nombres, apellidos, correo_institucional, tipo_documento, numero_documento, telefono, cargo_titulo, codigo_sunat, ultimo_acceso, estado_activo) VALUES
    (1, 'ADMINISTRADOR_TI', 1, 'admin', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Administrador', 'Sistema TI', 'admin@transaduanas.pe', 'DNI', '00000001', '+51 999 000 001', 'Administrador de TI', 'TI-001', NOW(), TRUE),
    (2, 'GERENTE_GENERAL', 1, 'gerente', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Carlos', 'Mendoza Valdivia', 'gerencia@transaduanas.pe', 'DNI', '00000002', '+51 999 000 002', 'Gerente General', 'GG-001', NOW(), TRUE),
    (3, 'AGENTE_COMERCIAL', 2, 'comercial', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Sofía', 'Paredes Romero', 'ventas.sanisidro@transaduanas.pe', 'DNI', '00000003', '+51 999 000 003', 'Agente Comercial', 'COM-001', NOW(), TRUE),
    (4, 'SUPERVISOR_ADUANERO', 3, 'supervisor', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Martín', 'Sotomayor Vega', 'operaciones.maritimo@transaduanas.pe', 'DNI', '00000004', '+51 999 000 004', 'Supervisor Aduanero', 'SUP-001', NOW(), TRUE),
    (5, 'AGENTE_ADUANERO', 4, 'agente', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Jorge', 'Herrera Díaz', 'operaciones.aereo@transaduanas.pe', 'DNI', '00000005', '+51 999 000 005', 'Agente Aduanero', 'AGE-001', NOW(), TRUE),
    (6, 'SUPERVISOR_TRANSPORTE', 3, 'transporte', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Raúl', 'Cárdenas Meléndez', 'transporte@transaduanas.pe', 'DNI', '00000006', '+51 999 000 006', 'Supervisor de Transporte', 'TRA-001', NOW(), TRUE),
    (7, 'CONTADOR', 1, 'contador', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Elena', 'Morales Benítez', 'finanzas@transaduanas.pe', 'DNI', '00000007', '+51 999 000 007', 'Contador', 'CON-001', NOW(), TRUE),
    (8, 'ASESOR_LEGAL', 1, 'legal', '$2a$10$vbDgD4A8kg.sizd9I.P/AO44N1VeIvOSzvk8BbxZoqb5j9iJZ48Ee', 'Fernando', 'Arana Silva', 'legal@transaduanas.pe', 'DNI', '00000008', '+51 999 000 008', 'Asesor Legal', 'LEG-001', NOW(), TRUE);

-- =============================================================================
SET FOREIGN_KEY_CHECKS = 1;
