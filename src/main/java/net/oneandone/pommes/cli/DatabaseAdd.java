/**
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
package net.oneandone.pommes.cli;

import net.oneandone.inline.ArgumentException;
import net.oneandone.inline.Console;
import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.Field;
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.project.Project;
import net.oneandone.pommes.repository.Repository;
import net.oneandone.sushi.fs.NodeInstantiationException;
import org.apache.lucene.document.Document;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DatabaseAdd extends Base {
    private final boolean dryrun;
    private final List<Repository> repositories;

    public DatabaseAdd(Environment environment, boolean dryrun) {
        super(environment);
        this.dryrun = dryrun;
        this.repositories = new ArrayList<>();
    }

    public void add(String str) throws URISyntaxException, NodeInstantiationException {
        if (str.startsWith("-")) {
            previous(str).addExclude(str.substring(1));
        } else if (str.startsWith("%")) {
            previous(str).addOption(str.substring(1));
        } else {
            repositories.add(Repository.create(world, str));
        }
    }

    private Repository previous(String str) {
        if (repositories.isEmpty()) {
            throw new ArgumentException("missing url before '" + str + "'");
        }
        return repositories.get(repositories.size() - 1);
    }

    @Override
    public void run(Database database) throws Exception {
        Indexer indexer;

        indexer = new Indexer(dryrun, environment, database);
        indexer.start();
        try {
            for (Repository repository : repositories) {
                repository.scan(indexer.src);
            }
        } finally {
            indexer.src.put(Project.END_OF_QUEUE);
            indexer.join();
        }
        if (indexer.exception != null) {
            throw indexer.exception;
        }
    }

    public static class Indexer extends Thread implements Iterator<Document> {
        private final boolean dryrun;
        private final Environment environment;

        public final BlockingQueue<Project> src;
        private final Database database;
        private Exception exception;

        private Document current;
        private int count;
        private int errors;

        private final Map<String, String> existing;

        public Indexer(boolean dryrun, Environment environment, Database database) {
            super("Indexer");

            this.dryrun = dryrun;
            this.environment = environment;

            this.src = new ArrayBlockingQueue<>(25);
            this.database = database;
            this.exception = null;

            // CAUTION: current is not defined until this thread is started (because it would block this constructor)!

            this.count = 0;
            this.errors = 0;

            this.existing = new HashMap<>();
        }

        public void run() {
            long started;

            try {
                started = System.currentTimeMillis();
                database.list(existing);
                environment.console().verbose.println("scanned existing revisions: " + (System.currentTimeMillis() - started) + " ms");
                current = iter();
                database.index(this);
                summary();
            } catch (Exception e) {
                exception = e;
                try {
                    // consume remaining to avoid blocking scans
                    while (iter() != null);
                } catch (Exception e2) {
                    e.addSuppressed(e2);
                }
            }
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public Document next() {
            Document result;

            if (current == null) {
                throw new NoSuchElementException();
            }
            result = current;
            current = iter();
            return result;
        }

        private Document iter() {
            Pom pom;
            Console console;
            String existingRevision;

            console = environment.console();
            while (true) {
                try {
                    pom = iterPom();
                } catch (IOException | InterruptedException e) {
                    console.error.println(e.getMessage());
                    e.printStackTrace(console.verbose);
                    errors++;
                    continue;
                }
                if (pom == null) {
                    return null;
                }
                existingRevision = existing.get(pom.origin);
                if (pom.revision.equals(existingRevision)) {
                    console.info.println("  " + pom.origin);
                    continue;
                }
                console.info.println((existingRevision == null ? "A " : "U ") + pom.origin);
                if (!dryrun) {
                    return Field.document(pom);
                }
            }
        }

        private Pom iterPom() throws IOException, InterruptedException {
            Project project;

            while (true) {
                project = src.take();
                if (project == Project.END_OF_QUEUE) {
                    return null;
                }
                try {
                    count++;
                    return project.load(environment);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IOException("error processing " + project + ": " + e.getMessage(), e);
                }
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void summary() {
            environment.console().info.println("Added " + (count - errors) + "/" + count + " poms successfully.");
        }
    }
}