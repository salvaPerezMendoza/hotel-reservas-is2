# Hotel Aurora · Sistema de reservas

TP final de **Ingeniería de Software II (FIUBA)**, Grupo 11.

Sistema web completo para un hotel ficticio de 18 habitaciones, con:

- **Sitio para huéspedes.** Buscar fechas, ver el precio noche por noche, reservar, pagar la seña o el total, modificar y cancelar.
- **Panel de recepción.** Llegadas y salidas del día, check-in y check-out, cobros, consumos, reservas telefónicas y estado de las habitaciones.
- **Panel de administración.** Reportes con exportación a Excel, tipos y habitaciones, tarifas por temporada, bloqueos, usuarios, políticas y auditoría.

> La pasarela de pago es **simulada**: espera unos segundos y aprueba. No se cobra nada ni se guardan datos de tarjeta.

---

## Cómo levantarlo

### Opción A: Docker (recomendada)

Requiere Docker Desktop.

```bash
docker compose up --build
```

| Qué | Dónde |
|---|---|
| Sitio y paneles | http://localhost:3000 |
| Mails que manda el sistema (Mailpit) | http://localhost:8025 |
| API | http://localhost:8080/api |

La primera vez carga datos de demostración: 18 habitaciones, temporadas, unas 300 reservas de los últimos dos meses y huéspedes de ejemplo. Para empezar de cero: `docker compose down -v`.

### Opción B: sin Docker, para desarrollar

Requiere Java 21, Maven, Node 20 y PostgreSQL 16, con una base `hotel` y usuario y contraseña `postgres`.

```bash
# Backend: crea las tablas con Flyway y carga los datos de demo
cd backend
mvn spring-boot:run

# Frontend, en otra terminal
cd frontend
npm install
npm run dev        # http://localhost:5173
```

Si la base usa otros datos de conexión: `DB_URL`, `DB_USER` y `DB_PASSWORD`.

## Cuentas de demostración

También aparecen como botones en la pantalla de ingreso.

| Rol | Email | Contraseña |
|---|---|---|
| Huésped | huesped@demo.com | Huesped123! |
| Recepción | recepcion@hotelaurora.com | Recepcion123! |
| Administración | admin@hotelaurora.com | Admin123! |

## Recorrido sugerido para la demo

1. **Huésped.** Buscá fechas que crucen el comienzo del verano (por ejemplo, del 13 al 21 de diciembre). La tira de noches muestra cómo cambia el precio entre temporada baja y alta.
2. **Reserva.** Al reservar, la habitación queda retenida 10 minutos mientras pagás; se ve la cuenta regresiva. Pagá la seña: tarda unos segundos y queda confirmada.
3. **Pago rechazado.** En "Opciones de demostración" del pago se puede simular un rechazo del banco.
4. **Mis reservas.** Desde ahí se cambian fechas o habitación, y se cancela. Antes de confirmar la cancelación muestra cuánto se reintegra según la política vigente.
5. **Recepción.** En "Hoy" están las llegadas y salidas. Hacé un check-in: si la habitación asignada sigue ocupada, el sistema propone otra del mismo tipo. Agregá un consumo e intentá el check-out: no se puede con saldo pendiente hasta registrar el pago.
6. **Administración.** En Configuración cambiá la política de cancelación o la estrategia de precio; afecta a las reservas nuevas sin tocar código. En Reportes se ven ocupación e ingresos y se puede descargar el Excel.

## Arquitectura

```
┌────────────────────┐   /api (JSON + JWT)   ┌───────────────────────────┐      ┌────────────┐
│ React + TypeScript │ ────────────────────▶ │ Spring Boot (Java 21)     │ ───▶ │ PostgreSQL │
│ (Vite, Tailwind)   │                       │ web → servicio → dominio  │      │ 16         │
│ servido por nginx  │ ◀──────────────────── │      ↘ repositorio (JPA)  │      └────────────┘
└────────────────────┘                       └────────────┬──────────────┘
                                                          │ SMTP
                                                    ┌─────▼─────┐
                                                    │  Mailpit  │
                                                    └───────────┘
```

**Backend** (`backend/src/main/java/ar/fiuba/is2/hotel`), por capas:

| Paquete | Responsabilidad |
|---|---|
| `dominio` | Entidades y reglas de negocio. No depende de la web. |
| `repositorio` | Acceso a datos con Spring Data JPA. |
| `servicio` | Casos de uso y transacciones. Coordina dominio, repositorios, pagos y mails. |
| `web` | Controladores REST, DTOs y manejo de errores. |
| `seguridad` | Login con JWT y permisos por rol. |
| `config` | Configuración y datos de demostración. |

El esquema de la base está versionado con **Flyway** (`backend/src/main/resources/db/migration`).

**Frontend** (`frontend/src`): `paginas/publico` es el sitio del huésped, `paginas/panel` la recepción y `paginas/admin` la administración. El panel se descarga aparte, así el huésped no baja los gráficos.

## Conceptos de la materia en el código

| Concepto | Dónde está |
|---|---|
| **Algoritmo de disponibilidad** | `dominio/disponibilidad/VerificadorDeDisponibilidad` y `Estadia`. Dos estadías chocan si se superponen en al menos una noche; el día de salida queda libre para otra llegada. |
| **Strategy (precio)** | `dominio/politica/precio`. `PoliticaDePrecio` con `PrecioPorTemporada` y `PrecioConDescuentoPorEstadiaLarga` (10% desde 7 noches). Se elige en Configuración. |
| **Strategy + Template Method (cancelación)** | `dominio/politica/cancelacion`. `PoliticaDeCancelacion` define el cálculo y cada política (Flexible, Moderada, Estricta) sus plazos. |
| **Factory** | `TipoPoliticaPrecio.crear()` y `TipoPoliticaCancelacion.crear()` crean la estrategia guardada en la configuración. |
| **Herencia y polimorfismo** | `dominio/usuario`. `Usuario` abstracto con `Huesped`, `Recepcionista` y `Administrador`; cada uno responde qué puede hacer. |
| **Encapsulamiento** | `dominio/reserva/Reserva`. Los cambios de estado (pagar, cancelar, check-in, check-out) solo pasan por sus métodos, que validan las reglas. |
| **Concurrencia (sin overbooking)** | Restricción `EXCLUDE` de PostgreSQL en `V1__esquema_inicial.sql` más un bloqueo pesimista al reservar. Aunque dos personas reserven la última habitación al mismo tiempo, solo una lo logra. |
| **Retención durante el pago** | La reserva online nace "pendiente de pago" y retiene la habitación 10 minutos. `TareasProgramadas` libera las vencidas. |
| **Auditoría** | `servicio/AuditoriaService`: cada acción relevante queda registrada con usuario y fecha. |
| **Notificaciones** | `servicio/notificacion`. Los mails salen después de confirmar la transacción y en segundo plano (`@TransactionalEventListener` + `@Async`). |

## Tests

```bash
cd backend
mvn verify
```

- **Unitarios de dominio:** estadías, disponibilidad, políticas de precio y cancelación, ciclo de vida de la reserva.
- **De integración contra PostgreSQL real:**
  - el flujo completo de reserva y pago;
  - los permisos por rol;
  - la reserva telefónica;
  - una prueba con 8 reservas simultáneas de la última habitación, donde exactamente una gana y las otras 7 reciben 409.

Con Docker disponible, los tests de integración levantan su propia base (Testcontainers). Si no, usan la base de `TEST_DB_URL` (por defecto `localhost:5432/hotel_test`).

## API

Todas las rutas empiezan con `/api`. Errores con formato `{fecha, estado, mensaje, campos}`.

| Grupo | Acceso | Rutas principales |
|---|---|---|
| `auth` | público | `POST registro`, `POST login`, `GET/PUT me`, `POST recuperar`, `POST restablecer` |
| catálogo | público | `GET hotel`, `GET catalogo/tipos`, `GET disponibilidad?checkIn&checkOut&huespedes` |
| `reservas` | huésped | `POST`, `GET mias`, `GET {codigo}`, `POST {codigo}/pagos`, `PUT {codigo}`, `GET {codigo}/cancelacion`, `POST {codigo}/cancelar` |
| `recepcion` | recepción y administración | `tablero`, `reservas`, `llegadas`, `salidas`, `check-in`, `check-out`, `pagos`, `consumos`, `habitaciones` |
| `admin` | administración | `tipos`, `habitaciones`, `temporadas`, `tarifas`, `configuracion`, `bloqueos`, `usuarios`, `auditoria`, `reportes/*` |

## Variables de entorno del backend

| Variable | Para qué | Por defecto |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión a PostgreSQL | `localhost:5432/hotel`, `postgres` |
| `JWT_SECRET` | Firma de las sesiones | uno de desarrollo |
| `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT` | Envío de mails. Apagado, se escriben en el log. | `false`, `localhost`, `1025` |
| `PAGO_DEMORA_SEGUNDOS` | Cuánto tarda la pasarela simulada | `3` |
| `DEMO_DATA` | Cargar datos de demostración si la base está vacía | `true` |
| `FRONTEND_URL` | Base de los enlaces en los mails | `http://localhost:5173` |
