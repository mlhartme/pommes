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
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.fs.DirectoryNotFoundException;
import net.oneandone.sushi.fs.ListException;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Umount extends Base {
    private FileNode root;

    @Option("stale")
    private boolean stale;

    @Remaining
    public void add(String str) {
        if (root != null) {
            throw new ArgumentException("too many root arguments");
        }
        root = console.world.file(str);
    }

    public Umount(Console console, Maven maven) {
        super(console, maven);
    }

    @Override
    public void invoke() throws Exception {
        Fstab fstab;
        List<FileNode> checkouts;
        String scannedUrl;
        Map<FileNode, String> removes;
        String origin;
        FileNode located;

        if (root == null) {
            root = (FileNode) console.world.getWorking();
        }
        fstab = Fstab.load(console.world);
        checkouts = new ArrayList<>();
        scanCheckouts(root, checkouts);
        if (checkouts.isEmpty()) {
            throw new ArgumentException("no checkouts under " + root);
        }
        removes = new HashMap<>();
        for (FileNode directory : checkouts) {
            scannedUrl = scanUrl(directory);
            located = fstab.locateOpt(scannedUrl);
            if (located == null) {
                console.info.println("? " + directory + " (" + scannedUrl + ")");
            } else if (directory.equals(located)) {
                removes.put(directory, scannedUrl);
            } else {
                console.info.println("C " + directory + " (" + scannedUrl + ")");
                console.info.println("  expected in directory " + located);
            }
        }
        if (stale) {
            try (Database database = Database.load(console.world)) {
                for (FileNode directory : new HashSet<>(removes.keySet())) {
                    origin = Database.withSlash(removes.get(directory)) + "pom.xml";
                    if (database.lookup(origin) != null) {
                        removes.remove(directory);
                    }
                }
            }
        }
        updateCheckouts(new HashMap<FileNode, String>(), removes);
    }
}
