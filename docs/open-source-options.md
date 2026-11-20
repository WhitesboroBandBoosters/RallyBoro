# Open-source options: Inscriva and OpenVolunteer

Reviewed 2026-11-07. This is a source and documentation review, not a running-product evaluation. No dependencies were installed, applications started, or deployments made. Existing EventManager requirements remain unchanged.

## Sources fetched

Full Git clones, with upstream history and remotes, are stored under `research/`:

| Product | Upstream | Local checkout | Reviewed commit |
| --- | --- | --- | --- |
| Inscriva | https://github.com/marvin-wtt/CampRegistration | `research/inscriva` | `bf9286e79cdaf9cc93e8339228c72108ba4d1ef3` |
| OpenVolunteer | https://github.com/drraccoony/mnfursvolunteers | `research/openvolunteer` | `5d0c45209a164d66121212fe8c1562d0867d6246` |

The conversation archive called the first product **Inscriva**, not Invecta. The earlier Inscriva website/research named `marvin-wtt/EventRegistration`, which returned 404 during this review. `marvin-wtt/CampRegistration` is public and its frontend identifies itself as Inscriva (`frontend/src/i18n/en-US/index.ts`). OpenVolunteer's official website links directly to `drraccoony/mnfursvolunteers`.

## Inscriva

### Languages and structure

- TypeScript throughout an npm-workspaces monorepo: shared domain/types, backend, frontend, and Playwright end-to-end tests.
- Frontend: Vue 3, Quasar, Pinia, SurveyJS, and multilingual UI. It does not use React.
- Backend: Node.js / Express 5, Prisma ORM, Passport/JWT authentication, TOTP two-factor authentication, and dependency injection with InversifyJS.
- README requires Node >=22 and <25, npm 10+, and MySQL >=8.0.1 or MariaDB >=10.6.
- Queues support a database driver (default), Redis/BullMQ, or memory; Redis is optional.

### Capabilities and fit

The product supports configurable registration forms, participant lists, capacity/waitlists, automatic email, team invitations and scoped permissions, organization management, room/bed allocation, program scheduling, duty rosters, documents, newsletters, and public event discovery. It is oriented toward youth camps and participant registration.

There is an actual event-scoped task model (`backend/prisma/schema.prisma`, model `Task`): title, notes, optional due date, completed flag, and zero or one assignee. This is useful overlap with committee planning, but is simpler than EventManager's agreed section organization, four task statuses, comments, titled reference links, and recoverable removal. The task schema does not contain those additional structures.

The duty model is chores, slots, and assignments to registered participants/staff. It should not be assumed to implement public volunteer self-selection into time-based shifts just because it has a duty roster. Registration waitlists likewise do not prove volunteer-shift waitlists.

No EventManager-style immutable event-template revisions and selective administrator approval workflow were identified in this inspection. Message templates and table templates in the schema are different concepts.

### Expected deployment

The production `Dockerfile` builds the frontend and backend into a Node 22 image. One long-running Node process serves both the API and built SPA on port 3000.

The root `compose.yaml` targets Coolify and uses `ghcr.io/marvin-wtt/campregistration`. A one-shot migration service applies schema/data migrations before the app starts. MySQL/MariaDB and optional Redis are separate resources; Compose does not provision them. Persistent volumes hold uploads/temp files and logs. SMTP settings supply outbound mail. Disk and S3 storage drivers exist, with disk selected in the production Compose file.

A practical self-hosted installation therefore needs a container host, database, persistent storage, SMTP, and public HTTPS routing. Coolify is the supplied deployment integration, not a necessary application dependency. This is a conventional server deployment, not a ready-made Workers/D1 application.

### Licensing detail

The application has an explicit AGPL-3.0-only license. Its form builder also depends on SurveyJS Creator and accepts a `SURVEYJS_LICENSE_KEY` at build time; the frontend warns if missing in production. Establish the applicable dependency licensing before treating the complete form-builder deployment as cost-free. This review did not determine entitlement or pricing.

Primary evidence: [repository README](https://github.com/marvin-wtt/CampRegistration#readme), `backend/README.md`, `backend/package.json`, `frontend/package.json`, `backend/prisma/schema.prisma`, `compose.yaml`, `Dockerfile`, and `frontend/src/boot/surveyJS.ts`. The [product website](https://inscriva.com/) provides the broader advertised feature list.

## OpenVolunteer

### Languages and structure

- PHP application built with Laravel 10 (`composer.json`), Eloquent ORM, and server-rendered Blade views.
- JavaScript/Alpine.js and Tailwind CSS, with Vite building frontend assets. It is not a React SPA.
- README recommends PHP 8.2+, Composer, Node 21, and MySQL 8.0.36. The official installation guide instead says Node 18+, MySQL 8+ or PostgreSQL 14+. Composer's configured platform is PHP 8.3. Use PHP 8.3 and MySQL as the conservative evaluation baseline; PostgreSQL compatibility was not tested.
- Authentication includes local accounts and optional WordPress account integration. Google sign-in was not identified in the inspected auth routes.

### Capabilities and fit

Source models, routes, and controllers include events, capacity-limited shifts, self-assignment and cancellation, overlapping-shift checks, eligibility by tags/departments, volunteer-hour tracking and reports, calendar feeds, reminders, staff check-in, departments/sectors, fiscal ledgers, custom fields, recognition/perks, job applications, and elections. This is a broader staff/volunteer operations system.

Yearly reuse has real support: `Admin/EventController.php::advancedDuplicate` copies events and shifts with date adjustments and clears volunteers. This is closer to reuse than the earlier history suggested, though it is not versioned templates with selective approval of improvements.

Public event/shift browsing exists, but the ordinary shift signup controller uses an authenticated user. Anonymous browsing should not be confused with account-free volunteer registration. No shift waitlist or committee planning task model was identified in the inspected models/routes/controllers. These are inspection findings rather than a claim that every application path was exhaustively audited.

### Expected deployment

The [official installation guide](https://docs.openvolunteer.net/setup/installation.html) recommends Laravel Cloud, with an application, database, storage bucket, and mail configuration. It also describes self-hosting with PHP, Composer, a web server, SSL, database, built frontend assets, migrations, scheduler, and queue-worker setup.

The repository's Docker Compose file is a **Laravel Sail development environment**, comprising PHP 8.3, MySQL 8, and Mailpit. It is not a complete production container deployment. A checked-in GitHub Actions deployment connects over SSH to an existing host and runs Git pull, Composer install, frontend build, cache clearing, and migrations.

For conventional self-hosting, expect a PHP-capable host (typically a web server plus PHP-FPM), database, persistent files or object storage, SMTP, and a scheduled Laravel command. `app/Console/Kernel.php` schedules hour logging and reminder jobs. Queues default to synchronous execution; a separate worker is needed if asynchronous queues are enabled. Redis is not inherently required.

This also requires a different deployment approach from Workers/D1. Managed Laravel hosting may offer its own lifecycle controls; this review did not establish current costs or prove a near-zero-cost deployment.

### Licensing detail

The repository's `composer.json` declares MIT, but there is no top-level LICENSE file in the fetched checkout. The README's license paragraph describes the Laravel framework. Confirm the application's intended license with the maintainer before adopting it as a base.

Primary evidence: [official source repository](https://github.com/drraccoony/mnfursvolunteers), [official introduction](https://docs.openvolunteer.net/), [installation guide](https://docs.openvolunteer.net/setup/installation.html), `composer.json`, `package.json`, `routes/web.php`, `routes/auth.php`, `app/Models`, `app/Http/Controllers/Volunteer/ShiftSignupController.php`, `app/Http/Controllers/Admin/EventController.php`, `app/Console/Kernel.php`, and `.github/workflows/main.yml`.

## Implication for EventManager

Inscriva is the closer starting point for evaluating committee task planning and is aligned with TypeScript skills, although adopting it means Vue and a conventional Node/database deployment. OpenVolunteer is the closer starting point for volunteer staffing and already supports copying events without volunteer assignments, but it introduces PHP/Laravel and considerably more organizational machinery.

Neither inspected implementation supplies the complete approved-template learning loop. Adapting either would trade some custom development for a broader existing product and a different operating model. The useful next evaluation is a local walkthrough of event creation, permissions, assignment/completion, yearly reuse, and volunteer registration; no adoption decision is made here.
