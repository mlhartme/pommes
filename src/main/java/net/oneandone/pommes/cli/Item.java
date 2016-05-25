package net.oneandone.pommes.cli;

import net.oneandone.sushi.fs.Node;

public class Item {
    public static final Item END_OF_QUEUE = new Item(null, null);

    public final String origin;
    public final Node node;

    public Item(Node node) {
        this(node.getURI().toString(), node);
    }

    public Item(String origin, Node node) {
        this.origin = origin;
        this.node = node;
    }
}
