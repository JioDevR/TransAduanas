package pe.sga.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.sga.entity.CatalogoPuerto;
import pe.sga.entity.CatalogoTipoCarga;
import pe.sga.entity.Cliente;
import pe.sga.entity.Sede;
import pe.sga.entity.Usuario;
import pe.sga.entity.UsuarioCliente;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.repository.CatalogoPuertoRepository;
import pe.sga.repository.CatalogoTipoCargaRepository;
import pe.sga.repository.ClienteRepository;
import pe.sga.repository.SedeRepository;
import pe.sga.repository.UsuarioClienteRepository;
import pe.sga.repository.UsuarioRepository;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final SedeRepository sedeRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoPuertoRepository puertoRepository;
    private final CatalogoTipoCargaRepository tipoCargaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioClienteRepository usuarioClienteRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            SedeRepository sedeRepository,
            UsuarioRepository usuarioRepository,
            CatalogoPuertoRepository puertoRepository,
            CatalogoTipoCargaRepository tipoCargaRepository,
            ClienteRepository clienteRepository,
            UsuarioClienteRepository usuarioClienteRepository,
            PasswordEncoder passwordEncoder) {
        this.sedeRepository = sedeRepository;
        this.usuarioRepository = usuarioRepository;
        this.puertoRepository = puertoRepository;
        this.tipoCargaRepository = tipoCargaRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioClienteRepository = usuarioClienteRepository;
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

        log.info("Inicialización limpia del sistema SGA completada exitosamente.");
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
