# TeamBoard - multi-tenant project management SaaS

Every company gets its **own private workspace**. Admins add people, managers create projects and tasks,
members move work across a live Kanban board. Nobody can ever see another company's data.

**Stack:** Java 21, Spring Boot 3.3, Spring Security (JWT + refresh tokens), Spring Data JPA, WebSocket (STOMP),
Spring Cache + Redis, MySQL 8, Spring Mail, React 18 + TypeScript + Vite,
Docker, JUnit 5 + Mockito, GitHub Actions.

## Run it on your computer

You need **Java 21**, **Maven**, **Node 20** and **MySQL 8** running on port 3306.

**Step 0 - MySQL** (only if your MySQL password is not `root`)

The backend logs in as user `root` with password `root`. If yours is different, set it before step 1.
On Windows Command Prompt:
```
set DB_PASSWORD=your-mysql-password
```
The database `teamboard` is created for you. Nothing else to set up.

**Step 1 - backend** (new terminal)
```bash
cd backend
mvn spring-boot:run
```
Wait for `Started TeamBoardApplication`. It uses port **8081**. The tables are created automatically in the
`teamboard` database, and demo data for two companies is added on the first start.

**Step 2 - frontend** (another terminal)
```bash
cd frontend
npm install
npm run dev
```

**Step 3** - open http://localhost:5173

Tests: `cd backend && mvn test`. API docs (local only): http://localhost:8081/swagger-ui.html

## Demo logins (local / demo mode only)

| Company | Role | Email | Password |
|---|---|---|---|
| Acme Technologies | Admin | admin@acme.com | Admin@123 |
| Acme Technologies | Manager | manager@acme.com | Manager@123 |
| Acme Technologies | Member | ravi@acme.com | Member@123 |
| Globex Corp | Admin | admin@globex.com | Admin@123 |

**See multi-tenancy working:** log in as `admin@globex.com`. You see only Globex's project.
Log in as `admin@acme.com` and you see only Acme's two projects. Same app, same database, separate data.
You can also click **Create a workspace** on the login page to sign up a brand-new company.

**See real-time working:** open two browsers (or one normal and one private window). Log in as the Acme manager in one
and as Ravi in the other, both on the WEB project board. Move a task in one window and it moves in the other.
Assign Ravi a task and his bell lights up straight away.

## Run with Docker (MySQL + Redis)

```bash
docker compose up --build
```
Open http://localhost:3000. Same demo logins. For a real server use `SPRING_PROFILES_ACTIVE=prod` with
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` set (and `CACHE_TYPE=redis` to use Redis).

## How multi-tenancy works (shared database, shared schema)

- Every company is an `Organization`. Users, projects, tasks, audit logs all belong to one.
- The logged-in person is an `AuthUser(id, orgId, role ...)`, resolved from the access token on every request.
- **Every repository query includes the organization id** (`findByIdAndOrganizationId(id, orgId)`).
  A record from another company is "not found", never "forbidden", so its existence is not even revealed.
- WebSocket topics are per company (`/topic/org/{orgId}/tasks`), and the server refuses a subscription to anyone else's topic.

## Security design

| Topic | What is done |
|---|---|
| Login | BCrypt passwords, 8+ chars with a letter and number, 5 wrong tries blocks an email for 15 minutes |
| Tokens | Access token (JWT, 15 min) + refresh token (random, 7 days). Only the **hash** of the refresh token is stored |
| Refresh rotation | Each refresh token works once. Reusing an old one signs the user out everywhere |
| Roles | Admin, Manager, Member: `@PreAuthorize` on endpoints plus ownership rules in services |
| Temporary passwords | Admin-created accounts must change their password at first login (enforced by the server) |
| Deactivated users | Lose access at once (the cached user is evicted) and their refresh tokens are revoked |
| WebSocket | Token checked on connect, subscriptions restricted, clients cannot publish |

## Where each feature lives

| Feature | Files |
|---|---|
| JWT + refresh tokens | `security/JwtService`, `service/RefreshTokenService`, `service/AuthService` |
| Tenant isolation | every `*Repository` and `service/*Service` (look for `orgId`) |
| Role rules | `service/TaskService` (Javadoc at the top), controllers (`@PreAuthorize`) |
| Real-time | `config/WebSocketConfig`, `realtime/*`, `frontend/src/realtime.ts` |
| Redis caching | `service/DashboardService`, `security/AuthUserService`, `application-prod.properties` |
| Email alerts | `service/EmailService` (turn on with `MAIL_ENABLED=true` and `SPRING_MAIL_*`) |
| Audit log | `service/AuditService`, `/api/audit` (paged) |
| Overdue reminders | `scheduler/OverdueReminderJob` (daily 9 AM) |
| Kanban board | `frontend/src/pages/Board.tsx` |

## Roles

| | Admin | Manager | Member |
|---|---|---|---|
| Add people, change roles, reset passwords | yes | no | no |
| Create / archive projects | yes | yes | no |
| Create tasks | yes | yes | yes |
| Edit or reassign any task, delete tasks | yes | yes | no |
| Edit tasks they created | yes | yes | yes |
| Move tasks | any | any | assigned to or created by them |
| Comment on tasks | yes | yes | yes |
| See the activity log | yes | yes | no |

## Interview talking points

- Shared-schema multi-tenancy, why `orgId` is on every query, and what the alternatives are (schema per tenant, database per tenant)
- Access token vs refresh token, rotation, and reuse detection
- Why cached users are evicted when a role changes (and the cache is `simple` locally, Redis in Docker)
- Sending WebSocket messages only after the database commit (`AfterCommit`)
- Pessimistic lock on the project row so task numbers (WEB-1, WEB-2 ...) never repeat
- DTO records so entities never reach the API

## Known limits (honest list)

- Tables are created by Hibernate `ddl-auto=update`. For production use Flyway migrations.
- Tokens are kept in browser `localStorage`. HttpOnly cookies are safer against XSS.
- Login attempt blocking is in memory, so it is per server instance.
- A WebSocket stays open after its token expires until the page reconnects.
- No file attachments, SSO, or password reset by email yet.
- If you change an entity later, drop the `teamboard` database (`DROP DATABASE teamboard;`) so it is rebuilt.
