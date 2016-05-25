package net.oneandone.pommes.source;

import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;

public interface Source {
    void addOption(String option);
    void addExclude(String exclude);
    void scan(BlockingQueue<Node> dest) throws IOException, InterruptedException, URISyntaxException;
}
