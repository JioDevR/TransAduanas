package pe.sga.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import pe.sga.entity.CatalogoPuerto;
import pe.sga.entity.CatalogoTipoCarga;
import pe.sga.entity.Cliente;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.CotizacionPago;
import pe.sga.entity.Expediente;
import pe.sga.entity.Sede;
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
import pe.sga.repository.CotizacionPagoRepository;
import pe.sga.repository.CotizacionRepository;
import pe.sga.repository.ExpedienteRepository;
import pe.sga.repository.SedeRepository;
import pe.sga.repository.UsuarioClienteRepository;
import pe.sga.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@ConditionalOnProperty(
    name = "app.data-initializer.enabled",
    havingValue = "true",
    matchIfMissing = true
)

public class DataInitializer implements CommandLineRunner {

    private static final BigDecimal TIPO_CAMBIO_SEMILLA = new BigDecimal("3.7500");

    private final SedeRepository sedeRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoPuertoRepository puertoRepository;
    private final CatalogoTipoCargaRepository tipoCargaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioClienteRepository usuarioClienteRepository;
    private final CotizacionRepository cotizacionRepository;
    private final CotizacionPagoRepository cotizacionPagoRepository;
    private final ExpedienteRepository expedienteRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            SedeRepository sedeRepository,
            UsuarioRepository usuarioRepository,
            CatalogoPuertoRepository puertoRepository,
            CatalogoTipoCargaRepository tipoCargaRepository,
            ClienteRepository clienteRepository,
            UsuarioClienteRepository usuarioClienteRepository,
            CotizacionRepository cotizacionRepository,
            CotizacionPagoRepository cotizacionPagoRepository,
            ExpedienteRepository expedienteRepository,
            PasswordEncoder passwordEncoder) {
        this.sedeRepository = sedeRepository;
        this.usuarioRepository = usuarioRepository;
        this.puertoRepository = puertoRepository;
        this.tipoCargaRepository = tipoCargaRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioClienteRepository = usuarioClienteRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.cotizacionPagoRepository = cotizacionPagoRepository;
        this.expedienteRepository = expedienteRepository;
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

        // 6. Operaciones semilla coherentes con db/seed.sql
        inicializarOperacionesDemo();

        log.info("Inicialización limpia del sistema SGA completada exitosamente.");
    }

    private void inicializarOperacionesDemo() {
        if (expedienteRepository.count() == 0) {
            Cliente camposol = clienteRepository.findByNumeroDocumento("20370146994").orElse(null);
            Cliente lg = clienteRepository.findByNumeroDocumento("20511894520").orElse(camposol);
            Cliente danper = clienteRepository.findByNumeroDocumento("20440058771").orElse(camposol);
            Cliente samsung = clienteRepository.findByNumeroDocumento("20300263578").orElse(lg);
            Usuario admin = usuarioRepository.findByUsername("admin").orElse(null);

            if (camposol != null && admin != null) {
                Usuario comercial = usuarioRepository.findByUsername("comercial").orElse(admin);
                Usuario contador = usuarioRepository.findByUsername("contador").orElse(admin);
                Usuario agente = usuarioRepository.findByUsername("agente").orElse(admin);

                CatalogoPuerto puertoCallao = puertoRepository.findByCodigoPuerto("PECAL-DPW").orElse(null);
                CatalogoPuerto puertoShanghai = puertoRepository.findByCodigoPuerto("CNSHA").orElse(puertoCallao);
                CatalogoTipoCarga cargaFcl = tipoCargaRepository.findByCodigo("CARGA_FCL").orElse(null);
                CatalogoTipoCarga cargaReefer = tipoCargaRepository.findByCodigo("CARGA_REEFER").orElse(cargaFcl);

                crearExpedienteConPagoSiNoExiste(new SeedOperacionSpec(
                        "EXP-2026-0001", "COT-2026-0001", "PAG-COT-2026-0001",
                        camposol, comercial, contador, agente, cargaReefer,
                        "EXPORTACION_DEFINITIVA", puertoCallao, puertoShanghai,
                        "La Libertad", "Virú", "Callao", "Callao",
                        new BigDecimal("1450.00"), new BigDecimal("48000.00"), new BigDecimal("3200.00"),
                        new BigDecimal("450.00"), new BigDecimal("650.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("53750.00"), "BCP", "BCP Corporativo", "OP-2026-1001",
                        "118-2026-10-001234", "MSCU9876543", "SUNAT-09871", EstadoExpediente.LEVANTE_AUTORIZADO,
                        "RECOJO_EXPORTACION",
                        "Almacén Central Camposol, Panamericana Norte Km 497, Chao",
                        "Terminal Portuario Muelle Sur (DP World), Callao",
                        "Callao", "Ing. Javier Morales (+51 944 112 233)",
                        LocalDateTime.now().plusDays(1),
                        "Solicitud de transporte externo para recojo de exportación en planta"
                ));
                crearExpedienteConPagoSiNoExiste(new SeedOperacionSpec(
                        "EXP-2026-0002", "COT-2026-0002", "PAG-COT-2026-0002",
                        danper, comercial, contador, agente, cargaReefer,
                        "EXPORTACION_DEFINITIVA", puertoCallao, puertoShanghai,
                        "La Libertad", "Trujillo", "Callao", "Callao",
                        new BigDecimal("1350.00"), new BigDecimal("52000.00"), new BigDecimal("3400.00"),
                        new BigDecimal("500.00"), new BigDecimal("700.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("57950.00"), "BBVA Perú", "BBVA Corporativo", "OP-2026-1002",
                        "118-2026-10-001235", "TGHU1234567", "SUNAT-09872", EstadoExpediente.LEVANTE_AUTORIZADO,
                        "RECOJO_EXPORTACION",
                        "Planta Agroindustrial Danper, Mz L Lote 5, Trujillo",
                        "Terminal Portuario Muelle Sur (DP World), Callao",
                        "Callao", "Sr. Carlos Benites (+51 944 223 344)",
                        LocalDateTime.now().plusDays(2),
                        "Programado con operador externo para recojo de contenedor refrigerado"
                ));
                crearExpedienteConPagoSiNoExiste(new SeedOperacionSpec(
                        "EXP-2026-0003", "COT-2026-0003", "PAG-COT-2026-0003",
                        samsung, comercial, contador, agente, cargaFcl,
                        "IMPORTACION_DEFINITIVA", puertoShanghai, puertoCallao,
                        "Callao", "Callao", "Lima", "San Isidro",
                        new BigDecimal("850.00"), new BigDecimal("95000.00"), new BigDecimal("4200.00"),
                        new BigDecimal("950.00"), new BigDecimal("1100.00"), new BigDecimal("3.50"), new BigDecimal("18500.00"),
                        new BigDecimal("120600.00"), "Scotiabank", "BCP Corporativo", "OP-2026-1003",
                        "118-2026-10-001236", "CMAU5566778", "SUNAT-09873", EstadoExpediente.LEVANTE_AUTORIZADO,
                        "ENTREGA_IMPORTACION",
                        "Depósito Aduanero DP World Callao",
                        "Centro de Distribución Samsung, Av. Elmer Faucett 3450",
                        "Callao", "Lic. Patricia Vega (+51 988 554 433)",
                        LocalDateTime.now(),
                        "Coordinación de entrega con operador de transporte externo"
                ));
                crearExpedienteConPagoSiNoExiste(new SeedOperacionSpec(
                        "EXP-2026-0004", "COT-2026-0004", "PAG-COT-2026-0004",
                        lg, comercial, contador, agente, cargaFcl,
                        "IMPORTACION_DEFINITIVA", puertoShanghai, puertoCallao,
                        "Callao", "Callao", "Lima", "Lurín",
                        new BigDecimal("1100.00"), new BigDecimal("72000.00"), new BigDecimal("3800.00"),
                        new BigDecimal("720.00"), new BigDecimal("900.00"), new BigDecimal("3.50"), new BigDecimal("14200.00"),
                        new BigDecimal("92720.00"), "Interbank", "BBVA Corporativo", "OP-2026-1004",
                        "118-2026-10-001237", "EGLV9988776", "SUNAT-09874", EstadoExpediente.CERRADO,
                        "ENTREGA_IMPORTACION",
                        "Depósito Aduanero APM Terminals Callao",
                        "Almacén Central LG Electronics, Panamericana Sur Km 29.5",
                        "Lurín", "Ing. Roberto Huamán (+51 977 665 544)",
                        LocalDateTime.now().minusDays(1),
                        "Traslado externo coordinado y expediente cerrado conforme"
                ));

                log.info("Operaciones semilla (4 expedientes con solicitud externa) inicializadas exitosamente.");
            }
        }
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
                    .transporteSolicitado(true)
                    .transporteTipoTraslado(spec.transporteTipoTraslado())
                    .transporteDireccionPartida(spec.transporteDireccionPartida())
                    .transporteDireccionLlegada(spec.transporteDireccionLlegada())
                    .transporteDistritoLlegada(spec.transporteDistritoLlegada())
                    .transporteContactoRecepcion(spec.transporteContactoRecepcion())
                    .transporteFechaProgramada(spec.transporteFechaProgramada())
                    .transporteObservaciones(spec.transporteObservaciones())
                    .transporteFechaSolicitud(LocalDateTime.now())
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
            EstadoExpediente estadoExpediente,
            String transporteTipoTraslado,
            String transporteDireccionPartida,
            String transporteDireccionLlegada,
            String transporteDistritoLlegada,
            String transporteContactoRecepcion,
            LocalDateTime transporteFechaProgramada,
            String transporteObservaciones
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
