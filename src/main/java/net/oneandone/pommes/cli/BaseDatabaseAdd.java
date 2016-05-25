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
import net.oneandone.sushi.fs.Node;
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

public abstract class BaseDatabaseAdd extends Base {
    private List<Source> sources;

    public BaseDatabaseAdd(Environment environment) {
        super(environment);
        sources = new ArrayList<>();
    }

    public void add(String urlOrExclude) {
        if (urlOrExclude.startsWith("-")) {
            if (sources.isEmpty()) {
                throw new ArgumentException("missing url before exclude " + urlOrExclude);
            }
            sources.get(sources.size() - 1).excludes.add(urlOrExclude.substring(1));
        } else {
            sources.add(new Source(urlOrExclude));
        }
    }

    @Override
    public void run(Database database) throws Exception {
        Node endOfQueue;
        Indexer indexer;

        endOfQueue = world.getHome();
        indexer = new Indexer(database, endOfQueue);
        indexer.start();
        for (Source source : sources) {
            collect(source, indexer.src);
        }
        indexer.src.put(endOfQueue);
        indexer.join();
        if (indexer.exception != null) {
            throw indexer.exception;
        }
    }

    public abstract void collect(Source src, BlockingQueue<Node> dest) throws IOException, URISyntaxException, InterruptedException;

    public class Indexer extends Thread {
        public final BlockingQueue<Node> src;
        private final Database database;
        private final Node endOfQueue;
        private Exception exception;

        public Indexer(Database database, Node endOfQueue) {
            super("Indexer");
            this.src = new ArrayBlockingQueue<>(25);
            this.database = database;
            this.endOfQueue = endOfQueue;
            this.exception = null;
        }

        public void run() {
            ProjectIterator iterator;

            try {
                iterator = new ProjectIterator(console, world, environment.maven(), endOfQueue, src);
                database.index(iterator);
                iterator.summary();
            } catch (Exception e) {
                exception = e;
            }
        }
    }

    public static class ProjectIterator implements Iterator<Document> {
        private final Console console;
        private final World world;
        private final Maven maven;
        private final Node endOfQueue;
        private final BlockingQueue<Node> projects;

        private Document current;
        private int count;
        private int errors;

        public ProjectIterator(Console console, World world, Maven maven, Node endOfQueue, BlockingQueue<Node> projects) {
            this.console = console;
            this.world = world;
            this.maven = maven;
            this.endOfQueue = endOfQueue;
            this.projects = projects;
            this.current = iter();
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
            while (true) {
                try {
                    return iterUnchecked();
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
            Node pom;
            FileNode local;
            MavenProject project;
            String origin;

            while (true) {
                pom = projects.take();
                if (pom == endOfQueue) {
                    return null;
                }
                try {
                    if (pom.getName().equals("composer.json")) {
                        count++;
                        return Database.document(pom.getURI().toString(), Pom.forComposer(pom));
                    } else {
                        count++;
                        local = world.getTemp().createTempFile();
                        try {
                            pom.copyFile(local);
                            console.info.println(pom.getURI().toString());
                            project = maven.loadPom(local);
                            origin = pom.getURI().toString();
                            if (origin.startsWith("https://artifactory")) {
                                origin = "svn:" + origin;
                            }
                            return Database.document(origin, project);
                        } finally {
                            local.deleteFile();
                        }
                    }
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IOException("error processing " + pom.getURI() + ": " + e.getMessage(), e);
                }
            }
        }

        @Override
        public void remove() {

        }

        public void summary() {
            console.info.println("Indexed " + (count - errors) + "/" + count + " poms successfully.");
        }
    }
}