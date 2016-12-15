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


import net.oneandone.inline.Console;
import net.oneandone.setenv.Setenv;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;

public class Setup {
    private final World world;
    private final Console console;

    public Setup(World world, Console console) {
        this.world = world;
        this.console = console;
    }

    public void run() throws IOException {
        FileNode home;

        home = Home.directory(world);
        console.info.println("Ready to setup pommes in " + home.getAbsolute());
        console.readline("Press return to continue, ctl-c to abort: ");
        Home.create(world, console, true);
        if (!Setenv.create().isConfigured()) {
            console.info.println("To complete the setup, please add the following to your bash initialization:");
            console.info.println(Setenv.get().setenvBash());
        }
    }
}
