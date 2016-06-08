package net.oneandone.pommes.project;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.GetLastModifiedException;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;

/** Factory for Poms */
public abstract class Project {
    public static final Project END_OF_QUEUE = new Project() {
        @Override
        protected Pom doLoad(Environment environment, String zone, String origin, String revision) throws IOException {
            throw new IllegalStateException();
        }
    };

    public static Project probe(Environment environment, Node node) throws IOException {
        Project result;

        result = MavenProject.probe(node);
        if (result != null) {
            return result;
        }
        result = ComposerProject.probe(node);
        if (result != null) {
            return result;
        }
        result = JsonProject.probe(environment.gson(), node);
        if (result != null) {
            return result;
        }
        return null;
    }

    private String origin;
    private String revision;

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public Pom load(Environment environment, String zone) throws IOException {
        if (origin == null) {
            throw new IllegalStateException();
        }
        if (revision == null) {
            throw new IllegalStateException();
        }
        return doLoad(environment, zone, origin, revision);
    }

    protected abstract Pom doLoad(Environment environment, String zone, String origin, String revision) throws IOException;

    public String toString() {
        return origin;
    }
}
