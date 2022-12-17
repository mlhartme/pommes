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

import net.oneandone.pommes.checkout.Action;
import net.oneandone.pommes.checkout.Checkout;
import net.oneandone.pommes.checkout.Nop;
import net.oneandone.pommes.checkout.Problem;
import net.oneandone.pommes.database.Project;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Goto extends Base {
    private final boolean external;
    private final List<String> query;

    public Goto(boolean external, Environment environment, List<String> query) {
        super(environment);
        this.external = external;
        this.query = query;
    }

    @Override
    public void run(Scope scope) throws Exception {
        AutoCd autoCd;
        List<Action> actions;
        Action action;

        actions = getActions(external ? scope.queryCentral(query) : scope.queryDatabase(query));
        action = runSingle(actions);
        if (action == null) {
            throw new IOException("nothing selected");
        }
        if (!AutoCd.set(action.directory)) {
            console.info.println("cd " + action.directory.getAbsolute());
        }
    }

    private List<Action> getActions(List<Project> projects) {
        FileNode directory;
        Action action;
        List<Action> actions;

        actions = new ArrayList<>();
        for (Project project : projects) {
            try {
                directory = environment.lib.projectDirectory(project);
                action = Checkout.createOpt(directory, project);
                if (action == null) {
                    action = new Nop(directory);
                }
            } catch (IllegalArgumentException /* TODO */| IOException e) {
                action = new Problem(world.getWorking(), project + ": " + e.getMessage());
            }
            if (!actions.contains(action)) {
                actions.add(action);
            }
        }
        return actions;
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
