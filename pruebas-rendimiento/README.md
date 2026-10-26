# Pruebas de rendimiento con JMeter (Parte 4)

Pruebas de carga sobre `POST /api/creatures` con **10, 50 y 100 usuarios simultáneos**.
El informe completo está en [`docs/Informe_Parte4_JMeter.docx`](../docs/Informe_Parte4_JMeter.docx).

## Contenido

| Archivo / carpeta | Qué es |
|---|---|
| `plan_pruebas_zoo.jmx` | Plan de pruebas de JMeter |
| `ejecutar_pruebas.bat` | Corre las tres pruebas (Windows) |
| `analizar_resultados.py` | Genera la tabla comparativa y los gráficos a partir de los CSV |
| `resultados/` | Resultados de JMeter en CSV (uno por prueba) |
| `analisis/` | Tabla comparativa y gráficos |

## Cómo repetir las pruebas

1. Dejar corriendo MySQL y la API (`mvnw.cmd spring-boot:run`, con la variable `DB_PASSWORD` definida).
2. Ejecutar `ejecutar_pruebas.bat` desde esta carpeta y arrastrar a la ventana el `jmeter.bat` de la carpeta `bin` de JMeter.
3. Para las tablas y gráficos: `pip install pandas matplotlib` y luego `python analizar_resultados.py`.

Parámetros de cada prueba: ramp-up de 1 s, 100 peticiones por usuario, sin pausa entre peticiones.

## Resultados (resumen)

| | 10 usuarios | 50 usuarios | 100 usuarios |
|---|---|---|---|
| Peticiones | 1.000 | 5.000 | 10.000 |
| Errores | 0 % | 0 % | 0 % |
| Tiempo promedio (ms) | 27,7 | 109,9 | 236,2 |
| Percentil 95 (ms) | 72 | 243 | 547 |
| Peticiones por segundo | 281,3 | 415,2 | 398,5 |

Cada prueba crea una zona y criaturas de prueba en la base de datos (`Zona Carga JMeter ...` y `JM-Criatura-...`).
