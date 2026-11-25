# CoWorkHub

API REST para gestionar las reservas de una red de coworking: sedes, salas, equipamiento,
servicios adicionales, planes de membresía, miembros y reservas, con control de horas
consumidas, cargos de cancelación y permisos por rol con JWT.

Java 21 · Spring Boot 4.1 · Maven · MySQL 8 · Spring Security (JWT) · springdoc-openapi.

## 1. Base de datos

La conexión está en `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/coworkhub
spring.datasource.username=coworkhub
spring.datasource.password=${DB_PASSWORD:coworkhub}
```

### Opción A: Docker (recomendada)

```bash
docker run -d --name coworkhub-mysql -e MYSQL_DATABASE=coworkhub -e MYSQL_USER=coworkhub -e MYSQL_PASSWORD=coworkhub -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 -v coworkhub-datos:/var/lib/mysql mysql:8.4
```

La primera vez, espere a que `docker logs coworkhub-mysql` muestre `ready for connections`.

| Para… | Comando |
|---|---|
| Ver si está corriendo | `docker ps` |
| Detenerlo / iniciarlo | `docker stop coworkhub-mysql` · `docker start coworkhub-mysql` |
| Consola de MySQL (contraseña `coworkhub`) | `docker exec -it coworkhub-mysql mysql -u coworkhub -p coworkhub` |
| Borrarlo con sus datos | `docker rm -f coworkhub-mysql` y `docker volume rm coworkhub-datos` |

### Opción B: MySQL instalado

```sql
CREATE DATABASE coworkhub;
CREATE USER 'coworkhub'@'localhost' IDENTIFIED BY 'coworkhub';
GRANT ALL PRIVILEGES ON coworkhub.* TO 'coworkhub'@'localhost';
```

Si usa otra contraseña, defínala en la variable de entorno `DB_PASSWORD` o en un archivo
`local.properties` en la raíz del proyecto (está fuera de git).

## 2. Ejecutar

```bash
mvn spring-boot:run        # o ./mvnw spring-boot:run (Windows: mvnw.cmd spring-boot:run)
```

- La API queda en **http://localhost:8084**. El puerto es 8084 y no 8080 porque el 8080
  estaba ocupado en el equipo de desarrollo; se cambia con `server.port`.
- `spring.jpa.hibernate.ddl-auto=create`: **cada arranque borra las tablas, las crea y
  carga `src/main/resources/import.sql`**. Siempre se empieza con los mismos datos; lo que
  se cree a mano se pierde al reiniciar.
- Salud del servicio: `GET http://localhost:8084/actuator/health` → `{"status":"UP"}`.

Pruebas automáticas (necesitan la base corriendo): `mvn test`.

### Variables de entorno opcionales

| Variable | Para qué | Por defecto |
|---|---|---|
| `DB_PASSWORD` | Contraseña de MySQL | `coworkhub` |
| `JWT_SECRET` | Secreto HS256 de los tokens (mínimo 32 bytes) | uno de desarrollo; **cámbielo fuera de desarrollo** |

## 3. Usuarios de prueba

Cargados por `import.sql` (las contraseñas están en BCrypt en la base):

| Usuario | Contraseña | Rol | Miembro |
|---|---|---|---|
| `admin` | `admin123` | `ADMIN` | — |
| `recepcion` | `recepcion123` | `RECEPTIONIST` | — |
| `ana` | `ana123` | `MEMBER` | 1 · Ana Gómez |
| `carlos` | `carlos123` | `MEMBER` | 2 · Carlos Ruiz |

Inicio de sesión:

```bash
curl -X POST http://localhost:8084/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}'
```

Devuelve `{"token": "...", "tipo": "Bearer", "expiraEn": "..."}`. El token vence a los 60
minutos y se envía en cada solicitud como `Authorization: Bearer <token>`.

| Rol | Qué puede hacer |
|---|---|
| `ADMIN` | Todo. Es el único que crea, modifica o elimina catálogos. |
| `RECEPTIONIST` | Gestiona los miembros y todas las reservas. |
| `MEMBER` | Consulta catálogos y disponibilidad. Crea, ve, modifica y cancela solo sus reservas, y ve solo su consumo. |

## 4. Documentación (Swagger)

- Swagger UI: **http://localhost:8084/doc/swagger-ui.html**
- OpenAPI: `http://localhost:8084/v3/api-docs/todo` (exportado en [`docs/openapi.json`](docs/openapi.json))

Para probar desde Swagger: ejecute `POST /auth/login`, copie el `token` y péguelo en
**Authorize**.

## 5. Colección de Postman

[`docs/coworkhub.postman_collection.json`](docs/coworkhub.postman_collection.json) tiene los
14 escenarios de aceptación con sus verificaciones.

1. Arranque la aplicación (así `import.sql` deja los datos iniciales).
2. En Postman: **Import** → el archivo → **Run collection** (en orden).

La carpeta *0. Autenticación* inicia sesión con `admin`, `recepcion`, `ana` y `carlos` y guarda
cada token en una variable de la colección. *0. Preparación* crea un plan sin horas
incluidas (para que el costo de la sala no sea 0) y una sede abierta de 00:00 a 23:59 para
los escenarios de cancelación, que se calculan desde la hora actual. Para volver a correrla,
reinicie la aplicación.

También se puede correr desde la terminal:

```bash
npx newman run docs/coworkhub.postman_collection.json
```

| # | Escenario | Resultado |
|---|---|---|
| 1 | Reservar una sala libre con 2 servicios | `201` con el costo calculado |
| 2 | Reservar en un horario que se cruza con otra reserva | `409` `RN-01` |
| 3 | Reservar de 10:00 a 11:00 cuando otra va de 09:00 a 10:00 | `201` |
| 4 | Reservar 45 min, o fuera del horario de la sede | `409` |
| 5 | Cancelar con 30 h de anticipación | `200`, cargo 0 |
| 6 | Cancelar con 5 h de anticipación | `200`, cargo del 50 % |
| 7 | Cancelar con 1 h de anticipación | `409` |
| 8 | Confirmar una reserva cancelada | `409` |
| 9 | Un miembro consulta la reserva de otro | `403` |
| 10 | Llamar a un endpoint protegido sin token, o con un token alterado | `401` |
| 11 | Eliminar una sala con reservas futuras | `409` |
| 12 | Crear una sede sin nombre | `400` |
| 13 | Enviar dos solicitudes simultáneas para la misma sala y horario | Una `201` y la otra `409` |
| 14 | Listar miembros con `?estado=SUSPENDED` | `200` solo con los miembros suspendidos |

## 6. Errores

Todo error responde con el mismo cuerpo:

```json
{"error": "La sala ya está reservada en ese horario", "codigo": "RN-01", "timestamp": "2030-06-03T13:00:00Z"}
```

`codigo` es la regla de negocio violada (`RN-xx`) o el tipo de error (`DATOS_INVALIDOS`,
`NO_ENCONTRADO`, `NO_AUTENTICADO`, `ACCESO_DENEGADO`, …).

## 7. Estructura

```
com.academy.coworkhub
├── config/                  Clock, PasswordEncoder, JWT, Security, OpenAPI
├── presentation/
│   ├── controller/          @RestController (uno por recurso) y /auth
│   ├── dto/request|response records de entrada (@Valid) y de salida
│   └── advice/              @RestControllerAdvice con el cuerpo de error único
├── business/
│   ├── service/(impl)       interfaces e implementaciones (@Transactional)
│   ├── security/            login JWT, UserDetailsService y dueño del recurso (@ownership)
│   └── exception/           excepciones de negocio
└── persistence/
    ├── entity/              entidades JPA y enums
    └── repository/          repositorios Spring Data
```
