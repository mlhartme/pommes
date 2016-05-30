package net.oneandone.pommes.model;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.type.Type;

import java.io.IOException;

public class Item {
    public static final Item END_OF_QUEUE = new Item(null);

    private final Type type;

    public Item(Type type) {
        this.type = type;
    }

    public Pom createPom(Environment environment) throws IOException {
        return type.createPom(environment);
    }

    public String toString() {
        return type.toString();
    }
}
