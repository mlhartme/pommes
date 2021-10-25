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
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;

/** A place to search for descriptors */
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
        repository = JsonRepository.createOpt(world, url);
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
    void scan(BlockingQueue<Descriptor> dest) throws IOException, InterruptedException, URISyntaxException;
}
