package net.oneandone.pommes.type;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.GetLastModifiedException;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;

/** Factory for Poms */
public abstract class Type {
    public static final Type END_OF_QUEUE = new Type(null) {
        @Override
        public Pom createPom(Environment environment) throws IOException {
            throw new IllegalStateException();
        }
    };

    public static Type probe(Node node) {
        Type result;

        result = MavenType.probe(node);
        if (result != null) {
            return result;
        }
        result = ComposerType.probe(node);
        if (result != null) {
            return result;
        }
        return null;
    }

    private String overrideOrigin = null;
    private String overrideRevision = null;

    //--

    protected final Node node;

    protected Type(Node node) {
        this.node = node;
    }

    public abstract Pom createPom(Environment environment) throws IOException;

    public void setOrigin(String origin) {
        this.overrideOrigin = origin;
    }

    public String getOrigin() {
        return overrideOrigin == null ? node.getURI().toString() : overrideOrigin;
    }

    public String getRevision() throws GetLastModifiedException {
        return overrideRevision == null ? Long.toString(node.getLastModified()) : overrideRevision;
    }

    public void setRevision(String revision) {
        this.overrideRevision = revision;
    }
}
