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

import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Separator;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Properties {
    public static List<String> defaultConfig(Map<String, String> repositories) {
        List<String> lines;

        lines = new ArrayList<>();
        lines.add("# Pommes configuration file, see https://github.com/mlhartme/pommes");
        lines.add("");
        lines.add("# repositories");
        for (var entry : repositories.entrySet()) {
            lines.add("repository." + entry.getKey() + "=" + entry.getValue());
        }
        lines.add("");
        lines.add("# query macros");
        lines.add("query.users=d:=ga=");
        lines.add("");
        lines.add("# format macros");
        lines.add("format.default=%a");
        return lines;
    }

    public static Properties load(FileNode file) throws IOException {
        String queryPrefix = "query.";
        String formatPrefix = "format.";
        String repoPrefix = "repository.";
        String giteaKey;
        Map<String, String> props;
        Map<String, List<String>> queries;
        Map<String, String> formats;
        Map<String, String> repositories;
        FileNode checkouts;

        giteaKey = null;
        queries = new HashMap<>();
        formats = new HashMap<>();
        repositories = new LinkedHashMap<>();
        props = readSequencedProperties(file);
        checkouts = file.getParent().getParent();
        for (String key : props.keySet()) {
            if (key.equals("gitea.key")) {
                giteaKey = props.get(key);
            } else if (key.startsWith(queryPrefix)) {
                queries.put(key.substring(queryPrefix.length()), Separator.SPACE.split(props.get(key)));
            } else if (key.startsWith(formatPrefix)) {
                formats.put(key.substring(formatPrefix.length()), props.get(key));
            } else if (key.startsWith(repoPrefix)) {
                repositories.put(key.substring(repoPrefix.length()), props.get(key));
            } else {
                throw new IOException("unknown property: " + key);
            }
        }
        if (repositories.isEmpty()) {
            throw new IOException("missing repositories: " + file);
        }
        return new Properties(checkouts, giteaKey, queries, formats, repositories, repositories.keySet().iterator().next());
    }

    public static Map<String, String> readSequencedProperties(FileNode file) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();

        try (Reader src = file.newReader()) {
            java.util.Properties p = new java.util.Properties() {
                public synchronized Object put(Object key, Object value) {
                    result.put((String) key, (String) value);
                    return super.put(key, value);
                }
            };
            p.load(src);
        }
        return result;
    }

    //--

    public final FileNode checkouts;
    public final String giteaKey;
    private Map<String, List<String>> queries;
    private Map<String, String> formats;

    /** maps names to repository urls */
    public final Map<String, String> repositories;
    public final String defaultRepository;

    public Properties(FileNode checkouts, String giteaKey, Map<String, List<String>> queries,
                      Map<String, String> formats, Map<String, String> repositories, String defaultRepository) {
        this.checkouts = checkouts;
        this.giteaKey = giteaKey;
        this.queries = queries;
        this.formats = formats;
        this.repositories = repositories;
        this.defaultRepository = defaultRepository;
    }

    public List<String> lookupQuery(String name) {
        return queries.get(name);
    }

    public String lookupFormat(String name) {
        return formats.get(name);
    }
}
