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
package net.oneandone.pommes.storage;

import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.sushi.fs.World;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GithubStorageIT {
    private GithubStorage storage() throws IOException {
        Environment environment;
        GithubStorage hub;

        environment = new Environment(Console.create(), World.create());
        try {
            hub = GithubStorage.create(environment, "name", "https://github.com");
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return hub;
    }

    @Test
    public void repo() throws IOException {
        var hub = storage();
        var repo = hub.getRepo("mlhartme", "pommes");
        assertEquals("pommes", repo.name());
        assertTrue(repo.clone_url().startsWith("https://"));
        var pom = hub.fileNode(repo, "pom.xml").readString();
        assertTrue(hub.listRoot(repo).stream().map(GithubStorage.GithubFile::path).toList().contains("pom.xml"));
        assertTrue(pom.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"), pom);
        var descriptor = hub.load(repo);
        var project = descriptor.load();
        System.out.println("" + project.revision);
        assertEquals("net.oneandone:pommes", project.artifact.toGaString());
    }

    @Test
    public void orgOrgRepos() throws IOException {
        var hub = storage();
        var repo = hub.listOrganizationOrUserRepos("1and1");
        assertTrue(repo.size() > 100);
    }
    @Test
    public void orgUserRepos() throws IOException {
        var hub = storage();
        var repo = hub.listOrganizationOrUserRepos("~mlhartme");
        assertEquals(26, repo.size());
    }
}
