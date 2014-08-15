package com.oneandone.sales.tools.pommes.lucene;

import org.apache.lucene.document.Document;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * Refers from one artifact to another.
 */
public class Reference implements Comparable<Reference> {
    public final Document document;
    public final GroupArtifactVersion from;
    public final GroupArtifactVersion to;

    public Reference(Document document, GroupArtifactVersion from, GroupArtifactVersion to) {
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

        if (!from.getGroupId().equals(other.from.getGroupId())) {
            return from.getGroupId().compareTo(other.from.getGroupId());
        }
        if (!from.getArtifactId().equals(other.from.getArtifactId())) {
            return from.getArtifactId().compareTo(other.from.getArtifactId());
        }

        ArtifactVersion v1 = new DefaultArtifactVersion(from.getVersion());
        ArtifactVersion v2 = new DefaultArtifactVersion(other.from.getVersion());
        return v1.compareTo(v2);
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}
