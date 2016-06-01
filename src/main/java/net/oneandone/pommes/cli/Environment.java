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
import net.oneandone.pommes.model.Variables;
import net.oneandone.pommes.mount.Point;
import net.oneandone.pommes.project.Project;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Separator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Environment implements Variables {
    private final Console console;
    private final World world;

    private boolean download;
    private boolean noDownload;
    private boolean upload;

    private Point mount;

    private Maven lazyMaven;
    private Pom lazyCurrentPom;
    private Map<String, List<String>> lazyQueries;
    private Map<String, String> lazyFormats;

    public Environment(Console console, World world, boolean download, boolean noDownload, boolean upload) throws IOException {
        String m;

        if (download && noDownload) {
            throw new ArgumentException("incompatible load options");
        }
        this.console = console;
        this.world = world;
        this.download = download;
        this.noDownload = noDownload;
        this.upload = upload;

        m = System.getenv("POMMES_MOUNT");
        this.mount = new Point(m == null ? (FileNode) world.getHome().join("Pommes") : world.file(m));

        this.lazyMaven = null;
        this.lazyCurrentPom = null;
        this.lazyQueries = null;
        this.lazyFormats = null;
    }

    public void begin(Database database) throws IOException {
        if (download) {
            database.download(true);
            console.verbose.println("database downloaded.");
        } else if (noDownload) {
            console.verbose.println("no database download.");
            // nothing to do
        } else {
            database.downloadOpt();
        }
    }

    public void end(Database database) throws IOException {
        Node node;

        if (upload) {
            node = database.upload();
            console.info.println("uploaded global pommes database: " + node.getURI() + ", " + (node.size() / 1024) + "k");
        }
    }

    public Console console() {
        return console;
    }

    public World world() {
        return world;
    }

    public Point mount() {
        return mount;
    }

    public Maven maven() throws IOException {
        if (lazyMaven == null) {
            lazyMaven = Maven.withSettings(world);
        }
        return lazyMaven;
    }

    public Pom currentPom() throws IOException {
        if (lazyCurrentPom == null) {
            lazyCurrentPom = scanPom(world.getWorking());
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
                return currentPom().coordinates.toGavString();
            case "ga":
                return currentPom().coordinates.toGaString();
            default:
                return arguments.get(name);
        }
    }

    public List<String> lookupQuery(String name) throws IOException {
        lazyMacros();
        return lazyQueries.get(name);
    }

    public String lookupFormat(String name) throws IOException {
        lazyMacros();
        return lazyFormats.get(name);
    }

    private void lazyMacros() throws IOException {
        String queryPrefix = "query.";
        String formatPrefix = "format.";
        FileNode file;
        Properties props;

        if (lazyQueries == null) {
            if (lazyFormats != null) {
                throw new IllegalStateException();
            }
            lazyQueries = new HashMap<>();
            lazyFormats = new HashMap<>();
            file = (FileNode) world.getHome().join(".pommes.properties");
            if (!file.exists()) {
                file.writeLines(
                        "query.users=d:=ga=",
                        "format.default=%a",
                        "format.users=%a -> %d[=ga=]");
                console.info.println("create default configuration at " + file);
            }
            props = file.readProperties();
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith(queryPrefix)) {
                    lazyQueries.put(key.substring(queryPrefix.length()), Separator.SPACE.split(props.getProperty(key)));
                } else if (key.startsWith(formatPrefix)) {
                    lazyFormats.put(key.substring(formatPrefix.length()), props.getProperty(key));
                } else {
                    throw new IOException("unknown property");
                }
            }
        }
    }


    //--

    public Pom scanPom(FileNode directory) throws IOException {
        Pom result;

        result = scanPomOpt(directory);
        if (result == null) {
            throw new IllegalStateException(directory.toString());
        }
        return result;
    }

    /** @return null if not a checkout */
    public Pom scanPomOpt(FileNode directory) throws IOException {
        Scm scm;
        Project project;

        scm = Scm.probeCheckout(directory);
        if (scm == null) {
            return null;
        }
        for (Node child : directory.list()) {
            project = Project.probe(child);
            if (project != null) {
                project.setOrigin(scm.getUrl(directory));
                project.setRevision("checkout");
                return project.load(this);
            }
        }
        return null;
    }
}
