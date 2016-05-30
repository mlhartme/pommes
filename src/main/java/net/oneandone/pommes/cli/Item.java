package net.oneandone.pommes.cli;

import net.oneandone.pommes.type.Type;

public class Item {
    public static final Item END_OF_QUEUE = new Item(null, null, null);

    public final String origin;
    public final String revision;
    public final Type type;

    public Item(String origin, String revision, Type type) {
        this.origin = origin;
        this.revision = revision;
        this.type = type;
    }
}
