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
import pe.sga.entity.DespachoTransporte;
import pe.sga.entity.Expediente;
import pe.sga.entity.Sede;
import pe.sga.entity.UnidadTransporte;
import pe.sga.entity.Usuario;
import pe.sga.entity.UsuarioCliente;
import pe.sga.entity.enums.CanalSunat;
import pe.sga.entity.enums.EstadoCotizacion;
import pe.sga.entity.enums.EstadoExpediente;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.repository.CatalogoPuertoRepository;
import pe.sga.repository.CatalogoTipoCargaRepository;
import pe.sga.repository.ClienteRepository;
import pe.sga.repository.ConductorRepository;
import pe.sga.repository.CotizacionRepository;
import pe.sga.repository.DespachoTransporteRepository;
import pe.sga.repository.ExpedienteRepository;
import pe.sga.repository.SedeRepository;
import pe.sga.repository.UnidadTransporteRepository;
import pe.sga.repository.UsuarioClienteRepository;
import pe.sga.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final SedeRepository sedeRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoPuertoRepository puertoRepository;
    private final CatalogoTipoCargaRepository tipoCargaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioClienteRepository usuarioClienteRepository;
    private final UnidadTransporteRepository unidadTransporteRepository;
    private final ConductorRepository conductorRepository;
    private final CotizacionRepository cotizacionRepository;
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
        this.expedienteRepository = expedienteRepository;
        this.despachoTransporteRepository = despachoTransporteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Iniciando verificación e inicialización mínima del sistema SGA...");

        // 1. Sede única de prueba si no existen sedes
        Sede sedePrueba = sedeRepository.findAll().stream().findFirst().orElseGet(() -> {
            Sede nueva = Sede.builder()
                    .codigoSede("SED-PRUEBA")
                    .nombre("Prueba")
                    .tipo("SEDE_CENTRAL")
                    .direccion("Dirección de Prueba")
                    .distrito("Callao")
                    .telefono("+51 1 000-0000")
                    .estado("OPERATIVO")
                    .build();
            log.info("Sede inicial 'Prueba' creada exitosamente.");
            return sedeRepository.save(nueva);
        });

        // 2. Catálogo de puertos: un único puerto de prueba si no existen
        if (puertoRepository.count() == 0) {
            puertoRepository.save(CatalogoPuerto.builder()
                    .codigoPuerto("PRUEBA")
                    .nombre("Prueba")
                    .pais("Perú")
                    .tipo("MARITIMO")
                    .estado("ACTIVO")
                    .build());
            log.info("Catálogo de puertos: registrado puerto 'Prueba'.");
        }

        // 3. Catálogo de tipos de carga: un único tipo de carga de prueba si no existen
        if (tipoCargaRepository.count() == 0) {
            tipoCargaRepository.save(CatalogoTipoCarga.builder()
                    .codigo("PRUEBA")
                    .nombre("Prueba")
                    .estado("ACTIVO")
                    .build());
            log.info("Catálogo de tipos de carga: registrado tipo 'Prueba'.");
        }

        // 4. Usuario Administrador único (admin)
        if (!usuarioRepository.existsByUsername("admin")) {
            usuarioRepository.save(Usuario.builder()
                    .rol(RolUsuario.ADMINISTRADOR_TI)
                    .sede(sedePrueba)
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

        // 6. Parque automotor y conductores semilla de Transporte y Flota
        inicializarTransporteYFlota();

        log.info("Inicialización limpia del sistema SGA completada exitosamente.");
    }

    private void inicializarTransporteYFlota() {
        if (unidadTransporteRepository.count() == 0) {
            unidadTransporteRepository.save(UnidadTransporte.builder()
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
                    .build());

            unidadTransporteRepository.save(UnidadTransporte.builder()
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
                    .build());

            unidadTransporteRepository.save(UnidadTransporte.builder()
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
                    .build());

            unidadTransporteRepository.save(UnidadTransporte.builder()
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
                    .build());

            unidadTransporteRepository.save(UnidadTransporte.builder()
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
                    .build());

            unidadTransporteRepository.save(UnidadTransporte.builder()
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
                    .build());

            log.info("Parque automotor semilla (6 unidades) inicializado exitosamente.");
        }

        if (conductorRepository.count() == 0) {
            Usuario userTransp = usuarioRepository.findByUsername("transporte").orElse(null);

            conductorRepository.save(Conductor.builder()
                    .usuario(userTransp)
                    .nombres("Raul")
                    .apellidos("Cardenas Melendez")
                    .numeroDocumento("60123456")
                    .telefono("+51 987 667 001")
                    .tipoLicencia("A-IIIc")
                    .numeroLicencia("Q60123456")
                    .fechaVencimientoLicencia(LocalDate.of(2028, 5, 15))
                    .estadoOperativo("DISPONIBLE")
                    .estadoActivo(true)
                    .observaciones("Conductor master con certificación MATPEL y 15 años de experiencia")
                    .build());

            conductorRepository.save(Conductor.builder()
                    .nombres("Enrique")
                    .apellidos("Bravo Rojas")
                    .numeroDocumento("60234567")
                    .telefono("+51 987 667 002")
                    .tipoLicencia("A-IIIc")
                    .numeroLicencia("Q60234567")
                    .fechaVencimientoLicencia(LocalDate.of(2027, 12, 10))
                    .estadoOperativo("DISPONIBLE")
                    .estadoActivo(true)
                    .observaciones("Especialista en tractos de alto tonelaje Callao-Sur")
                    .build());

            conductorRepository.save(Conductor.builder()
                    .nombres("Wilmer")
                    .apellidos("Galarza Silva")
                    .numeroDocumento("60345678")
                    .telefono("+51 987 667 003")
                    .tipoLicencia("A-IIIb")
                    .numeroLicencia("Q60345678")
                    .fechaVencimientoLicencia(LocalDate.now().plusDays(20))
                    .estadoOperativo("DISPONIBLE")
                    .estadoActivo(true)
                    .observaciones("Alerta preventiva: Brevete profesional próximo a revalidar en 20 días")
                    .build());

            conductorRepository.save(Conductor.builder()
                    .nombres("Carlos")
                    .apellidos("Santillan Medina")
                    .numeroDocumento("44556677")
                    .telefono("+51 987 667 004")
                    .tipoLicencia("A-IIIc")
                    .numeroLicencia("Q44556677")
                    .fechaVencimientoLicencia(LocalDate.of(2028, 8, 20))
                    .estadoOperativo("DISPONIBLE")
                    .estadoActivo(true)
                    .observaciones("Operador de furgón refrigerado y control de cadena de frío")
                    .build());

            log.info("Directorio de conductores semilla (4 choferes) inicializado exitosamente.");
        }

        // Semilla de despachos de transporte demostrativos (4 estados operativos)
        if (despachoTransporteRepository.count() == 0) {
            Cliente cliente = clienteRepository.findByNumeroDocumento("20370146994").orElse(null);
            Usuario admin = usuarioRepository.findByUsername("admin").orElse(null);
            List<UnidadTransporte> unidades = unidadTransporteRepository.findAll();
            List<Conductor> conductores = conductorRepository.findAll();

            if (cliente != null && admin != null && !unidades.isEmpty() && !conductores.isEmpty()) {
                Expediente exp1 = crearExpedienteSiNoExiste("EXP-2026-0001", "COT-2026-0001", cliente, admin, "118-2026-10-001234", "MSCU9876543");
                Expediente exp2 = crearExpedienteSiNoExiste("EXP-2026-0002", "COT-2026-0002", cliente, admin, "118-2026-10-001235", "TGHU1234567");
                Expediente exp3 = crearExpedienteSiNoExiste("EXP-2026-0003", "COT-2026-0003", cliente, admin, "118-2026-10-001236", "CMAU5566778");
                Expediente exp4 = crearExpedienteSiNoExiste("EXP-2026-0004", "COT-2026-0004", cliente, admin, "118-2026-10-001237", "EGLV9988776");

                UnidadTransporte u1 = unidades.get(0);
                UnidadTransporte u2 = unidades.size() > 1 ? unidades.get(1) : u1;
                UnidadTransporte u3 = unidades.size() > 2 ? unidades.get(2) : u1;
                Conductor c1 = conductores.get(0);
                Conductor c2 = conductores.size() > 1 ? conductores.get(1) : c1;
                Conductor c3 = conductores.size() > 2 ? conductores.get(2) : c1;

                // 1. PENDIENTE_ASIGNACION
                despachoTransporteRepository.save(DespachoTransporte.builder()
                        .expediente(exp1)
                        .codigoTracking("TRK-2026-0001")
                        .tipoTraslado("ENTREGA_IMPORTACION")
                        .direccionPartida("Terminal Portuario Muelle Sur, Callao")
                        .direccionLlegada("Almacén Central Camposol, Panamericana Norte Km 497")
                        .distritoLlegada("Chao")
                        .contactoRecepcion("Ing. Javier Morales (+51 944 112 233)")
                        .fechaProgramada(LocalDateTime.now().plusDays(1))
                        .estadoTransporte("PENDIENTE_ASIGNACION")
                        .observaciones("Pendiente asignación de tracto refrigerado y conductor con MATPEL")
                        .build());

                // 2. PROGRAMADO
                despachoTransporteRepository.save(DespachoTransporte.builder()
                        .expediente(exp2)
                        .codigoTracking("TRK-2026-0002")
                        .tipoTraslado("ENTREGA_IMPORTACION")
                        .unidad(u2)
                        .conductor(c2)
                        .guiaRemisionTransporte("GRT-001-000142")
                        .direccionPartida("Terminal Portuario Muelle Norte APM, Callao")
                        .direccionLlegada("Planta Agroindustrial Danper, Mz L Lote 5")
                        .distritoLlegada("Trujillo")
                        .contactoRecepcion("Sr. Carlos Benites (+51 944 223 344)")
                        .fechaProgramada(LocalDateTime.now().plusDays(2))
                        .estadoTransporte("PROGRAMADO")
                        .observaciones("Programado para recojo de contenedor refrigerado")
                        .build());

                // 3. EN_RUTA
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
                        .direccionLlegada("Centro de Distribución, Av. Elmer Faucett 3450")
                        .distritoLlegada("Callao")
                        .contactoRecepcion("Lic. Patricia Vega (+51 988 554 433)")
                        .fechaProgramada(LocalDateTime.now())
                        .fechaSalidaRuta(LocalDateTime.now().minusHours(2))
                        .estadoTransporte("EN_RUTA")
                        .detalleIncidencia("Tránsito fluido por Av. Néstor Gambetta")
                        .observaciones("Despacho con escolta de seguridad satelital")
                        .build());

                // 4. ENTREGADO
                despachoTransporteRepository.save(DespachoTransporte.builder()
                        .expediente(exp4)
                        .codigoTracking("TRK-2026-0004")
                        .tipoTraslado("ENTREGA_IMPORTACION")
                        .unidad(u3)
                        .conductor(c3)
                        .guiaRemisionTransporte("GRT-001-000138")
                        .direccionPartida("Depósito Aduanero APM Terminals Callao")
                        .direccionLlegada("Almacén Central, Panamericana Sur Km 29.5")
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

    private Expediente crearExpedienteSiNoExiste(String codExp, String codCot, Cliente cliente, Usuario agente, String dam, String contenedor) {
        return expedienteRepository.findByCodigoExpediente(codExp).orElseGet(() -> {
            Cotizacion cot = cotizacionRepository.findByCodigoCotizacion(codCot).orElseGet(() ->
                    cotizacionRepository.save(Cotizacion.builder()
                            .codigoCotizacion(codCot)
                            .cliente(cliente)
                            .usuarioComercial(agente)
                            .servicioTransporte(true)
                            .fechaEmision(LocalDate.now())
                            .estado(EstadoCotizacion.APROBADA)
                            .build())
            );

            return expedienteRepository.save(Expediente.builder()
                    .codigoExpediente(codExp)
                    .cotizacion(cot)
                    .cliente(cliente)
                    .agenteAduanero(agente)
                    .numeroDam(dam)
                    .canalSunat(CanalSunat.VERDE)
                    .numeroContenedor(contenedor)
                    .estadoExpediente(EstadoExpediente.LEVANTE_AUTORIZADO)
                    .build());
        });
    }

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
