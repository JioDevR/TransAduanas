# 2. Inventario de Dependencias y Librerías del Backend

**Sistema de Gestión Aduanera - Transaduanas**  
*Explicación Técnica y Funcional de cada Componente del `pom.xml`*

---

## Índice del Capítulo
1. [Matriz General del Stack Tecnológico](#2-matriz-general-del-stack-tecnológico)
2. [Catálogo Detallado de Librerías y Dependencias](#3-catálogo-detallado-de-librerías-y-dependencias)
   - [2.1 Ecosistema Spring Boot 4.1.2 (Starters Oficiales)](#31-ecosistema-spring-boot-412-starters-oficiales)
   - [2.2 Persistencia y Base de Datos (MySQL Connector & JPA)](#32-persistencia-y-base-de-datos-mysql-connector--jpa)
   - [2.3 Seguridad Criptográfica y JWT (JJWT 0.13.0)](#33-seguridad-criptográfica-y-jwt-jjwt-0130)
   - [2.4 Productividad y Calidad de Código (Project Lombok)](#34-productividad-y-calidad-de-código-project-lombok)
   - [2.5 Motor Híbrido de Reportes y Documentos PDF](#35-motor-híbrido-de-reportes-y-documentos-pdf)
3. [Plugins del Ciclo de Compilación de Maven](#4-plugins-del-ciclo-de-compilación-de-maven)

---

## 1. Matriz General del Stack Tecnológico

| Componente | Versión | Tipo de Licencia | Rol en la Arquitectura |
| :--- | :---: | :---: | :--- |
| **Java Platform (JDK)** | **21 LTS** | Eclipse OpenJ9 / Temurin | Lenguaje base. Proporciona *Virtual Threads*, records y mejoras de rendimiento de memoria. |
| **Spring Boot Parent** | **4.1.2** | Apache 2.0 | Framework empresarial maestro para autoconfiguración, inyección de dependencias y servidor embebido. |
| **MySQL Connector/J** | Runtime | GPL v2 / FOSS Exception | Controlador JDBC para comunicar Java con el motor MySQL 8.4 InnoDB. |
| **JJWT (Java JWT)** | **0.13.0** | Apache 2.0 | Librería para generar, firmar con HMAC-SHA256 y validar tokens de sesión Bearer. |
| **Project Lombok** | **1.18.x** | MIT | Generador de código en tiempo de compilación (elimina getters, setters y constructores repetitivos). |
| **JasperReports** | **7.0.8** | LGPL v3 | Motor dinámico para compilar y exportar padrones y certificados aduaneros oficiales en PDF. |
| **Thymeleaf** | Starter | Apache 2.0 | Motor de plantillas HTML5 server-side para diseño visual de reportes ejecutivos. |
| **OpenHTMLtoPDF** | **1.0.10** | LGPL v2.1 | Convertidor de HTML5 + CSS3 a documentos PDF corporativos de alta fidelidad. |

---

## 2. Catálogo Detallado de Librerías y Dependencias

A continuación se detalla cada dependencia incluida en el archivo [`backend/pom.xml`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/pom.xml):

### 2.1 Ecosistema Spring Boot 4.1.2 (Starters Oficiales)

#### A. `spring-boot-starter-webmvc`
- **Artefacto:** `org.springframework.boot:spring-boot-starter-webmvc`
- **¿Para qué sirve? (Explicación Funcional):** Es el encargado de recibir las peticiones de los navegadores web (como cuando alguien hace clic en "Crear Cotización" o "Iniciar Sesión"), interpretar los datos enviados y responder con la información adecuada en formato JSON.
- **Detalle Técnico:** Incluye el servidor HTTP embebido (Apache Tomcat), el framework Spring MVC para despachar URLs mediante `@RestController`, `@GetMapping`, `@PostMapping` y la biblioteca **Jackson** para serialización y deserialización bidireccional entre objetos Java y JSON.
- **¿Qué pasaría si no estuviera?** El backend sería incapaz de comunicarse con el frontend; no podría abrir ningún puerto HTTP ni responder peticiones web.

#### B. `spring-boot-starter-data-jpa`
- **Artefacto:** `org.springframework.boot:spring-boot-starter-data-jpa`
- **¿Para qué sirve? (Explicación Funcional):** Es el "traductor universal" entre el código Java y las tablas de la base de datos MySQL. En lugar de tener que escribir cientos de líneas de código SQL manual propenso a errores, JPA permite guardar, buscar y actualizar información usando directamente objetos Java.
- **Detalle Técnico:** Proporciona la especificación JPA respaldada por **Hibernate ORM**, el pool de conexiones de alto rendimiento **HikariCP** y los repositorios Spring Data (`JpaRepository`), con soporte transaccional declarativo mediante `@Transactional`.
- **¿Qué pasaría si no estuviera?** Habría que escribir consultas SQL crudas mediante JDBC puro, aumentando el riesgo de inyecciones SQL y cuadruplicando el código necesario para consultar las 17 tablas.

#### C. `spring-boot-starter-security`
- **Artefacto:** `org.springframework.boot:spring-boot-starter-security`
- **¿Para qué sirve? (Explicación Funcional):** Es el "guardia de seguridad blindado" del sistema. Verifica la identidad de quien ingresa, comprueba su contraseña encriptada y vigila que ningún usuario intente realizar acciones fuera de su cargo (por ejemplo, evita que un chofer apruebe cotizaciones comerciales o que un cliente B2B modifique datos internos de aduanas).
- **Detalle Técnico:** Filtros HTTP (`SecurityFilterChain`), hashing irreversible de contraseñas mediante **BCrypt** (coste computacional 10), desactivación de CSRF para APIs stateless y evaluación de expresiones de autorización SpEL mediante `@PreAuthorize`.
- **¿Qué pasaría si no estuviera?** El sistema no tendría control de acceso; cualquier persona en internet podría consultar datos fiscales, modificar expedientes o suplantar la identidad de administradores.

#### D. `spring-boot-starter-validation`
- **Artefacto:** `org.springframework.boot:spring-boot-starter-validation`
- **¿Para qué sirve? (Explicación Funcional):** Es el filtro de calidad de los datos de entrada. Si un usuario intenta registrar una cotización con peso negativo, sin seleccionar un puerto de destino, o con un RUC de 8 dígitos en lugar de 11, esta librería rechaza la solicitud de inmediato antes de que llegue a la base de datos.
- **Detalle Técnico:** Implementación de **Jakarta Bean Validation** (Hibernate Validator) en DTOs de entrada mediante anotaciones declarativas: `@NotNull`, `@NotBlank`, `@Size(min = 11, max = 11)`, `@Positive`, `@Email` y `@Pattern`.
- **¿Qué pasaría si no estuviera?** Entrarían datos corruptos o incompletos a la base de datos, provocando caídas del sistema (*NullPointerException*) o cálculos fiscales erróneos.

---

### 2.2 Persistencia y Base de Datos (MySQL Connector & JPA)

#### `mysql-connector-j`
- **Artefacto:** `com.mysql:mysql-connector-j` (Scope: `runtime`)
- **¿Para qué sirve? (Explicación Funcional):** Es el cable de comunicación oficial entre el backend Java y el servidor MySQL.
- **Detalle Técnico:** Driver JDBC nativo de MySQL compatible con el protocolo binario de MySQL 8.4 LTS InnoDB. Soporta reconexión automática, SSL/TLS y `utf8mb4`.
- **¿Qué pasaría si no estuviera?** El backend no podría abrir conexión con el puerto `3306` de MySQL y fallaría al iniciar.

---

### 2.3 Seguridad Criptográfica y JWT (JJWT 0.13.0)

El proyecto utiliza la suite **JJWT** (*Java JWT*) en su versión estándar moderna 0.13.0, dividida en 3 módulos:

| Dependencia | Artefacto | Propósito Técnico |
| :--- | :--- | :--- |
| **API** | `io.jsonwebtoken:jjwt-api` | Define los contratos e interfaces para construir (*Builder*) y parsear (*Parser*) tokens JWT. |
| **Implementación** | `io.jsonwebtoken:jjwt-impl` (runtime) | Motor criptográfico que realiza la firma digital mediante el algoritmo **HMAC-SHA256** utilizando la clave secreta configurada. |
| **Serializador Jackson** | `io.jsonwebtoken:jjwt-jackson` (runtime) | Se encarga de serializar y deserializar los *claims* (datos dentro del token, como `sub`, `roles`, `idUsuario`) en formato JSON. |

- **¿Para qué sirve en la práctica?** Permite que un usuario inicie sesión una sola vez y reciba una "tarjeta digital sellada" (token). Cada vez que hace una acción, presenta este sello al servidor; si alguien intenta alterar su nombre o rol dentro del token, el sello digital se rompe automáticamente y el servidor bloquea la petición.

---

### 2.4 Productividad y Calidad de Código (Project Lombok)

#### `lombok`
- **Artefacto:** `org.projectlombok:lombok` (Optional: `true`)
- **¿Para qué sirve? (Explicación Funcional):** Elimina el código repetitivo y aburrido en Java. En lugar de escribir manualmente 80 líneas de código para los métodos "leer dato" (*get*) y "guardar dato" (*set*) de una tabla aduanera con 20 columnas, basta con poner `@Getter` y `@Setter`.
- **Detalle Técnico:** Procesador de anotaciones en tiempo de compilación (`maven-compiler-plugin`). Inyecta en el bytecode `.class` constructores (`@NoArgsConstructor`, `@AllArgsConstructor`), getters/setters, patrones Builder (`@Builder`) y registradores de bitácora (`@Slf4j`).
- **¿Qué pasaría si no estuviera?** El tamaño del código fuente en archivos `.java` se duplicaría innecesariamente, dificultando la lectura y el mantenimiento.

---

### 2.5 Motor Híbrido de Reportes y Documentos PDF

El backend cuenta con una arquitectura de reportería dual diseñada para satisfacer tanto reportes técnicos de alta densidad de datos como documentos ejecutivos de presentación visual:

#### A. JasperReports 7.0.8 (Para Reportes Corporativos Vectoriales)
- **Artefactos:**
  - `net.sf.jasperreports:jasperreports` (Motor central)
  - `net.sf.jasperreports:jasperreports-pdf` (Exportador nativo PDF)
  - `net.sf.jasperreports:jasperreports-jdt` (Compilador de expresiones en memoria con Eclipse JDT)
- **¿Para qué sirve?** Compila plantillas estructuradas en memoria para generar padrones maestros de transporte, hojas de liquidación y documentos oficiales con tablas extensas y paginación matemática estricta.

#### B. Thymeleaf & OpenHTMLtoPDF (Para Documentos con Diseño Visual Moderno)
- **Artefactos:**
  - `org.springframework.boot:spring-boot-starter-thymeleaf`
  - `com.openhtmltopdf:openhtmltopdf-pdfbox` (Versión `1.0.10`)
- **¿Para qué sirve?** Permite diseñar documentos en **HTML5 y CSS3 estándar** (con la tipografía, colores del sistema *Warm Editorial* y logos de la empresa) ubicados en `templates/reports/` (`reporte-mercancias.html`, `reporte-puertos.html`, `reporte-sedes.html`, `reporte-tipos-carga.html`) y convertirlos directamente a archivos PDF vectoriales de alta resolución.
- **Ventaja de esta combinación:** Los diseñadores o desarrolladores web pueden maquetar un reporte usando código HTML/CSS idéntico al del frontend sin necesidad de aprender herramientas complejas de diseño gráfico de reportes.

---

## 3. Plugins del Ciclo de Compilación de Maven

El archivo [`backend/pom.xml`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/pom.xml) cuenta con 2 plugins esenciales en su sección `<build>`:

```xml
<plugins>
    <!-- Plugin oficial de Spring Boot para empaquetado y arranque -->
    <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
    </plugin>

    <!-- Compilador Java con soporte explícito de anotaciones Lombok -->
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <executions>
            <execution>
                <id>default-compile</id>
                <phase>compile</phase>
                <goals>
                    <goal>compile</goal>
                </goals>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </execution>
        </executions>
    </plugin>
</plugins>
```

1. **`spring-boot-maven-plugin`:** Permite ejecutar la aplicación con el comando `mvn spring-boot:run` y compilar el backend como un archivo ejecutable autocontenido (*fat JAR* / *uber JAR*) que incluye el servidor Tomcat integrado, listo para ser ejecutado con `java -jar backend.jar` en cualquier servidor o contenedor Docker.
2. **`maven-compiler-plugin`:** Configurado específicamente con `<annotationProcessorPaths>` apuntando a Lombok. Esto asegura que el compilador de Java 21 reconozca y genere todos los métodos `@Builder`, constructores y getters **antes** de compilar las clases dependientes, evitando errores de símbolos no encontrados durante la compilación.
