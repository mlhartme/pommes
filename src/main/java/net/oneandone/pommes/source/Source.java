package net.oneandone.pommes.source;

import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.cli.Item;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.tmatesoft.svn.core.SVNException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;

public interface Source {
    static Source create(World world, String url) throws URISyntaxException, NodeInstantiationException {
        Source source;
        FileNode file;

        source = ArtifactorySource.createOpt(world, url);
        if (source != null) {
            return source;
        }
        source = NodeSource.createOpt(world, url);
        if (source != null) {
            return source;
        }
        file = world.file(url);
        if (file.exists()) {
            return new NodeSource(file);
        }
        throw new ArgumentException("unknown source type: " + url);
    }

    void addOption(String option);
    void addExclude(String exclude);
    void scan(BlockingQueue<Item> dest) throws IOException, InterruptedException, URISyntaxException, SVNException;
}
