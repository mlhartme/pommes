package net.oneandone.pommes.project;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Gav;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.GetLastModifiedException;
import net.oneandone.sushi.fs.Node;

public class ComposerProject extends NodeProject {
    public static boolean matches(String name) {
        return name.equals("composer.json");
    }

    public static ComposerProject create(Environment environment, Node node) {
        return new ComposerProject(node);
    }

    public ComposerProject(Node node) {
        super(node);
    }

    @Override
    protected Pom doLoad(Environment notUsed, String zone, String origin, String revision) throws GetLastModifiedException {
        return new Pom(zone, origin, revision, null, new Gav("1and1-sales.php", artifact(origin), "0"), null, null);
    }

    private static String artifact(String origin) {
        int last;
        int before;

        last = origin.lastIndexOf('/');
        if (last == -1) {
            return "unknown";
        }
        before = origin.lastIndexOf('/', last - 1);
        if (before == -1) {
            return "unknown";
        }
        return origin.substring(before + 1, last);
    }
}
