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
import net.oneandone.pommes.descriptor.ErrorDescriptor;
import net.oneandone.pommes.storage.Storage;
import net.oneandone.pommes.scm.Git;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.util.Separator;
import org.apache.lucene.document.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Index extends Base {
    private final List<String> storages;

    public Index(Environment environment, List<String> storages) {
        super(environment);

        Set<String> available = environment.lib.properties().storages.keySet();
        this.storages = storages;
        for (String storage : storages) {
            if (!available.contains(storage)) {
                throw new ArgumentException("storage not found: " + storage);
            }
        }
    }

    @Override
    public void run(Scope scope) throws Exception {
        Storage storage;
        Indexer indexer;

        for (Map.Entry<String, String> entry : environment.lib.properties().storages.entrySet()) {
            if (!storages.isEmpty() && !storages.contains(entry.getKey())) {
                // not selected
                continue;
            }
            storage = null;
            for (String str : Separator.SPACE.split(entry.getValue())) {
                if (str.equals("§§")) {
                    String host = storage(storage, str).getTokenHost();
                    if (host != null) {
                        Git.UP up = Scm.GIT.getCredentials(environment.console(), environment.world().getWorking(), host);
                        storage(storage, str).setToken(up.password());
                    }
                } else if (str.startsWith("-")) {
                    storage(storage, str).addExclude(str.substring(1));
                } else if (str.startsWith("%")) {
                    storage(storage, str).addOption(str.substring(1));
                } else {
                    if (storage != null) {
                        throw new ArgumentException("duplicate storage: " + storage);
                    }
                    storage = Storage.createStorage(environment, entry.getKey(), str);
                }
            }

            console.info.println("indexing " + entry.getKey() + ": " + entry.getValue());
            indexer = new Indexer(environment, scope.getDatabase(), entry.getKey());
            indexer.start();
            try {
                storage.scan(indexer.src, environment.console());
            } finally {
                indexer.src.put(ErrorDescriptor.END_OF_QUEUE);
                indexer.join();
            }
            if (indexer.exception != null) {
                throw indexer.exception;
            }
        }
    }

    private Storage storage(Storage storage, String str) {
        if (storage == null) {
            throw new ArgumentException("missing url before '" + str + "'");
        }
        return storage;
    }

    /** Iterates modified or new documents, skips unmodified ones */
    public static class Indexer extends Thread implements Iterator<Document> {
        private final Environment environment;

        public final BlockingQueue<Descriptor> src;
        private final Database database;
        private final String storage;

        private Exception exception;

        private Document current;
        private int count;
        private int errors;

        private final Map<String, String> existing;

        public Indexer(Environment environment, Database database, String storage) {
            super("Indexer");

            this.environment = environment;

            this.src = new ArrayBlockingQueue<>(25);

            this.database = database;
            this.storage = storage;
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
                database.list(storage, existing);
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
                exception.printStackTrace();
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
                if (descriptor == ErrorDescriptor.END_OF_QUEUE) {
                    return null;
                }
                count++;
                existingRevision = existing.remove(descriptor.getStorage() + Field.ORIGIN_DELIMITER + descriptor.getPath());
                if (descriptor.getRevision().equals(existingRevision)) {
                    console.info.println("  " + descriptor.getPath());
                    continue;
                }
                try {
                    project = descriptor.load();
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
