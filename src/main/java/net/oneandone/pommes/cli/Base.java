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
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class Base implements Command {
    protected final Console console;
    protected final Maven maven;

    public Base(Console console, Maven maven) {
        this.console = console;
        this.maven = maven;
    }

    protected void scanUpdate(FileMap checkouts, String baseUrl, FileNode baseDirectory,
                        Map<FileNode, String> adds, Map<FileNode, String> removes) throws IOException {
        List<String> urls;
        String path;
        FileNode workspace;
        String existingUrl;

        try (Database database = Database.load(console.world).updateOpt()) {
            urls = database.list(baseUrl);
        }
        for (String url : urls) {
            path = Strings.removeLeft(url, baseUrl);
            path = Strings.removeRight(path, "/trunk/");
            workspace = baseDirectory.join(path);
            existingUrl = checkouts.lookupUrl(workspace);
            if (existingUrl == null) {
                adds.put(workspace, url);
            } else if (!existingUrl.equals(url)) {
                throw new IOException("cannot checkout " + url + " to " + workspace + " because that's already used for " + existingUrl);
            }
        }
        for (FileNode directory : checkouts.under(baseDirectory)) {
            if (!urls.contains(checkouts.lookupUrl(directory))) {
                removes.put(directory, checkouts.lookupUrl(directory));
            }
        }
    }

    protected void performUpdate(FileMap mounts, Map<FileNode, String> mountAdds, Map<FileNode, String> mountRemoves,
                          FileMap checkouts, Map<FileNode, String> checkoutAdds, Map<FileNode, String> checkoutRemoves) throws IOException {
        boolean problems;

        if (mountAdds.isEmpty() && mountRemoves.isEmpty() && checkoutAdds.isEmpty() && checkoutRemoves.isEmpty()) {
            console.info.println("no changes");
        } else {
            problems = false;
            for (Map.Entry<FileNode, String> entry : mountAdds.entrySet()) {
                console.info.println("[mount " + entry.getValue() + " " + entry.getKey() + "]");
            }
            for (Map.Entry<FileNode, String> entry : mountRemoves.entrySet()) {
                console.info.println("[umount " + entry.getKey() + " (" + entry.getValue() + ")]");
            }
            for (Map.Entry<FileNode, String> entry : checkoutAdds.entrySet()) {
                console.info.println("A " + entry.getKey() + " (" + entry.getValue() + ")");
                if (entry.getKey().exists()) {
                    console.error.println("  ERROR: directory already exists");
                    problems = true;
                }
            }
            for (Map.Entry<FileNode, String> entry : checkoutRemoves.entrySet()) {
                console.info.println("D " + entry.getKey() + " (" + entry.getValue() + ")");
                if (modified(entry.getKey())) {
                    console.error.println("  ERROR: checkout has modifications");
                    problems = true;
                }
            }
            if (problems) {
                throw new IOException("aborted - fix the above error(s) first");
            }
            console.pressReturn();
            checkoutRemoves(checkouts, checkoutRemoves);
            checkoutAdds(checkouts, checkoutAdds);
            checkouts.save();
            mountRemoves(mounts, mountRemoves);
            mountAdds(mounts, mountAdds);
            mounts.save();
        }
    }

    private void checkoutRemoves(FileMap checkouts, Map<FileNode, String> removes) throws DeleteException, NodeNotFoundException {
        FileNode directory;

        for (Map.Entry<FileNode, String> entry : removes.entrySet()) {
            directory = entry.getKey();
            console.info.println("rm -rf " + entry.getKey());
            directory.deleteTree();
            checkouts.removeDirectory(directory);
        }
    }

    private void checkoutAdds(FileMap checkouts, Map<FileNode, String> adds) throws MkdirException, Failure {
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
            checkouts.addDirectory(directory, entry.getValue());
        }
    }

    private void mountRemoves(FileMap mounts, Map<FileNode, String> removes) throws DeleteException, NodeNotFoundException {
        FileNode directory;

        for (Map.Entry<FileNode, String> entry : removes.entrySet()) {
            directory = entry.getKey();
            mounts.removeDirectory(directory);
        }
    }

    private void mountAdds(FileMap mounts, Map<FileNode, String> adds) throws MkdirException, Failure {
        for (Map.Entry<FileNode, String> entry : adds.entrySet()) {
            mounts.addDirectory(entry.getKey(), entry.getValue());
        }
    }

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
}
