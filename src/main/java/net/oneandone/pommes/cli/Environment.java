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
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.database.SearchEngine;
import net.oneandone.pommes.database.Variables;
import net.oneandone.pommes.descriptor.Descriptor;
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
    private final boolean scanNow;
    private final boolean scanDaily;

    private Maven lazyMaven;
    private Project lazyCurrentPom;
    private Filter lazyExcludes;

    public Environment(Console console, World world) throws IOException {
        this(console, world, false, false);
    }

    public Environment(Console console, World world, boolean scanNow, boolean scanDaily) throws IOException {
        if (scanNow && scanDaily) {
            throw new ArgumentException("conflicting scan options");
        }

        this.console = console;
        this.world = world;
        this.home = Home.create(world, console, false);
        this.scanNow = scanNow;
        this.scanDaily = scanDaily;
        this.lazyMaven = null;
        this.lazyCurrentPom = null;
        this.lazyExcludes = null;
    }

    public Filter excludes() {
        if (lazyExcludes == null) {
            lazyExcludes = new Filter();
            lazyExcludes.include("**/.idea");
            lazyExcludes.include(".pommes");
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

    public Project currentPom() throws IOException {
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

    public Project scanPom(FileNode directory) throws IOException {
        Project result;

        result = scanPomOpt(directory);

        if (result == null) {
            throw new IllegalStateException(directory.toString());
        }
        return result;
    }

    /** @return null if not a checkout */
    public Project scanPomOpt(FileNode directory) throws IOException {
        Scm scm;
        Descriptor descriptor;

        scm = Scm.probeCheckout(directory);
        if (scm == null) {
            return null;
        }
        for (Node child : directory.list()) {
            descriptor = Descriptor.probe(this, child);
            if (descriptor != null) {
                descriptor.setOrigin(scm.getUrl(directory) + "/" + child.getName());
                descriptor.setRevision(child.sha());
                descriptor.setScm(scm.getUrl(directory));
                return descriptor.load(this);
            }
        }
        return null;
    }

    public void implicitScan(SearchEngine search) throws Exception {
        FileNode marker;

        marker = search.getDatabase().scannedMarker();
        if (scanNow || (scanDaily && (!marker.exists() || (System.currentTimeMillis() - marker.getLastModified()) / 1000 / 3600 > 24))) {
            scan(search);
        }
    }

    public void scan(SearchEngine search) throws Exception {
        DatabaseAdd cmd;
        FileNode marker;

        marker = search.getDatabase().scannedMarker();
        for (Map.Entry<String, String> entry : home.properties().seeds.entrySet()) {
            console.verbose.println("adding " + entry.getKey());
            cmd = new DatabaseAdd(this, true, false);
            cmd.add(entry.getValue());
            cmd.run(search);
        }
        marker.writeBytes();
    }
}
