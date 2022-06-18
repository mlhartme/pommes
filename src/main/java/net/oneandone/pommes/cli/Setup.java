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


import net.oneandone.inline.Console;
import net.oneandone.pommes.database.Database;
import net.oneandone.setenv.Setenv;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

public class Setup {
    private final World world;
    private final Console console;
    private final boolean batch;

    public Setup(World world, Console console, boolean batch) {
        this.world = world;
        this.console = console;
        this.batch = batch;
    }

    public void run() throws Exception {
        FileNode directory;
        Environment environment;

        directory = Home.directory(world);
        if (!batch) {
            console.info.println("Ready to setup Pommes in " + directory.getAbsolute());
            console.info.println("Nothing outside this directory is touched; to uninstall, simply delete this directory");
            console.readline("Press return to continue, ctl-c to abort: ");
        }
        Home.create(world, console, true);
        environment = new Environment(console, world);
        console.info.println("initial scan ...");
        try (Database database = environment.home.loadDatabase()) {
            environment.scan(database);
        }
        console.info.println("done");
        console.info.println("Have a look at " + directory.join("pommes.properties") + " and adjust Pommes to your needs");
        if (!Setenv.create().isConfigured()) {
            console.info.println("To complete the setup, please add the following to your bash initialization:");
            console.info.println(Setenv.get().setenvBash());
        }
    }
}
