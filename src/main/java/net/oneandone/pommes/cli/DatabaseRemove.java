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
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Remaining;

import java.util.ArrayList;
import java.util.List;

public class DatabaseRemove extends Base {
    private List<String> removes = new ArrayList<>();

    @Remaining
    public void remaining(String prefix) {
        removes.add(prefix);
    }

    public DatabaseRemove(Console console, Maven maven) {
        super(console, maven);
    }

    public void invoke(Database database) throws Exception {
        database.remove(removes);
    }
}