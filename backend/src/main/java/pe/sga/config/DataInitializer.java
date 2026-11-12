package pe.sga.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.sga.entity.CatalogoPuerto;
import pe.sga.entity.CatalogoTipoCarga;
import pe.sga.entity.Cliente;
import pe.sga.entity.Conductor;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.CotizacionPago;
import pe.sga.entity.DespachoTransporte;
import pe.sga.entity.Expediente;
import pe.sga.entity.Sede;
import pe.sga.entity.UnidadTransporte;
import pe.sga.entity.Usuario;
import pe.sga.entity.UsuarioCliente;
import pe.sga.entity.enums.CanalSunat;
import pe.sga.entity.enums.EstadoCotizacion;
import pe.sga.entity.enums.EstadoExpediente;
import pe.sga.entity.enums.EstadoPagoCotizacion;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.entity.enums.TipoAbonoCotizacion;
import pe.sga.repository.CatalogoPuertoRepository;
import pe.sga.repository.CatalogoTipoCargaRepository;
import pe.sga.repository.ClienteRepository;
import pe.sga.repository.ConductorRepository;
import pe.sga.repository.CotizacionPagoRepository;
import pe.sga.repository.CotizacionRepository;
import pe.sga.repository.DespachoTransporteRepository;
import pe.sga.repository.ExpedienteRepository;
import pe.sga.repository.SedeRepository;
import pe.sga.repository.UnidadTransporteRepository;
import pe.sga.repository.UsuarioClienteRepository;
import pe.sga.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final BigDecimal TIPO_CAMBIO_SEMILLA = new BigDecimal("3.7500");

    private final SedeRepository sedeRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoPuertoRepository puertoRepository;
    private final CatalogoTipoCargaRepository tipoCargaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioClienteRepository usuarioClienteRepository;
    private final UnidadTransporteRepository unidadTransporteRepository;
    private final ConductorRepository conductorRepository;
    private final CotizacionRepository cotizacionRepository;
    private final CotizacionPagoRepository cotizacionPagoRepository;
    private final ExpedienteRepository expedienteRepository;
    private final DespachoTransporteRepository despachoTransporteRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            SedeRepository sedeRepository,
            UsuarioRepository usuarioRepository,
            CatalogoPuertoRepository puertoRepository,
            CatalogoTipoCargaRepository tipoCargaRepository,
            ClienteRepository clienteRepository,
            UsuarioClienteRepository usuarioClienteRepository,
            UnidadTransporteRepository unidadTransporteRepository,
            ConductorRepository conductorRepository,
            CotizacionRepository cotizacionRepository,
            CotizacionPagoRepository cotizacionPagoRepository,
            ExpedienteRepository expedienteRepository,
            DespachoTransporteRepository despachoTransporteRepository,
            PasswordEncoder passwordEncoder) {
        this.sedeRepository = sedeRepository;
        this.usuarioRepository = usuarioRepository;
        this.puertoRepository = puertoRepository;
        this.tipoCargaRepository = tipoCargaRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioClienteRepository = usuarioClienteRepository;
        this.unidadTransporteRepository = unidadTransporteRepository;
        this.conductorRepository = conductorRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.cotizacionPagoRepository = cotizacionPagoRepository;
        this.expedienteRepository = expedienteRepository;
        this.despachoTransporteRepository = despachoTransporteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Iniciando verificación e inicialización mínima del sistema SGA...");

        // 1. Sede corporativa inicial si no existen sedes
        Sede sedePrincipal = sedeRepository.findAll().stream().findFirst().orElseGet(() -> {
            Sede nueva = Sede.builder()
                    .codigoSede("SED-CORP-01")
                    .nombre("Sede Central Corporativa")
                    .tipo("SEDE_CENTRAL")
                    .direccion("Av. José Pardo N° 835, Piso 11, Edificio Empresarial Pardo Central")
                    .distrito("Miraflores")
                    .telefono("(01) 640-8200")
                    .estado("OPERATIVO")
                    .build();
            log.info("Sede inicial 'Sede Central Corporativa' creada exitosamente.");
            return sedeRepository.save(nueva);
        });

        // 2. Catálogo de puertos mínimo alineado con seed.sql si está vacío
        if (puertoRepository.count() == 0) {
            puertoRepository.saveAll(List.of(
                    CatalogoPuerto.builder()
                            .codigoPuerto("PECAL-DPW")
                            .nombre("Callao - Muelle Sur (DP World)")
                            .pais("Perú")
                            .tipo("MARITIMO")
                            .estado("ACTIVO")
                            .build(),
                    CatalogoPuerto.builder()
                            .codigoPuerto("CNSHA")
                            .nombre("Puerto de Shanghái")
                            .pais("China")
                            .tipo("MARITIMO")
                            .estado("ACTIVO")
                            .build()
            ));
            log.info("Catálogo de puertos base inicializado (PECAL-DPW, CNSHA).");
        }

        // 3. Catálogo de tipos de carga mínimo alineado con seed.sql si está vacío
        if (tipoCargaRepository.count() == 0) {
            tipoCargaRepository.saveAll(List.of(
                    CatalogoTipoCarga.builder()
                            .codigo("CARGA_FCL")
                            .nombre("Carga Contenedorizada Completa (FCL / Dry & HC)")
                            .descripcion("Contenedor exclusivo de 20 o 40 pies para un único consignatario")
                            .estado("ACTIVO")
                            .build(),
                    CatalogoTipoCarga.builder()
                            .codigo("CARGA_REEFER")
                            .nombre("Carga Perecible Refrigerada (Contenedor Reefer)")
                            .descripcion("Mercancías con cadena de frío continua y atmósfera controlada")
                            .estado("ACTIVO")
                            .build()
            ));
            log.info("Catálogo de tipos de carga base inicializado (CARGA_FCL, CARGA_REEFER).");
        }

        // 4. Usuario Administrador único (admin)
        if (!usuarioRepository.existsByUsername("admin")) {
            usuarioRepository.save(Usuario.builder()
                    .rol(RolUsuario.ADMINISTRADOR_TI)
                    .sede(sedePrincipal)
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("Password123!"))
                    .nombres("Administrador")
                    .apellidos("Sistema")
                    .correoInstitucional("admin@sga.pe")
                    .tipoDocumento("DNI")
                    .numeroDocumento("00000000")
                    .telefono("+51 999 999 999")
                    .cargoTitulo(RolUsuario.ADMINISTRADOR_TI.getEtiqueta())
                    .codigoSunat("ADM-001")
                    .estadoActivo(true)
                    .build());
            log.info("Usuario administrador 'admin' creado exitosamente.");
        }

        // 5. Aprovisionamiento de cuentas demo del Portal del Cliente B2B si no existen
        asegurarCuentaDemoPortal("20370146994", "Camposol S.A.", "Guillermo Lozano Benites",
                "Carretera Panamericana Norte Km 497, Chao", "import.export@camposol.com.pe", "(044) 60-7000");
        asegurarCuentaDemoPortal("20511894520", "LG Electronics Peru S.A.", "Ki Wook Kim",
                "Av. Republica de Colombia 791, Piso 12, San Isidro, Lima", "logistica.lg@lge.com", "(01) 618-2000");

        // 6. Parque automotor, conductores y despachos semilla coherentes con db/seed.sql
        inicializarTransporteYFlota();

        log.info("Inicialización limpia del sistema SGA completada exitosamente.");
    }

    private void inicializarTransporteYFlota() {
        if (unidadTransporteRepository.count() == 0) {
            unidadTransporteRepository.saveAll(List.of(
                    UnidadTransporte.builder()
                            .placaTracto("V1A-780")
                            .placaCarreta("T1A-981")
                            .marcaModelo("Volvo FH 500 Globetrotter")
                            .anoFabricacion(2022)
                            .tipoVehiculo("TRACTO")
                            .capacidadCargaKg(new BigDecimal("32000.00"))
                            .soatVencimiento(LocalDate.of(2027, 8, 15))
                            .citvVencimiento(LocalDate.of(2027, 9, 20))
                            .kilometrajeActual(145000)
                            .estadoOperativo("DISPONIBLE")
                            .estadoActivo(true)
                            .observaciones("Tracto con suspensión neumática y GPS satelital")
                            .build(),
                    UnidadTransporte.builder()
                            .placaTracto("V2B-890")
                            .placaCarreta("T2B-982")
                            .marcaModelo("Scania R500 V8 Streamline")
                            .anoFabricacion(2021)
                            .tipoVehiculo("TRACTO")
                            .capacidadCargaKg(new BigDecimal("32000.00"))
                            .soatVencimiento(LocalDate.of(2027, 4, 10))
                            .citvVencimiento(LocalDate.of(2027, 5, 12))
                            .kilometrajeActual(198000)
                            .estadoOperativo("DISPONIBLE")
                            .estadoActivo(true)
                            .observaciones("Asignado a rutas interprovinciales")
                            .build(),
                    UnidadTransporte.builder()
                            .placaTracto("V3C-910")
                            .marcaModelo("International ProStar 450")
                            .anoFabricacion(2020)
                            .tipoVehiculo("TRACTO")
                            .capacidadCargaKg(new BigDecimal("30000.00"))
                            .soatVencimiento(LocalDate.now().plusDays(15))
                            .citvVencimiento(LocalDate.now().plusDays(45))
                            .kilometrajeActual(220000)
                            .estadoOperativo("DISPONIBLE")
                            .estadoActivo(true)
                            .observaciones("Alerta preventiva: SOAT próximo a vencer en 15 días")
                            .build(),
                    UnidadTransporte.builder()
                            .placaTracto("T1A-981")
                            .marcaModelo("Randon Plataforma 3 Ejes")
                            .anoFabricacion(2022)
                            .tipoVehiculo("PLATAFORMA")
                            .capacidadCargaKg(new BigDecimal("34000.00"))
                            .soatVencimiento(LocalDate.of(2027, 11, 30))
                            .citvVencimiento(LocalDate.of(2027, 12, 15))
                            .kilometrajeActual(85000)
                            .estadoOperativo("DISPONIBLE")
                            .estadoActivo(true)
                            .observaciones("Plataforma para contenedores 20ft y 40ft con piñas de seguridad")
                            .build(),
                    UnidadTransporte.builder()
                            .placaTracto("T2B-982")
                            .marcaModelo("Tremac Semirremolque Baranda Rebatible")
                            .anoFabricacion(2023)
                            .tipoVehiculo("PLATAFORMA")
                            .capacidadCargaKg(new BigDecimal("32000.00"))
                            .soatVencimiento(LocalDate.of(2028, 2, 28))
                            .citvVencimiento(LocalDate.of(2028, 3, 15))
                            .kilometrajeActual(62000)
                            .estadoOperativo("DISPONIBLE")
                            .estadoActivo(true)
                            .observaciones("Plataforma con barandas desmontables para carga suelta")
                            .build(),
                    UnidadTransporte.builder()
                            .placaTracto("F4D-550")
                            .marcaModelo("Scania P360 Furgon Refrigerado ThermoKing")
                            .anoFabricacion(2023)
                            .tipoVehiculo("FURGON")
                            .capacidadCargaKg(new BigDecimal("22000.00"))
                            .soatVencimiento(LocalDate.of(2027, 10, 18))
                            .citvVencimiento(LocalDate.of(2027, 11, 10))
                            .kilometrajeActual(85000)
                            .estadoOperativo("DISPONIBLE")
                            .estadoActivo(true)
                            .observaciones("Furgón reefer bi-temperatura certificado Senasa")
                            .build(),
                    UnidadTransporte.builder()
                            .placaTracto("F5E-660")
                            .marcaModelo("Hino 500 Series Furgon Seco")
                            .anoFabricacion(2022)
                            .tipoVehiculo("FURGON")
                            .capacidadCargaKg(new BigDecimal("18000.00"))
                            .soatVencimiento(LocalDate.of(2027, 6, 25))
                            .citvVencimiento(LocalDate.of(2027, 7, 30))
                            .kilometrajeActual(110000)
                            .estadoOperativo("DISPONIBLE")
                            .estadoActivo(true)
                            .observaciones("Furgón cerrado con rampa hidráulica de 2.5T")
                            .build(),
                    UnidadTransporte.builder()
                            .placaTracto("B7G-110")
                            .marcaModelo("Isuzu Forward 12T Baranda Alta")
                            .anoFabricacion(2021)
                            .tipoVehiculo("CAMION_BARANDA")
                            .capacidadCargaKg(new BigDecimal("12000.00"))
                            .soatVencimiento(LocalDate.of(2027, 9, 5))
                            .citvVencimiento(LocalDate.of(2027, 10, 1))
                            .kilometrajeActual(130000)
                            .estadoOperativo("DISPONIBLE")
                            .estadoActivo(true)
                            .observaciones("Camión baranda para reparto urbano y consolidado")
                            .build()
            ));
            log.info("Parque automotor semilla (8 unidades) inicializado exitosamente.");
        }

        if (conductorRepository.count() == 0) {
            conductorRepository.saveAll(List.of(
                    construirConductor("transporte", "Raul", "Cardenas Melendez", "60123456", "+51 987 667 001",
                            "A-IIIc", "Q60123456", LocalDate.of(2028, 5, 15),
                            "Conductor master con certificación MATPEL y 15 años de experiencia"),
                    construirConductor("ebravor", "Enrique", "Bravo Rojas", "60234567", "+51 987 667 002",
                            "A-IIIc", "Q60234567", LocalDate.of(2027, 12, 10),
                            "Especialista en tractos de alto tonelaje Callao-Sur"),
                    construirConductor("wgalarza", "Wilmer", "Galarza Silva", "60345678", "+51 987 667 003",
                            "A-IIIb", "Q60345678", LocalDate.now().plusDays(20),
                            "Alerta preventiva: Brevete profesional próximo a revalidar"),
                    construirConductor("osanchez", "Oscar", "Sanchez Huamanchumo", "60456789", "+51 987 667 004",
                            "A-IIIc", "Q60456789", LocalDate.of(2028, 3, 22),
                            "Operador de furgón refrigerado y control de cadena de frío"),
                    construirConductor("fparedes", "Felix", "Paredes Valles", "60567890", "+51 987 667 005",
                            "A-IIIc", "Q60567890", LocalDate.of(2027, 9, 30),
                            "Rutas interprovinciales y puertos norte (Paita / Salaverry)"),
                    construirConductor("jcastaneda", "Juan", "Castaneda Luna", "60678901", "+51 987 667 006",
                            "A-IIIb", "Q60678901", LocalDate.of(2028, 1, 14),
                            "Distribución local en parques industriales de Lima")
            ));
            log.info("Directorio de conductores semilla (6 choferes) inicializado exitosamente.");
        }

        // Semilla de despachos de transporte demostrativos (4 estados operativos)
        if (despachoTransporteRepository.count() == 0) {
            Cliente camposol = clienteRepository.findByNumeroDocumento("20370146994").orElse(null);
            Cliente lg = clienteRepository.findByNumeroDocumento("20511894520").orElse(camposol);
            Cliente danper = clienteRepository.findByNumeroDocumento("20440058771").orElse(camposol);
            Cliente samsung = clienteRepository.findByNumeroDocumento("20300263578").orElse(lg);
            Usuario admin = usuarioRepository.findByUsername("admin").orElse(null);
            List<UnidadTransporte> unidades = unidadTransporteRepository.findAll();
            List<Conductor> conductores = conductorRepository.findAll();

            if (camposol != null && admin != null && !unidades.isEmpty() && !conductores.isEmpty()) {
                Usuario comercial = usuarioRepository.findByUsername("comercial").orElse(admin);
                Usuario contador = usuarioRepository.findByUsername("contador").orElse(admin);
                Usuario agente = usuarioRepository.findByUsername("agente").orElse(admin);

                CatalogoPuerto puertoCallao = puertoRepository.findByCodigoPuerto("PECAL-DPW").orElse(null);
                CatalogoPuerto puertoShanghai = puertoRepository.findByCodigoPuerto("CNSHA").orElse(puertoCallao);
                CatalogoTipoCarga cargaFcl = tipoCargaRepository.findByCodigo("CARGA_FCL").orElse(null);
                CatalogoTipoCarga cargaReefer = tipoCargaRepository.findByCodigo("CARGA_REEFER").orElse(cargaFcl);

                Expediente exp1 = crearExpedienteConPagoSiNoExiste(new SeedOperacionSpec(
                        "EXP-2026-0001", "COT-2026-0001", "PAG-COT-2026-0001",
                        camposol, comercial, contador, agente, cargaReefer,
                        "EXPORTACION_DEFINITIVA", puertoCallao, puertoShanghai,
                        "La Libertad", "Virú", "Callao", "Callao",
                        new BigDecimal("1450.00"), new BigDecimal("48000.00"), new BigDecimal("3200.00"),
                        new BigDecimal("450.00"), new BigDecimal("650.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("53750.00"), "BCP", "BCP Corporativo", "OP-2026-1001",
                        "118-2026-10-001234", "MSCU9876543", "SUNAT-09871", EstadoExpediente.LEVANTE_AUTORIZADO
                ));
                Expediente exp2 = crearExpedienteConPagoSiNoExiste(new SeedOperacionSpec(
                        "EXP-2026-0002", "COT-2026-0002", "PAG-COT-2026-0002",
                        danper, comercial, contador, agente, cargaReefer,
                        "EXPORTACION_DEFINITIVA", puertoCallao, puertoShanghai,
                        "La Libertad", "Trujillo", "Callao", "Callao",
                        new BigDecimal("1350.00"), new BigDecimal("52000.00"), new BigDecimal("3400.00"),
                        new BigDecimal("500.00"), new BigDecimal("700.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("57950.00"), "BBVA Perú", "BBVA Corporativo", "OP-2026-1002",
                        "118-2026-10-001235", "TGHU1234567", "SUNAT-09872", EstadoExpediente.LEVANTE_AUTORIZADO
                ));
                Expediente exp3 = crearExpedienteConPagoSiNoExiste(new SeedOperacionSpec(
                        "EXP-2026-0003", "COT-2026-0003", "PAG-COT-2026-0003",
                        samsung, comercial, contador, agente, cargaFcl,
                        "IMPORTACION_DEFINITIVA", puertoShanghai, puertoCallao,
                        "Callao", "Callao", "Lima", "San Isidro",
                        new BigDecimal("850.00"), new BigDecimal("95000.00"), new BigDecimal("4200.00"),
                        new BigDecimal("950.00"), new BigDecimal("1100.00"), new BigDecimal("3.50"), new BigDecimal("18500.00"),
                        new BigDecimal("120600.00"), "Scotiabank", "BCP Corporativo", "OP-2026-1003",
                        "118-2026-10-001236", "CMAU5566778", "SUNAT-09873", EstadoExpediente.LEVANTE_AUTORIZADO
                ));
                Expediente exp4 = crearExpedienteConPagoSiNoExiste(new SeedOperacionSpec(
                        "EXP-2026-0004", "COT-2026-0004", "PAG-COT-2026-0004",
                        lg, comercial, contador, agente, cargaFcl,
                        "IMPORTACION_DEFINITIVA", puertoShanghai, puertoCallao,
                        "Callao", "Callao", "Lima", "Lurín",
                        new BigDecimal("1100.00"), new BigDecimal("72000.00"), new BigDecimal("3800.00"),
                        new BigDecimal("720.00"), new BigDecimal("900.00"), new BigDecimal("3.50"), new BigDecimal("14200.00"),
                        new BigDecimal("92720.00"), "Interbank", "BBVA Corporativo", "OP-2026-1004",
                        "118-2026-10-001237", "EGLV9988776", "SUNAT-09874", EstadoExpediente.CERRADO
                ));

                UnidadTransporte u1 = unidadTransporteRepository.findByPlacaTracto("V1A-780").orElse(unidades.get(0));
                UnidadTransporte u2 = unidadTransporteRepository.findByPlacaTracto("V2B-890")
                        .orElse(unidades.size() > 1 ? unidades.get(1) : u1);
                UnidadTransporte u6 = unidadTransporteRepository.findByPlacaTracto("F4D-550")
                        .orElse(unidades.size() > 5 ? unidades.get(5) : u1);
                Conductor c1 = conductorRepository.findByNumeroLicencia("Q60123456").orElse(conductores.get(0));
                Conductor c2 = conductorRepository.findByNumeroLicencia("Q60234567")
                        .orElse(conductores.size() > 1 ? conductores.get(1) : c1);
                Conductor c4 = conductorRepository.findByNumeroLicencia("Q60456789")
                        .orElse(conductores.size() > 3 ? conductores.get(3) : c1);

                // 1. PENDIENTE_ASIGNACION (Exportación: Planta Camposol -> Puerto Callao)
                despachoTransporteRepository.save(DespachoTransporte.builder()
                        .expediente(exp1)
                        .codigoTracking("TRK-2026-0001")
                        .tipoTraslado("RECOJO_EXPORTACION")
                        .direccionPartida("Almacén Central Camposol, Panamericana Norte Km 497, Chao")
                        .direccionLlegada("Terminal Portuario Muelle Sur (DP World), Callao")
                        .distritoLlegada("Callao")
                        .contactoRecepcion("Ing. Javier Morales (+51 944 112 233)")
                        .fechaProgramada(LocalDateTime.now().plusDays(1))
                        .estadoTransporte("PENDIENTE_ASIGNACION")
                        .observaciones("Pendiente asignación de tracto refrigerado para recojo de exportación en planta y traslado a zona primaria Callao")
                        .build());

                // 2. PROGRAMADO (Exportación: Planta Danper -> Puerto Callao)
                despachoTransporteRepository.save(DespachoTransporte.builder()
                        .expediente(exp2)
                        .codigoTracking("TRK-2026-0002")
                        .tipoTraslado("RECOJO_EXPORTACION")
                        .unidad(u2)
                        .conductor(c2)
                        .guiaRemisionTransporte("GRT-001-000142")
                        .direccionPartida("Planta Agroindustrial Danper, Mz L Lote 5, Trujillo")
                        .direccionLlegada("Terminal Portuario Muelle Sur (DP World), Callao")
                        .distritoLlegada("Callao")
                        .contactoRecepcion("Sr. Carlos Benites (+51 944 223 344)")
                        .fechaProgramada(LocalDateTime.now().plusDays(2))
                        .estadoTransporte("PROGRAMADO")
                        .observaciones("Programado para recojo de contenedor refrigerado en planta Trujillo con destino al puerto del Callao")
                        .build());

                // 3. EN_RUTA (Importación: Puerto Callao -> Centro de Distribución Samsung)
                u1.setEstadoOperativo("EN_RUTA");
                unidadTransporteRepository.save(u1);
                c1.setEstadoOperativo("EN_RUTA");
                conductorRepository.save(c1);

                despachoTransporteRepository.save(DespachoTransporte.builder()
                        .expediente(exp3)
                        .codigoTracking("TRK-2026-0003")
                        .tipoTraslado("ENTREGA_IMPORTACION")
                        .unidad(u1)
                        .conductor(c1)
                        .guiaRemisionTransporte("GRT-001-000140")
                        .direccionPartida("Depósito Aduanero DP World Callao")
                        .direccionLlegada("Centro de Distribución Samsung, Av. Elmer Faucett 3450")
                        .distritoLlegada("Callao")
                        .contactoRecepcion("Lic. Patricia Vega (+51 988 554 433)")
                        .fechaProgramada(LocalDateTime.now())
                        .fechaSalidaRuta(LocalDateTime.now().minusHours(2))
                        .estadoTransporte("EN_RUTA")
                        .detalleIncidencia("Tránsito fluido por Av. Néstor Gambetta")
                        .observaciones("Despacho con escolta de seguridad satelital")
                        .build());

                // 4. ENTREGADO (Importación: Puerto Callao -> Almacén Central LG)
                despachoTransporteRepository.save(DespachoTransporte.builder()
                        .expediente(exp4)
                        .codigoTracking("TRK-2026-0004")
                        .tipoTraslado("ENTREGA_IMPORTACION")
                        .unidad(u6)
                        .conductor(c4)
                        .guiaRemisionTransporte("GRT-001-000138")
                        .direccionPartida("Depósito Aduanero APM Terminals Callao")
                        .direccionLlegada("Almacén Central LG Electronics, Panamericana Sur Km 29.5")
                        .distritoLlegada("Lurín")
                        .contactoRecepcion("Ing. Roberto Huamán (+51 977 665 544)")
                        .receptorNombre("Roberto Huamán Cárdenas")
                        .receptorDni("44889911")
                        .fechaProgramada(LocalDateTime.now().minusDays(1))
                        .fechaSalidaRuta(LocalDateTime.now().minusDays(1))
                        .fechaEntregaConforme(LocalDateTime.now().minusHours(18))
                        .estadoTransporte("ENTREGADO")
                        .detalleIncidencia("Entrega conforme sin observaciones")
                        .observaciones("GRE-T firmada electrónicamente y precintos verificados")
                        .build());

                log.info("Despachos operativos semilla (4 estados de ciclo de vida) inicializados exitosamente.");
            }
        }
    }

    private Conductor construirConductor(String username, String nombres, String apellidos, String numeroDocumento,
                                         String telefono, String tipoLicencia, String numeroLicencia,
                                         LocalDate fechaVencimientoLicencia, String observaciones) {
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
        return Conductor.builder()
                .usuario(usuario)
                .nombres(nombres)
                .apellidos(apellidos)
                .numeroDocumento(numeroDocumento)
                .telefono(telefono)
                .tipoLicencia(tipoLicencia)
                .numeroLicencia(numeroLicencia)
                .fechaVencimientoLicencia(fechaVencimientoLicencia)
                .estadoOperativo("DISPONIBLE")
                .estadoActivo(true)
                .observaciones(observaciones)
                .build();
    }

    private Expediente crearExpedienteConPagoSiNoExiste(SeedOperacionSpec spec) {
        return expedienteRepository.findByCodigoExpediente(spec.codigoExpediente()).orElseGet(() -> {
            Cotizacion cot = cotizacionRepository.findByCodigoCotizacion(spec.codigoCotizacion()).orElseGet(() ->
                    cotizacionRepository.save(Cotizacion.builder()
                            .codigoCotizacion(spec.codigoCotizacion())
                            .cliente(spec.cliente())
                            .usuarioComercial(spec.usuarioComercial())
                            .tipoCarga(spec.tipoCarga())
                            .regimenAduanero(spec.regimenAduanero())
                            .puertoEnvio(spec.puertoEnvio())
                            .puertoDestino(spec.puertoDestino())
                            .servicioTransporte(true)
                            .origenDepartamento(spec.origenDepartamento())
                            .origenProvincia(spec.origenProvincia())
                            .destinoDepartamento(spec.destinoDepartamento())
                            .destinoProvincia(spec.destinoProvincia())
                            .costoTransporteInterno(spec.costoTransporteInterno())
                            .valorFob(spec.valorFob())
                            .montoFleteEstimado(spec.montoFlete())
                            .montoSeguroEstimado(spec.montoSeguro())
                            .montoHonorariosEstimado(spec.montoHonorarios())
                            .tasaPercepcion(spec.tasaPercepcion())
                            .montoTributosEstimado(spec.montoTributos())
                            .totalCotizado(spec.totalCotizado())
                            .fechaEmision(LocalDate.now().minusDays(3))
                            .estado(EstadoCotizacion.APROBADA)
                            .estadoPago(EstadoPagoCotizacion.PAGADO_TOTAL.name())
                            .totalAbonado(spec.totalCotizado())
                            .saldoPendiente(BigDecimal.ZERO)
                            .build())
            );

            if (!cotizacionPagoRepository.existsByCodigoPago(spec.codigoPago())) {
                BigDecimal equivalentePen = spec.totalCotizado()
                        .multiply(TIPO_CAMBIO_SEMILLA)
                        .setScale(2, RoundingMode.HALF_UP);
                cotizacionPagoRepository.save(CotizacionPago.builder()
                        .cotizacion(cot)
                        .codigoPago(spec.codigoPago())
                        .tipoAbono(TipoAbonoCotizacion.PAGO_TOTAL)
                        .montoAbonado(spec.totalCotizado())
                        .moneda("USD")
                        .tipoCambio(TIPO_CAMBIO_SEMILLA)
                        .montoEquivalentePen(equivalentePen)
                        .metodoPago("TRANSFERENCIA_BANCARIA")
                        .bancoOrigen(spec.bancoOrigen())
                        .bancoDestino(spec.bancoDestino())
                        .numeroOperacion(spec.numeroOperacion())
                        .fechaOperacion(LocalDate.now().minusDays(2))
                        .estadoPago(EstadoPagoCotizacion.APROBADO)
                        .usuarioFinanzas(spec.usuarioFinanzas())
                        .fechaValidacion(LocalDateTime.now().minusDays(2))
                        .observacionFinanzas("Abono total verificado en extracto bancario. Habilitado para apertura de expediente.")
                        .usuarioRegistro(spec.usuarioComercial())
                        .fechaRegistro(LocalDateTime.now().minusDays(2))
                        .build());
            }

            return expedienteRepository.save(Expediente.builder()
                    .codigoExpediente(spec.codigoExpediente())
                    .cotizacion(cot)
                    .cliente(spec.cliente())
                    .tipoCarga(spec.tipoCarga())
                    .agenteAduanero(spec.agenteAduanero())
                    .numeroDam(spec.numeroDam())
                    .canalSunat(CanalSunat.VERDE)
                    .numeroContenedor(spec.numeroContenedor())
                    .precintoAduanero(spec.precintoAduanero())
                    .estadoExpediente(spec.estadoExpediente())
                    .build());
        });
    }

    private record SeedOperacionSpec(
            String codigoExpediente,
            String codigoCotizacion,
            String codigoPago,
            Cliente cliente,
            Usuario usuarioComercial,
            Usuario usuarioFinanzas,
            Usuario agenteAduanero,
            CatalogoTipoCarga tipoCarga,
            String regimenAduanero,
            CatalogoPuerto puertoEnvio,
            CatalogoPuerto puertoDestino,
            String origenDepartamento,
            String origenProvincia,
            String destinoDepartamento,
            String destinoProvincia,
            BigDecimal costoTransporteInterno,
            BigDecimal valorFob,
            BigDecimal montoFlete,
            BigDecimal montoSeguro,
            BigDecimal montoHonorarios,
            BigDecimal tasaPercepcion,
            BigDecimal montoTributos,
            BigDecimal totalCotizado,
            String bancoOrigen,
            String bancoDestino,
            String numeroOperacion,
            String numeroDam,
            String numeroContenedor,
            String precintoAduanero,
            EstadoExpediente estadoExpediente
    ) {}

    private void asegurarCuentaDemoPortal(String ruc, String razonSocial, String representante,
                                          String direccion, String correo, String telefono) {
        Cliente cliente = clienteRepository.findByNumeroDocumento(ruc).orElseGet(() -> {
            Cliente nuevo = Cliente.builder()
                    .tipoDocumento("RUC")
                    .numeroDocumento(ruc)
                    .razonSocial(razonSocial)
                    .contactoRepresentante(representante)
                    .direccionFiscal(direccion)
                    .correoContacto(correo)
                    .telefonoContacto(telefono)
                    .estadoCliente("CLIENTE")
                    .estadoActivo(true)
                    .build();
            log.info("Cliente demo '{}' ({}) creado para el Portal B2B.", razonSocial, ruc);
            return clienteRepository.save(nuevo);
        });

        if (usuarioClienteRepository.findByCliente_IdCliente(cliente.getIdCliente()).isEmpty()
                && !usuarioClienteRepository.existsByUsername(ruc)) {
            usuarioClienteRepository.save(UsuarioCliente.builder()
                    .cliente(cliente)
                    .username(ruc)
                    .passwordHash(passwordEncoder.encode("Password123!"))
                    .estadoActivo(true)
                    .debeCambiarPassword(false)
                    .build());
            log.info("Cuenta demo de Portal B2B creada exitosamente para RUC: {}", ruc);
        }
    }
}
