@echo off
setlocal
cd /d "%~dp0"
rem Requiere Java 21 en JAVA_HOME o PATH. Mockito evita la carga dinamica del agente.
call mvnw.cmd test "-DargLine=-javaagent:%USERPROFILE%/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" %*
exit /b %errorlevel%
