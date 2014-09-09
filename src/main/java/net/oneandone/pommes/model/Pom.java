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
package net.oneandone.pommes.model;

import net.oneandone.sushi.fs.Node;
import org.apache.maven.project.MavenProject;

public class Pom extends Coordinates {
    public static Pom forComposer(Node composer) {
        Node trunk;

        trunk = composer.getParent();
        return new Pom("1and1-sales", trunk.getParent().getName(), "0", "scm:" + trunk.getURI().toString());
    }

    public static Pom forProject(MavenProject project, String origin) {
        return new Pom(project.getGroupId(), project.getArtifactId(), project.getVersion(), origin);
    }

    //--

    public final String origin;

    public Pom(String groupId, String artifactId, String version, String origin) {
        super(groupId, artifactId, version);
        if (origin == null || origin.endsWith("/")) {
            throw new IllegalArgumentException(origin);
        }
        this.origin = origin;
    }

    public String toLine() {
        return groupId + ":" + artifactId + ":" + version + " @ " + origin;
    }

    /**
     * URL to checkout whole project.
     * @return always with tailing slash
     */
    public String projectUrl() {
        return origin.substring(0, origin.lastIndexOf('/') + 1);
    }

    @Override
    public String toString() {
        return toLine();
    }
}
