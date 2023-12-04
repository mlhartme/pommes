package net.oneandone.pommes.storage;

import net.oneandone.pommes.cli.Environment;

import java.io.IOException;
import java.net.URISyntaxException;

@FunctionalInterface
public interface Constructor {
    Storage create(Environment environment, String name, String url) throws URISyntaxException, IOException;
}
