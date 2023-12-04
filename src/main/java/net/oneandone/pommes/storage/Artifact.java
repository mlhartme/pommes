package net.oneandone.pommes.storage;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.util.Strings;

public record Artifact(String uri, String sha1) {
    public String name() {
        int idx = uri.lastIndexOf('/');
        if (idx < 0) {
            throw new IllegalStateException(uri);
        }
        return uri.substring(idx + 1);
    }

    public Descriptor create(Node<?> root, String storage, Environment environment, Descriptor.Creator creator) {
        Node<?> node = root.join(Strings.removeLeft(uri, "/"));
        return creator.create(environment, node, storage, "artifactory:" + node.getPath(), sha1, null);
    }
}
