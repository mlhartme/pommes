package net.oneandone.pommes.source;

import net.oneandone.inline.ArgumentException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;

public interface Source {
    static Source create(World world, String url) throws URISyntaxException, NodeInstantiationException {
        Source source;

        source = ArtifactorySource.createOpt(world, url);
        if (source != null) {
            return source;
        }
        source = NodeSource.createOpt(world, url);
        if (source != null) {
            return source;
        }
        throw new ArgumentException("unknown source url: " + url);
    }

    void addOption(String option);
    void addExclude(String exclude);
    void scan(BlockingQueue<Node> dest) throws IOException, InterruptedException, URISyntaxException;
}
