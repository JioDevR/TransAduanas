# PA01 Disponibilidad y conectividad del sistema

Prueba adicional independiente de PM01 a PM15. No cambia su numeracion, sus ejecutores, sus evidencias ni el lanzador Ejecutar-Prueba.ps1.

## Que comprueba

- Salud HTTP y arranque de una copia del backend.
- Acceso con la cuenta SQL existente, TLS y transacciones de lectura.
- Tablas y recuentos reales, incluidos los catalogos consultados mediante la API.
- HTML y recursos locales de las versiones compiladas de SGA y B2B.
- Consultas de catalogos desde el proxy temporal de ambos frontends hasta el backend y MySQL.
- Integridad de los datos y archivos originales; conservacion del estado de los servicios.
- Cierre de los procesos propios y retirada de la credencial temporal.

## Entorno y requisitos

Ejecutar una prueba por vez en Windows, con Python 3, Java 21 y MySQL 8.4. La base original debe estar disponible y el usuario debe tener acceso al almacen DPAPI protegido del proyecto. No ejecutar otro mantenimiento al mismo tiempo.

Se utiliza el JAR conservado en TransAduanas-desplegado y los dist existentes de app-sga y app-b2b. No instala dependencias, no recompila y no cambia las configuraciones de Vite. Los puertos 8085, 5180, 5174 y 19085 deben estar libres. Si estan ocupados, la prueba se detiene sin cerrar los procesos existentes.

La copia Java y los servidores de los frontends solo escuchan en 127.0.0.1. Los proxies temporales permiten lectura de salud y de dos catalogos publicos; no sustituyen el servidor del proyecto. La copia Java usa validacion del esquema, inicializador desactivado y conexiones configuradas en lectura. Las sesiones SQL de comprobacion usan START TRANSACTION READ ONLY y ROLLBACK.

El servicio Windows original no se inicia, reinicia ni detiene. El resultado de PA01 describe el entorno temporal, no declara disponible un servicio Windows que ya estuviera detenido. Los datos personales y las credenciales no se imprimen en la evidencia.

## Como ejecutar

Desde la carpeta raiz del proyecto:

```powershell
.\docs\pruebas\mantenimiento\Ejecutar-Adicional.ps1
```

El lanzador es independiente del de PM01-PM15. Para consultar que ejecutara sin iniciar la prueba:

```powershell
.\docs\pruebas\mantenimiento\Ejecutar-Adicional.ps1 -Plan
```

## Que mostrar

Archivo principal: evidencias-mantenimiento\Evidencia_Adicional_1\resultado.txt.

```powershell
Get-Content .\evidencias-mantenimiento\Evidencia_Adicional_1\resultado.txt
```

Mostrar el entorno temporal, el backend HTTP 200 y UP, los recuentos SQL, los recursos de cada frontend y las rutas API correctas. Incluir la integridad, la conservacion del estado del backend original, el cierre de procesos y el estado de las 12 verificaciones.

Para sustentar el resumen, mostrar catalogos_verificados.json, frontends_verificados.json y criterios.csv de la misma carpeta. El resultado.json indica las limitaciones. No mostrar el cliente SQL temporal, claves, datos personales ni el contenido del almacen DPAPI.

Cada repeticion conserva la evidencia previa y crea Evidencia_Adicional_1_2, _3, etc. Usar la carpeta de la ejecucion que se este mostrando; la ruta aparece al terminar. No editar resultados fallidos ni trasladarlos a las evidencias de PM01-PM15.

Titulo sugerido para la figura: Verificacion adicional de disponibilidad y conectividad.

## Como interpretar el resultado

Un estado EXITOSO requiere las 12 verificaciones. Una respuesta HTTP 200 aislada no demuestra todo el funcionamiento. No se comprueban el inicio de sesion, la interaccion visual ni todos los flujos de escritura; tampoco se ejecuta Maven, npm ni pruebas de regresion.

Los recuentos deben coincidir entre SQL y API, pero no se exige una cantidad fija de registros: pueden variar si hubo cambios legitimos anteriores. Si cambian los datos durante el ensayo, se informa la diferencia sin atribuir automaticamente su causa a la prueba y no se restaura ni modifica la base para forzar un resultado exitoso.

Un fallo de preparacion, como puerto ocupado, MySQL detenido o archivo ausente, se conserva en diagnostico.json. Revisar esa causa antes de repetir; no terminar otros procesos para liberar un puerto.
