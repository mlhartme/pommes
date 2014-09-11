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
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.mount.Action;
import net.oneandone.pommes.mount.Checkout;
import net.oneandone.pommes.mount.Fstab;
import net.oneandone.pommes.mount.Nop;
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.cli.Value;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Goto extends Base {
    @Value(name = "query", position = 1)
    private String query;

    private FileNode root;

    @Remaining
    public void add(String str) {
        if (root != null) {
            throw new ArgumentException("too many root arguments");
        }
        root = console.world.file(str);
    }

    public Goto(Console console, Maven maven) {
        super(console, maven);
    }

    @Override
    public void invoke() throws Exception {
        Fstab fstab;
        List<Action> actions;
        List<FileNode> directories;
        String svnurl;
        Action action;
        String result;

        if (root == null) {
            root = (FileNode) console.world.getWorking();
        }
        fstab = Fstab.load(console.world);
        actions = new ArrayList<>();
        try (Database database = Database.load(console.world)) {
            for (Pom pom : database.query(null, query)) {
                svnurl = pom.projectUrl();
                directories = fstab.directories(svnurl);
                if (directories.isEmpty()) {
                    throw new ArgumentException("no mount point for " + svnurl);
                }
                for (FileNode directory : directories) {
                    action = Checkout.createOpt(directory, svnurl);
                    actions.add(action != null ? action : new Nop(directory, svnurl));
                }
            }
        }
        action = runSingle(actions);
        if (action == null) {
            throw new IOException("nothing selected");
        }
        result = "cd " + action.directory.getAbsolute();
        console.info.println(result);
        console.world.getHome().join(".pommes.goto").writeString(result);
    }

    /** @return never null */
    protected Action runSingle(Collection<Action> actionsOrig) throws Exception {
        List<Action> actions;
        int no;
        String selection;
        Action last;

        if (actionsOrig.isEmpty()) {
            throw new IOException("not found: " + query);
        }
        actions = new ArrayList<>(actionsOrig);
        if (actions.size() == 1) {
            last = actions.get(0);
            last.run(console);
            return last;
        }
        Collections.sort(actions);
        last = null;
        do {
            no = 1;
            for (Action action : actions) {
                console.info.println("[" + no + "] " + action);
                no++;
            }
            selection = console.readline("Please select: ");
            try {
                no = Integer.parseInt(selection);
                if (no > 0 && no <= actions.size()) {
                    last = actions.remove(no - 1);
                    last.run(console);
                } else {
                    console.info.println("action not found: " + no);
                }
            } catch (NumberFormatException e) {
                console.info.println("action not found: " + no);
            }
        } while (last == null);
        return last;
    }
}
