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

import net.oneandone.pommes.mount.Point;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Separator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Properties {
    public static Properties load(FileNode file) throws IOException {
        String queryPrefix = "query.";
        String formatPrefix = "format.";
        java.util.Properties props;
        Point mount;
        Map<String, List<String>> queries;
        Map<String, String> formats;

        mount = null;
        queries = new HashMap<>();
        formats = new HashMap<>();
        props = file.readProperties();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(queryPrefix)) {
                queries.put(key.substring(queryPrefix.length()), Separator.SPACE.split(props.getProperty(key)));
            } else if (key.startsWith(formatPrefix)) {
                formats.put(key.substring(formatPrefix.length()), props.getProperty(key));
            } else if (key.equals("mount")) {
                mount = new Point(file.getWorld().file(props.getProperty(key)));
            } else {
                throw new IOException("unknown property: " + key);
            }
        }
        if (mount == null) {
            throw new IOException(file + ": missing property: mount");
        }
        return new Properties(mount, queries, formats);
    }

    //--

    public final Point mount;
    private Map<String, List<String>> queries;
    private Map<String, String> formats;

    public Properties(Point mount, Map<String, List<String>> queries, Map<String, String> formats) throws IOException {
        this.mount = mount;
        this.queries = queries;
        this.formats = formats;
    }

    public List<String> lookupQuery(String name) throws IOException {
        return queries.get(name);
    }

    public String lookupFormat(String name) throws IOException {
        return formats.get(name);
    }
}
