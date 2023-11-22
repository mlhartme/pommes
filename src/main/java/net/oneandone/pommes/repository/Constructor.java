package net.oneandone.pommes.repository;

import net.oneandone.pommes.cli.Environment;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;

@FunctionalInterface
public interface Constructor {
    Repository create(Environment environment, String name, String url, PrintWriter log) throws URISyntaxException, IOException;
}
