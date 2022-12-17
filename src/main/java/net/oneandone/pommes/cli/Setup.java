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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Setup {
    private final World world;
    private final Console console;
    private final boolean batch;

    private final Map<String, String> repositories;

    public Setup(World world, Console console, boolean batch, List<String> keyValues) {
        this.world = world;
        this.console = console;
        this.batch = batch;
        this.repositories = new LinkedHashMap<>();
        for (String kv : keyValues) {
            add(kv);
        }
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
        directory = Lib.directory(world);
        if (!batch) {
            console.info.println("Ready to setup Pommes for this directory: " + directory.getAbsolute());
            console.info.println("Nothing outside this directory is touched; to revert setup, simply remove "
                    + directory.join(".pommes"));
            console.readline("Press return to continue, ctl-c to abort: ");
        }
        Lib lib = Lib.create(world, console, repositories);
        environment = new Environment(console, world);
        console.info.println("initial scan ...");
        new Index(environment, new ArrayList<>()).run();
        console.info.println("done");
        console.info.println("Edit " + directory.join("config") + " to adjust Pommes configuration.");
        console.info.println("Consider running 'pommes profile' to setup auto-cd");
    }
}
