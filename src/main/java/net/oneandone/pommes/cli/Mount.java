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

import java.util.HashMap;
import java.util.Map;

public class Mount extends Base {
    @Value(name = "pattern", position = 1)
    private String substring;

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
        String svnurl;
        Map<FileNode, String> adds;
        FileNode directory;

        if (root == null) {
            root = (FileNode) console.world.getWorking();
        }
        fstab = Fstab.load(console.world);
        adds = new HashMap<>();
        try (Database database = Database.load(console.world)) {
            for (Pom pom : database.substring(substring)) {
                svnurl = pom.svnUrl();
                if (svnurl != null) {
                    directory = fstab.locateOpt(svnurl);
                    if (directory != null && directory.hasAnchestor(root)) {
                        adds.put(directory, svnurl);
                    }
                }
            }
        }
        updateCheckouts(adds, new HashMap<FileNode, String>());
    }
}
