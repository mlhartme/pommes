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
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

public class Pom {
    public static Pom forComposer(Node composer) {
        Node trunk;

        trunk = composer.getParent();
        return new Pom("1and1-sales", trunk.getParent().getName(), "0", "scm:" + trunk.getURI().toString());
    }

    public static Pom forGav(String gav, String scm) {
        String[] splitted;

        splitted = gav.split(":");
        if (splitted.length != 3) {
            throw new IllegalArgumentException("expected groupId:artifactId:version, got " + gav);
        }
        return new Pom(splitted[0], splitted[1], splitted[2], scm);
    }

    public static Pom forGa(String ga, String v, String scm) {
        String[] splitted;

        splitted = ga.split(":");
        if (splitted.length < 2) {
            throw new IllegalArgumentException("expected groupId:artifactId, got " + ga);
        }
        return new Pom(splitted[0], splitted[1], v, scm);
    }

    public static Pom forDependency(Dependency dependency) {
        return new Pom(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), "depdendency/pom.xml");
    }

    public static Pom forProject(MavenProject project, String origin) {
        return new Pom(project.getGroupId(), project.getArtifactId(), project.getVersion(), origin);
    }

    //--

    public final String origin;

    public final String groupId;

    public final String artifactId;

    public final String version;

    public Pom(String groupId, String artifactId, String version, String origin) {
        if (origin == null || origin.endsWith("/")) {
            throw new IllegalArgumentException(origin);
        }
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.origin = origin;
    }

    public String toGaString() {
        return groupId + ":" + artifactId;
    }

    public String toGavString() {
        return toGaString() + ":" + version;
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

    public boolean gavEquals(Pom other) {
        return artifactId.equals(other.artifactId) && groupId.equals(other.groupId) && version.equals(other.version);
    }
}
