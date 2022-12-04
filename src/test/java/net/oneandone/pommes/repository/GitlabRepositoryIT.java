/*
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oneandone.pommes.repository;

import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.World;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.Visibility;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GitlabRepositoryIT {
    public static void main(String[] args) throws GitLabApiException {
        GitLabApi gitlab = new GitLabApi("https://gitlab.com", null);
        System.out.println("version: " + gitlab.getApiVersion());
        var pager = gitlab.getProjectApi().getProjects(null, Visibility.PUBLIC, null, null, null, null, null, null, null, null, 50);
        while (pager.hasNext()) {
            for (Project project : pager.next()) {
                System.out.println(project.getPathWithNamespace() + ": " + project.getId());
            }
        }
    }

    @Test
    public void test() throws GitLabApiException, IOException {
        Environment environment;
        GitlabRepository gitlab;
        Project gitlabProject;

        Descriptor descriptor;

        environment = new Environment(Console.create(), World.create());
        gitlab = new GitlabRepository(environment, "name", "https://gitlab.com");
        gitlabProject = gitlab.findProject("sochilenkov", "jse-16");
        assertTrue(gitlab.list(gitlabProject).contains("pom.xml"));
        descriptor = gitlab.scanOpt(gitlabProject);
        var project = descriptor.load(environment);
        System.out.println("project: " + project);
        assertEquals("ru.t1.sochilenkov.tm", project.artifact.groupId);
        assertEquals("task-manager", project.artifact.artifactId);
    }

    @Test
    public void raw() throws IOException {
        Environment environment;
        GitlabRepository gitlab;

        environment = new Environment(Console.create(), World.create());
        gitlab = new GitlabRepository(environment, "name", "https://gitlab.com");
        System.out.println(gitlab.listProjects());
    }

    @Test
    public void getProject() throws IOException {
        Environment environment;
        GitlabRepository gitlab;
        GitlabProject project;

        environment = new Environment(Console.create(), World.create());
        gitlab = new GitlabRepository(environment, "name", "https://gitlab.com");
        project = gitlab.getProject(41573530);
        System.out.println("" + gitlab.list(project));
    }
}
