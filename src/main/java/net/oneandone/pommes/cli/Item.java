package net.oneandone.pommes.cli;

import net.oneandone.sushi.fs.Node;

public class Item {
    public static final Item END_OF_QUEUE = new Item(null, null, null);

    public final String origin;
    public final String revision;
    public final Node node;

    public Item(String revision, Node node) {
        this(node.getURI().toString(), revision, node);
    }

    public Item(String origin, String revision, Node node) {
        this.origin = origin;
        this.revision = revision;
        this.node = node;
    }
}
