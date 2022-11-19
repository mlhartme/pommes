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

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Separator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Properties {
    public static void writeDefaults(FileNode file) throws IOException {
        file.writeLines(
                "# Pommes configuration file, see https://github.com/mlhartme/pommes",
                "",
                "# where to manage checkouts; defaults is ~/Projects",
                "# checkouts=/some/path",
                "",
                "# urls where to import from",
                "#import.first=url1",
                "#import.second=url2",
                "",
                "# query macros",
                "query.users=d:=ga=",
                "",
                "# format macros",
                "format.default=%a",
                "format.users=%a -> %d[=ga=]");
    }

    public static Properties load(FileNode file) throws IOException {
        World world;
        String queryPrefix = "query.";
        String formatPrefix = "format.";
        String repoPrefix = "repository.";
        String giteaKey;
        java.util.Properties props;
        Map<String, List<String>> queries;
        Map<String, String> formats;
        Map<String, String> repositories;
        FileNode checkouts;

        world = file.getWorld();
        giteaKey = null;
        queries = new HashMap<>();
        formats = new HashMap<>();
        repositories = new HashMap<>();
        props = file.readProperties();
        checkouts = world.getHome().join("Projects");
        for (String key : props.stringPropertyNames()) {
            if (key.equals("checkouts")) {
                checkouts = world.file(props.getProperty(key));
            } else if (key.equals("gitea.key")) {
                giteaKey = props.getProperty(key);
            } else if (key.startsWith(queryPrefix)) {
                queries.put(key.substring(queryPrefix.length()), Separator.SPACE.split(props.getProperty(key)));
            } else if (key.startsWith(formatPrefix)) {
                formats.put(key.substring(formatPrefix.length()), props.getProperty(key));
            } else if (key.startsWith(repoPrefix)) {
                repositories.put(key.substring(repoPrefix.length()), props.getProperty(key));
            } else {
                throw new IOException("unknown property: " + key);
            }
        }
        return new Properties(checkouts, giteaKey, queries, formats, repositories);
    }

    //--

    public final FileNode checkouts;
    public final String giteaKey;
    private Map<String, List<String>> queries;
    private Map<String, String> formats;

    /** maps names to repository urls */
    public final Map<String, String> repositories;

    public Properties(FileNode checkouts, String giteaKey, Map<String, List<String>> queries,
                      Map<String, String> formats, Map<String, String> repositories) {
        this.checkouts = checkouts;
        this.giteaKey = giteaKey;
        this.queries = queries;
        this.formats = formats;
        this.repositories = repositories;
    }

    public List<String> lookupQuery(String name) {
        return queries.get(name);
    }

    public String lookupFormat(String name) {
        return formats.get(name);
    }
}
