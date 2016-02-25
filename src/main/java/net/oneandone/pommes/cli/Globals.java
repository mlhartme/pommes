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
import net.oneandone.pommes.model.Database;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.svn.SvnFilesystem;

import java.io.IOException;

public class Globals {
    private final Console console;
    private final World world;

    public FileNode shellFile;
    private String svnuser;
    private String svnpassword;

    private boolean download;
    private boolean noDownload;
    private boolean upload;


    public Globals(Console console, FileNode shellFile, String svnuser, String svnpassword,
                   boolean download, boolean noDownload, boolean upload) throws IOException {
        if (download && noDownload) {
            throw new ArgumentException("incompatible load options");
        }
        this.console = console;
        this.world = World.create();
        this.shellFile = shellFile;
        this.svnuser = svnuser;
        this.svnpassword = svnpassword;
        this.download = download;
        this.noDownload = noDownload;
        this.upload = upload;
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

    public Environment env() {
        return new Environment(world);
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
}
