package net.oneandone.pommes.project;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Gav;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.GetLastModifiedException;
import net.oneandone.sushi.fs.Node;

public class ComposerProject extends Project {
    public static ComposerProject probe(Node node) {
        if (node.getName().equals("composer.json")) {
            return new ComposerProject(node);
        }
        return null;
    }

    public ComposerProject(Node node) {
        super(node);
    }

    @Override
    public Pom load(Environment notUsed) throws GetLastModifiedException {
        return new Pom(getOrigin(), getRevision(), null, new Gav("1and1-sales", file.getParent().getParent().getName(), "0"), null);
    }
}