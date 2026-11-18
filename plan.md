# Plan de desarrollo — CoWorkHub

> Comparación entre lo que pide `requerimientos.md` y el estado actual del proyecto, y fases para cerrar la brecha.
> Cada fase termina con **un commit** (entregable 8.1).

---

## 0. Estado actual (diagnóstico)

| Área | Estado | Observación |
|---|---|---|
| Proyecto base (Initializr, Java 21, Boot 4.1.3, Maven) | ✅ Hecho | `pom.xml` correcto. |
| Starters: web, JPA, validation, security, actuator, springdoc, MySQL, Lombok | ✅ Hecho | Están todas las dependencias principales. |
| Dependencia para JWT | ❌ Falta | Agregar `spring-boot-starter-oauth2-resource-server` (trae Nimbus para firmar y validar JWT, sin librerías de terceros). |
| MySQL en Docker (`coworkhub-mysql`, puerto 3306) | ✅ Corriendo | Verificado con `docker ps`. |
| `application.properties` | ⚠️ Parcial | Puerto **8084** (8080 está ocupado en el equipo; se documenta en el README). `ddl-auto=update` → debe ser **`create`** para que se ejecute `import.sql`. La contraseña está en texto plano: pasarla a `${DB_PASSWORD:coworkhub}`. |
| Entidades, repositorios, servicios, controladores | ❌ No existen | Solo está `CoworkhubApplication`. |
| `import.sql` | ❌ No existe | |
| Seguridad | ⚠️ Por defecto | Spring Security bloquea todo con un usuario generado; no hay configuración propia. |
| Swagger | ⚠️ Solo la dependencia | Falta la configuración (`/doc/swagger-ui.html`, grupos, Authorize). |
| Repositorio git | ❌ No inicializado | Hace falta para "un commit por fase": `git init` en la fase 1. |
| `README.md`, `docs/openapi.json`, colección de Postman | ❌ No existen | |
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

- [ ] `git init` y primer commit con el estado actual.
- [ ] `application.properties`: `server.port=8084`, `ddl-auto=create`, contraseña por variable de entorno, `springdoc.swagger-ui.path=/doc/swagger-ui.html`, `management.endpoints.web.exposure.include=health`.
- [ ] Propiedades propias del negocio con `@Value` (por ejemplo `coworkhub.jwt.secret`, `coworkhub.jwt.expiration-minutes=60`, `coworkhub.booking.min-advance-hours=1`).
- [ ] `AppConfig` con `@Configuration`:
  - `@Bean Clock clock()` → `Clock.systemDefaultZone()`.
  - `@Bean PasswordEncoder` → `BCryptPasswordEncoder`.
  - `@PostConstruct` que valide la configuración (por ejemplo, un secreto JWT de ≥ 32 bytes y una expiración > 0); si falla, la aplicación no arranca.
- [ ] Un `@Primary` o `@Qualifier`: por ejemplo dos estrategias de `CostCalculator`, o dos `Clock` (`systemClock` como `@Primary` y otro fijo para pruebas).
- [ ] Verificar `GET /actuator/health` → `{"status":"UP"}` (RF-15). Por ahora se permite en público con una `SecurityFilterChain` mínima.

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

- [ ] `/api/sedes`, `/api/equipamientos`, `/api/servicios-adicionales`, `/api/planes`: CRUD.
- [ ] `/api/salas` con `?sedeId=` y una lista `equipmentIds` en la entrada.
- [ ] `/api/miembros`: crear `Member` y `UserAccount` en **una misma transacción** (rol `MEMBER`, contraseña con BCrypt); filtros `nombre` y `estado`; `PATCH /suspender` y `/activar`.
- [ ] RN-11: validar duplicados con `existsBy…` → `409` (además de la restricción en la base).
- [ ] RN-12: antes de eliminar, verificar el uso (`existsByLocationId`, `existsByPlanId`, `existsByRoomId`, …) → `409`.
- [ ] `DELETE` responde `200` sin cuerpo; `POST` responde `201`.

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

- [ ] `BusinessRuleException(String codigo, String mensaje)` → `409`; `ResourceNotFoundException` → `404`; `DuplicateResourceException` → `409`.
- [ ] `GlobalExceptionHandler` (`@RestControllerAdvice`):
  - `MethodArgumentNotValidException`, `HttpMessageNotReadableException` y `MethodArgumentTypeMismatchException` → `400` (escenario 12).
  - `DataIntegrityViolationException` → `409` (respaldo de RN-11 y RN-12).
  - `AccessDeniedException` → `403`.
  - `Exception` → `500` genérico (no debería alcanzarlo ninguna regla de negocio).
- [ ] Todas las respuestas con `ErrorResponse(error, codigo, timestamp)`.

**Commit:** `fase 5: manejo de excepciones`

### Fase 6 — Swagger (módulo 06) — RF-13

- [ ] `OpenApiConfig`: `@OpenAPIDefinition` con título y descripción, y `@SecurityScheme(type = HTTP, scheme = "bearer", bearerFormat = "JWT")` para el botón **Authorize**.
- [ ] Grupos con `GroupedOpenApi`: Autenticación, Catálogos, Miembros, Reservas.
- [ ] En los controladores: `@Tag`, `@Operation`, `@ApiResponse` para 400/401/403/404/409 con el esquema `ErrorResponse`, y `@Schema(example=…)` en los DTO.
- [ ] Exportar `docs/openapi.json` (`curl http://localhost:8084/v3/api-docs -o docs/openapi.json`).

**Commit:** `fase 6: documentación Swagger`

### Fase 7 — Seguridad (módulo 08) — RF-11, RF-12, RF-14, RN-13

**7a. Basic Auth (commit propio, como pide el módulo):**
- [ ] `UserDetailsServiceImpl` que lee `UserAccount` y concede la authority `ROLE_<role>`.
- [ ] `SecurityFilterChain` con `httpBasic()`, `csrf` deshabilitado y `@EnableMethodSecurity`.

**7b. JWT stateless:**
- [ ] `POST /auth/login` → `AuthenticationManager.authenticate` → JWT HS256 que vence en 60 min, con los claims `sub=username`, `role` y `memberId` (`JwtEncoder` de Nimbus).
- [ ] `oauth2ResourceServer().jwt()` con `JwtDecoder` del mismo secreto y un `JwtAuthenticationConverter` que mapea `role` a `ROLE_…`.
- [ ] `SessionCreationPolicy.STATELESS`.
- [ ] Públicos: `/auth/login`, `/actuator/health`, `/doc/**`, `/v3/api-docs/**`.
- [ ] `GET /auth/perfil` → `{username, role, miembroId}`.
- [ ] `AuthenticationEntryPoint` (`401`) y `AccessDeniedHandler` (`403`) que escriben `ErrorResponse` (escenario 10).

**Permisos con `@PreAuthorize`:**
- Catálogos: escritura `hasRole('ADMIN')`, lectura `isAuthenticated()`.
- Miembros: `hasAnyRole('ADMIN','RECEPTIONIST')`; en consultar uno y en consumo, `or @ownership.isMember(#id)`.
- Reservas: un bean `@ownership` con `isBookingOwner(#id)` y `isMember(#miembroId)`; al crear, si el rol es `MEMBER`, se fuerza `memberId` al del token (escenario 9).

**Commits:** `fase 7a: basic auth` y `fase 7b: JWT`

### Fase 8 — Pruebas y entregables

- [ ] Tests unitarios (con `Clock` fijo) de `CostCalculator`, de la cancelación (30 h / 5 h / 1 h) y de RN-02 y RN-03.
- [ ] Test de integración de la concurrencia de RN-01 (escenario 13).
- [ ] Tests con `@WebMvcTest` o `MockMvc` para 401 y 403 (escenarios 9 y 10).
- [ ] Colección de Postman con los **14 escenarios**: login como `admin`, `recepcion` y un miembro, guardando el token en una variable de la colección. Guardarla en `docs/coworkhub.postman_collection.json`.
- [ ] `docs/openapi.json` actualizado.
- [ ] `README.md`: base con Docker o con MySQL instalado, `mvn spring-boot:run`, usuarios y contraseñas de prueba, y la URL `http://localhost:8084/doc/swagger-ui.html`.

**Commit:** `fase 8: pruebas y documentación`

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
- **Boot 4.1 + springdoc 3.2.1:** verificar al arrancar que Swagger UI cargue; si falla, ajustar la versión de springdoc.
- **Zona horaria:** la base, el `Clock` y `CURDATE()` tienen que coincidir. Fijar `spring.jpa.properties.hibernate.jdbc.time_zone` y la zona del `Clock`.
- **Horas "devueltas" al cancelar (RN-08):** se modela excluyendo las reservas `CANCELLED` del consumo. Con la cancelación tardía se cobra el 50 % y las horas tampoco cuentan; confirmar esta interpretación con la rúbrica.
- **Falta `rubrica.md`:** pedirla para validar los criterios finos.
