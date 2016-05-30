package net.oneandone.pommes.cli;

import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.type.Type;

import java.io.IOException;

public class Item {
    public static final Item END_OF_QUEUE = new Item(null, null, null);

    public final String origin;
    private final String revision;
    private final Type type;

    public Item(String origin, String revision, Type type) {
        this.origin = origin;
        this.revision = revision;
        this.type = type;
    }

    public Pom createPom(Environment environment) throws IOException {
        return type.createPom(origin, revision, environment);
    }
}
