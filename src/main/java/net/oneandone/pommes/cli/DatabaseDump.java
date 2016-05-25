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
import java.net.URISyntaxException;
import java.util.List;

import com.google.gson.Gson;

import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;

public class DatabaseDump extends Base {
    private final Gson gson = new Gson();
    private final String query;
    private final Node target;

    public DatabaseDump(Environment environment, String query, String target) throws URISyntaxException, NodeInstantiationException {
        super(environment);
        this.query = query;
        this.target = target.isEmpty() ? world.node("console:///") : world.node(target);
    }

    @Override
    public void run(Database database) throws Exception {
        List<Pom> poms;

        console.verbose.println("Selecting all documents matching '" + query + "'");
        poms = database.query(query, environment);
        console.verbose.println("Got " + poms.size() + " doucments");
        target.writeString(gson.toJson(poms));

    }
}
