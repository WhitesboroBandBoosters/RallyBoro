# RallyBoro

RallyBoro is an early-stage project for volunteer staffing and event signup, with committee planning to follow later. Mobile and desktop experiences are equally important.

The goal is simple, privacy-conscious event coordination: organizations, reusable templates and event series, jobs and shifts, capacity management, volunteer signup, waitlists, and approval-required roles. The product is intended to support independent hosting and data portability. Event credits and payment processors are optional deployment features.

## Project status

Initiative 1 implements the runnable Java 25/Spring Boot foundation: a responsive Thymeleaf shell, a small JSON status API, PostgreSQL connectivity, Flyway migrations, container packaging, and database-backed integration tests. Event staffing is initiative 2 and has not been implemented yet. PostgreSQL deployment and credentials are independently managed infrastructure, excluded from project commits.

## Development setup

Use Java 25, Docker, and the checked-in Maven Wrapper (Maven 3.10.1):

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

The integration tests automatically use disposable PostgreSQL containers. See [running and recovery](docs/running.md) for application configuration, startup, tailnet access, and backup/restore; [architecture decisions](docs/decisions/001-foundation.md) for selected tools; and [API conventions](docs/api.md) for current endpoints. The wrapper pins Maven 3.10.1 and validates its distribution checksum; no separate Maven install is needed. Spring Boot manages dependency/plugin versions in `pom.xml`; build output is ignored under `target/`. On Windows use `mvnw.cmd`. Database passwords and local connection files must stay outside the checkout.

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
