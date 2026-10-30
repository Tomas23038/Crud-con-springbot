@echo off
rem ============================================================
rem  Corre las 3 pruebas de carga (10, 50 y 100 usuarios)
rem  Antes: MySQL y la API (Spring Boot) deben estar corriendo.
rem  Este archivo y plan_pruebas_zoo.jmx deben estar en la misma carpeta.
rem  No hay que editar nada: el archivo te pide donde esta jmeter.bat.
rem ============================================================
setlocal
set "PLAN=plan_pruebas_zoo.jmx"
set "RAMP=1"
set "LOOPS=100"

if not exist "%PLAN%" (
  echo No encuentro %PLAN% en esta carpeta.
  echo Pon plan_pruebas_zoo.jmx junto a este archivo y vuelve a abrirlo.
  goto :fin
)

echo.
echo Arrastra el archivo jmeter.bat hasta esta ventana, sueltalo y presiona Enter.
echo (Esta en la carpeta bin de JMeter. Es el que dice "Archivo por lotes".)
echo.
set "JMETER="
set /p "JMETER=Ruta de jmeter.bat: "
set "JMETER=%JMETER:"=%"
:trim
if "%JMETER:~-1%"==" " set "JMETER=%JMETER:~0,-1%" & goto :trim

if not exist "%JMETER%" (
  echo.
  echo No encuentro ese archivo: %JMETER%
  echo Cierra esta ventana y vuelve a intentarlo.
  goto :fin
)

set "HAYVIEJOS="
if exist resultados_*.csv set "HAYVIEJOS=1"
for %%D in (10 50 100) do if exist dashboard_%%D set "HAYVIEJOS=1"
if not defined HAYVIEJOS goto :sinviejos
set "VIEJO=corrida_anterior_%RANDOM%"
mkdir "%VIEJO%"
move resultados_*.csv "%VIEJO%" >nul 2>&1
for %%D in (10 50 100) do if exist dashboard_%%D move dashboard_%%D "%VIEJO%" >nul 2>&1
echo Los resultados de la corrida anterior se guardaron en la carpeta %VIEJO%
:sinviejos

for %%U in (10 50 100) do call :correr %%U
echo.
echo Listo. Deberias tener resultados_10.csv, resultados_50.csv, resultados_100.csv
echo y las carpetas dashboard_10, dashboard_50 y dashboard_100.
goto :fin

:correr
if exist resultados_%1.csv goto :existe
if exist dashboard_%1 goto :existe
echo.
echo ===== Prueba con %1 usuarios =====
call "%JMETER%" -n -t "%PLAN%" -Jusers=%1 -Jramp=%RAMP% -Jloops=%LOOPS% -l resultados_%1.csv -e -o dashboard_%1
echo Esperando 20 segundos para que el servidor se calme...
timeout /t 20 /nobreak >nul
goto :eof

:existe
echo.
echo Ya existe resultados_%1.csv o dashboard_%1 de una corrida anterior.
echo Muevelos o renombralos a otra carpeta y vuelve a correr. No se ejecuto la prueba de %1 usuarios.
goto :eof

:fin
endlocal
pause
