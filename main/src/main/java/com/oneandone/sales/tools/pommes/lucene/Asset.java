package com.oneandone.sales.tools.pommes.lucene;

public class Asset {
    public final String groupId;

    public final String artifactId;

    public final String scm;

    public Asset(String groupId, String artifactId, String scm) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.scm = scm;
    }

    public String toLine() {
        return groupId + ":" + artifactId + " @ " + scm;
    }
}
