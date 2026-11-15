# Native Windows RH demo — preparation, not a runnable deployment

14 November 2026. Source: publication-clean, 390bcee2bd744b0e3255cb94699534bbc0fc41c6.

Status: BLOCKED. The two PowerShell entry points are safe preparation placeholders: start performs read-only checks and fails explicitly; stop does nothing. They are not advertised as a working launcher. No Docker, MySQL or application has been started.

## Intended runtime

Five Java processes: Identity, Department, Employee, HR and Gateway; a separately provisioned native MySQL 8 instance with a NEW data directory and NEW port; Angular production files served by a loopback static server with SPA fallback. No Discovery, Docker Desktop, Payroll, Fees, Invoice or Customer process. Keep all source modules.

Prefer Gateway localhost:8080 and Angular localhost:4200 to reuse the existing Angular API origins and CORS. Refuse occupied ports; never replace an existing listener. Other candidate loopback ports: Identity 18082, Department 18084, Employee 18083, HR 18086, MySQL 13307. These ports have not been reserved or approved for runtime.

Discovery removal requires BOTH Eureka disabled (`eureka.client.enabled=false`) and Spring SimpleDiscoveryClient static instances, e.g. `spring.cloud.discovery.client.simple.instances.identity-service[0].uri=http://localhost:18082`, plus employee-service and department-service entries. Keep discovery infrastructure enabled for HR's DiscoveryClient and Employee's load-balanced Feign client. Explicit Gateway routes must retain existing predicates and StripPrefix, and the Gateway security filters. Changing Gateway URLs alone breaks HR reference validation. This proposed replacement has not been integration-tested.

## Evidence and blockers

- Current measured free RAM: 2239 MiB. No MySQL executable in PATH, MySQL/MariaDB Windows service, usual Program Files installation directory, or listener on 3306/3307/13307 was found. This is not an exhaustive disk search or a connection to any database.
- Identity/Gateway JARs: no newer production source files; Identity's three embedded SQL files match source bytes. Existing artifact hashes match the prior preparation.
- Department/Employee: embedded SQL matches, but one production file each is newer than the JAR. Timestamp alone does not establish a binary mismatch: inspect before deciding to rebuild.
- HR: 24 production files newer than JAR; six source migrations do NOT all match the embedded entries. Rebuild HR with tests before using it. Do not rebuild Angular or Identity/Gateway without another reason.
- Native Employee TLS adds a second connector without an explicit address in its source. `server.address` alone must not be assumed to bind this connector to loopback. Qualify a supported Tomcat connector address override or a reviewed binding fix before launch; retain TLS/signature enforcement.
- Existing private certificates, if reused, need fresh expiry/SAN/trust/ACL checks. No private material was read for this preparation.

## Memory decision

No safe native minimum has been measured. Do not reuse Docker's 4480 MiB threshold as a native five-JVM guarantee, and do not lower it merely to pass a check.

Keeping existing maximum heaps gives Gateway 256 MiB + four domains 4 x 384 MiB = 1792 MiB. Existing maximum metaspace allowances add 128 + 4 x 192 = 896 MiB. These are ceilings, NOT simultaneous measured consumption, and exclude code cache, threads, direct buffers, MySQL, static server and Windows margin. They already exceed current free RAM before native overhead. Removing Discovery/Desktop does not establish a stable runtime at 2239 MiB free.

Keep Xms64 and existing Xmx/metaspace ceilings for a first instrumented qualification, with no parallel build/browser load. Before any launch, define and approve a measured total envelope plus Windows reserve; during runtime sample available RAM AND per-process private/resident memory, restarts and JVM failures. The prior 1536 MiB host reserve is a conservative candidate, not a newly validated native threshold. No executable startup guard is claimed until the envelope is qualified. Never rely on swap as proof of stability.

## Required next implementation (not performed)

1. Obtain a native MySQL 8 distribution and provision an exclusively synthetic instance under a new absolute directory, unique port and loopback bind, with dedicated credentials. Never use a development service/data directory. Installation/download and instance creation remain separate actions.
2. Rebuild HR sequentially with its tests; compare Department/Employee compiled sources and resources; refresh only stale artifacts. Verify all embedded SQL bytes.
3. Generate separate external per-service config: dedicated schema/user/password per domain; Flyway enabled, baseline false, clean disabled, validation enabled; Hibernate validate; pool max 5; SQL/bind logging disabled. No SQL admin-account insert. Use the existing explicit local bootstrap with first-password-change enforcement, then disable bootstrap after initialization.
4. Configure SimpleDiscoveryClient, explicit Gateway routing, mandatory Identity validation URLs, JWT and full signed HR-to-Employee HTTPS. Validate second TLS connector binding, certificates and private-file ACLs. No security fallback.
5. Qualify memory and lifecycle management. The real launcher must record PID + start time + executable for ONLY its children, wait at most 180 seconds per service, verify migration/health/routing and stop progression on failure. Stop must verify ownership and preserve MySQL data; no broad process kills. Add graceful owned-process shutdown before forced termination is considered.
6. Reuse a qualified Angular production bundle only after checking its origin and source manifest; provide a static SPA server, not `ng serve`. No UI profile currently hides absent modules: demonstrate direct RH routes and disclose unavailable widgets.
7. Exercise actual MySQL/Flyway and four-role refusals, association creation, leave submission/decision, refresh/revocation, last SUPER_ADMIN, TLS and document ownership before declaring the demo operational.

## Current manual commands

From repo root, in PowerShell under the existing execution policy:

```powershell
& .\scripts\deployment\native-demo\start-demo.ps1
# Expected: read-only preflight, then explicit BLOCKED exception; nothing starts.
& .\scripts\deployment\native-demo\stop-demo.ps1
# Expected: nothing stopped.
```

Do not bypass an execution-policy restriction. There is no truthful service-start command yet.

## Demonstration scope after qualification

SUPER_ADMIN: accounts, roles, settings and audit. ADMIN: departments/employees, UID associations, leave approval. EMPLOYEE: own leave request and decision, profile over signed TLS. CLIENT: login and RBAC refusal only; no client business journey without Customer/Invoice. Dashboards remain partial where they request absent services. No integrated behavior has been validated by this preparation.

Git: no staging/commit/push. docs/, backend/, b.png and existing scripts remain intact. This directory contains no secrets or production overrides.
