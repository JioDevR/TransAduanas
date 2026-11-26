# Verificación de compatibilidad con main

Fecha: 26 de noviembre de 2026, America/Lima. Base consultada: `37da034dd62f2ce535481621bac6b0a66282b117` (`main`).

La rama parte de main y conserva sus cambios de transporte, tipos enum, auditoría, dependencias y componentes actuales. Las correcciones se adaptaron por bloques; no se sustituyeron los archivos completos por las versiones antiguas de la copia modificada. No se modifica main ni se fusiona automáticamente.

## Resultados ejecutados sobre esta rama

| Verificación | Resultado |
|---|---|
| Suite habitual backend | 415 descubiertas: 413 ejecutadas sin fallos ni errores; 2 omitidas por activación explícita (ZAP y rendimiento). |
| Selección docente15 con `-Dgroups=docente15` | 15 ejecutadas; cero fallos, errores u omisiones. |
| Ejecutor funcional Caso 1 en Windows PowerShell 5.1 | APROBADO, con verificación del XML y mensajes. |
| SEG-01 sobre árbol de esta rama | 130 dependencias públicas consultadas; cero asociaciones con avisos OSV. No son las 147 de la copia histórica. |
| Ejecutor SEG-02 en Windows PowerShell 5.1 | Ejecutado con valores ocultos; candidatos requieren clasificación. Se corrigió el tratamiento de stderr normal de Gitleaks en PowerShell 5.1. |
| Revisión del contenido a publicar | Los seis candidatos detectados en archivos modificados/nuevos corresponden a claves sintéticas de tests o valores generados en memoria. No se identificó un secreto real nuevo en esos candidatos. Los demás candidatos pertenecen a archivos heredados y no se certifican aquí. |
| SEG-05 en componentes actuales de main | 4 comprobaciones de navegador aprobadas; se adaptó el selector de la ruta y se conservó la interfaz actual. |
| Suite B2B actual | 124 pruebas aprobadas, en 23 archivos. |
| Compilación B2B | Correcta. Se conserva chart.js y se añade solamente Playwright para pruebas. |
| SEG-09 y SEG-10 activadas expresamente | 2 pruebas ejecutadas, sin fallos ni omisiones. ZAP: cero alertas; rendimiento: 30 muestras y cero errores. |

Se verificó que los bloques de seguridad no eliminen llamadas de auditoría ni usos de los tipos enum de main en los tres servicios/inicializador afectados. Los resultados previos mencionados en los manuales son históricos y sus cantidades no se deben sumar a estas verificaciones.

## Alcance del cambio

Pruebas, ejecutores y manuales de funcionales/seguridad, más las correcciones mínimas necesarias para que los casos no acepten errores: credenciales iniciales y JWT, validación documental y respuestas HTTP. Las modificaciones de configuración y semilla SQL son por SEG-02; no se agregan archivos generales de despliegue o monitoreo.

No se suben dependencias descargadas, target, dist, documentos de usuarios, resultados temporales, sesiones de ZAP, .env ni propiedades locales. Los informes se regeneran localmente mediante los comandos de cada manual.

## Límites pendientes

No se ha verificado Docker en ejecución en este equipo, ni rotación de credenciales en instalaciones existentes, limpieza del historial, TLS/proxy publicado ni todos los candidatos de secretos heredados de main. SEG-02 no certifica automáticamente esos ámbitos. Ninguna prueba equivale a garantía de seguridad completa. No se copia la tabla Word histórica como si fuera evidencia de esta versión.

La comprobación de selección usa `-Dgroups=docente15`; no existe un perfil Maven llamado docente15. Un primer intento con `-Pdocente15` ejecutó la suite general y no se contó como validación de los 15. Después se ejecutó el selector correcto y se comprobaron exactamente 15.
