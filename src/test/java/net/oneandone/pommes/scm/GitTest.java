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
package net.oneandone.pommes.scm;

import net.oneandone.pommes.database.Gav;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GitTest {
    @Test
    public void path() throws URISyntaxException {
        Git git;

        git = new Git();
        assertEquals("github.com/pustefix-projects/pustefix-framework", git.path("git:https://github.com/pustefix-projects/pustefix-framework.git"));
        assertEquals("github.com/mlhartme/maven-active-markdown-plugin", git.path("git:ssh://git@github.com/mlhartme/maven-active-markdown-plugin.git"));
        assertEquals("github.com/tcurdt/jdeb", git.path("git:git://github.com:tcurdt/jdeb.git"));
        assertEquals("github.com/jkschoen/jsma", git.path("git:git@github.com:jkschoen/jsma.git"));
    }

    @Test
    public void dflt() {
        Git git;

        git = new Git();
        assertEquals(new Gav("cisoops", "clm", "1-SNAPSHOT"), git.defaultGav("ssh://git@bitbucket.1and1.org/cisoops/clm.git"));
    }
}
