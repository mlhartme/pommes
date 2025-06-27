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
import net.oneandone.pommes.database.Project;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.Map;

/** Represents the .pommes directory */
public class Lib {
    public static final String DIR = ".pommes";
    public static final String DEFAULT_ROOT = "Projects";

    public static Lib load(World world) throws IOException {
        FileNode lib;

        lib = directoryOpt(world);
        if (lib == null || !lib.isDirectory()) {
            throw new IOException("pommes is not set up. Please run 'pommes setup'");
        }
        return new Lib(lib);
    }

    public static void create(World world, FileNode root, Console console, Map<String, String> storages) throws IOException {
        FileNode lib;

        lib = root.join(DIR);
        if (lib.isDirectory()) {
            throw new IOException("already set up at " + lib);
        }
        console.info.println("creating " + lib);
        lib.mkdirs();
        lib.join("logs").mkdir();
        world.resource("profiles").copyDirectory(lib.join("profiles").mkdir());
        configFile(lib).writeLines(Properties.defaultConfig(storages));
    }

    /** @return .pommes directory to use or null if not defined */
    public static FileNode directoryOpt(World world) {
        String path;

        path = System.getenv("POMMES_ROOT"); // usually points to ~/Projects
        return path != null ? world.file(path).join(DIR) : null;
    }

    //--

    /** points to .pommes directory */
    private final FileNode home;
    private final Properties properties;

    public Lib(FileNode home) throws IOException {
        this.home = home;
        this.properties = Properties.load(configFile(home));
    }

    public FileNode projectDirectory(Project project) throws IOException {
        if (project.scm == null) {
            throw new IOException(project + ": missing scm");
        }
        return properties.checkouts.join(project.scm.directory());
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
