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

public class GAV {
    public static GAV forGav(String gav) {
        String[] splitted;

        splitted = gav.split(":");
        if (splitted.length != 3) {
            throw new IllegalArgumentException("expected groupId:artifactId:version, got " + gav);
        }
        return new GAV(splitted[0], splitted[1], splitted[2]);
    }

    public static GAV forGa(String ga, String v) {
        String[] splitted;

        splitted = ga.split(":");
        if (splitted.length < 2) {
            throw new IllegalArgumentException("expected groupId:artifactId, got " + ga);
        }
        return new GAV(splitted[0], splitted[1], v);
    }

    public static GAV forDependency(Dependency dependency) {
        return new GAV(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }

    //--

    public final String groupId;

    public final String artifactId;

    public final String version;

    public GAV(String groupId, String artifactId, String version) {
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

    public boolean gavEquals(GAV other) {
        return artifactId.equals(other.artifactId) && groupId.equals(other.groupId) && version.equals(other.version);
    }
}
