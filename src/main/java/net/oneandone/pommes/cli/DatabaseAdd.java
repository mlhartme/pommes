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
import net.oneandone.maven.embedded.Maven;
import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.source.Source;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.apache.lucene.document.Document;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.project.MavenProject;

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

        indexer = new Indexer(dryrun, console, world, environment.maven(), database);
        indexer.start();
        for (Source source : sources) {
            source.scan(indexer.src);
        }
        indexer.src.put(Item.END_OF_QUEUE);
        indexer.join();
        if (indexer.exception != null) {
            throw indexer.exception;
        }
    }

    public static class Indexer extends Thread implements Iterator<Document> {
        private final boolean dryrun;

        private final Console console;
        private final World world;
        private final Maven maven;

        public final BlockingQueue<Item> src;
        private final Database database;
        private Exception exception;

        private Document current;
        private int count;
        private int errors;

        public Indexer(boolean dryrun, Console console, World world, Maven maven, Database database) {
            super("Indexer");

            this.dryrun = dryrun;

            this.console = console;
            this.world = world;
            this.maven = maven;

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
            Document document;

            while (true) {
                try {
                    document = iterUnchecked();
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

        private Document iterUnchecked() throws IOException, InterruptedException {
            Item item;
            FileNode local;
            MavenProject project;

            while (true) {
                item = src.take();
                if (item == Item.END_OF_QUEUE) {
                    return null;
                }
                try {
                    if (item.node.getName().equals("composer.json")) {
                        count++;
                        return Database.document(Pom.forComposer(item.origin, item.revision, item.node));
                    } else {
                        count++;
                        local = world.getTemp().createTempFile();
                        try {
                            item.node.copyFile(local);
                            console.info.println(item.origin);
                            project = maven.loadPom(local);
                            return Database.document(item.origin, item.revision, project);
                        } finally {
                            local.deleteFile();
                        }
                    }
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IOException("error processing " + item.origin + ": " + e.getMessage(), e);
                }
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void summary() {
            console.info.println("Indexed " + (count - errors) + "/" + count + " poms successfully.");
        }
    }
}