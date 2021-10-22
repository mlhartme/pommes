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
import net.oneandone.pommes.database.Database;
import net.oneandone.pommes.database.Field;
import net.oneandone.pommes.database.Pom;
import net.oneandone.pommes.project.Project;
import net.oneandone.pommes.repository.NodeRepository;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class Maintenance extends Base {
    private final FileNode directory;

    public Maintenance(Environment environment, FileNode directory) {
        super(environment);
        this.directory = directory;
    }

    @Override
    public void run(Database database) throws Exception {
        Map<FileNode, Scm> checkouts;
        FileNode found;
        Pom foundPom;
        Scm scm;
        String input;
        String scmUrl;
        Pom newPom;
        Project probed;

        checkouts = Scm.scanCheckouts(directory, environment.excludes());
        if (checkouts.isEmpty()) {
            throw new ArgumentException("no checkouts in " + directory);
        }
        for (Map.Entry<FileNode, Scm> entry : checkouts.entrySet()) {
            doRun(database, entry.getKey(), entry.getValue());
        }
    }

    private void doRun(Database database, FileNode found, Scm scm) throws IOException {
        Pom foundPom;
        String input;
        String scmUrl;
        Pom newPom;
        Project probed;
        FileNode expected;
        Relocation relocation;

        scmUrl = scm.getUrl(found);
        foundPom = database.pomByScm(scmUrl);
        if (foundPom != null) {
            return;
        }
        probed = NodeRepository.probe(environment, found);
        if (probed == null) {
            throw new IllegalStateException();
        }
        newPom = probed.load(environment, "local");
        expected = environment.home.root().directory(newPom);
        relocation = new Relocation(found, expected);
        console.info.println("? " + found + " " + scmUrl + " " + relocation);
        input = console.readline("add (y/n)? ");
        if ("y".equalsIgnoreCase(input)) {
            database.index(Collections.singleton(Field.document(newPom)).iterator());
            relocation.apply();
        }
    }

    public static class Relocation {
        private final FileNode found;
        private final FileNode expected;

        public Relocation(FileNode found, FileNode expected) {
            this.found = found;
            this.expected = expected;
        }

        public void apply() throws IOException {
            if (found.equals(expected)) {
                return;
            }
            expected.getParent().mkdirsOpt();
            found.move(expected);
        }

        public String toString() {
            FileNode parent;
            String mkdir;

            if (found.equals(expected)) {
                return "";
            }
            parent = expected.getParent();
            mkdir = parent.exists() ? "" : "mkdir -p " + parent + "; ";
            return mkdir + "relocation: mv " + found + " " + expected;
        }
    }
}
