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
import net.oneandone.sushi.util.Strings;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Scm;
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
        return new Pom(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), null);
    }

    public static Pom forProject(MavenProject project) {
        return new Pom(project.getGroupId(), project.getArtifactId(), project.getVersion(), scm(project));
    }

    private static String scm(MavenProject project) {
        Scm scm;
        String connection;

        scm = project.getScm();
        if (scm != null) {
            connection = scm.getConnection();
            if (connection != null) {
                return connection;
            }
        }
        return "";
    }

    //--

    public final String groupId;

    public final String artifactId;

    public final String version;

    /** may be null */
    public final String scm;

    public Pom(String groupId, String artifactId, String version, String scm) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scm = scm;
    }

    public String toGaString() {
        return groupId + ":" + artifactId;
    }

    public String toGavString() {
        return toGaString() + ":" + version;
    }

    public String toLine() {
        return groupId + ":" + artifactId + ":" + version + " @ " + scm;
    }

    /** @return null if not specified or not svn; otherwise always with tailing slash */
    public String svnUrl() {
        if (scm != null && scm.startsWith(Database.SCM_SVN)) {
            return Database.withSlash(Strings.removeLeft(scm, Database.SCM_SVN));
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return toLine();
    }

    public boolean gavEquals(Pom other) {
        return artifactId.equals(other.artifactId) && groupId.equals(other.groupId) && version.equals(other.version);
    }
}