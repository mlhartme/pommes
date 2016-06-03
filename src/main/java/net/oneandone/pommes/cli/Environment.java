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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import net.oneandone.sushi.io.OS;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Environment implements Variables {
    private final Console console;
    private final World world;

    private boolean download;
    private boolean noDownload;
    private boolean upload;

    private Gson lazyGson;
    private Maven lazyMaven;
    private Pom lazyCurrentPom;
    private Properties lazyProperties;

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
        this.lazyGson = null;
        this.lazyMaven = null;
        this.lazyCurrentPom = null;
        this.lazyProperties = null;
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

    public Properties properties() throws IOException {
        String path;
        FileNode file;
        FileNode local;

        if (lazyProperties == null) {
            path = System.getenv("POMMES_PROPERTIES");
            if (path == null) {
                file = (FileNode) world.getHome().join(".pommes.properties");
            } else {
                file = world.file(path);
            }
            if (!file.exists()) {
                if (OS.CURRENT == OS.MAC) {
                    // see https://developer.apple.com/library/mac/qa/qa1170/_index.html
                    local = (FileNode) world.getHome().join("Library/Caches/pommes");
                } else {
                    local = world.getTemp().join("pommes-" + System.getProperty("user.name"));
                }
                file.writeLines(
                        "# where to store the database locally",
                        "database.local=" + local.getAbsolute(),
                        "",
                        "#database.global=",
                        "",
                        "# directory where to checkout",
                        "mount=" + ((FileNode) world.getHome().join("Pommes")).getAbsolute(),
                        "",
                        "# query macros",
                        "query.users=d:=ga=",
                        "",
                        "# format macros",
                        "format.default=%a",
                        "format.users=%a -> %d[=ga=]");
                console.info.println("create default configuration at " + file);
            }
            lazyProperties = Properties.load(file);
        }
        return lazyProperties;
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

    public Gson gson() {
        if (lazyGson == null) {
            lazyGson = new GsonBuilder().create();
        }
        return lazyGson;
    }
}
