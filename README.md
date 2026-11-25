# RallyBoro

RallyBoro is an early-stage project for volunteer staffing and event signup, with committee planning to follow later. Mobile and desktop experiences are equally important.

The goal is simple, privacy-conscious event coordination: organizations, reusable templates and event series, jobs and shifts, capacity management, volunteer signup, waitlists, and approval-required roles. The product is intended to support independent hosting and data portability. Event credits and payment processors are optional deployment features.

## Project status

The project currently contains product requirements, design documents, and a Maven build foundation. No application has been implemented. The prototype direction is Java with Spring Boot, self-hosted on the maintainer's infrastructure with access through Tailscale. Database, frontend, application deployment tooling, and the final open-source license remain open. The implementation language can be revisited after prototyping. Some requirements remain proposals or open questions; see the design brief for their status.

## Development setup

Install a Java 25 JDK and Docker Engine with Docker Compose and Buildx. Ensure your account can access Docker. Use the checked-in Maven Wrapper; a separate Maven installation is unnecessary. The wrapper downloads Maven 3.10.1 on first use and validates its SHA-256 checksum. Initial builds require internet access to download Maven and dependencies.

```bash
java -version
javac -version
docker version
docker compose version
docker buildx version
./mvnw --version
./mvnw --batch-mode clean verify
```

On Windows, use `mvnw.cmd` in place of `./mvnw`.

The build targets Java 25 and uses the Spring Boot 4.2.0 parent to manage dependency and build-plugin versions. Add dependencies in `pom.xml`; omit individual versions when Spring Boot manages them. Run `./mvnw dependency:tree` to inspect resolved dependencies. Build outputs are written to the ignored `target/` directory.

This foundation includes the core Spring Boot and test starters. There are no application sources or tests yet, so verification currently checks build configuration and dependency resolution. Add the Spring Boot Maven plugin when introducing the application entry point to produce an executable application. No database or frontend framework is selected by this setup.

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
