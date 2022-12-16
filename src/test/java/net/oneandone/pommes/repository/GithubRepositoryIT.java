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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GithubRepositoryIT {
    private GithubRepository repository() throws IOException {
        Environment environment;
        GithubRepository hub;

        environment = new Environment(Console.create(), World.create());
        hub = new GithubRepository(environment, "name", "https://api.github.com", null);
        return hub;
    }

    @Test
    public void repo() throws IOException {
        var hub = repository();
        var repo = hub.getRepo("mlhartme", "pommes");
        assertEquals("pommes", repo.name());
        assertTrue(repo.clone_url().startsWith("https://"));
    }

    @Test
    public void orgOrgRepos() throws IOException {
        var hub = repository();
        var repo = hub.listOrganizationOrUserRepos("1and1");
        assertTrue(repo.size() == 100);
    }
    @Test
    public void orgUserRepos() throws IOException {
        var hub = repository();
        var repo = hub.listOrganizationOrUserRepos("~mlhartme");
        assertEquals(25, repo.size());
    }
}
