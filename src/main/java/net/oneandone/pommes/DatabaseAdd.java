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
package net.oneandone.pommes;

import net.oneandone.maven.embedded.Maven;
import net.oneandone.pommes.lucene.Database;
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Action;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;
import org.apache.lucene.document.Document;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class DatabaseAdd extends Base {
    @Option("global")
    private boolean global;

    private List<Node> nodes = new ArrayList<>();
    private List<Filter> filters = new ArrayList<>();

    @Remaining
    public void remaining(String str) {
        Filter filter;

        if (!str.startsWith("-")) {
            try {
                nodes.add(console.world.node("svn:" + str));
            } catch (URISyntaxException e) {
                throw new ArgumentException(str + ": invalid url", e);
            } catch (NodeInstantiationException e) {
                throw new ArgumentException(str + ": access failed", e);
            }
            filter = new Filter();
            filter.include("**/trunk");
            filter.exclude("**/trunk/*");
            filter.exclude("**/trunk/**/*");
            filter.exclude("**/branches/**/*");
            filter.exclude("**/tags/**/*");
            filter.exclude("**/old/**/*");
            filter.exclude("**/src/**/*");
            filter.predicate(Predicate.DIRECTORY);
            filters.add(filter);
        } else {
            if (filters.isEmpty()) {
                throw new ArgumentException("missing url before exclude " + str);
            }
            if (str.startsWith("/") || str.endsWith("/")) {
                throw new ArgumentException("do not use '/' before or after excludes: " + str);
            }
            filter = filters.get(filters.size() - 1);
            filter.exclude("**/" + str.substring(1) + "/**/*");
        }
    }

    public DatabaseAdd(Console console, Maven maven) {
        super(console, maven);
    }

    public void invoke() throws IOException {
        List<Node> trunks;
        ProjectIterator iterator;

        if (nodes.size() == 0) {
            throw new ArgumentException("missing urls");
        }
        console.info.println("scanning svn ...");
        trunks = trunks();
        console.info.println("indexing ...");
        iterator = new ProjectIterator(console, maven, trunks.iterator());
        try (Database database = Database.load(console.world)) {
            database.index(iterator);
            iterator.summary();
            if (global) {
                console.info.println("uploaded global pommes database: " + database.upload().getURI());
            }
        }
    }

    public static class ProjectIterator implements Iterator<Document> {
        private final Console console;
        private final Maven maven;
        private final Iterator<Node> trunks;

        private Document current;
        private int count;
        private int errors;

        public ProjectIterator(Console console, Maven maven, Iterator<Node> trunks) {
            this.console = console;
            this.maven = maven;
            this.trunks = trunks;
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
                } catch (IOException e) {
                    console.error.println(e.getMessage());
                    e.getCause().printStackTrace(console.verbose);
                    errors++;
                    // fall-through
                }
            }
        }

        private Document iterUnchecked() throws IOException {
            Node trunk;
            Node pom;
            FileNode local;
            MavenProject project;

            while (trunks.hasNext()) {
                trunk = trunks.next();
                pom = trunk.join("pom.xml");
                try {
                    if (pom.exists()) {
                        count++;
                        local = console.world.getTemp().createTempFile();
                        try {
                            pom.copyFile(local);
                            console.info.println(pom.getURI().toString());
                            project = maven.loadPom(local);
                            checkScm(project, pom);
                            return Database.document(trunk.getURI().toString(), project);
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

    private static void checkScm(MavenProject project, Node node) throws IOException {
        Scm scm;
        String devel;
        String origin;

        scm = project.getScm();
        if (scm != null) {
            devel = scm.getDeveloperConnection();
            if (devel != null) {
                if (devel.startsWith(Database.SCM_SVN)) {
                    devel = Database.withSlash(devel);
                    origin = "scm:" + Database.withSlash(node.getParent().getURI().toString());
                    if (!devel.equals(origin)) {
                        throw new IOException("scm uri mismatch: expected " + origin + ", got " + devel);
                    }
                }
            }
        }
    }

    public List<Node> trunks() throws IOException {
        List<Node> result;

        result = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            scan(nodes.get(i), filters.get(i), result);
        }
        return result;
    }

    public void scan(Node root, Filter filter, final List<Node> result) throws IOException {
        filter.invoke(root, new Action() {
            @Override
            public void enter(Node node, boolean b) {
                console.verbose.println("scanning ... " + node.getURI());
            }

            @Override
            public void enterFailed(Node node, boolean b, IOException e) throws IOException {
                throw e;
            }

            @Override
            public void leave(Node node, boolean b) {
            }

            @Override
            public void select(Node node, boolean b) {
                result.add(node);
            }
        });
    }
}