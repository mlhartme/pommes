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

import java.io.IOException;

public class Environment implements Variables {
    private final Console console;
    private final World world;

    private boolean download;
    private boolean noDownload;
    private boolean upload;

    private Point mount;

    private Maven lazyMaven;
    private Pom lazyCurrentPom;

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

    public String lookup(String name) throws IOException {
        switch (name) {
            case "scm":
                return currentPom().scm;
            case "gav":
                return currentPom().coordinates.toGavString();
            case "ga":
                return currentPom().coordinates.toGaString();
            default:
                return null;
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
