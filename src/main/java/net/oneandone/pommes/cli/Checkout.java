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

import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.Field;
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.model.PommesQuery;
import net.oneandone.pommes.checkout.Action;
import net.oneandone.sushi.fs.file.FileNode;

import java.util.ArrayList;
import java.util.List;

public class Checkout extends Base {
    private final List<String> query;

    public Checkout(Environment environment, List<String> query) {
        super(environment);
        this.query = query;
    }

    @Override
    public void run(Database database) throws Exception {
        List<Action> adds;
        Action action;
        FileNode directory;

        adds = new ArrayList<>();
        for (Pom pom : Field.poms(database.query(PommesQuery.create(query, environment)))) {
            directory = environment.home.root().directory(pom);
            action = net.oneandone.pommes.checkout.Checkout.createOpt(directory, pom);
            if (action != null) {
                if (!adds.contains(action)) {
                    adds.add(action);
                }
            } else {
                console.verbose.println("already mounted: " + directory);
            }
        }
        runAll(adds);
    }
}
