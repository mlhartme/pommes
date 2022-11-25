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
import net.oneandone.pommes.database.Database;
import net.oneandone.pommes.database.Field;
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.pommes.repository.Repository;
import net.oneandone.sushi.util.Separator;
import org.apache.lucene.document.Document;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Index extends Base {

    public Index(Environment environment) {
        super(environment);

    }


    @Override
    public void run(SearchEngine search) throws Exception {
        Repository repository;
        Indexer indexer;
        PrintWriter log;

        log = new PrintWriter(environment.home.logs().join("pommes.log").newWriter(), true);
        for (Map.Entry<String, String> entry : environment.home.properties().repositories.entrySet()) {
            repository = null;
            for (String str : Separator.SPACE.split(entry.getValue())) {
                if (str.startsWith("-")) {
                    repo(repository, str).addExclude(str.substring(1));
                } else if (str.startsWith("%")) {
                    repo(repository, str).addOption(str.substring(1));
                } else {
                    if (repository != null) {
                        throw new ArgumentException("duplicate repository");
                    }
                    repository = Repository.create(environment, entry.getKey(), str, log);
                }
            }

            try {
                console.info.println("indexing " + entry.getKey() + ": " + entry.getValue());
                indexer = new Indexer(environment, search.getDatabase(), entry.getKey());
                indexer.start();
                try {
                    repository.scan(indexer.src);
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

    private Repository repo(Repository repository, String str) {
        if (repository == null) {
            throw new ArgumentException("missing url before '" + str + "'");
        }
        return repository;
    }

    /** Iterates modified or new documents, skips unmodified ones */
    public static class Indexer extends Thread implements Iterator<Document> {
        private final Environment environment;

        public final BlockingQueue<Descriptor> src;
        private final Database database;
        private final String repository;

        private Exception exception;

        private Document current;
        private int count;
        private int errors;

        private final Map<String, String> existing;

        public Indexer(Environment environment, Database database, String repository) {
            super("Indexer");

            this.environment = environment;

            this.src = new ArrayBlockingQueue<>(25);

            this.database = database;
            this.repository = repository;
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
                database.list(repository, existing);
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
                existingRevision = existing.remove(descriptor.getRepository() + Field.ORIGIN_DELIMITER + descriptor.getPath());
                if (descriptor.getRevision().equals(existingRevision)) {
                    console.info.println("  " + descriptor.getPath());
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
                console.info.println((existingRevision == null ? "A " : "U ") + project.origin());
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
