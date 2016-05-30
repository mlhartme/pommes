package net.oneandone.pommes.type;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Gav;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.Node;

public class ComposerType extends Type {
    public static ComposerType probe(Node node) {
        if (node.getName().equals("composer.json")) {
            return new ComposerType(node);
        }
        return null;
    }

    private final Node node;

    public ComposerType(Node node) {
        this.node = node;
    }

    @Override
    public Pom createPom(String origin, String revision, Environment notUsed) {
        return new Pom(origin, revision, null, new Gav("1and1-sales", node.getParent().getParent().getName(), "0"), "");
    }
}
