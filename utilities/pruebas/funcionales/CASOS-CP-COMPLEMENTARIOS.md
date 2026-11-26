# Referencia complementaria: los antiguos casos CP01–CP15

**Este archivo no es el manual de la sustentación vigente.** Para mostrar 4 unitarias, 4 de integración, 4 de sistema y 3 de aceptación, utiliza exclusivamente el [manual principal](README.md) y `ejecutar-prueba-docente.ps1`.

## Por qué se conservan

Los CP01–CP15 son la selección anterior: 8 casos de integración y 7 de sistema del backend. Permanecen como métodos adicionales del proyecto y algunos se reutilizan en la selección actual con códigos PI/PS. No se suman nuevamente al total de 346 ni sustituyen las unitarias y aceptación de la selección vigente.

## Casos disponibles

| Código anterior | Funcionalidad | Método de BusinessFunctionalCasesTest |
|---|---|---|
| CP01 | Formalización de prospecto a cliente | `CP01_formalizacionProspectoACliente` |
| CP02 | Registro de clientes | `CP02_registroClienteYDuplicado` |
| CP03 | Consulta de puertos | `CP03_puertosListaDetalleReal` |
| CP04 | Validación de anticipos | `CP04_validarAnticipoPersisteYRecalcula` |
| CP05 | Búsqueda de clientes | `CP05_busquedaClientesPorRazonSocial` |
| CP06 | Consulta de tipos de carga | `CP06_tiposCargaListaDetalleReal` |
| CP07 | Apertura de expedientes | `CP07_aperturaExigePagoYPersiste` |
| CP08 | Actualización de clientes | `CP08_actualizacionClientePersiste` |
| CP09 | Consulta de sedes | `CP09_sedesListaDetalleReal` |
| CP10 | Consulta de expedientes | `CP10_consultaListadoYDetalleExpediente` |
| CP11 | Solicitud de cotización | `CP11_solicitudPortalCreaRegistroYRechazaInvalido` |
| CP12 | Aceptación de cotización | `CP12_aceptarCotizacionPersisteSaldoYObservacion` |
| CP13 | Liquidación financiera | `CP13_liquidacionEmitePersisteYConsulta` |
| CP14 | Carga y descarga de documentos | `CP14_cargaYDescargaConservaContenido` |
| CP15 | Consulta de mercancías | `CP15_mercanciasListaDetalleCodigoReal` |

## Ejecutar opcionalmente los casos anteriores

Desde `backend-sga`, el ejecutor anterior conserva su numeración CP:

```powershell
# Solo CP01 de la selección anterior
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 1

# Los 15 CP anteriores: no es la nueva selección de cuatro tipos
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest"
```

Para un método concreto, reemplaza el selector por el de la tabla:

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP02_registroClienteYDuplicado"
```

Este ejecutor guarda evidencias en `target/casos-funcionales/CPxx/FECHA-HORA/`. El ejecutor **vigente para el docente** usa otra carpeta: `target/evaluacion-docente/PUxx|PIxx|PSxx|PAxx/FECHA-HORA/`.

## Evitar confusiones

| Necesidad | Comando o manual correcto |
|---|---|
| Sustentar las 15 de los cuatro tipos | [Manual principal](README.md), `ejecutar-prueba-docente.ps1` o `-Dgroups=docente15`. |
| Consultar un caso anterior CP | Este archivo y `ejecutar-caso-funcional.ps1`. |
| Ejecutar todas las pruebas del proyecto | [Inventario general](../../../test/README.md) y `mvnw.cmd test`. |

Los datos de estas pruebas viven en H2 temporal. Este documento no acredita una ejecución nueva: consulta siempre los reportes de tu ejecución actual.
