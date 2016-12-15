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
import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.mount.Root;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;

public class Home {
    public static Home create(World world, Console console, boolean setup) throws IOException {
        FileNode rootHome;
        FileNode internalHome;
        FileNode dflt;
        FileNode dest;

        rootHome = directory(world);
        internalHome = rootHome.join(".pommes");
        if (!internalHome.isDirectory()) {
            if (!setup) {
                throw new IOException("pommes is not set up properly. Please run 'pommes setup'");
            }
            console.info.println("creating pommes home: " + rootHome);
            rootHome.mkdirOpt();
            internalHome.mkdir();
            internalHome.join("logs").mkdir();
            dflt = world.locateClasspathItem(Home.class).getParent().join("pommes.properties.default");
            dest = internalHome.join("pommes.properties");
            if (dflt.exists()) {
                dflt.copy(dest);
            } else {
                Properties.writeDefaults(dest);
            }
        }
        return new Home(rootHome);
    }

    public static FileNode directory(World world) {
        String path;

        path = System.getenv("POMMES_HOME");
        if (path == null) {
            return world.getHome().join("Pommes");
        } else {
            return world.file(path);
        }
    }

    //--

    private final FileNode rootHome;
    private final FileNode internalHome;
    private final Properties properties;

    public Home(FileNode home) throws IOException {
        this.rootHome = home;
        this.internalHome = rootHome.join(".pommes").checkDirectory();
        this.properties = Properties.load(propertiesFile());
    }

    public Root root() throws IOException {
        return new Root(rootHome);
    }

    public Database loadDatabase() throws IOException {
        return Database.load(internalHome.join("database"));
    }

    private FileNode propertiesFile() {
        return internalHome.join("pommes.properties");
    }

    public Properties properties() throws IOException {
        return properties;
    }

    public FileNode logs() throws IOException {
        return internalHome.join("logs");
    }
}
