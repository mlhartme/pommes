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
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.pommes.repository.NodeRepository;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Separator;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
        Map<String, Step> steps;
        int id;

        checkouts = Scm.scanCheckouts(directory, environment.excludes());
        if (checkouts.isEmpty()) {
            throw new ArgumentException("no checkouts in " + directory);
        }
        steps = new LinkedHashMap<>();
        id = 0;
        for (Map.Entry<FileNode, Scm> entry : checkouts.entrySet()) {
            FileNode found = entry.getKey();

            Step step = Step.createOpt(environment, database, found, entry.getValue());
            if (step != null) {
                steps.put(Integer.toString(++id), step);
            }
        }
        if (steps.isEmpty()) {
            console.info.println("nothing to do, " + checkouts.size() + " ok");
            return;
        }

        while (!steps.isEmpty()) {
            for (var entry : steps.entrySet()) {
                console.info.println(entry.getValue().toString(entry.getKey()));
            }
            String input;
            input = console.readline("What do you want to fix, ctrl-c to abort (<numbers>/all)? ");
            if ("all".equals(input)) {
                for (var entry : steps.entrySet()) {
                    entry.getValue().apply(database);
                    console.info.println("fixed " + entry.getKey());
                }
                steps.clear();
            } else {
                Step step;

                for (String item : Separator.SPACE.split(input)) {
                    step = steps.remove(item);
                    if (step == null) {
                        console.info.println("unknown input: " + item);
                        break;
                    }
                    step.apply(database);
                    console.info.println("fixed " + item);
                }
            }
        }
    }

    public static class Step {
        public static Step createOpt(Environment environment, Database database, FileNode found, Scm scm) throws IOException {
            Project foundPom;
            String scmUrl;
            Project newPom;
            Descriptor probed;
            FileNode expected;
            Relocation relocation;
            String marker;

            scmUrl = scm.getUrl(found);
            foundPom = database.projectByScm(scmUrl);
            if (foundPom == null) {
                probed = NodeRepository.probe(environment, found);
                if (probed == null) {
                    throw new IllegalStateException();
                }
                newPom = probed.load(environment, "local");
                expected = environment.home.root().directory(newPom);
                marker = "?";
            } else {
                newPom = null;
                expected = environment.home.root().directory(foundPom);
                marker = "C";
            }
            relocation = found.equals(expected)? null : new Relocation(found, expected);
            return newPom == null && relocation == null ? null : new Step(marker, found, scmUrl, newPom, relocation);
        }

        private final String marker;
        private final FileNode found;
        private final String scmUrl;
        private final Project newPom;
        private final Relocation relocation;

        public Step(String marker, FileNode found, String scmUrl, Project newPom, Relocation relocation) {
            this.marker = marker;
            this.found = found;
            this.scmUrl = scmUrl;
            this.newPom = newPom;
            this.relocation = relocation;
        }

        public void apply(Database database) throws IOException {
            if (newPom != null) {
                database.index(Collections.singleton(Field.document(newPom)).iterator());
            }
            if (relocation != null) {
                relocation.apply();
            }
        }

        public String toString(String id) {
            String head = "[" + id + "]" + " " + marker + " ";
            List<String> lines;
            StringBuilder result;

            lines = new ArrayList<>();
            lines.add(found.toString());
            if (newPom != null) {
                lines.add("add " + scmUrl);
            }
            if (relocation != null) {
                lines.add(relocation.toString());
            }
            if (lines.isEmpty()) {
                throw new IllegalStateException();
            }
            result = new StringBuilder();
            boolean first = true;
            for (String line : lines) {
                if (first) {
                    first = false;
                    result.append(head);
                } else {
                    result.append('\n');
                    result.append(Strings.times(' ', head.length()));
                }
                result.append(line);
            }
            return result.toString();
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
            expected.getParent().mkdirsOpt();
            found.move(expected);
        }

        public String toString() {
            FileNode parent;
            String mkdir;

            parent = expected.getParent();
            mkdir = parent.exists() ? "" : "mkdir -p " + parent + "; ";
            return "relocate: " + mkdir + "mv " + found + " " + expected;
        }
    }
}
