package net.oneandone.pommes.repository;


import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GitlabProject(
     String defaultBranch,
     String httpUrlToRepo,
     int id,
     String path,
     String pathWithNamespace,
     String sshUrlToRepo,
     String webUrl) {
}
