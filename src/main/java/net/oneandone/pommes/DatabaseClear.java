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
package net.oneandone.pommes;

import net.oneandone.maven.embedded.Maven;
import net.oneandone.pommes.lucene.Database;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Option;

import java.io.IOException;

public class DatabaseClear extends Base {
    @Option("global")
    private boolean global;

    public DatabaseClear(Console console, Maven maven) {
        super(console, maven);
    }

    public void invoke() throws IOException {
        Database database;

        database = Database.load(console.world, maven);
        database.clear();
        if (global) {
            console.info.println("uploaded global pommes database: " + database.upload().getURI());
        }
    }
}