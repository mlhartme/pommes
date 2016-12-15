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
package net.oneandone.pommes.model;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

public class Gav {
    public static Gav forGavOpt(String gavOpt) {
        return gavOpt == null ? null : forGav(gavOpt);
    }

    public static Gav forGav(String gav) {
        String[] splitted;

        splitted = gav.split(":");
        if (splitted.length != 3) {
            throw new IllegalArgumentException("expected groupId:artifactId:version, got " + gav);
        }
        return new Gav(splitted[0], splitted[1], splitted[2]);
    }

    public static Gav forDependency(Dependency dependency) {
        return new Gav(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }

    public static Gav forArtifact(Artifact artifact) {
        return new Gav(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
    }

    //--

    public final String groupId;

    public final String artifactId;

    public final String version;

    public Gav(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public String toGaString() {
        return groupId + ":" + artifactId;
    }

    public String toGavString() {
        return toGaString() + ":" + version;
    }

    @Override
    public String toString() {
        return toGavString();
    }


    @Override
    public boolean equals(Object o) {
        Gav gav = (Gav) o;

        if (o instanceof Gav) {
            return groupId.equals(gav.groupId) && artifactId.equals(gav.artifactId) && version.equals(gav.version);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return artifactId.hashCode();
    }
}
