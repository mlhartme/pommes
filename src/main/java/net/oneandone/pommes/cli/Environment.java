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
package net.oneandone.pommes.cli;

import net.oneandone.inline.ArgumentException;
import net.oneandone.inline.Console;
import net.oneandone.maven.embedded.Maven;
import net.oneandone.pommes.database.Database;
import net.oneandone.pommes.database.Field;
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.database.SearchEngine;
import net.oneandone.pommes.database.Variables;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.pommes.repository.Repository;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import org.apache.lucene.document.Document;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Environment implements Variables {
    private final Console console;
    private final World world;
    public final Home home;
    private Maven lazyMaven;
    private Project lazyCurrentPom;
    private Filter lazyExcludes;

    public Environment(Console console, World world) throws IOException {
        this.console = console;
        this.world = world;
        this.home = Home.create(world, console, false);
        this.lazyMaven = null;
        this.lazyCurrentPom = null;
        this.lazyExcludes = null;
    }

    public Filter excludes() {
        if (lazyExcludes == null) {
            lazyExcludes = new Filter();
            lazyExcludes.include("**/.idea");
            lazyExcludes.include(".pommes");
        }
        return lazyExcludes;
    }

    public Console console() {
        return console;
    }

    public World world() {
        return world;
    }

    public Maven maven() throws IOException {
        if (lazyMaven == null) {
            lazyMaven = Maven.withSettings(world);
        }
        return lazyMaven;
    }

    public Project currentPom() throws IOException {
        if (lazyCurrentPom == null) {
            lazyCurrentPom = scanPom("current", world.getWorking());
        }
        return lazyCurrentPom;
    }

    //-- Variables interface

    private Map<String, String> arguments = new HashMap<>();

    public void defineArgument(int i, String argument) {
        arguments.put(Integer.toString(i), argument);
    }

    public void clearArguments() {
        arguments.clear();
    }

    public String lookup(String name) throws IOException {
        switch (name) {
            case "scm":
                return currentPom().scm;
            case "gav":
                return currentPom().artifact.toGavString();
            case "ga":
                return currentPom().artifact.toGaString();
            default:
                return arguments.get(name);
        }
    }

    //--

    public Project scanPom(String repository, FileNode directory) throws IOException {
        Project result;

        result = scanPomOpt(repository, directory);
        if (result == null) {
            throw new IllegalStateException(directory.toString());
        }
        return result;
    }

    /** @return null if not a checkout */
    public Project scanPomOpt(String repository, FileNode directory) throws IOException {
        Scm scm;
        Descriptor descriptor;

        scm = Scm.probeCheckout(directory);
        if (scm == null) {
            return null;
        }
        for (Node child : directory.list()) {
            descriptor = Descriptor.probe(this, child);
            if (descriptor != null) {
                descriptor.setRepository(repository);
                descriptor.setOrigin(scm.getUrl(directory) + "/" + child.getName());
                descriptor.setRevision(child.sha());
                descriptor.setScm(scm.getUrl(directory));
                return descriptor.load(this);
            }
        }
        return null;
    }

    public void index(SearchEngine search) throws Exception {
        DatabaseAdd cmd;
        FileNode marker;

        marker = search.getDatabase().scannedMarker();
        for (Map.Entry<String, String> entry : home.properties().repositories.entrySet()) {
            console.verbose.println("indexing " + entry.getKey());
            cmd = new DatabaseAdd(this, entry.getKey(), entry.getValue());
            cmd.run(this, search);
        }
        marker.writeBytes();
    }

    public static class DatabaseAdd {
        private final List<Repository> repositories;
        private final PrintWriter log;

        public DatabaseAdd(Environment environment, String repository, String str) throws IOException, URISyntaxException {
            this.repositories = new ArrayList<>();
            this.log = new PrintWriter(environment.home.logs().join("pommes.log").newWriter(), true);
            if (str.startsWith("-")) {
                previous(str).addExclude(str.substring(1));
            } else if (str.startsWith("%")) {
                previous(str).addOption(str.substring(1));
            } else {
                repositories.add(Repository.create(environment, repository, str, log));
            }
        }

        private Repository previous(String str) {
            if (repositories.isEmpty()) {
                throw new ArgumentException("missing url before '" + str + "'");
            }
            return repositories.get(repositories.size() - 1);
        }

        public void run(Environment environment, SearchEngine search) throws Exception {
            Environment.Indexer indexer;

            try {
                indexer = new Environment.Indexer(environment, search.getDatabase());
                indexer.start();
                try {
                    for (Repository repository : repositories) {
                        repository.scan(indexer.src);
                    }
                } finally {
                    indexer.src.put(Descriptor.END_OF_QUEUE);
                    indexer.join();
                }
                if (indexer.exception != null) {
                    throw indexer.exception;
                }
            } finally {
                log.close();
            }
        }

    }

    /** Iterates modified or new documents, skips unmodified ones */
    public static class Indexer extends Thread implements Iterator<Document> {
        private final Environment environment;

        public final BlockingQueue<Descriptor> src;
        private final Database database;
        private Exception exception;

        private Document current;
        private int count;
        private int errors;

        private final Map<String, String> existing;

        public Indexer(Environment environment, Database database) {
            super("Indexer");

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
                environment.console().verbose.println("scanned " + existing.size() + " existing projects: "
                        + (System.currentTimeMillis() - started) + " ms");
                current = iter();
                database.index(this);
                for (String origin : existing.keySet()) {
                    environment.console().info.println("D " + origin);
                }
                database.removeOrigins(existing.keySet());
                summary();
            } catch (Exception e) {
                exception = e;
                try {
                    // consume remaining to avoid blocking scans
                    while (iter() != null) {
                        // nop
                    }
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
            Descriptor descriptor;
            Project project;
            Console console;
            String existingRevision;

            console = environment.console();
            while (true) {
                try {
                    descriptor = src.take();
                } catch (InterruptedException e) {
                    continue; // TODO: ok so?
                }
                if (descriptor == Descriptor.END_OF_QUEUE) {
                    return null;
                }
                count++;
                existingRevision = existing.remove(descriptor.getOrigin());
                if (descriptor.getRevision().equals(existingRevision)) {
                    console.info.println("  " + descriptor.getOrigin());
                    continue;
                }
                try {
                    project = descriptor.load(environment);
                } catch (IOException e) {
                    console.error.println(e.getMessage());
                    e.printStackTrace(console.verbose);
                    errors++;
                    continue;
                }
                console.info.println((existingRevision == null ? "A " : "U ") + project.origin);
                return Field.document(project);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void summary() {
            environment.console().info.println((count - errors) + "/" + count + " poms processed successfully.");
        }
    }
}
