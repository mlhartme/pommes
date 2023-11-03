/*
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oneandone.pommes.repository;

import net.oneandone.inline.ArgumentException;
import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;

/** A place to search for descriptors. */
public abstract class Repository {
    public static Repository create(Environment environment, String name, String url, PrintWriter log) throws URISyntaxException, IOException {
        World world;
        Repository repository;
        FileNode file;

        world = environment.world();
        repository = ArtifactoryRepository.createOpt(environment, name, url);
        if (repository != null) {
            return repository;
        }
        repository = NodeRepository.createOpt(environment, name, url, log);
        if (repository != null) {
            return repository;
        }
        repository = GithubRepository.createOpt(environment, name, url, log);
        if (repository != null) {
            return repository;
        }
        repository = BitbucketRepository.createOpt(environment, name, url);
        if (repository != null) {
            return repository;
        }
        repository = GiteaRepository.createOpt(environment, name, url);
        if (repository != null) {
            return repository;
        }
        repository = GitlabRepository.createOpt(environment, name, url);
        if (repository != null) {
            return repository;
        }
        repository = JsonRepository.createOpt(world, name, url);
        if (repository != null) {
            return repository;
        }
        file = world.file(url);
        if (file.exists()) {
            return new NodeRepository(environment, name, file, log);
        }
        throw new ArgumentException("unknown repository type: " + url);
    }

    protected final String name;

    public Repository(String name) {
        this.name = name;
    }


    public void addOption(String option) {
        throw new ArgumentException(name + ": unknown option: " + option);
    }

    public void addExclude(String exclude) {
        throw new ArgumentException(name + ": excludes not supported: " + exclude);
    }
    public abstract void scan(BlockingQueue<Descriptor> dest, Console console) throws IOException, InterruptedException, URISyntaxException;
}
