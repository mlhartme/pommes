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
package net.oneandone.pommes.project;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.util.function.BiFunction;

/** Factory for Poms */
public abstract class Project {
    public static final Project END_OF_QUEUE = new Project() {
        @Override
        protected Pom doLoad(Environment environment, String zone, String origin, String revision, String scm) throws IOException {
            throw new IllegalStateException();
        }
    };

    public static Project probe(Environment environment, Node node) throws IOException {
        BiFunction<Environment, Node, Project> m;

        m = match(node.getName());
        return m != null ? m.apply(environment, node) : null;
    }

    public static BiFunction<Environment, Node, Project> match(String name) throws IOException {
        if (MavenProject.matches(name)) {
            return MavenProject::create;
        }
        if (ComposerProject.matches(name)) {
            return ComposerProject::create;
        }
        if (JsonProject.matches(name)) {
            return JsonProject::create;
        }
        return null;
    }

    private String origin;
    private String revision;
    private String scm;

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public void setScm(String scm) {
        this.scm = scm;
    }

    public Pom load(Environment environment, String zone) throws IOException {
        if (origin == null) {
            throw new IllegalStateException();
        }
        if (revision == null) {
            throw new IllegalStateException();
        }
        return doLoad(environment, zone, origin, revision, scm);
    }

    protected abstract Pom doLoad(Environment environment, String zone, String origin, String revision, String scm) throws IOException;

    public String toString() {
        return origin;
    }
}
