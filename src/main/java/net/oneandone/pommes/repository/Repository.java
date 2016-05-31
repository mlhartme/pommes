package net.oneandone.pommes.repository;

import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.project.Project;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;

/** A location to search for projects */
public interface Repository {
    static Repository create(World world, String url) throws URISyntaxException, NodeInstantiationException {
        Repository source;
        FileNode file;

        source = ArtifactoryRepository.createOpt(world, url);
        if (source != null) {
            return source;
        }
        source = NodeRepository.createOpt(world, url);
        if (source != null) {
            return source;
        }
        source = GithubRepository.createOpt(world, url);
        if (source != null) {
            return source;
        }
        file = world.file(url);
        if (file.exists()) {
            return new NodeRepository(file);
        }
        throw new ArgumentException("unknown source type: " + url);
    }

    void addOption(String option);
    void addExclude(String exclude);
    void scan(BlockingQueue<Project> dest) throws IOException, InterruptedException, URISyntaxException;
}
