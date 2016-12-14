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
import net.oneandone.pommes.project.Project;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class Environment implements Variables {
    private final Console console;
    private final World world;

    private boolean import_;
    private boolean noImport;

    private Gson lazyGson;
    private Maven lazyMaven;
    private Pom lazyCurrentPom;
    private Properties lazyProperties;

    public Environment(Console console, World world, boolean import_, boolean noImport) throws IOException {
        if (import_ && noImport) {
            throw new ArgumentException("incompatible imports options");
        }
        this.console = console;
        this.world = world;
        this.import_ = import_;
        this.noImport = noImport;
        this.lazyGson = null;
        this.lazyMaven = null;
        this.lazyCurrentPom = null;
        this.lazyProperties = null;
    }

    public void imports(Database database) throws Exception {
        DatabaseAdd cmd;
        FileNode marker;

        if (noImport) {
            console.verbose.println("skip database imports.");
        } else {
            marker = database.importMarker();
            if (import_ || !marker.exists() || (System.currentTimeMillis() - marker.getLastModified()) / 1000 / 3600 > 24) {
                for (Map.Entry<String, String> entry : properties().imports.entrySet()) {
                    console.verbose.println("importing " + entry.getKey());
                    cmd = new DatabaseAdd(this, true, false, false, entry.getKey());
                    cmd.add(entry.getValue());
                    cmd.run(database);
                }
                marker.writeBytes();
            }
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
                return currentPom().artifact.toGavString();
            case "ga":
                return currentPom().artifact.toGaString();
            default:
                return arguments.get(name);
        }
    }

    public Properties properties() throws IOException {
        String path;
        FileNode file;

        if (lazyProperties == null) {
            path = System.getenv("POMMES_PROPERTIES");
            if (path == null) {
                file = world.getHome().join(".pommes.properties");
            } else {
                file = world.file(path);
            }
            if (!file.exists()) {
                Properties.writeDefaults(file);
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
        Pom result;

        scm = Scm.probeCheckout(directory);
        if (scm == null) {
            return null;
        }
        for (Node child : directory.list()) {
            project = Project.probe(this, child);
            if (project != null) {
                project.setOrigin(scm.getUrl(directory) + "/" + child.getName());
                project.setRevision("checkout");
                result = project.load(this, "checkout");
                try {
                    return result.fixScm(world);
                } catch (URISyntaxException e) {
                    throw new IOException(e);
                }
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
