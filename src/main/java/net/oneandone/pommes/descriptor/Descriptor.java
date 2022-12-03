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
package net.oneandone.pommes.descriptor;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.database.Project;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.util.function.BiFunction;

/** Factory for Poms */
public abstract class Descriptor {
    public static final Descriptor END_OF_QUEUE = new ErrorDescriptor(new IOException()) {
        @Override
        protected Project doLoad(Environment environment, String repository, String origin, String revision, String scm) {
            throw new IllegalStateException();
        }
    };

    public static Descriptor probeChecked(Environment environment, Node<?> node) {
        BiFunction<Environment, Node<?>, Descriptor> m;

        try {
            m = match(node.getName());
            return m != null ? m.apply(environment, node) : null;
        } catch (IOException | RuntimeException e) {
            return new ErrorDescriptor(e);
        }
    }

    public static Descriptor probe(Environment environment, Node<?> node) throws IOException {
        BiFunction<Environment, Node<?>, Descriptor> m;

        m = match(node.getName());
        return m != null ? m.apply(environment, node) : null;
    }

    /** @return null if no descriptor matches */
    public static BiFunction<Environment, Node<?>, Descriptor> match(String name) throws IOException {
        if (MavenDescriptor.matches(name)) {
            return MavenDescriptor::create;
        }
        if (ComposerDescriptor.matches(name)) {
            return ComposerDescriptor::create;
        }
        if (JsonDescriptor.matches(name)) {
            return JsonDescriptor::create;
        }
        return null;
    }

    private String repository;
    private String path;
    private String revision;
    private String scm;

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getRepository() {
        return repository;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getRevision() {
        return revision;
    }

    public void setScm(String scm) {
        this.scm = scm;
    }

    public Project load(Environment environment) throws IOException {
        if (repository == null) {
            throw new IllegalStateException();
        }
        if (path == null) {
            throw new IllegalStateException();
        }
        if (revision == null) {
            throw new IllegalStateException();
        }
        return doLoad(environment, repository, path, revision, scm);
    }

    protected abstract Project doLoad(Environment environment, String withRepository, String withOrigin, String withRevision, String withScm) throws IOException;

    public String toString() {
        return path;
    }
}
