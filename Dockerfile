FROM node:24.22.3-bookworm-slim AS frontend
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN npm ci --no-audit --no-fund
COPY frontend ./
COPY src/main/resources/graphql /build/src/main/resources/graphql
RUN npm run build

FROM eclipse-temurin:25-jdk AS build
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
COPY --from=frontend /build/frontend/dist frontend/dist
RUN ./mvnw -B -ntp -Dfrontend.skip=true -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --uid 10001 --create-home app
COPY --from=build /build/target/rallyboro-0.0.1-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
