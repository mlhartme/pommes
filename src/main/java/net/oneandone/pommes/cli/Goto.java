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
import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.mount.Action;
import net.oneandone.pommes.mount.Checkout;
import net.oneandone.pommes.mount.Fstab;
import net.oneandone.pommes.mount.Nop;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Goto extends Base {
    private final String query;
    private final FileNode shellFile;

    public Goto(Environment environment, FileNode shellFile, String query) {
        super(environment);
        this.shellFile = shellFile;
        this.query = query;
    }

    @Override
    public void run(Database database) throws Exception {
        Fstab fstab;
        List<Action> actions;
        List<FileNode> directories;
        Action action;
        String result;

        fstab = Fstab.load(world);
        actions = new ArrayList<>();
        for (Pom pom : database.query(query, environment)) {
            directories = fstab.directories(pom);
            if (directories.isEmpty()) {
                console.info.println("ignored (no mount points): " + pom);
            } else {
                for (FileNode directory : directories) {
                    action = Checkout.createOpt(environment, directory, pom);
                    actions.add(action != null ? action : new Nop(directory, pom.scm));
                }
            }
        }
        action = runSingle(actions);
        if (action == null) {
            throw new IOException("nothing selected");
        }
        result = "cd " + action.directory.getAbsolute();
        console.info.println(result);
        shellFile.writeString(result);
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
