package org.whitesborobandboosters.rallyboro;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.boot.SpringApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Isolated browser-test host; deliberately absent from the packaged production JAR. */
public class PilotUiServer {
  public static void main(String[] args) throws Exception {
    PostgreSQLContainer database = new PostgreSQLContainer("postgres:18.7-bookworm");
    database.start();
    Runtime.getRuntime().addShutdownHook(new Thread(database::stop));
    Path directory = Path.of("target");
    Files.createDirectories(directory);
    StringBuilder accounts = new StringBuilder(), credentials = new StringBuilder();
    for (String username : new String[] {"organizer", "volunteer", "helper"}) {
      byte[] bytes = new byte[24];
      new SecureRandom().nextBytes(bytes);
      String password = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
      credentials.append(username).append('=').append(password).append('\n');
      accounts
          .append(username)
          .append(".name=Synthetic ")
          .append(username)
          .append('\n')
          .append(username)
          .append(".role=")
          .append(username.equals("organizer") ? "ORGANIZER" : "VOLUNTEER")
          .append('\n')
          .append(username)
          .append(".password-hash=")
          .append(new BCryptPasswordEncoder(4).encode(password))
          .append('\n');
    }
    Path file = directory.resolve("ui-accounts.properties"),
        secret = directory.resolve("ui-credentials");
    Files.writeString(file, accounts);
    Files.writeString(secret, credentials);
    Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
    Files.setPosixFilePermissions(secret, PosixFilePermissions.fromString("rw-------"));
    System.setProperty("spring.datasource.url", database.getJdbcUrl());
    System.setProperty("spring.datasource.username", database.getUsername());
    System.setProperty("spring.datasource.password", database.getPassword());
    System.setProperty("rallyboro.pilot-login", "true");
    System.setProperty("rallyboro.accounts-file", file.toAbsolutePath().toString());
    System.setProperty("server.port", "18080");
    System.setProperty("server.address", "127.0.0.1");
    SpringApplication.run(RallyBoroApplication.class, args);
  }
}
