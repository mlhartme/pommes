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

import net.oneandone.inline.Console;
import net.oneandone.pommes.search.CentralSearch;
import net.oneandone.pommes.database.Database;
import net.oneandone.pommes.checkout.Action;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.util.Separator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class Base {
    protected final Environment environment;
    protected final World world;
    protected final Console console;

    public Base(Environment environment) {
        this.environment = environment;
        this.world = this.environment.world();
        this.console = this.environment.console();
    }

    public void run() throws Exception {
        try (Database database = environment.lib.loadDatabase()) {
            run(new Scope(database, environment, new CentralSearch(environment.world(), environment.maven())));
        }
    }

    public abstract void run(Scope scope) throws Exception;

    protected void runAll(Collection<Action> actionsOrig) throws Exception {
        List<Action> actions;
        int no;
        int problems;
        String selection;

        actions = new ArrayList<>(actionsOrig);
        if (actions.isEmpty()) {
            console.info.println("nothing to do");
            return;
        }
        do {
            no = 1;
            for (Action action : actions) {
                console.info.println("[" + no + "] " + action);
                no++;
            }
            console.info.println("[a]      all");
            console.info.println("[return] quit");
            selection = console.readline("Selection(s): ");
            if (selection.isEmpty()) {
                actions.clear();
            } else if ("a".equals(selection)) {
                problems = 0;
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
                if (problems > 0) {
                    throw new IOException(problems + " actions failed");
                }
            } else {
                for (String one : Separator.SPACE.split(selection)) {
                    try {
                        no = Integer.parseInt(one);
                        if (no > 0 && no <= actions.size()) {
                            actions.remove(no - 1).run(console);
                        } else {
                            console.info.println("action not found: " + one);
                        }
                    } catch (NumberFormatException e) {
                        console.info.println("action not found: " + one);
                    }
                }
            }
        } while (!actions.isEmpty());
    }
}
