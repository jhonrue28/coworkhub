# Plan de desarrollo — CoWorkHub

> Comparación entre lo que pide `requerimientos.md` y el estado del proyecto, y fases para cerrar la brecha.
> Cada fase termina con **un commit** (entregable 8.1). **Estado: fases 1 a 9 terminadas.**

---

## 0. Estado actual

> Actualizado al terminar la fase 9. El diagnóstico inicial (antes de la fase 1) era: solo existía
> `CoworkhubApplication`, sin git, sin `import.sql`, sin seguridad propia ni Swagger configurado.

| Área | Estado | Dónde |
|---|---|---|
| Proyecto base (Initializr, Java 21, Boot 4.1.3, Maven) | ✅ Hecho | `pom.xml` |
| Starters: web, JPA, validation, security, actuator, springdoc, MySQL, Lombok | ✅ Hecho | `pom.xml` |
| Dependencia para JWT | ✅ Hecho (fase 7b) | En Boot 4 se llama `spring-boot-starter-security-oauth2-resource-server` |
| MySQL en Docker (`coworkhub-mysql`, puerto 3306) | ✅ Corriendo | `docker ps` |
| `application.properties` | ✅ Hecho | Puerto 8084, `ddl-auto=create`, `${DB_PASSWORD}` y `${JWT_SECRET}` |
| Entidades, repositorios, servicios, controladores | ✅ Hecho (fases 2 a 4) | `persistence/`, `business/`, `presentation/` |
| `import.sql` | ✅ Hecho (fase 2) | `src/main/resources/import.sql` |
| Excepciones y cuerpo de error único | ✅ Hecho (fase 5) | `GlobalExceptionHandler` |
| Swagger | ✅ Hecho (fase 6) | `/doc/swagger-ui.html`, grupos y Authorize |
| Seguridad: Basic Auth y luego JWT, roles y dueño | ✅ Hecho (fases 7a y 7b) | `config/SecurityConfig`, `business/security/` |
| Pruebas | ✅ 38 tests | `mvnw test` |
| `README.md`, `docs/openapi.json`, colección de Postman | ✅ Hecho (fase 8) | 14 escenarios, 46 verificaciones con newman |
| Panel web (frontend) | ✅ Hecho (fase 9) | `src/main/resources/static/` |
| Repositorio git | ✅ Un commit por fase | `git log` |
| Capturas de evidencia | ⏳ Pendiente | Tomarlas y guardarlas en `docs/evidencias/` (README, sección 12) |
| `rubrica.md` | ❌ No está en el proyecto | El requerimiento la referencia; conviene conseguirla para verificar contra ella. |

---

## 1. Arquitectura objetivo

```
com.academy.coworkhub
├── config/                  @Configuration: Clock, PasswordEncoder, OpenAPI, Security, JWT
├── presentation/
│   ├── controller/          @RestController (uno por recurso)
│   ├── dto/request/         records de entrada con @Valid
│   ├── dto/response/        records de salida (nunca entidades)
│   └── advice/              @RestControllerAdvice + ErrorResponse
├── business/
│   ├── service/             interfaces
│   ├── service/impl/        implementaciones (@Transactional en escrituras)
│   ├── exception/           BusinessRuleException(codigo), NotFoundException, ...
│   └── mapper/              entidad ↔ DTO (métodos estáticos o una clase simple)
└── persistence/
    ├── entity/              entidades JPA + enums
    └── repository/          JpaRepository con consultas derivadas y @Query
```

**Cuerpo de error único:** `record ErrorResponse(String error, String codigo, Instant timestamp)`.

---

## 2. Fases

### Fase 1 — Proyecto base y configuración (módulos 01, 02)

- [x] `git init` y primer commit con el estado actual.
- [x] `application.properties`: `server.port=8084`, `ddl-auto=create`, contraseña por variable de entorno, `springdoc.swagger-ui.path=/doc/swagger-ui.html`, `management.endpoints.web.exposure.include=health`.
- [x] Propiedades propias del negocio con `@Value` (por ejemplo `coworkhub.jwt.secret`, `coworkhub.jwt.expiration-minutes=60`, `coworkhub.booking.min-advance-hours=1`).
- [x] `AppConfig` con `@Configuration`:
  - `@Bean Clock clock()` → `Clock.systemDefaultZone()`.
  - `@Bean PasswordEncoder` → `BCryptPasswordEncoder`.
  - `@PostConstruct` que valide la configuración (por ejemplo, un secreto JWT de ≥ 32 bytes y una expiración > 0); si falla, la aplicación no arranca.
- [x] Un `@Primary` o `@Qualifier`: por ejemplo dos estrategias de `CostCalculator`, o dos `Clock` (`systemClock` como `@Primary` y otro fijo para pruebas).
- [x] Verificar `GET /actuator/health` → `{"status":"UP"}` (RF-15). Por ahora se permite en público con una `SecurityFilterChain` mínima.

**Commit:** `fase 1: configuración base`

### Fase 2 — Modelo JPA e `import.sql` (módulos 03, 04)

**Enums:** `RoomType`, `MemberStatus`, `BookingStatus`, `Role` → `@Enumerated(EnumType.STRING)`.

**Entidades y lado dueño de cada relación (decisión):**

| Relación | Dueño | Mapeo |
|---|---|---|
| Location 1→N Room | `Room` | `@ManyToOne(fetch = LAZY) location`; `Location` con `@OneToMany(mappedBy, LAZY)` |
| Room N↔N Equipment | `Room` | `@ManyToMany(LAZY) @JoinTable(name="room_equipment")` |
| MembershipPlan 1→N Member | `Member` | `@ManyToOne(LAZY) plan` |
| Member 0..1↔0..1 UserAccount | `UserAccount` | `@OneToOne(LAZY) @JoinColumn(name="member_id", unique=true, nullable=true)` (los empleados quedan con `null`) |
| Member 1→N Booking | `Booking` | `@ManyToOne(LAZY) member` |
| Room 1→N Booking | `Booking` | `@ManyToOne(LAZY) room` |
| Booking 1→N BookingItem | `BookingItem` | `Booking` con `@OneToMany(mappedBy, cascade = ALL, orphanRemoval = true, LAZY)` |
| AdditionalService 1→N BookingItem | `BookingItem` | `@ManyToOne(LAZY) service` |

**Unicidad (RN-11) con `@UniqueConstraint` / `unique = true`:**
`members.document_number`, `members.email`, `user_accounts.username`, `rooms(location_id, name)`, `equipment.name`, `additional_services.name`, `membership_plans.name`.

**Montos:** `BigDecimal` con `@Column(precision = 10, scale = 2)`.

**Repositorios:** uno por entidad, con consultas derivadas como `existsByName`, `findByLocationId`, `existsByRoomId`, `findByFullNameContainingIgnoreCaseAndStatus` y `existsByPlanId`.
**Sin N+1:** usar `@EntityGraph` o `JOIN FETCH` en los listados de salas (con sede y equipamiento) y de reservas (con sala, miembro e ítems).

**`src/main/resources/import.sql`** (una sentencia por línea, en orden de clave foránea):

| Datos | Cantidad |
|---|---|
| locations | 2 |
| equipment | 4 |
| rooms | 6 (1 con `active=false`) + filas en `room_equipment` |
| additional_services | 3 |
| membership_plans | 3 |
| members | 5 (1 `SUSPENDED`) |
| user_accounts | 4: `admin`, `recepcion`, 2 `MEMBER` con `member_id` (contraseñas ya en BCrypt) |
| bookings + booking_items | 15 en todos los estados, algunas con ítems |

- Fechas relativas a hoy: `TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '10:00:00')`.
- Antes de escribirlo, correr la aplicación una vez y revisar los nombres reales de las columnas con `DESCRIBE`.
- Generar los hashes BCrypt con el `PasswordEncoder` (un test o un `main` temporal) y documentar las contraseñas en texto claro en el README.

**Commit:** `fase 2: modelo JPA e import.sql`

### Fase 3 — Catálogos y miembros (módulo 05) — RF-01, RF-02

Para cada recurso: DTO de entrada (`record` con `@NotBlank`, `@Positive`, …), DTO de salida, interfaz de servicio con su implementación, y controlador.

- [x] `/api/sedes`, `/api/equipamientos`, `/api/servicios-adicionales`, `/api/planes`: CRUD.
- [x] `/api/salas` con `?sedeId=` y una lista `equipmentIds` en la entrada.
- [x] `/api/miembros`: crear `Member` y `UserAccount` en **una misma transacción** (rol `MEMBER`, contraseña con BCrypt); filtros `nombre` y `estado`; `PATCH /suspender` y `/activar`.
- [x] RN-11: validar duplicados con `existsBy…` → `409` (además de la restricción en la base).
- [x] RN-12: antes de eliminar, verificar el uso (`existsByLocationId`, `existsByPlanId`, `existsByRoomId`, …) → `409`.
- [x] `DELETE` responde `200` sin cuerpo; `POST` responde `201`.

**Commit:** `fase 3: catálogos y miembros`

### Fase 4 — Reservas, disponibilidad y consumo (módulo 05) — núcleo del 30 %

#### `BookingService.create` (RF-04), validaciones en este orden

| Regla | Validación |
|---|---|
| RN-02 | `30 ≤ minutos ≤ 480`, `minutos % 30 == 0`, `start.toLocalDate() == end.toLocalDate()` |
| RN-03 | `start ≥ now(clock) + 1h`; `openingTime ≤ start.time` y `end.time ≤ closingTime` |
| RN-04 | `room.active` y `attendees ≤ capacity` |
| RN-05 | `member.status == ACTIVE` |
| RN-06 | `countByMemberIdAndStatusInAndStartTimeAfter(PENDING, CONFIRMED; now) < plan.maxActiveBookings` |
| RN-01 | Cruce: `existsByRoomIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(roomId, [PENDING, CONFIRMED], end, start)` (las reservas que solo se tocan en un extremo no se cruzan) |

**Concurrencia de RN-01 (escenario 13):** dentro de la transacción, **bloquear la fila de la sala** antes de revisar el cruce:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select r from Room r where r.id = :id")
Optional<Room> findByIdForUpdate(Long id);
```
Así la segunda solicitud espera a que la primera haga commit, ve la reserva nueva y responde `409`. Se prueba con un test de dos hilos y `CountDownLatch`.

#### Costo (RN-07): un único `CostCalculator`, que reusan create, servicios y consumo
```
horasReserva   = minutos / 60  (BigDecimal)
usadasMes      = suma de horas de las reservas PENDING/CONFIRMED/COMPLETED del miembro en ese mes (sin contar esta)
restantes      = max(0, includedHoursPerMonth − usadasMes)
excedentes     = max(0, horasReserva − restantes)
roomCost       = excedentes × hourlyRate × (1 − overageDiscount/100)
servicesCost   = Σ quantity × appliedUnitPrice   (appliedUnitPrice = unitPrice al momento de agregarlo)
totalCost      = roomCost + servicesCost
```
Redondear con `setScale(2, HALF_UP)`.

#### Transiciones (RN-09): método `Booking.transitionTo(nuevoEstado)` con tabla de transiciones permitidas
- `confirmar`: PENDING → CONFIRMED (RF-07)
- `completar`: CONFIRMED → COMPLETED (RF-09)
- `cancelar`: PENDING/CONFIRMED → CANCELLED (RF-08)
- Cualquier otra transición → `409 RN-09` (escenario 8).

#### Cancelación (RN-08)
```
h = horas entre now(clock) y startTime
h ≥ 24      → fee = 0      (las horas vuelven: una reserva CANCELLED no cuenta en usadasMes)
2 ≤ h < 24  → fee = roomCost × 0.5
h < 2       → 409 RN-08
```

#### Servicios (RF-06, RN-10)
`POST /api/reservas/{id}/servicios` y `DELETE …/{servicioId}`, solo con estado `PENDING` (si no, `409 RN-10`); después se recalcula el costo.

#### Disponibilidad (RF-03)
Salas `active` de la sede, dentro del horario de la sede, sin cruce, con `capacity ≥ capacidadMinima` y que tengan **todos** los `equipamientoIds` pedidos.

#### Consumo (RF-10)
`GET /api/miembros/{id}/consumo?mes=yyyy-MM` (`YearMonth`) → `{incluidas, usadas, restantes, excedentes}`.

#### Listados (RF-05)
`?miembroId=` · `?salaId=&fecha=` · `?estado=`: un solo filtro a la vez; sin filtro, `400`.

**Commit:** `fase 4: reservas, disponibilidad y consumo`

### Fase 5 — Excepciones (módulo 07)

- [x] `BusinessRuleException(String codigo, String mensaje)` → `409`; `ResourceNotFoundException` → `404`; `DuplicateResourceException` → `409`.
- [x] `GlobalExceptionHandler` (`@RestControllerAdvice`):
  - `MethodArgumentNotValidException`, `HttpMessageNotReadableException` y `MethodArgumentTypeMismatchException` → `400` (escenario 12).
  - `DataIntegrityViolationException` → `409` (respaldo de RN-11 y RN-12).
  - `AccessDeniedException` → `403`.
  - `Exception` → `500` genérico (no debería alcanzarlo ninguna regla de negocio).
- [x] Todas las respuestas con `ErrorResponse(error, codigo, timestamp)`.

**Commit:** `fase 5: manejo de excepciones`

### Fase 6 — Swagger (módulo 06) — RF-13

- [x] `OpenApiConfig`: `@OpenAPIDefinition` con título y descripción, y `@SecurityScheme(type = HTTP, scheme = "bearer", bearerFormat = "JWT")` para el botón **Authorize**.
- [x] Grupos con `GroupedOpenApi`: Autenticación, Catálogos, Miembros, Reservas.
- [x] En los controladores: `@Tag`, `@Operation`, `@ApiResponse` para 400/401/403/404/409 con el esquema `ErrorResponse`, y `@Schema(example=…)` en los DTO.
- [x] Exportar `docs/openapi.json` (`curl http://localhost:8084/v3/api-docs -o docs/openapi.json`).

**Commit:** `fase 6: documentación Swagger`

### Fase 7 — Seguridad (módulo 08) — RF-11, RF-12, RF-14, RN-13

**7a. Basic Auth (commit propio, como pide el módulo):**
- [x] `UserDetailsServiceImpl` que lee `UserAccount` y concede la authority `ROLE_<role>`.
- [x] `SecurityFilterChain` con `httpBasic()`, `csrf` deshabilitado y `@EnableMethodSecurity`.

**7b. JWT stateless:**
- [x] `POST /auth/login` → `AuthenticationManager.authenticate` → JWT HS256 que vence en 60 min, con los claims `sub=username`, `role` y `memberId` (`JwtEncoder` de Nimbus).
- [x] `oauth2ResourceServer().jwt()` con `JwtDecoder` del mismo secreto y un `JwtAuthenticationConverter` que mapea `role` a `ROLE_…`.
- [x] `SessionCreationPolicy.STATELESS`.
- [x] Públicos: `/auth/login`, `/actuator/health`, `/doc/**`, `/v3/api-docs/**`.
- [x] `GET /auth/perfil` → `{username, role, miembroId}`.
- [x] `AuthenticationEntryPoint` (`401`) y `AccessDeniedHandler` (`403`) que escriben `ErrorResponse` (escenario 10).

**Permisos con `@PreAuthorize`:**
- Catálogos: escritura `hasRole('ADMIN')`, lectura `isAuthenticated()`.
- Miembros: `hasAnyRole('ADMIN','RECEPTIONIST')`; en consultar uno y en consumo, `or @ownership.isMember(#id)`.
- Reservas: un bean `@ownership` con `isBookingOwner(#id)` y `isMember(#miembroId)`; al crear, si el rol es `MEMBER`, se fuerza `memberId` al del token (escenario 9).

**Commits:** `fase 7a: basic auth` y `fase 7b: JWT`

### Fase 8 — Pruebas y entregables

- [x] Tests unitarios (con `Clock` fijo) de `CostCalculator`, de la cancelación (30 h / 5 h / 1 h) y de RN-02 y RN-03.
- [x] Test de integración de la concurrencia de RN-01 (escenario 13).
- [x] Tests con `@WebMvcTest` o `MockMvc` para 401 y 403 (escenarios 9 y 10).
- [x] Colección de Postman con los **14 escenarios**: login como `admin`, `recepcion` y un miembro, guardando el token en una variable de la colección. Guardarla en `docs/coworkhub.postman_collection.json`.
- [x] `docs/openapi.json` actualizado.
- [x] `README.md`: base con Docker o con MySQL instalado, `mvn spring-boot:run`, usuarios y contraseñas de prueba, y la URL `http://localhost:8084/doc/swagger-ui.html`.

**Commit:** `fase 8: pruebas y documentación`

### Fase 9 — Panel web (frontend)

No lo pide `requerimientos.md` ("No hay interfaz gráfica"), pero sí la lista de entregables posterior ("Código fuente del frontend y backend").

- [x] HTML, CSS y JavaScript sin frameworks en `src/main/resources/static/` (`index.html`, `styles.css`, `app.js`): Spring Boot los sirve en `http://localhost:8084/`, en el mismo origen que la API, así que no hace falta CORS.
- [x] Login con `POST /auth/login`; token en `sessionStorage`; vuelve al login ante un `401`.
- [x] Menú según el rol (`/auth/perfil`): el `MEMBER` no ve Miembros, Equipamiento ni Servicios adicionales; solo el `ADMIN` edita catálogos.
- [x] Vistas: inicio (consumo del miembro o reservas por confirmar), disponibilidad, reservas (crear, detalle, servicios, confirmar, completar, cancelar), miembros y CRUD de catálogos.
- [x] `SecurityConfig`: `GET /`, `/index.html`, `/app.js` y `/styles.css` públicos; los datos siguen con JWT.
- [x] Test de que el panel carga sin token (`SecurityTest`).
- [x] Corrección tras la prueba manual: "Ver" no abría el detalle de reservas no pendientes, y tras confirmar el detalle quedaba desactualizado (`addForm?.` sobre `false`).

**Commit sugerido:** `fase 9: panel web`

---

## 2.1. Decisiones que se apartaron del plan

| Plan | Qué se hizo | Por qué |
|---|---|---|
| Fase 7: al crear una reserva, si el rol es `MEMBER`, se fuerza `memberId` al del token | Responde `403` si un `MEMBER` reserva a nombre de otro | `requerimientos.md` §5: "Si pide los de otro miembro, recibe `403`". |
| Fase 4: un `CostCalculator` único | El cálculo vive en un método privado de `BookingServiceImpl` que reusan crear, servicios y consumo | Mismo efecto (una sola regla) sin una clase extra; lo cubre el escenario 1 de `BookingServiceTest`. |
| Fase 6/8: `docs/openapi.json` desde `/v3/api-docs` | Se exporta desde `/v3/api-docs/todo` | Con grupos de springdoc, el grupo "todo" contiene toda la API. |
| Lista de entregables: `migration.sql` en `resources` | Los datos siguen en `import.sql` | Decisión del autor: no cambiar el nombre; `requerimientos.md` exige `import.sql`. |

---

## 3. Trazabilidad de los escenarios de aceptación

| # | Se cubre en | Clave |
|---|---|---|
| 1, 3 | Fase 4 | Creación + CostCalculator; cruce con `<` / `>` estrictos |
| 2 | Fase 4 | RN-01 |
| 4 | Fase 4 | RN-02 / RN-03 |
| 5, 6, 7 | Fase 4 | RN-08 con `Clock` |
| 8 | Fase 4 | RN-09 |
| 9 | Fase 7 | `@ownership.isBookingOwner` |
| 10 | Fase 7 | EntryPoint 401 |
| 11 | Fase 3 | RN-12 |
| 12 | Fase 5 | `@Valid` → 400 |
| 13 | Fase 4 | Bloqueo pesimista de la sala |
| 14 | Fase 3 | Filtro `estado` |

---

## 4. Riesgos y decisiones abiertas

- **`ddl-auto=create` borra los datos** en cada arranque: es lo esperado según el requerimiento, pero ningún dato cargado a mano sobrevive.
- **Boot 4.1 + springdoc 3.2.1:** ✅ verificado: Swagger UI carga y el `openapi.json` se exporta.
- **Zona horaria:** la base, el `Clock` y `CURDATE()` tienen que coincidir. Fijar `spring.jpa.properties.hibernate.jdbc.time_zone` y la zona del `Clock`.
- **Horas "devueltas" al cancelar (RN-08):** se modela excluyendo las reservas `CANCELLED` del consumo. Con la cancelación tardía se cobra el 50 % y las horas tampoco cuentan; confirmar esta interpretación con la rúbrica.
- **Falta `rubrica.md`:** pedirla para validar los criterios finos.
