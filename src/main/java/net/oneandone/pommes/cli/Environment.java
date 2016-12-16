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
import net.oneandone.maven.embedded.Maven;
import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.model.Variables;
import net.oneandone.pommes.project.Project;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Environment implements Variables {
    private final Console console;
    private final World world;
    public final Home home;
    private final boolean importNow;
    private final boolean importDaily;

    private Maven lazyMaven;
    private Pom lazyCurrentPom;
    private Filter lazyExcludes;

    public Environment(Console console, World world, boolean importNow, boolean importDaily) throws IOException {
        if (importNow && importDaily) {
            throw new ArgumentException("conflicting imports options");
        }

        this.console = console;
        this.world = world;
        this.home = Home.create(world, console, false);
        this.importNow = importNow;
        this.importDaily = importDaily;
        this.lazyMaven = null;
        this.lazyCurrentPom = null;
        this.lazyExcludes = null;
    }

    public Filter excludes() {
        if (lazyExcludes == null) {
            lazyExcludes = new Filter();
            lazyExcludes.include("**/.idea");
            lazyExcludes.include(".database");
        }
        return lazyExcludes;
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
            project = Project.probe(this, child);
            if (project != null) {
                project.setOrigin(scm.getUrl(directory) + "/" + child.getName());
                project.setRevision(child.sha());
                project.setScm(scm.getUrl(directory));
                return project.load(this, "checkout");
            }
        }
        return null;
    }

    public void imports(Database database) throws Exception {
        FileNode marker;

        marker = database.importMarker();
        if (importNow || (importDaily && (!marker.exists() || (System.currentTimeMillis() - marker.getLastModified()) / 1000 / 3600 > 24))) {
            DatabaseAdd cmd;

            for (Map.Entry<String, String> entry : home.properties().imports.entrySet()) {
                console.verbose.println("importing " + entry.getKey());
                cmd = new DatabaseAdd(this, true, false, entry.getKey());
                cmd.add(entry.getValue());
                cmd.run(database);
            }
            marker.writeBytes();
        }
    }
}
