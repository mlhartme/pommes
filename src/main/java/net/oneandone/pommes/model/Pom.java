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

import org.apache.maven.model.Dependency;

public class Pom {
    public static Pom forGav(String gav) {
        String[] splitted;

        splitted = gav.split(":", 3);
        if (splitted.length < 3) {
            throw new IllegalArgumentException("groupId and artifactId reqiried");
        }
        return new Pom(splitted[0], splitted[1], splitted[2], null);
    }

    public static Pom forDependency(Dependency dependency) {
        return new Pom(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), null);
    }

    //--

    public final String groupId;

    public final String artifactId;

    public final String version;

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

    @Override
    public String toString() {
        return toLine();
    }

    public boolean gavEquals(Pom other) {
        return artifactId.equals(other.artifactId) && groupId.equals(other.groupId) && version.equals(other.version);
    }
}
