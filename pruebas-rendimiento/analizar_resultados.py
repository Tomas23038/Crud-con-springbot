#!/usr/bin/env python3
"""
Lee los resultados de JMeter (CSV) de las pruebas con 10, 50 y 100 usuarios y genera:
  - tabla_resumen.csv / tabla_resumen.md   (comparacion de las tres cargas)
  - codigos_respuesta.csv                  (cuantas respuestas 201, 409, 500, etc.)
  - grafico_tiempos_respuesta.png          (promedio, mediana, p90, p95 por carga)
  - grafico_throughput.png                 (solicitudes por segundo)
  - grafico_errores.png                    (% de errores)
  - grafico_evolucion.png                  (tiempo de respuesta durante la prueba)

Uso (desde la carpeta donde estan los CSV):
    pip install pandas matplotlib
    python analizar_resultados.py
Opciones:
    --dir CARPETA      carpeta con resultados_10.csv, resultados_50.csv, ...
    --users 10 50 100  cargas a comparar
    --label TEXTO      parte del nombre de la peticion a analizar (por defecto "Crear criatura")
    --out CARPETA      donde guardar las salidas (por defecto "analisis")
"""
import argparse
import logging
import os
import sys

logging.getLogger("matplotlib.font_manager").setLevel(logging.ERROR)

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import pandas as pd

# Estilo sobrio: blanco y negro / grises, letra tipo Arial
plt.rcParams["font.family"] = ["Arial", "Liberation Sans", "DejaVu Sans"]
plt.rcParams["axes.spines.top"] = False
plt.rcParams["axes.spines.right"] = False
GRISES = ["#000000", "#555555", "#999999", "#cccccc"]
RELLENOS = ["#ffffff", "#cccccc", "#999999", "#666666"]
HATCH = ["", "///", "...", "xxx"]
LINEAS = ["-", "--", ":", "-."]


def cargar(carpeta, users, label):
    datos = {}
    for u in users:
        ruta = os.path.join(carpeta, f"resultados_{u}.csv")
        if not os.path.exists(ruta):
            sys.exit(f"No encontre {ruta}. Revisa --dir y los nombres de los archivos.")
        df = pd.read_csv(ruta)
        df = df[df["label"].astype(str).str.contains(label, case=False, regex=False)].copy()
        if df.empty:
            sys.exit(f"En {ruta} no hay filas con la etiqueta '{label}'.")
        df["success"] = df["success"].astype(str).str.lower().eq("true")
        df["responseCode"] = df["responseCode"].astype(str)
        datos[u] = df
    return datos


def resumen(datos):
    filas = []
    for u, df in datos.items():
        inicio = df["timeStamp"].min()
        fin = (df["timeStamp"] + df["elapsed"]).max()
        duracion = max((fin - inicio) / 1000.0, 0.001)
        e = df["elapsed"]
        fila = {
            "Usuarios": u,
            "Peticiones": len(df),
            "Promedio (ms)": e.mean(),
            "Mediana (ms)": e.median(),
            "Percentil 90 (ms)": e.quantile(0.90),
            "Percentil 95 (ms)": e.quantile(0.95),
            "Percentil 99 (ms)": e.quantile(0.99),
            "Minimo (ms)": e.min(),
            "Maximo (ms)": e.max(),
            "Desv. estandar (ms)": e.std(),
            "Errores (%)": 100.0 * (~df["success"]).mean(),
            "Throughput (peticiones/s)": len(df) / duracion,
            "Duracion (s)": duracion,
        }
        if "Latency" in df.columns:
            fila["Latencia promedio (ms)"] = df["Latency"].mean()
        if "allThreads" in df.columns:
            fila["Usuarios activos a la vez (max)"] = int(df["allThreads"].max())
            fila["Usuarios activos a la vez (prom)"] = df["allThreads"].mean()
        filas.append(fila)
    return pd.DataFrame(filas).set_index("Usuarios")


def codigos(datos):
    filas = []
    for u, df in datos.items():
        for cod, n in df["responseCode"].value_counts().items():
            filas.append({"Usuarios": u, "Codigo de respuesta": cod, "Cantidad": int(n)})
    return pd.DataFrame(filas)


def barras_agrupadas(tabla, columnas, titulo, ylabel, archivo):
    users = list(tabla.index)
    ancho = 0.8 / len(columnas)
    fig, ax = plt.subplots(figsize=(8, 5))
    for i, col in enumerate(columnas):
        xs = [j + i * ancho for j in range(len(users))]
        barras = ax.bar(xs, tabla[col], ancho, label=col, color=RELLENOS[i % 4],
                        edgecolor="black", hatch=HATCH[i % 4])
        for b in barras:
            ax.annotate(f"{b.get_height():.0f}", (b.get_x() + b.get_width() / 2, b.get_height()),
                        ha="center", va="bottom", fontsize=8)
    ax.set_xticks([j + ancho * (len(columnas) - 1) / 2 for j in range(len(users))])
    ax.set_xticklabels([f"{u} usuarios" for u in users])
    ax.set_title(titulo)
    ax.set_ylabel(ylabel)
    ax.legend(frameon=False)
    fig.tight_layout()
    fig.savefig(archivo, dpi=200)
    plt.close(fig)


def barras_simples(tabla, col, titulo, ylabel, archivo, decimales=1):
    users = list(tabla.index)
    fig, ax = plt.subplots(figsize=(7, 4.5))
    barras = ax.bar([f"{u} usuarios" for u in users], tabla[col], color="#999999", edgecolor="black", width=0.5)
    for b in barras:
        ax.annotate(f"{b.get_height():.{decimales}f}".replace(".", ","),
                    (b.get_x() + b.get_width() / 2, b.get_height()),
                    ha="center", va="bottom", fontsize=9)
    ax.set_title(titulo)
    ax.set_ylabel(ylabel)
    ax.set_ylim(0, max(tabla[col].max() * 1.15, 1))
    fig.tight_layout()
    fig.savefig(archivo, dpi=200)
    plt.close(fig)


def evolucion(datos, archivo):
    fig, ax = plt.subplots(figsize=(9, 5))
    for i, (u, df) in enumerate(datos.items()):
        seg = ((df["timeStamp"] - df["timeStamp"].min()) // 1000).astype(int)
        serie = df.groupby(seg)["elapsed"].mean()
        ax.plot(serie.index, serie.values, LINEAS[i % 4], color=GRISES[i % 3], linewidth=1.6,
                label=f"{u} usuarios")
    ax.set_title("Tiempo de respuesta promedio durante la prueba")
    ax.set_xlabel("Segundos desde el inicio de la prueba")
    ax.set_ylabel("Tiempo de respuesta promedio (ms)")
    ax.legend(frameon=False)
    fig.tight_layout()
    fig.savefig(archivo, dpi=200)
    plt.close(fig)


def a_markdown(tabla):
    t = tabla.copy().round(1)
    cab = ["Usuarios"] + list(t.columns)
    lineas = ["| " + " | ".join(cab) + " |", "|" + "|".join(["---"] * len(cab)) + "|"]
    for u, fila in t.iterrows():
        lineas.append("| " + " | ".join([str(u)] + [str(v) for v in fila.values]) + " |")
    return "\n".join(lineas)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", default=".")
    ap.add_argument("--users", nargs="+", type=int, default=[10, 50, 100])
    ap.add_argument("--label", default="Crear criatura")
    ap.add_argument("--out", default="analisis")
    a = ap.parse_args()

    datos = cargar(a.dir, a.users, a.label)
    os.makedirs(a.out, exist_ok=True)

    tabla = resumen(datos)
    tabla.round(2).to_csv(os.path.join(a.out, "tabla_resumen.csv"), encoding="utf-8-sig")
    with open(os.path.join(a.out, "tabla_resumen.md"), "w", encoding="utf-8") as f:
        f.write(a_markdown(tabla) + "\n")
    codigos(datos).to_csv(os.path.join(a.out, "codigos_respuesta.csv"), index=False, encoding="utf-8-sig")

    barras_agrupadas(tabla, ["Promedio (ms)", "Mediana (ms)", "Percentil 90 (ms)", "Percentil 95 (ms)"],
                     "Tiempos de respuesta segun la carga", "Milisegundos",
                     os.path.join(a.out, "grafico_tiempos_respuesta.png"))
    barras_simples(tabla, "Throughput (peticiones/s)", "Rendimiento (throughput) segun la carga",
                   "Peticiones por segundo", os.path.join(a.out, "grafico_throughput.png"))
    barras_simples(tabla, "Errores (%)", "Porcentaje de errores segun la carga", "% de peticiones con error",
                   os.path.join(a.out, "grafico_errores.png"), decimales=2)
    evolucion(datos, os.path.join(a.out, "grafico_evolucion.png"))

    pd.set_option("display.width", 200)
    pd.set_option("display.max_columns", 30)
    print(tabla.round(1).T.to_string())
    print("\nCodigos de respuesta:")
    print(codigos(datos).to_string(index=False))
    print(f"\nArchivos guardados en la carpeta '{a.out}'")


if __name__ == "__main__":
    main()
