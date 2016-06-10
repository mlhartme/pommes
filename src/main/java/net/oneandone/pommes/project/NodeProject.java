/**
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

/** Factory for Poms */
public abstract class NodeProject extends Project {
    private String overrideOrigin = null;
    private String overrideRevision = null;

    //--

    protected final Node file;

    protected NodeProject(Node file) {
        this.file = file;
    }

    public Pom load(Environment environment, String zone) throws IOException {
        return doLoad(environment, zone, getOrigin(), overrideRevision == null ? Long.toString(file.getLastModified()) : overrideRevision);
    }

    protected abstract Pom doLoad(Environment environment, String zone, String origin, String revision) throws IOException;

    public void setOrigin(String origin) {
        this.overrideOrigin = origin;
    }

    public void setRevision(String revision) {
        this.overrideRevision = revision;
    }

    private String getOrigin() {
        return overrideOrigin == null ? file.getUri().toString() : overrideOrigin;
    }

    public String toString() {
        return getOrigin();
    }
}
