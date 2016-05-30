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
        public Pom load(Environment environment) throws IOException {
            throw new IllegalStateException();
        }
    };

    public static Project probe(Node node) {
        Project result;

        result = MavenProject.probe(node);
        if (result != null) {
            return result;
        }
        result = ComposerProject.probe(node);
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

    public abstract Pom load(Environment environment) throws IOException;

    public void setOrigin(String origin) {
        this.overrideOrigin = origin;
    }

    public String getOrigin() {
        return overrideOrigin == null ? file.getURI().toString() : overrideOrigin;
    }

    public String getRevision() throws GetLastModifiedException {
        return overrideRevision == null ? Long.toString(file.getLastModified()) : overrideRevision;
    }

    public void setRevision(String revision) {
        this.overrideRevision = revision;
    }

    public String toString() {
        return getOrigin();
    }
}
