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

import net.oneandone.maven.embedded.Maven;
import net.oneandone.pommes.model.Database;
import net.oneandone.sushi.cli.Command;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.fs.DeleteException;
import net.oneandone.sushi.fs.NodeNotFoundException;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Separator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class Base implements Command {
    protected final Console console;
    protected final Maven maven;

    public Base(Console console, Maven maven) {
        this.console = console;
        this.maven = maven;
    }

    protected void updateCheckouts(Map<FileNode, String> adds, Map<FileNode, String> removes) throws IOException {
        List<FileNode> directories;
        FileNode directory;
        String svnurl;
        String scannedUrl;
        int problems;
        Iterator<FileNode> iterator;
        Launcher svn;

        problems = 0;
        directories = new ArrayList<>(adds.keySet());
        directories.addAll(removes.keySet());
        Collections.sort(directories, new Comparator<FileNode>() {
            @Override
            public int compare(FileNode left, FileNode right) {
                return left.getAbsolute().compareTo(right.getAbsolute());
            }
        });
        iterator = directories.iterator();
        while (iterator.hasNext()) {
            directory = iterator.next();
            svnurl = adds.get(directory);
            if (svnurl != null) {
                if (directory.exists()) {
                    scannedUrl = scanUrl(directory);
                    if (svnurl.equals(scannedUrl)) {
                        console.info.println("  " + directory + " (" + svnurl + ")");
                        iterator.remove();
                    } else {
                        console.error.println("C " + directory + " (" + svnurl + " vs " + scannedUrl + ")");
                        problems++;
                    }
                } else {
                    console.info.println("A " + directory + " (" + svnurl + ")");
                }
            } else {
                svnurl = removes.get(directory);
                if (svnurl == null) {
                    throw new IllegalStateException();
                }
                console.info.println("D " + directory + " (" + svnurl + ")");
                if (modified(directory)) {
                    console.error.println("  ERROR: checkout has modifications");
                    problems++;
                }
            }
        }
        if (problems > 0) {
            throw new IOException("aborted - fix the above " + problems + " error(s) first");
        }
        if (directories.isEmpty()) {
            console.info.println("no changes");
            return;
        }

        console.pressReturn();

        problems = 0;
        for (FileNode d : directories) {
            directory = d;
            svnurl = adds.get(directory);
            if (svnurl != null) {
                directory.getParent().mkdirsOpt();
                svn = svn(directory.getParent(), "co", svnurl, directory.getName());
                if (console.getVerbose()) {
                    console.verbose.println(svn.toString());
                } else {
                    console.info.println("[svn co " + svnurl + " " + directory.getName() + "]");
                }
                try {
                    svn.exec(console.verbose);
                } catch (Failure e) {
                    console.error.println(e.getMessage());
                    problems++;
                }
            } else {
                if (removes.get(directory) == null) {
                    throw new IllegalStateException();
                }
                console.info.println("rm -rf " + directory);
                directory.deleteTree();
            }
        }
        if (problems > 0) {
            throw new IOException(problems + " checkouts failed");
        }
    }

    //--

    protected Launcher svn(FileNode dir, String ... args) {
        Launcher launcher;

        launcher = new Launcher(dir);
        launcher.arg("svn");
        launcher.arg("--non-interactive");
        launcher.arg("--trust-server-cert"); // needs svn >= 1.6
        launcher.arg(args);
        return launcher;
    }

    public static boolean modified(FileNode directory) throws IOException {
        return isModified(directory.exec("svn", "status"));
    }

    private static boolean isModified(String lines) {
        for (String line : Separator.on("\n").split(lines)) {
            if (line.trim().length() > 0) {
                if (line.startsWith("X") || line.startsWith("Performing status on external item")) {
                    // needed for pws workspace svn:externals  -> ok
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public static String scanUrl(FileNode directory) throws IOException {
        String url;
        int idx;

        url = directory.launcher("svn", "info").exec();
        idx = url.indexOf("URL: ") + 5;
        return Database.withSlash(url.substring(idx, url.indexOf("\n", idx)));
    }
}
