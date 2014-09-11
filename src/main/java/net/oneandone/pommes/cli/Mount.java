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
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Mount extends Base {
    @Option("match")
    private String query = "";

    private FileNode root;

    @Remaining
    public void add(String str) {
        if (root != null) {
            throw new ArgumentException("too many root arguments");
        }
        root = console.world.file(str);
    }

    public Mount(Console console, Maven maven) {
        super(console, maven);
    }

    @Override
    public void invoke() throws Exception {
        Fstab fstab;
        Fstab.Line line;
        String svnurl;
        List<Action> adds;
        Action action;
        FileNode directory;
        int problems;

        if (root == null) {
            root = (FileNode) console.world.getWorking();
        }
        fstab = Fstab.load(console.world);
        line = fstab.line(root);
        if (line == null) {
            throw new ArgumentException("no url configured for directory " + root);
        }
        adds = new ArrayList<>();
        problems = 0;
        try (Database database = Database.load(console.world)) {
            for (Pom pom : database.query(line.svnurl(root), query)) {
                svnurl = pom.projectUrl();
                directory = fstab.locateOpt(svnurl);
                if (directory == null) {
                    throw new IllegalStateException(svnurl);
                }
                try {
                    action = Action.Checkout.createOpt(directory, svnurl);
                    if (action != null) {
                        adds.add(action);
                    } else {
                        console.verbose.println("already mounted: " + directory);
                    }
                } catch (Action.StatusException e) {
                    console.error.println(e.getMessage());
                    problems++;
                }
            }
        }
        if (problems > 0) {
            throw new IOException("aborted - fix the above problem(s) first: " + problems);
        }
        runAll(adds);
    }
}
