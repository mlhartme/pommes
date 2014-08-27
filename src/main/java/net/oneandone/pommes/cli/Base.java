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
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.NodeNotFoundException;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Separator;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Base implements Command {
    protected final Console console;
    protected final Maven maven;

    public Base(Console console, Maven maven) {
        this.console = console;
        this.maven = maven;
    }

    protected void updateCheckouts(Map<FileNode, String> originalAdds, Map<FileNode, String> originalRemoves) throws IOException {
        Map<FileNode, String> adds;
        Map<FileNode, String> removes;
        FileNode directory;
        String svnurl;
        String scannedUrl;
        boolean problems;

        adds = new LinkedHashMap<>(originalAdds);
        removes = new LinkedHashMap<>(originalRemoves);
        problems = false;
        for (Map.Entry<FileNode, String> entry : originalAdds.entrySet()) {
            directory = entry.getKey();
            svnurl = entry.getValue();
            if (directory.exists()) {
                scannedUrl = scanUrl(directory);
                if (svnurl.equals(scannedUrl)) {
                    console.info.println("  " + directory + " (" + svnurl + ")");
                    adds.remove(directory);
                } else {
                    console.error.println("C " + directory + " (" + svnurl + " vs " + scannedUrl + ")");
                    problems = true;
                }
            } else {
                console.info.println("A " + entry.getKey() + " (" + entry.getValue() + ")");
            }
        }
        for (Map.Entry<FileNode, String> entry : removes.entrySet()) {
            console.info.println("D " + entry.getKey() + " (" + entry.getValue() + ")");
            if (modified(entry.getKey())) {
                console.error.println("  ERROR: checkout has modifications");
                problems = true;
            }
        }
        if (problems) {
            throw new IOException("aborted - fix the above error(s) first");
        }
        if (adds.isEmpty() && removes.isEmpty()) {
            console.info.println("no changes");
        } else {
            console.pressReturn();
            checkoutRemoves(removes);
            checkoutAdds(adds);
        }
    }

    private void checkoutRemoves(Map<FileNode, String> removes) throws DeleteException, NodeNotFoundException {
        FileNode directory;

        for (Map.Entry<FileNode, String> entry : removes.entrySet()) {
            directory = entry.getKey();
            console.info.println("rm -rf " + entry.getKey());
            directory.deleteTree();
        }
    }

    private void checkoutAdds(Map<FileNode, String> adds) throws MkdirException, Failure {
        FileNode directory;
        Launcher svn;

        for (Map.Entry<FileNode, String> entry : adds.entrySet()) {
            directory = entry.getKey();
            directory.getParent().mkdirsOpt();
            svn = svn(directory.getParent(), "co", entry.getValue(), directory.getAbsolute());
            if (console.getVerbose()) {
                console.verbose.println(svn.toString());
            } else {
                console.info.println("[svn co " + entry.getValue() + " " + directory + "]");
            }
            svn.exec(console.verbose);
        }
    }

    //--

    protected Launcher svn(FileNode dir, String ... args) throws Failure {
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
