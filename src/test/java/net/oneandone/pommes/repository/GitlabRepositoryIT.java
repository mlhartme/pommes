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
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GitlabRepositoryIT {
    private GitlabRepository repository() throws IOException {
        Environment environment;
        GitlabRepository gitlab;

        environment = new Environment(Console.create(), World.create());
        gitlab = new GitlabRepository(environment, "name", "https://gitlab.com");
        return gitlab;
    }

    @Test
    public void listGroups() throws IOException {
        var gitlab = repository();
        var lst = gitlab.listGroupOrUserProjects("jeffster");
        System.out.println("common-tools: " + lst.size());
        for (var g : lst) {
            System.out.println(g.toString());
        }
    }

    @Test
    public void scanOpt() throws IOException {
        Environment environment;
        GitlabRepository gitlab;
        Descriptor descriptor;

        environment = new Environment(Console.create(), World.create());
        gitlab = new GitlabRepository(environment, "name", "https://gitlab.com");
        var project = gitlab.getProject(41573530);
        System.out.println("" + gitlab.files(project));
        System.out.println("default branch: " + project.default_branch());
        System.out.println("branch revision: " + gitlab.branchRevision(project, project.default_branch()));
        descriptor = gitlab.load(project);
        var pommes = descriptor.load(environment);
        System.out.println("project: " + project);
        assertEquals("ru.t1.sochilenkov.tm", pommes.artifact.groupId);
        assertEquals("task-manager", pommes.artifact.artifactId);

    }
}
