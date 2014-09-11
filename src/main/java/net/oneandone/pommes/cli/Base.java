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
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Separator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class Base implements Command {
    protected final Console console;
    protected final Maven maven;

    public Base(Console console, Maven maven) {
        this.console = console;
        this.maven = maven;
    }

    protected void runAll(Collection<Action> actionsOrig) throws Exception {
        List<Action> actions;
        int no;
        int problems;
        String selection;

        actions = new ArrayList<>(actionsOrig);
        if (actions.isEmpty()) {
            console.info.println("no changes");
            return;
        }
        Collections.sort(actions);
        do {
            no = 1;
            for (Action action : actions) {
                console.info.println("[" + no + "] " + action.status());
                no++;
            }
            console.info.println("[d] done, no more actions");
            console.info.println("[a] all");
            selection = console.readline("Please select (default is a): ");
            if (selection.isEmpty()) {
                selection = "a";
            }
            for (String one : Separator.SPACE.split(selection)) {
                problems = 0;
                if (one.equals("d")) {
                    console.info.println("done");
                    actions.clear();
                } else if (one.equals("a")) {
                    for (Action action : actions) {
                        try {
                            action.run(console);
                        } catch (RuntimeException e) {
                            throw e;
                        } catch (Exception e) {
                            console.error.println(e.getMessage());
                            problems++;
                        }
                    }
                    actions.clear();
                } else {
                    try {
                        no = Integer.parseInt(one);
                        if (no > 0 && no <= actions.size()) {
                            actions.remove(no - 1).run(console);
                        } else {
                            console.info.println("action not found: " + no);
                        }
                    } catch (NumberFormatException e) {
                        console.info.println("action not found: " + no);
                    }
                }
                if (problems > 0) {
                    throw new IOException(problems + " actions failed");
                }
            }
        } while (!actions.isEmpty());
    }

    //--

    public static Launcher svn(FileNode dir, String ... args) {
        Launcher launcher;

        launcher = new Launcher(dir);
        launcher.arg("svn");
        launcher.arg("--non-interactive");
        launcher.arg("--trust-server-cert"); // needs svn >= 1.6
        launcher.arg(args);
        return launcher;
    }

    public static boolean notCommitted(FileNode directory) throws IOException {
        return notCommitted(directory.exec("svn", "status"));
    }

    private static boolean notCommitted(String lines) {
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

    //--

    public void scanCheckouts(FileNode directory, List<FileNode> result) throws IOException {
        List<FileNode> children;

        if (directory.join(".svn").isDirectory()) {
            result.add(directory);
        } else {
            children = directory.list();
            if (children != null) {
                for (FileNode child : children) {
                    scanCheckouts(child, result);
                }
            }
        }
    }
}
