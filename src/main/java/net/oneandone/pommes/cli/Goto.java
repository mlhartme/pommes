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
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.cli.Value;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
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
        List<Pom> selection;
        Pom pom;
        FileNode directory;
        String svnurl;

        if (root == null) {
            root = (FileNode) console.world.getWorking();
        }
        fstab = Fstab.load(console.world);
        try (Database database = Database.load(console.world)) {
            selection = database.substring(query);
        }
        if (selection.isEmpty()) {
            throw new IOException("not found: " + query);
        }
        pom = select(selection);
        svnurl = pom.projectUrl();
        directory = fstab.locateOpt(svnurl);
        if (directory == null) {
            throw new ArgumentException("no mount point for " + svnurl);
        }
        if (directory.exists()) {
            if (!scanUrl(directory).equals(svnurl)) {
                throw new IOException("directory already exists with a different checkout: " + directory);
            }
        } else {
            new Action.Checkout(directory, svnurl).run(console);
        }
    }

    private Pom select(List<Pom> selection) {
        int no;
        String input;

        if (selection.size() == 1) {
            return selection.get(0);
        }
        no = 1;
        for (Pom pom : selection) {
            console.info.println("[" + no + "] " + pom.toLine());
            no++;
        }
        while (true) {
            input = console.readline("Please select: ");
            no = Integer.parseInt(input);
            if (no >= 1 && no <= selection.size()) {
                return selection.get(no - 1);
            }
            console.info.println("invalid selection");
        }
    }
}
