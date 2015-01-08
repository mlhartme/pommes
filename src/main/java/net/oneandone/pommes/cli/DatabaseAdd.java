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

import net.oneandone.maven.embedded.Maven;
import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import org.apache.lucene.document.Document;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class DatabaseAdd extends Base {
    @Option("noBranches")
    private boolean noBranches;

    private List<Node> nodes = new ArrayList<>();
    private List<Filter> excludes = new ArrayList<>();

    @Remaining
    public void remaining(String str) {
        Filter exclude;

        if (str.startsWith("-")) {
            if (excludes.isEmpty()) {
                throw new ArgumentException("missing url before exclude " + str);
            }
            if (str.startsWith("/") || str.endsWith("/")) {
                throw new ArgumentException("do not use '/' before or after excludes: " + str);
            }
            exclude = excludes.get(excludes.size() - 1);
            if (exclude == null) {
                exclude = new Filter();
                excludes.set(excludes.size() - 1, exclude);
            }
            // TODO: sushi bug? .exclude didn't work ...
            exclude.include("**/" + str.substring(1));
        } else {
            try {
                nodes.add(console.world.node("svn:" + str));
            } catch (URISyntaxException e) {
                throw new ArgumentException(str + ": invalid url", e);
            } catch (NodeInstantiationException e) {
                throw new ArgumentException(str + ": access failed", e);
            }
            excludes.add(null);
        }
    }

    public DatabaseAdd(Console console, Environment environment) {
        super(console, environment);
    }

    public void invoke(Database database) throws Exception {
        List<Node> projects;
        ProjectIterator iterator;

        if (nodes.size() == 0) {
            throw new ArgumentException("missing urls");
        }
        console.info.println("scanning svn ...");
        projects = projects();
        console.info.println("indexing ...");
        iterator = new ProjectIterator(console, environment.maven(), projects.iterator());
        database.index(iterator);
        iterator.summary();
    }

    public static class ProjectIterator implements Iterator<Document> {
        private final Console console;
        private final Maven maven;
        private final Iterator<Node> projects;

        private Document current;
        private int count;
        private int errors;

        public ProjectIterator(Console console, Maven maven, Iterator<Node> projects) {
            this.console = console;
            this.maven = maven;
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
                } catch (InvalidArtifactRTException | IOException e) {
                    console.error.println(e.getMessage());
                    e.getCause().printStackTrace(console.verbose);
                    errors++;
                    // fall-through
                }
            }
        }

        private Document iterUnchecked() throws IOException {
            Node pom;
            FileNode local;
            MavenProject project;

            while (projects.hasNext()) {
                pom = projects.next();
                try {
                    if (pom.getName().equals("composer.json")) {
                        count++;
                        return Database.document(pom.getURI().toString(), Pom.forComposer(pom));
                    } else {
                        count++;
                        local = console.world.getTemp().createTempFile();
                        try {
                            pom.copyFile(local);
                            console.info.println(pom.getURI().toString());
                            project = maven.loadPom(local);
                            return Database.document(pom.getURI().toString(), project);
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
            return null;
        }

        @Override
        public void remove() {

        }

        public void summary() {
            console.info.println("Indexed " + (count - errors) + "/" + count + " poms successfully.");
        }
    }

    //--

    public List<Node> projects() throws IOException {
        List<Node> result;

        result = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            scan(nodes.get(i), excludes.get(i), true, result);
        }
        return result;
    }

    public void scan(Node root, Filter excludes, boolean recurse, final List<Node> result) throws IOException {
        List<? extends Node> children;
        Node project;
        Node trunk;
        Node branches;
        List<? extends Node> grandChildren;

        if (excludes != null && excludes.matches(root.getPath())) {
            return;
        }
        console.verbose.println("scanning ... " + root.getURI());
        children = root.list();
        if (children == null) {
            return;
        }
        project = child(children, "pom.xml");
        if (project != null) {
            result.add(project);
            return;
        }
        project = child(children, "composer.json");
        if (project != null) {
            result.add(project);
            return;
        }
        trunk = child(children, "trunk");
        if (trunk != null) {
            scan(trunk, excludes, false, result);
        }
        branches = child(children, "branches");
        if (branches != null && !noBranches) {
            grandChildren = branches.list();
            if (grandChildren != null) {
                for (Node grandChild : grandChildren) {
                    scan(grandChild, excludes, false, result);
                }
            }
        }
        if (trunk != null || branches != null) {
            return;
        }

        if (child(children, "src") != null) {
            // probably an old ant build
            return;
        }

        if (recurse) {
            // recurse
            for (Node node : children) {
                scan(node, excludes, true, result);
            }
        }
    }

    private static Node child(List<? extends Node> childen, String name) {
        for (Node node : childen) {
            if (node.getName().equals(name)) {
                return node;
            }
        }
        return null;
    }
}