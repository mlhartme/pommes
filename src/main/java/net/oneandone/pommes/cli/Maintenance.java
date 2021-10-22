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

import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.database.Database;
import net.oneandone.pommes.database.Field;
import net.oneandone.pommes.database.Pom;
import net.oneandone.pommes.project.Project;
import net.oneandone.pommes.repository.NodeRepository;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.file.FileNode;

import java.util.Collections;
import java.util.Map;

public class Maintenance extends Base {
    private final FileNode directory;

    public Maintenance(Environment environment, FileNode directory) {
        super(environment);
        this.directory = directory;
    }

    @Override
    public void run(Database database) throws Exception {
        Map<FileNode, Scm> checkouts;
        FileNode found;
        Pom foundPom;
        Scm scm;
        String input;
        DatabaseAdd add;
        String scmUrl;

        checkouts = Scm.scanCheckouts(directory, environment.excludes());
        if (checkouts.isEmpty()) {
            throw new ArgumentException("no checkouts in " + directory);
        }
        for (Map.Entry<FileNode, Scm> entry : checkouts.entrySet()) {
            found = entry.getKey();
            scm = entry.getValue();
            scmUrl = scm.getUrl(found);
            foundPom = database.pomByScm(scmUrl);
            if (foundPom != null) {
                continue;
            }
            console.info.println(found + " " + scmUrl + " is unknown to pommes");
            input = console.readline("add (y/n)? ");
            if ("y".equalsIgnoreCase(input)) {
                Project probed;

                probed = NodeRepository.probe(environment, found);
                if (probed == null) {
                    throw new IllegalStateException();
                }
                database.index(Collections.singleton(Field.document(probed.load(environment, "local"))).iterator());
            }
        }
    }
}
