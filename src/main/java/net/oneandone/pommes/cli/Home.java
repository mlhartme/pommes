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
import net.oneandone.pommes.checkout.Root;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;

public class Home {
    public static Home create(World world, Console console, boolean setup) throws IOException {
        FileNode lib;

        lib = directory(world);
        if (!lib.isDirectory()) {
            if (!setup) {
                throw new IOException("pommes is not set up. Please run 'pommes setup'");
            }
            console.info.println("creating " + lib);
            lib.mkdir();
            lib.join("logs").mkdir();
            Properties.writeDefaults(configFile(lib));
        }
        return new Home(lib);
    }

    /** @return .pommes directory to use */
    public static FileNode directory(World world) {
        String path;

        path = System.getenv("POMMES_ROOT");
        return (path != null ? world.file(path) : world.getHome().join("Projects")).join(".pommes");
    }

    //--

    /** points to .pommes directory */
    private final FileNode home;
    private final Properties properties;

    public Home(FileNode home) throws IOException {
        this.home = home;
        this.properties = Properties.load(configFile(home));
    }

    public String tokenOpt(String protocol) throws IOException {
        var file = home.join("." + Strings.removeRight(protocol.toLowerCase(), ":"));
        return file.exists() ? file.readString().trim() : null;
    }

    public Root root() throws IOException {
        return new Root(properties.checkouts);
    }

    public Database loadDatabase() throws IOException {
        return Database.load(home.join("database"));
    }

    private static FileNode configFile(FileNode home) {
        return home.join("config");
    }

    public Properties properties() {
        return properties;
    }

    public FileNode logs() {
        return home.join("logs");
    }
}
