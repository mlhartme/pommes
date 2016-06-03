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

import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.mount.Root;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.io.OS;
import net.oneandone.sushi.util.Separator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Properties {
    public static void writeDefaults(FileNode file) throws IOException {
        FileNode local;
        World world;

        world = file.getWorld();
        if (OS.CURRENT == OS.MAC) {
            // see https://developer.apple.com/library/mac/qa/qa1170/_index.html
            local = (FileNode) world.getHome().join("Library/Caches/pommes");
        } else {
            local = world.getTemp().join("pommes-" + System.getProperty("user.name"));
        }
        file.writeLines(
                "# where to store the database locally",
                "database.local=" + local.getAbsolute(),
                "",
                "#database.global=",
                "",
                "# directory where to checkout",
                "mount=" + ((FileNode) world.getHome().join("Pommes")).getAbsolute(),
                "",
                "# query macros",
                "query.users=d:=ga=",
                "",
                "# format macros",
                "format.default=%a",
                "format.users=%a -> %d[=ga=]");
    }

    public static Properties load(FileNode file) throws IOException {
        String queryPrefix = "query.";
        String formatPrefix = "format.";
        World world;
        java.util.Properties props;
        FileNode databaseLocal;
        Node databaseGlobal;
        Root root;
        Map<String, List<String>> queries;
        Map<String, String> formats;

        world = file.getWorld();
        databaseLocal = null;
        databaseGlobal = null;
        root = null;
        queries = new HashMap<>();
        formats = new HashMap<>();
        props = file.readProperties();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(queryPrefix)) {
                queries.put(key.substring(queryPrefix.length()), Separator.SPACE.split(props.getProperty(key)));
            } else if (key.startsWith(formatPrefix)) {
                formats.put(key.substring(formatPrefix.length()), props.getProperty(key));
            } else if (key.equals("root")) {
                root = new Root(world.file(props.getProperty(key)));
            } else if (key.equals("database.local")) {
                databaseLocal = world.file(props.getProperty(key));
            } else if (key.equals("database.global")) {
                try {
                    databaseGlobal = world.node(props.getProperty(key));
                } catch (URISyntaxException e) {
                    throw new IOException(file + ": invalid uri: " + props.getProperty(key), e);
                }
            } else {
                throw new IOException("unknown property: " + key);
            }
        }
        if (databaseLocal == null) {
            throw new IOException(file + ": missing property: database.local");
        }
        if (root == null) {
            throw new IOException(file + ": missing property: root");
        }
        return new Properties(databaseLocal, databaseGlobal, root, queries, formats);
    }

    //--

    public final FileNode databaseLocal;
    public final Node databaseGlobal;

    public final Root root;
    private Map<String, List<String>> queries;
    private Map<String, String> formats;

    public Properties(FileNode databaseLocal, Node databaseGlobal,
                      Root root, Map<String, List<String>> queries, Map<String, String> formats) throws IOException {
        this.databaseLocal = databaseLocal;
        this.databaseGlobal = databaseGlobal;
        this.root = root;
        this.queries = queries;
        this.formats = formats;
    }

    public List<String> lookupQuery(String name) throws IOException {
        return queries.get(name);
    }

    public String lookupFormat(String name) throws IOException {
        return formats.get(name);
    }

    public Database loadDatabase() {
        return new Database(databaseLocal, databaseGlobal);
    }
}
