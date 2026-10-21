package pe.sga.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.sga.entity.CatalogoPuerto;
import pe.sga.entity.CatalogoTipoCarga;
import pe.sga.entity.Rol;
import pe.sga.entity.Sede;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.repository.CatalogoPuertoRepository;
import pe.sga.repository.CatalogoTipoCargaRepository;
import pe.sga.repository.RolRepository;
import pe.sga.repository.SedeRepository;
import pe.sga.repository.UsuarioRepository;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final SedeRepository sedeRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoPuertoRepository puertoRepository;
    private final CatalogoTipoCargaRepository tipoCargaRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            RolRepository rolRepository,
            SedeRepository sedeRepository,
            UsuarioRepository usuarioRepository,
            CatalogoPuertoRepository puertoRepository,
            CatalogoTipoCargaRepository tipoCargaRepository,
            PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.sedeRepository = sedeRepository;
        this.usuarioRepository = usuarioRepository;
        this.puertoRepository = puertoRepository;
        this.tipoCargaRepository = tipoCargaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Iniciando verificación e inicialización mínima del sistema SGA...");

        // 1. Roles del sistema según enum RolUsuario (con sus nombres y etiquetas normativas)
        for (RolUsuario rolEnum : RolUsuario.values()) {
            if (!rolRepository.existsByNombreRol(rolEnum)) {
                rolRepository.save(Rol.builder()
                        .nombreRol(rolEnum)
                        .descripcion(rolEnum.getEtiqueta())
                        .estadoActivo(true)
                        .build());
            }
        }
        log.info("Roles del sistema verificados.");

        // 2. Sede única de prueba
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

        // 3. Catálogo de puertos: un único puerto de prueba
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

        // 4. Catálogo de tipos de carga: un único tipo de carga de prueba
        if (tipoCargaRepository.count() == 0) {
            tipoCargaRepository.save(CatalogoTipoCarga.builder()
                    .codigo("PRUEBA")
                    .nombre("Prueba")
                    .estado("ACTIVO")
                    .build());
            log.info("Catálogo de tipos de carga: registrado tipo 'Prueba'.");
        }

        // 5. Usuario Administrador único (admin)
        if (!usuarioRepository.existsByUsername("admin")) {
            Rol rolAdminTi = rolRepository.findByNombreRol(RolUsuario.ADMINISTRADOR_TI)
                    .orElseThrow(() -> new IllegalStateException("El rol ADMINISTRADOR_TI no fue encontrado en la base de datos"));

            usuarioRepository.save(Usuario.builder()
                    .rol(rolAdminTi)
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

        log.info("Inicialización limpia del sistema SGA completada exitosamente.");
    }
}
