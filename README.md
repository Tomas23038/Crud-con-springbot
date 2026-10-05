# 🐉 Zoo de Mascotas Fantásticas — CRUD con Spring Boot

API REST para administrar las **criaturas fantásticas** de un zoológico y las **zonas** donde habitan. Es un laboratorio del curso *Desarrollo de Software* (Pontificia Universidad Javeriana) cuyo objetivo es construir un CRUD con una **arquitectura en 3 capas** (presentación, lógica de negocio y acceso a datos), persistencia en **MySQL** y control de versiones con **Git siguiendo Gitflow**.

---

## Tabla de contenidos

- [Tecnologías](#tecnologías)
- [Arquitectura](#arquitectura)
- [Modelo de datos](#modelo-de-datos)
- [Reglas de negocio](#reglas-de-negocio)
- [Instalación y ejecución](#instalación-y-ejecución)
- [Endpoints de la API](#endpoints-de-la-api)
- [Ejemplos de uso](#ejemplos-de-uso)
- [Manejo de errores](#manejo-de-errores)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Flujo de trabajo con Gitflow](#flujo-de-trabajo-con-gitflow)
- [Documentación](#documentación)
- [Equipo](#equipo)

---

## Tecnologías

| Herramienta | Versión / uso |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 (Web MVC, Data JPA, Validation) |
| Hibernate / JPA | ORM y generación automática del esquema |
| MySQL | Base de datos relacional (`mysql-connector-j`) |
| Lombok | Getters, setters y constructores de las entidades |
| Maven | Gestión de dependencias (incluye el wrapper `mvnw`) |

---

## Arquitectura

El proyecto sigue una arquitectura en 3 capas. Cada petición atraviesa las capas en un solo sentido:

```
Cliente (Postman / curl)
        │  HTTP + JSON
        ▼
┌──────────────────────────┐
│  Presentación            │  controller/  → CreatureController, ZoneController
│  (REST controllers)      │  exception/   → GlobalExcepHandl (respuestas de error)
└────────────┬─────────────┘
             ▼
┌──────────────────────────┐
│  Lógica de negocio       │  service/     → CreatureService, ZoneService
│  (reglas y validaciones) │
└────────────┬─────────────┘
             ▼
┌──────────────────────────┐
│  Acceso a datos          │  repository/  → CreatureRepository, ZoneRepository
│  (Spring Data JPA)       │  model/       → Creature, Zone
└────────────┬─────────────┘
             ▼
          MySQL
```

- **Controladores**: reciben la petición, validan el cuerpo con `@Valid` y delegan en el servicio. No contienen reglas de negocio.
- **Servicios**: concentran las reglas del dominio (capacidad de las zonas, estado de salud crítico, etc.).
- **Repositorios**: interfaces de Spring Data JPA; incluyen consultas propias para contar criaturas por zona.
- **DTOs** (`dto/`): `zoneresponse` devuelve cada zona junto con su número de criaturas; `zonecount` es la proyección de la consulta agregada.

---

## Modelo de datos

Relación **muchos a uno**: muchas criaturas pertenecen a una zona (`creature.zone_id → zone.id`).

### `Zone`

| Campo | Tipo | Validación |
|---|---|---|
| `id` | Long | Autogenerado |
| `name` | String | Obligatorio |
| `description` | String | Opcional |
| `capacity` | int | Mínimo 1 |

### `Creature`

| Campo | Tipo | Validación |
|---|---|---|
| `id` | Long | Autogenerado |
| `name` | String | Obligatorio |
| `species` | String | Obligatorio |
| `size` | double | Mayor o igual a 0 |
| `dangerLevel` | int | Entre 1 y 10 |
| `healthStatus` | String | Obligatorio |
| `zone` | Zone | Obligatoria; debe existir |

---

## Reglas de negocio

| # | Regla | Respuesta si se incumple |
|---|---|---|
| 1 | Una criatura solo puede asignarse a una zona que exista. | `404 Not Found` |
| 2 | No se puede agregar una criatura a una zona que ya alcanzó su capacidad máxima (también al moverla de zona). | `409 Conflict` |
| 3 | No se puede eliminar una criatura cuyo `healthStatus` sea `critical` (sin importar mayúsculas). | `409 Conflict` |
| 4 | No se puede reducir la capacidad de una zona por debajo del número de criaturas que ya tiene. | `409 Conflict` |
| 5 | No se puede eliminar una zona que tenga criaturas asignadas. | `409 Conflict` |

**Rendimiento (problema N+1):** el listado de criaturas carga sus zonas con un `@EntityGraph`, y el listado de zonas obtiene el conteo de criaturas de todas las zonas en una sola consulta agrupada (`GROUP BY`) en lugar de una consulta por zona.

---

## Instalación y ejecución

### Requisitos previos

- JDK 21
- MySQL 8 en ejecución en `localhost:3306`
- Git

> No hace falta instalar Maven: el proyecto incluye el wrapper `mvnw`.

### 1. Clonar el repositorio

```bash
git clone https://github.com/Tomas23038/Crud-con-springbot.git
cd Crud-con-springbot
git checkout develop
```

### 2. Configurar la base de datos

La configuración está en `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/zoo_fantastico?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
```

- La base de datos `zoo_fantastico` **se crea sola** si no existe.
- Las tablas se generan automáticamente a partir de las entidades (`ddl-auto=update`).
- La contraseña **no está en el código**: se lee de la variable de entorno `DB_PASSWORD`. Si tu usuario de MySQL no es `root`, cambia `spring.datasource.username`.

### 3. Definir la contraseña y ejecutar

**Linux / macOS**

```bash
export DB_PASSWORD=tu_contraseña
./mvnw spring-boot:run
```

**Windows (PowerShell)**

```powershell
$env:DB_PASSWORD="tu_contraseña"
.\mvnw.cmd spring-boot:run
```

La API queda disponible en **http://localhost:8080**.

### 4. Ejecutar las pruebas

```bash
./mvnw test
```

> La prueba `contextLoads` levanta el contexto completo de Spring, así que también necesita MySQL en ejecución y la variable `DB_PASSWORD` definida.

---

## Endpoints de la API

### Zonas — `/api/zones`

| Método | Ruta | Descripción | Respuesta exitosa |
|---|---|---|---|
| `POST` | `/api/zones` | Crear una zona | `201 Created` |
| `GET` | `/api/zones` | Listar zonas con su número de criaturas | `200 OK` |
| `GET` | `/api/zones/{id}` | Consultar una zona | `200 OK` |
| `PUT` | `/api/zones/{id}` | Actualizar una zona | `200 OK` |
| `DELETE` | `/api/zones/{id}` | Eliminar una zona sin criaturas | `204 No Content` |

### Criaturas — `/api/creatures`

| Método | Ruta | Descripción | Respuesta exitosa |
|---|---|---|---|
| `POST` | `/api/creatures` | Crear una criatura en una zona | `201 Created` |
| `GET` | `/api/creatures` | Listar criaturas (con su zona) | `200 OK` |
| `GET` | `/api/creatures/{id}` | Consultar una criatura | `200 OK` |
| `PUT` | `/api/creatures/{id}` | Actualizar una criatura o moverla de zona | `200 OK` |
| `DELETE` | `/api/creatures/{id}` | Eliminar una criatura que no esté en estado crítico | `204 No Content` |

---

## Ejemplos de uso

### Crear una zona

```bash
curl -X POST http://localhost:8080/api/zones \
  -H "Content-Type: application/json" \
  -d '{"name": "Bosque Encantado", "description": "Hogar de criaturas mágicas", "capacity": 5}'
```

```json
{
  "id": 1,
  "name": "Bosque Encantado",
  "description": "Hogar de criaturas mágicas",
  "capacity": 5,
  "creatureCount": 0
}
```

### Crear una criatura

La zona se indica solo con su `id`:

```bash
curl -X POST http://localhost:8080/api/creatures \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Fénix",
        "species": "Ave mítica",
        "size": 1.5,
        "dangerLevel": 4,
        "healthStatus": "healthy",
        "zone": { "id": 1 }
      }'
```

### Mover una criatura a otra zona

```bash
curl -X PUT http://localhost:8080/api/creatures/1 \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Fénix",
        "species": "Ave mítica",
        "size": 1.5,
        "dangerLevel": 4,
        "healthStatus": "healthy",
        "zone": { "id": 2 }
      }'
```

### Eliminar una criatura

```bash
curl -X DELETE http://localhost:8080/api/creatures/1
```

---

## Manejo de errores

Los errores se centralizan en `GlobalExcepHandl` (`@RestControllerAdvice`) y siempre se devuelven en JSON.

| Situación | Código | Ejemplo de cuerpo |
|---|---|---|
| Recurso inexistente | `404` | `{"error": "Creature not found"}` |
| Regla de negocio incumplida | `409` | `{"error": "The zone has reached its maximum capacity"}` |
| Datos inválidos | `400` | `{"dangerLevel": "must be less than or equal to 10"}` |

En los errores de validación (`400`) se devuelve un objeto con **un mensaje por cada campo inválido**.

---

## Estructura del proyecto

```
Crud-con-springbot/
├── src/
│   ├── main/
│   │   ├── java/com/javeriana/zoo_fantastico/
│   │   │   ├── controller/      # Endpoints REST
│   │   │   ├── service/         # Reglas de negocio
│   │   │   ├── repository/      # Acceso a datos (Spring Data JPA)
│   │   │   ├── model/           # Entidades Creature y Zone
│   │   │   ├── dto/             # zoneresponse, zonecount
│   │   │   ├── exception/       # Excepciones y manejador global
│   │   │   └── ZooFantasticoApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/                    # Prueba de carga del contexto
├── docs/                        # Partes del informe
├── Informe_Completo_Zoo_Fantastico_RealG4Life.docx
├── pom.xml
├── mvnw / mvnw.cmd
└── README.md
```

---

## Flujo de trabajo con Gitflow

| Rama | Propósito |
|---|---|
| `main` | Versión estable y entregable. |
| `develop` | Integración del trabajo terminado. |
| `feature/*` | Una rama por funcionalidad, creada desde `develop`. |

Cada funcionalidad se desarrolló en su propia rama `feature/*` y se integró a `develop` mediante **Pull Request**. Orden de construcción:

1. Proyecto base generado con Spring Initializr y configuración de MySQL.
2. Entidad `Creature` con validaciones.
3. `CreatureRepository`.
4. `CreatureService` y excepción de recurso no encontrado.
5. `CreatureController` y manejador global de errores.
6. Entidad `Zone`, su repositorio y la relación con `Creature`.
7. CRUD de zonas con `zoneresponse` y contador de criaturas.
8. Asignación y cambio de zona de una criatura.
9. Corrección del problema N+1.
10. Informe del laboratorio.

---

## Documentación

El informe completo del laboratorio está en [`Informe_Completo_Zoo_Fantastico_RealG4Life.docx`](Informe_Completo_Zoo_Fantastico_RealG4Life.docx); las partes individuales están en la carpeta [`docs/`](docs/).

---

## Equipo

**RealG4Life** — Pontificia Universidad Javeriana · Desarrollo de Software

| Integrante | GitHub |
|---|---|
| _Nombre_ | [@Tomas23038](https://github.com/Tomas23038) |
| _Nombre_ | _@usuario_ |
| _Nombre_ | _@usuario_ |
| _Nombre_ | _@usuario_ |
