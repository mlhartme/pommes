package net.oneandone.pommes.type;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;

public abstract class Type {
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

    public abstract Pom createPom(String origin, String revision, Environment environment) throws IOException;
}
