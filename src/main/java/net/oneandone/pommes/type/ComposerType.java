package net.oneandone.pommes.type;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Gav;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.GetLastModifiedException;
import net.oneandone.sushi.fs.Node;

public class ComposerType extends Type {
    public static ComposerType probe(Node node) {
        if (node.getName().equals("composer.json")) {
            return new ComposerType(node);
        }
        return null;
    }

    public ComposerType(Node node) {
        super(node);
    }

    @Override
    public Pom createPom(Environment notUsed) throws GetLastModifiedException {
        return new Pom(getOrigin(), getRevision(), null, new Gav("1and1-sales", node.getParent().getParent().getName(), "0"), null);
    }
}
