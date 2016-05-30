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
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.source.Source;
import net.oneandone.pommes.project.Project;
import net.oneandone.sushi.fs.NodeInstantiationException;
import org.apache.lucene.document.Document;
import org.apache.maven.artifact.InvalidArtifactRTException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DatabaseAdd extends Base {
    private final boolean dryrun;
    private final List<Source> sources;

    public DatabaseAdd(Environment environment, boolean dryrun) {
        super(environment);
        this.dryrun = dryrun;
        this.sources = new ArrayList<>();
    }

    public void add(String str) throws URISyntaxException, NodeInstantiationException {
        if (str.startsWith("-")) {
            previous(str).addExclude(str.substring(1));
        } else if (str.startsWith("%")) {
            previous(str).addOption(str.substring(1));
        } else {
            sources.add(Source.create(world, str));
        }
    }

    private Source previous(String str) {
        if (sources.isEmpty()) {
            throw new ArgumentException("missing url before '" + str + "'");
        }
        return sources.get(sources.size() - 1);
    }

    @Override
    public void run(Database database) throws Exception {
        Indexer indexer;

        indexer = new Indexer(dryrun, environment, database);
        indexer.start();
        try {
            for (Source source : sources) {
                source.scan(indexer.src);
            }
        } finally {
            indexer.src.put(Project.END_OF_QUEUE);
        }
        indexer.join();
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

        public Indexer(boolean dryrun, Environment environment, Database database) {
            super("Indexer");

            this.dryrun = dryrun;
            this.environment = environment;

            this.src = new ArrayBlockingQueue<>(25);
            this.database = database;
            this.exception = null;

            // CAUTION: current is not defined until this thread is started!

            this.count = 0;
            this.errors = 0;
        }

        public void run() {
            try {
                current = iter();
                database.index(this);
                summary();
            } catch (Exception e) {
                exception = e;
                try {
                    // consume remaining to avoid dead-lock
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
            Console console;
            Document document;

            console = environment.console();
            while (true) {
                try {
                    document = Database.document(iterUnchecked());
                    if (!dryrun) {
                        return document;
                    }
                } catch (InterruptedException | InvalidArtifactRTException | IOException e) {
                    console.error.println(e.getMessage());
                    if (e.getCause() == null) {
                        console.verbose.println("(unknown cause)");
                    } else {
                        e.getCause().printStackTrace(console.verbose);
                    }
                    errors++;
                    // fall-through
                }
            }
        }

        private Pom iterUnchecked() throws IOException, InterruptedException {
            Project project;

            while (true) {
                project = src.take();
                if (project == Project.END_OF_QUEUE) {
                    return null;
                }
                try {
                    count++;
                    environment.console().info.println(project);
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
            environment.console().info.println("Indexed " + (count - errors) + "/" + count + " poms successfully.");
        }
    }
}