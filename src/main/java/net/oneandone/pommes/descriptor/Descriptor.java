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
import net.oneandone.pommes.scm.ScmUrl;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;

/**
 * Base class for various kinds of project descriptors, e.g. Maven pom files. Factory for Projects.
 * Has itself a creator method to detect the suitable Descritor class. */
public abstract class Descriptor {
    @FunctionalInterface
    public interface Creator {
        Descriptor create(Environment environment, Node<?> node, String storage, String path, String revision, ScmUrl scmUrl);
    }

    /** @return create method for matching descriptor or null if no descriptor matches */
    public static Creator match(String name) {
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

    //--

    protected final String storage;
    protected final String path;
    protected final String revision;
    protected final ScmUrl storageScm;

    public Descriptor(String storage, String path, String revision, ScmUrl storageScm) {
        if (storage == null) {
            throw new IllegalArgumentException();
        }
        if (path == null) {
            throw new IllegalArgumentException();
        }
        if (revision == null) {
            throw new IllegalArgumentException();
        }
        this.storage = storage;
        this.path = path;
        this.revision = revision;
        this.storageScm = storageScm == null ? null : storageScm.normalize();
    }

    public String getStorage() {
        return storage;
    }

    public String getPath() {
        return path;
    }

    public String getRevision() {
        return revision;
    }

    public abstract Project load() throws IOException;

    public String toString() {
        return path;
    }
}
