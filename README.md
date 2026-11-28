# RallyBoro

RallyBoro is an early-stage project for volunteer staffing and event signup, with committee planning to follow later. Mobile and desktop experiences are equally important.

The goal is simple, privacy-conscious event coordination: organizations, reusable templates and event series, jobs and shifts, capacity management, volunteer signup, waitlists, and approval-required roles. The product is intended to support independent hosting and data portability. Event credits and payment processors are optional deployment features.

## Project status

The first two initiatives now provide a controlled volunteer staffing pilot: a React/TypeScript interface, schema-first GraphQL API, organization-scoped event/job/shift management, signup and cancellation, organizer coverage/assignments, and scoped agent tokens. PostgreSQL/JDBC/Flyway and the non-root application container remain the foundation. See [pilot decisions](docs/decisions/003-staffing-pilot.md) for access rules, the local pilot login option, and the still-deferred guest signup, waitlist, approval, template, notification, and contact-sharing features. The pilot currently uses synthetic identities; no phone or email data is collected.

PostgreSQL deployment and all real credentials are independently managed infrastructure, excluded from project commits. The open-source license remains undecided.

## Development setup

Use Java 25, Docker, and the checked-in Maven Wrapper (Maven 3.10.1):

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

The Maven build installs pinned Node tooling locally, uses `npm ci`, validates GraphQL operations, type-checks/builds the frontend, and packages its assets with Spring Boot. Integration tests automatically use disposable PostgreSQL containers. Browser checks run with `scripts/test-ui.sh` after installing the frontend Playwright browser; they start a separate isolated test server/database and create protected synthetic credentials under the ignored `target/` directory. See [running and recovery](docs/running.md) for application configuration, startup, tailnet access, and backup/restore; [architecture decisions](docs/decisions/001-foundation.md) for selected tools; and [API conventions](docs/api.md) for current endpoints. The wrapper pins Maven 3.10.1 and validates its distribution checksum; no separate Maven install is needed. Spring Boot manages dependency/plugin versions in `pom.xml`; build output is ignored under `target/`. On Windows use `mvnw.cmd`. Database passwords and local connection files must stay outside the checkout.

## Planning space

Read the [published planning site](https://whitesborobandboosters.github.io/RallyBoro/) for the roadmap and design documents. Use the linked GitHub issues for discussion, dependencies, and progress. Each published page links to its Markdown source for editing and history. Changes on `main` automatically republish the site.

GitHub Pages builds the Markdown with Jekyll from the root of `main`, using `_config.yml` and `_layouts/planning.html`. Edit the existing roadmap/design files directly; YAML front matter makes each document a published page. Conversation archives and local artifacts are excluded from publishing.

## Design documents

- [Implementation roadmap](roadmap/README.md): phased initiatives, starting with a runnable foundation and a controlled staffing pilot.
- [Design brief](docs/design-brief.md): principles, goals, requested capabilities, proposed architecture, and open decisions.
- [Product direction](docs/product-direction.md): evolving product decisions and stakeholder feedback.
- [Volunteer staffing and signup](docs/volunteer-signup.md): detailed requirements and remaining questions.
- [Committee planning](docs/event-planning.md): retained design for later development.
- [Existing-product research](docs/open-source-options.md): inspiration from other projects, not solutions being adopted.
- [Earlier conversation archive](docs/conversation-history.md): historical context; current specifications take precedence.

[AGENTS.md](AGENTS.md) contains project guidance for development agents. Earlier documents may refer to the working name EventManager and historical technology proposals; RallyBoro is the initial project name.
