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
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.pommes.storage.FileStorage;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.pommes.scm.ScmUrl;
import net.oneandone.sushi.fs.DirectoryNotFoundException;
import net.oneandone.sushi.fs.ListException;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.util.Separator;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Status extends Base {
    private final FileNode directory;

    public Status(Environment environment, FileNode directory) {
        super(environment);
        this.directory = directory;
    }

    @Override
    public void run(Scope scope) throws Exception {
        Map<FileNode, Scm> checkouts;
        Map<String, Step> steps;
        int id;
        FileNode found;
        Step step;

        checkouts = Scm.scanCheckouts(directory, environment.excludes());
        if (checkouts.isEmpty()) {
            throw new ArgumentException("no checkouts in " + directory);
        }
        steps = new LinkedHashMap<>();
        id = 0;
        for (Map.Entry<FileNode, Scm> entry : checkouts.entrySet()) {
            found = entry.getKey();
            try {
                step = Step.create(environment, scope.getDatabase(), found, entry.getValue());
            } catch (IOException e) {
                step = new Step("#", e.getMessage(), null);
                if (console.getVerbose()) {
                    e.printStackTrace(console.error);
                }
            }
            if (step.isNoop()) {
                console.info.println(step);
            } else {
                id++;
                steps.put(Integer.toString(id), step);
                console.info.println(step.toString(Integer.toString(id)));
            }
        }
        for (FileNode u : unknown(directory, checkouts.keySet(), environment.excludes())) {
            console.info.println(new Step("?", u + " (normal directory)", null));
        }

        if (!steps.isEmpty()) {
            while (true) {
                String input;
                input = console.readline("What do you want to fix, ctrl-c to abort (<numbers>/all)? ");
                if ("all".equals(input)) {
                    List<String> done = new ArrayList<>();
                    for (var entry : steps.entrySet()) {
                        if (entry.getValue().applyChecked(entry.getKey(), console)) {
                            done.add(entry.getKey());
                        }
                    }
                    steps.keySet().removeAll(done);
                } else {
                    for (String item : Separator.SPACE.split(input)) {
                        step = steps.get(item);
                        if (step == null) {
                            console.info.println("not found: " + item);
                        } else {
                            if (step.applyChecked(item, console)) {
                                steps.remove(item);
                            }
                        }
                    }
                }
                if (steps.isEmpty()) {
                    break;
                }
                for (var entry : steps.entrySet()) {
                    console.info.println(entry.getValue().toString(entry.getKey()));
                }
            }
        }
    }

    private static List<FileNode> unknown(FileNode root, Collection<FileNode> directories, Filter excludes) throws ListException, DirectoryNotFoundException {
        List<FileNode> lst;
        List<FileNode> result;

        result = new ArrayList<>();
        lst = new ArrayList<>();
        for (FileNode directory: directories) {
            candicates(root, directory, lst);
        }
        for (FileNode directory : lst) {
            for (FileNode node : directory.list()) {
                if (node.isFile()) {
                    // ignore
                } else if (hasAncestor(directories, node)) {
                    // required sub-directory
                } else if (excludes.matches(node.getRelative(root))) {
                    // excluded
                } else {
                    result.add(node);
                }
            }
        }
        return result;
    }

    private static boolean hasAncestor(Collection<FileNode> directories, FileNode node) {
        for (FileNode directory : directories) {
            if (directory.hasAncestor(node)) {
                return true;
            }
        }
        return false;
    }

    private static void candicates(FileNode root, FileNode directory, List<FileNode> result) {
        FileNode parent;

        if (directory.hasDifferentAncestor(root)) {
            parent = directory.getParent();
            if (!result.contains(parent)) {
                result.add(parent);
                candicates(root, parent, result);
            }
        }
    }

    public static class Step {
        public static Step create(Environment environment, Database database, FileNode found, Scm scm) throws IOException {
            List<Project> foundProjects;
            ScmUrl scmUrl;
            Project newPom;
            Descriptor probed;
            FileNode expected;
            Relocation relocation;

            scmUrl = scm.getUrl(found);
            foundProjects = database.projectsByScm(scmUrl);
            if (foundProjects.isEmpty()) {
                probed = FileStorage.probe(environment, "unused", found);
                if (probed == null) {
                    throw new IllegalStateException();
                }
                newPom = probed.load();
                try {
                    expected = environment.lib.projectDirectory(newPom);
                } catch (IOException e) {
                    throw new IOException(found + ": " + e.getMessage(), e);
                }
                relocation = found.equals(expected)? null : new Relocation(found, expected);
                return new Step("?", found.toString(), relocation);
            } else {
                try {
                    expected = expected(environment, foundProjects);
                } catch (IOException e) {
                    return new Step("#", found.toString() + " " + e.getMessage(), null);
                }
                if (found.equals(expected)) {
                    return new Step(" ", found.toString(), null);
                } else {
                    return new Step("!", found.toString(), new Relocation(found, expected));
                }
            }
        }

        private static FileNode expected(Environment environment, List<Project> foundProjects) throws IOException {
            FileNode result;
            FileNode step;

            result = null;
            for (var foundProject : foundProjects) {
                step = environment.lib.projectDirectory(foundProject);
                if (result == null) {
                    result = step;
                } else if (!step.equals(result)) {
                    throw new IOException("path ambiguous: " + foundProjects);
                }
            }
            return result;
        }
        private final String marker;
        private final String message;
        private final Relocation relocation;

        public Step(String marker, String message, Relocation relocation) {
            this.marker = marker;
            this.message = message;
            this.relocation = relocation;
        }

        public boolean isNoop() {
            return relocation == null;
        }

        public void apply() throws IOException {
            if (relocation != null) {
                relocation.apply();
            }
        }

        public boolean applyChecked(String name, Console console) {
            try {
                apply();
                console.info.println(name + " fixed");
                return true;
            } catch (IOException e) {
                console.error.println(name + " failed: " + e);
                e.printStackTrace(console.verbose);
                return false;
            }
        }

        public String toString() {
            return toString("-");
        }

        public String toString(String id) {
            String head = (id.equals("-") ? "    " : Strings.times(' ', id.length() > 1 ? 0 : 1) + "[" + id + "]") + " " + marker + " ";
            List<String> lines;
            StringBuilder result;

            lines = new ArrayList<>();
            lines.add(message);
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
