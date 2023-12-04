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
package net.oneandone.pommes.storage;

import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.pommes.scm.ScmUrl;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/** Storage organized as hierarchy of repositories and files */
public abstract class TreeStorage<R, F> extends Storage {
    private final Environment environment;

    public TreeStorage(Environment environment, String name) {
        super(name);
        this.environment = environment;
    }

    public void scan(BlockingQueue<Descriptor> dest, Console console) throws IOException, InterruptedException {
        console.info.print("collecting repositories ...");
        console.info.flush();
        List<R> repositories = list();
        console.info.println(" done: " + repositories.size());
        for (R repository : repositories) {
            try {
                var descriptor = load(repository);
                if (descriptor != null) {
                    dest.put(descriptor);
                }
            } catch (IOException e) {
                console.error.println("cannot load descriptor from repository " + repository + ": " + e.getMessage());
                e.printStackTrace(console.verbose);
            }
        }
    }

    /** Load descriptor from repository. */
    public Descriptor load(R repository) throws IOException {
        for (F file : listRoot(repository)) {
            var creator = Descriptor.match(fileName(file));
            if (creator != null) {
                // TODO: when to delete directory?
                Node<?> local = localFile(repository, file);
                return creator.create(environment, local, this.name,
                        repositoryPath(repository, file), fileRevision(repository, file, local), storageUrl(repository));
            }
        }
        return createDefault(repository);
    }

    //--

    /** List repositories in the storage */
    public abstract List<R> list() throws IOException;

    /** list top-level files in the repository */
    public abstract List<F> listRoot(R repository) throws IOException;
    public abstract String fileName(F file);
    public abstract Descriptor createDefault(R repository) throws IOException;

    public abstract ScmUrl storageUrl(R repository) throws IOException;

    // TODO: rename?
    public abstract String repositoryPath(R repository, F file) throws IOException;

    public abstract Node<?> localFile(R repository, F file) throws IOException;
    public abstract String fileRevision(R repository, F f, Node<?> local) throws IOException;
}
