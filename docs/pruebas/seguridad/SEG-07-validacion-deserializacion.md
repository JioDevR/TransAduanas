# SEG-07: validación de entradas y deserialización JSON

Objetivo: comprobar que los datos inválidos y los cuerpos JSON incompatibles no se guarden, reciban una respuesta apropiada y no expongan detalles internos.

Tipo: prueba de seguridad de validación de entradas y deserialización, ejecutada como prueba del sistema del backend. Herramientas: JUnit Jupiter, Spring Boot Test, Java HttpClient, H2 y Maven Surefire. HTTP, controladores, validadores y persistencia son reales; no se usa Mockito en estos casos.

Desde `backend-sga`, ejecutar en PowerShell:

```powershell
.\mvnw.cmd test "-Dtest=SecurityInputValidationTest"
```

| Comprobación | Criterio de aprobación |
| --- | --- |
| SEG-07.1 | Campos obligatorios ausentes, nulos, vacíos y en blanco reciben 400; se identifica el campo inválido y no se insertan registros. |
| SEG-07.2 | Correo inválido y documento, razón social o teléfono demasiado largos reciben 400 sin insertar registros. |
| SEG-07.3 | JSON truncado, raíz de tipo lista, cuerpo null y tipos de campos incompatibles reciben 400, sin insertar ni revelar detalles internos. Una solicitud válida posterior sí funciona. |
| SEG-07.4 | Content-Type text/plain para una operación JSON recibe 415, sin insertar ni revelar detalles internos. |
| SEG-07.5 | Documento de 15 caracteres y razón social de 150 se aceptan. Una actualización posterior que excede el límite recibe 400 y conserva el registro original. |

Las respuestas de error deben ser JSON con `success=false`. Se comprueba que no incluyan trazas, clases Java/Spring/Jackson ni detalles SQL. Se verifica la cantidad de registros antes y después de cada solicitud rechazada.

Cada comprobación que termina correctamente muestra `[SEG-07.n] APROBADO`. El resultado global debe mostrar cinco pruebas, cero fallos y cero errores, con BUILD SUCCESS. Los códigos 400 y 415 son los resultados esperados de rechazo, no fallos del caso.

Datos ficticios; servidor 127.0.0.1 con puerto aleatorio; H2 temporal. No se usan datos reales ni se envían objetos serializados ejecutables. Los cuerpos inválidos son pequeños y no son pruebas de carga.

Alcance: creación y actualización de clientes, lectura de JSON, restricciones declaradas y tipo de contenido en los endpoints evaluados. No certifica todas las entidades, formatos, reglas de negocio, límites de tamaño/profundidad ni todas las formas de deserialización insegura.

## Corrección detectada al ejecutar la prueba

La primera ejecución aprobó tres comprobaciones y detectó dos errores: JSON mal formado y Content-Type incompatible devolvían 500. El manejador global trataba esas excepciones como errores internos.

Se añadieron manejadores específicos en `GlobalExceptionHandler`:

- `HttpMessageNotReadableException`: respuesta 400 con mensaje genérico, sin devolver el detalle del deserializador.
- `HttpMediaTypeNotSupportedException`: respuesta 415 con mensaje genérico.

Las verificaciones conservan sus criterios originales; no se cambió lo esperado para aceptar el error. Para revisar también las pruebas anteriores del backend:

```powershell
.\mvnw.cmd test
```

## Resultado final verificado

SEG-07: cinco comprobaciones aprobadas, sin fallos. Después de corregir las respuestas HTTP se ejecutó toda la suite del backend: 373 pruebas, cero fallos, cero errores y ninguna omitida; BUILD SUCCESS. Las 373 corresponden a la suite completa y no solo a SEG-07.
