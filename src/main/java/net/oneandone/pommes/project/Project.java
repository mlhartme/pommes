package net.oneandone.pommes.project;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.GetLastModifiedException;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;

/** Factory for Poms */
public abstract class Project {
    public static final Project END_OF_QUEUE = new Project(null) {
        @Override
        protected Pom doLoad(Environment environment, String origin, String revision) throws IOException {
            throw new IllegalStateException();
        }
    };

    public static Project probe(Node node) throws IOException {
        Project result;

        result = MavenProject.probe(node);
        if (result != null) {
            return result;
        }
        result = ComposerProject.probe(node);
        if (result != null) {
            return result;
        }
        result = JsonProject.probe(node);
        if (result != null) {
            return result;
        }
        return null;
    }

    private String overrideOrigin = null;
    private String overrideRevision = null;

    //--

    protected final Node file;

    protected Project(Node file) {
        this.file = file;
    }

    public Pom load(Environment environment) throws IOException {
        return doLoad(environment, getOrigin(), overrideRevision == null ? Long.toString(file.getLastModified()) : overrideRevision);
    }

    protected abstract Pom doLoad(Environment environment, String origin, String revision) throws IOException;

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
