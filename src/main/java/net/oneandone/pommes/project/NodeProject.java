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
