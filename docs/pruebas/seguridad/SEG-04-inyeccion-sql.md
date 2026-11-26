# SEG-04: resistencia a inyección SQL

Objetivo: comprobar que las entradas de usuario se procesen como datos y no permitan omitir la autenticación ni ampliar consultas SQL. Se utilizan entradas no destructivas, cuentas ficticias y H2 temporal en 127.0.0.1. No se prueban servidores externos ni bases con información real.

Tipo: prueba de seguridad de inyección SQL ejecutada como prueba del sistema del backend. Herramientas: JUnit Jupiter, Spring Boot Test, Java HttpClient, H2 y Maven Surefire. Los casos usan HTTP, autenticación y persistencia reales; no utilizan Mockito.

Desde la carpeta `backend-sga`, ejecutar en PowerShell:

```powershell
.\mvnw.cmd test "-Dtest=SecuritySqlInjectionTest"
```

| Comprobación | Resultado esperado |
| --- | --- |
| SEG-04.1: autenticación interna | Entradas SQL en usuario y contraseña reciben 401, sin token ni detalles SQL. El acceso válido funciona antes y después. |
| SEG-04.2: autenticación B2B | Entradas SQL en usuario y contraseña reciben 401; la cuenta válida sigue funcionando. |
| SEG-04.3: búsqueda de clientes | Una razón social ficticia que contiene la entrada se encuentra literalmente, sin incluir otra empresa; una variante inexistente devuelve cero resultados. No cambia el número de registros ni el cliente de control. |
| SEG-04.4: consulta por documento | Entrada SQL no coincide con ningún documento y recibe 404, sin detalles internos SQL. El documento válido sigue disponible. |

La prueba imprime una línea `[SEG-04.n] APROBADO` por comprobación cuando sus verificaciones terminan. El resultado global debe indicar cuatro pruebas, cero fallos y cero errores, además de `BUILD SUCCESS`. Los códigos 401 y 404 son resultados esperados de los casos negativos.

Los datos de entrada son casos sintéticos de prueba. No se imprimen contraseñas ni tokens en los mensajes añadidos. No se ejecutan instrucciones destructivas, extracción de datos reales ni pruebas de carga.

La revisión del repositorio de clientes muestra métodos derivados de Spring Data JPA para buscar por nombre/documento. Las pruebas verifican el comportamiento observado; no sustituyen una auditoría completa de todas las consultas.

Alcance: login interno y B2B, búsqueda de clientes y consulta por documento en H2. Aprobar estos casos no certifica todos los endpoints ni el motor MySQL de despliegue. Esta es SEG-04 con cuatro comprobaciones, no cuatro pruebas SEG distintas.
