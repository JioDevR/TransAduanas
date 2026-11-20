# 07. Compilación y Empaquetado del Backend Java (`.jar`)

Guía paso a paso para compilar el proyecto **sga-backend** (Java 21 + Spring Boot 4.1.2 + Apache Maven) y generar el archivo `.jar` ejecutable listo para despliegue o ejecución independiente.

---

## 1. Requisitos Previos

Antes de iniciar la compilación, verifica que tu entorno cuente con **Java JDK 21 LTS**. No es obligatorio tener Maven instalado globalmente en el sistema, ya que el proyecto incluye **Maven Wrapper** (`mvnw` / `mvnw.cmd`).

| Requisito | Versión | Comando de Verificación | Notas |
| :--- | :---: | :--- | :--- |
| **Java JDK** | **21 LTS** | `java -version` | Definido en `<java.version>21</java.version>` dentro de [`backend/pom.xml`](../../backend/pom.xml). |
| **Maven Wrapper** | **3.9.x** | `./mvnw -version` *(Linux/macOS)*<br>`.\mvnw.cmd -version` *(Windows)* | Descarga y usa automáticamente la versión correcta de Apache Maven. |

> [!TIP]
> Si `java -version` muestra una versión distinta a la 21, configura la variable `JAVA_HOME` apuntando al JDK 21 antes de ejecutar los comandos de Maven.

---

## 2. Archivos Involucrados en la Compilación

Dentro del directorio [`backend/`](../../backend) se encuentran los archivos clave del proceso de build:

- [`backend/pom.xml`](../../backend/pom.xml): Define las coordenadas (`pe.sga:sga-backend:0.5.0`), dependencias, procesador de anotaciones **Lombok** (`maven-compiler-plugin`) y el empaquetador ejecutable (`spring-boot-maven-plugin`).
- [`backend/mvnw`](../../backend/mvnw): Script Maven Wrapper para **Linux / macOS**.
- [`backend/mvnw.cmd`](../../backend/mvnw.cmd): Script Maven Wrapper para **Windows**.
- [`backend/src/main/resources/application.properties`](../../backend/src/main/resources/application.properties): Configuración base empaquetada dentro del `.jar`.

---

## 3. Pasos para Compilar hasta Obtener el `.jar`

### Paso 1: Ingresar al directorio del backend

Abre una terminal en la raíz del repositorio y muévete a la carpeta `backend/`:

```bash
cd backend
```

### Paso 2: Dar permisos de ejecución al wrapper (solo Linux / macOS, primera vez)

Si el archivo `mvnw` no tiene permisos de ejecución, asígnalos con:

```bash
chmod +x mvnw
```

### Paso 3: Verificar la versión de Java y Maven Wrapper

Confirma que el wrapper detecta correctamente Java 21:

```bash
# Linux / macOS
./mvnw -version

# Windows (PowerShell / CMD)
.\mvnw.cmd -version
```

### Paso 4: Limpiar artefactos previos y compilar el código fuente

Ejecuta la limpieza de compilaciones anteriores (`target/`) y compila las clases Java junto con el procesador de anotaciones de Lombok:

```bash
# Linux / macOS
./mvnw clean compile

# Windows
.\mvnw.cmd clean compile
```

> [!NOTE]
> En la primera ejecución, Maven descargará las dependencias declaradas en `pom.xml` hacia el repositorio local `~/.m2/repository`. Esto requiere conexión a internet la primera vez.

### Paso 5: Generar el archivo `.jar` ejecutable (`package`)

Tienes dos opciones según necesites ejecutar las pruebas automatizadas durante el empaquetado o generar el artefacto directamente:

#### Opción A: Compilar, ejecutar pruebas y generar el `.jar` (Recomendado)

```bash
# Linux / macOS
./mvnw clean package

# Windows
.\mvnw.cmd clean package
```

#### Opción B: Compilar y generar el `.jar` omitiendo las pruebas (`-DskipTests`)

Útil cuando ya validaste los tests previamente y solo necesitas empaquetar el `.jar`:

```bash
# Linux / macOS
./mvnw clean package -DskipTests

# Windows
.\mvnw.cmd clean package -DskipTests
```

---

## 4. Verificación del Artefacto `.jar` Generado

Al finalizar el comando con `BUILD SUCCESS`, el plugin `spring-boot-maven-plugin` habrá empaquetado la aplicación en el directorio `backend/target/`:

```bash
ls -lh target/*.jar
```

Encontrarás los siguientes archivos:

| Archivo | Descripción |
| :--- | :--- |
| `target/sga-backend-0.5.0.jar` | **JAR ejecutable (Fat/Uber JAR)** con todas las dependencias y Tomcat embebido. Es el archivo que se debe usar para ejecutar o desplegar el backend. |
| `target/sga-backend-0.5.0.jar.original` | JAR original generado por Maven antes de ser reempaquetado por Spring Boot (no incluye dependencias embebidas). |

---

## 5. Ejecución del `.jar` Generado (Referencia)

Una vez obtenido `target/sga-backend-0.5.0.jar`, puede ejecutarse directamente con:

```bash
java -jar target/sga-backend-0.5.0.jar
```

Si requieres configuración local externa (credenciales MySQL, puerto, etc.):
- Asegúrate de tener `application-local.properties` en el directorio desde donde ejecutas el `.jar` (basado en [`backend/application-local.properties.example`](../../backend/application-local.properties.example)), **o**
- Pasa las variables de entorno soportadas (`SERVER_PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `STORAGE_DIR`).

---

## 6. Solución de Problemas Frecuentes

| Problema | Causa Probable | Solución |
| :--- | :--- | :--- |
| `mvn: command not found` | Maven no está instalado de forma global en el sistema. | Usa `./mvnw` (Linux/macOS) o `.\mvnw.cmd` (Windows) en lugar de `mvn`. |
| `Permission denied: ./mvnw` | Falta permiso de ejecución en el script. | Ejecuta `chmod +x mvnw` dentro de `backend/`. |
| Error de versión de Java (`release version 21 not supported`) | `JAVA_HOME` apunta a un JDK inferior a 21. | Instala JDK 21 LTS y exporta `JAVA_HOME` hacia esa ruta. |
| Errores de getters/setters o `@Builder` al compilar | Fallo en procesamiento de anotaciones de Lombok. | Ejecuta `./mvnw clean compile` para regenerar las clases procesadas por `maven-compiler-plugin`. |
