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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class BaseDatabaseAdd extends Base {
    public BaseDatabaseAdd(Environment environment) {
        super(environment);
    }

    @Override
    public void run(Database database) throws Exception {
        List<Node> nodes;

        console.info.println("scanning ...");
        nodes = collect();
        console.info.println("indexing ...");
        index(database, nodes);
    }

    public abstract List<Node> collect() throws IOException, URISyntaxException;

    public void index(Database database, List<Node> nodes) throws IOException {
        ProjectIterator iterator;

        iterator = new ProjectIterator(console, world, environment.maven(), nodes.iterator());
        database.index(iterator);
        iterator.summary();
    }

    public static class ProjectIterator implements Iterator<Document> {
        private final Console console;
        private final World world;
        private final Maven maven;
        private final Iterator<Node> projects;

        private Document current;
        private int count;
        private int errors;

        public ProjectIterator(Console console, World world, Maven maven, Iterator<Node> projects) {
            this.console = console;
            this.world = world;
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

        private Document iterUnchecked() throws IOException {
            Node pom;
            FileNode local;
            MavenProject project;
            String origin;

            while (projects.hasNext()) {
                pom = projects.next();
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
            return null;
        }

        @Override
        public void remove() {

        }

        public void summary() {
            console.info.println("Indexed " + (count - errors) + "/" + count + " poms successfully.");
        }
    }
}