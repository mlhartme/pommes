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

import org.apache.lucene.document.Document;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * Refers from one artifact to another.
 */
public class Reference implements Comparable<Reference> {
    public final Document document;
    public final Coordinates from;
    public final Coordinates to;

    public Reference(Document document, Coordinates from, Coordinates to) {
        this.document = document;
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Reference)) {
            return false;
        }

        Reference other = (Reference) obj;
        return from.toGavString().equals(other.from.toGavString());
    }

    @Override
    public int hashCode() {
        return from.toGavString().hashCode();
    }

    public int compareTo(Reference other) {
        int result;
        ArtifactVersion left;
        ArtifactVersion right;

        result = from.groupId.compareTo(other.from.groupId);
        if (result != 0) {
            return result;
        }
        result = from.artifactId.compareTo(other.from.artifactId);
        if (result != 0) {
            return result;
        }

        left = new DefaultArtifactVersion(from.version);
        right = new DefaultArtifactVersion(other.from.version);
        return left.compareTo(right);
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}
