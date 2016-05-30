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
import net.oneandone.pommes.mount.Fstab;
import net.oneandone.pommes.mount.Point;
import net.oneandone.pommes.project.Project;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.svn.SvnFilesystem;

import java.io.IOException;

public class Environment implements Variables {
    private final Console console;
    private final World world;

    private String svnuser;
    private String svnpassword;

    private boolean download;
    private boolean noDownload;
    private boolean upload;

    private Maven lazyMaven;
    private Pom lazyCurrentPom;
    private Fstab lazyFstab;

    public Environment(Console console, World world, String svnuser, String svnpassword,
                       boolean download, boolean noDownload, boolean upload) throws IOException {
        if (download && noDownload) {
            throw new ArgumentException("incompatible load options");
        }
        this.console = console;
        this.world = world;
        this.svnuser = svnuser;
        this.svnpassword = svnpassword;
        this.download = download;
        this.noDownload = noDownload;
        this.upload = upload;

        this.lazyMaven = null;
        this.lazyCurrentPom = null;
        this.lazyFstab = null;
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
        if (svnuser != null && svnpassword != null) {
            SvnFilesystem filesystem = (SvnFilesystem) world.getFilesystem("svn");
            filesystem.setDefaultCredentials(svnuser, svnpassword);
            console.verbose.println("Using credentials from cli.");
        }
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

    public Fstab fstab() throws IOException {
        if (lazyFstab == null) {
            lazyFstab = Fstab.load(world);
        }
        return lazyFstab;
    }

    public Pom currentPom() throws IOException {
        if (lazyCurrentPom == null) {
            lazyCurrentPom = scanPom(world.getWorking());
        }
        return lazyCurrentPom;
    }

    //-- Variables interface

    public String lookup(String name) throws IOException {
        Pom p;
        FileNode here;
        Point point;

        switch (name) {
            case "scm":
                here = world.getWorking();
                point = fstab().pointOpt(here);
                if (point == null) {
                    throw new IllegalArgumentException("no mount point for directory " + here.getAbsolute());
                }
                return point.group(here);
            case "gav":
                p = currentPom();
                return p.coordinates.toGavString();
            case "ga":
                p = currentPom();
                return p.coordinates.toGaString();
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

    /** @return null if not a working copy; or url without "svn:" prefix, but with tailing slash */
    public Pom scanPomOpt(FileNode directory) throws IOException {
        Project project;
        String url;
        int idx;

        if (!directory.join(".svn").exists()) {
            return null;
        }
        for (Node child : directory.list()) {
            project = Project.probe(child);
            if (project != null) {
                url = directory.launcher("svn", "info").exec();
                idx = url.indexOf("URL: ") + 5;
                project.setOrigin("scm:" + withSlash(url.substring(idx, url.indexOf("\n", idx))));
                project.setRevision("checkout");
                return project.createPom(this);
            }
        }
        return null;
    }

    public static String withSlash(String url) {
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        return url;
    }
}
