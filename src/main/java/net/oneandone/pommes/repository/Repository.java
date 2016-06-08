package net.oneandone.pommes.repository;

import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.project.Project;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;

/** A location to search for projects */
public interface Repository {
    static Repository create(Environment environment, String url, PrintWriter log) throws URISyntaxException, NodeInstantiationException {
        World world;
        Repository repository;
        FileNode file;

        world = environment.world();
        repository = ArtifactoryRepository.createOpt(environment, url);
        if (repository != null) {
            return repository;
        }
        repository = NodeRepository.createOpt(environment, url, log);
        if (repository != null) {
            return repository;
        }
        repository = GithubRepository.createOpt(environment, url, log);
        if (repository != null) {
            return repository;
        }
        repository = BitbucketRepository.createOpt(environment, url);
        if (repository != null) {
            return repository;
        }
        repository = JsonRepository.createOpt(environment.gson(), world, url);
        if (repository != null) {
            return repository;
        }
        file = world.file(url);
        if (file.exists()) {
            return new NodeRepository(environment, file, log);
        }
        throw new ArgumentException("unknown repository type: " + url);
    }

    void addOption(String option);
    void addExclude(String exclude);
    void scan(BlockingQueue<Project> dest) throws IOException, InterruptedException, URISyntaxException;
}
