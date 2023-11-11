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
import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Setup {
    private final World world;
    private final Console console;
    private final boolean batch;
    private final FileNode root;

    private final Map<String, String> repositories;

    public Setup(World world, Console console, boolean batch, List<String> dirKeyValues) {
        this.world = world;
        this.console = console;
        this.batch = batch;
        this.repositories = new LinkedHashMap<>();
        this.root = eatRoot(dirKeyValues);
        for (String kv : dirKeyValues) {
            add(kv);
        }
    }


    private FileNode eatRoot(List<String> dirKeyValues) {
        if (!dirKeyValues.isEmpty()) {
            String kv = dirKeyValues.get(0);
            int idx = kv.indexOf('=');
            if (idx == -1) {
                dirKeyValues.remove(0);
                return world.file(kv);
            }
        }
        return world.getHome().join("Pommes");
    }

    private void add(String kv) {
        int idx = kv.indexOf('=');
        if (idx == -1) {
            throw new ArgumentException("delimiter '=' not found: " + kv);
        }
        String key = kv.substring(0, idx).trim();
        if (repositories.put(key, kv.substring(idx +1).trim()) != null) {
            throw new ArgumentException("duplicate key: " + key);
        }
    }

    public void run() throws Exception {
        FileNode directory;
        Environment environment;

        // TODO: duplicate file names ...
        directory = Lib.directoryOpt(world);
        if (directory != null && directory.exists())  {
            throw new IOException("pommes is already set up: " + directory);
        }
        if (!batch) {
            console.info.println("Ready to set up pommes in directory " + root);
            console.info.println("Directory " + directory.getAbsolute() + " will be created.");
            console.readline("Press return to continue, ctl-c to abort: ");
        }
        Lib.create(world, root, console, repositories);
        environment = new Environment(console, world);
        console.info.println("initial indexing ...");
        new Index(environment, new ArrayList<>()).run();
        console.info.println("indexing done");
        console.info.println();
        console.info.println("TODO for YOU:");
        console.info.println("1. source the appropriate profile in your shell initialization");
        console.info.println(shellInit(directory));
        console.info.println("2. restart terminal");
        console.info.println("3. optionally adjust " + directory.join("config"));
    }
    private String shellInit(FileNode directory) {
        String shell = getShell();
        FileNode file;

        file = directory.join("profiles", shell + ".rc");
        if (file.exists()) {
            return "     echo \"source '" + directory.join("profiles/zsh.rc").getAbsolute() + "'\" >> ~/." + shell + "rc";
        }
        return "   (choose profile from " + directory.join("profiles") + " and source it in your shell initialization";
    }

    private static String getShell() {
        String shell = System.getenv("SHELL");
        return shell == null ? "unknown" : shell.substring(shell.lastIndexOf('/') + 1);
    }
}
