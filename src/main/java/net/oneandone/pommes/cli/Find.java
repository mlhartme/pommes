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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.Field;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.apache.lucene.document.Document;

import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class Find extends Base {
    private final boolean json;
    private final boolean dump;
    private final String format;
    private final String query;
    private final Node target;

    public Find(Environment environment, boolean json, boolean dump, String format, String query, String target)
            throws URISyntaxException, NodeInstantiationException {
        super(environment);

        if (count(json, dump, format != null) > 1) {
            throw new ArgumentException("ambiguous output options; you cannot mix -json, -dump and -format options");
        }

        this.json = json;
        this.dump = dump;
        this.format = format == null ? "%a @ %s %c" : format;
        this.query = query;
        this.target = target.isEmpty() ? world.node("console:///") : fileOrNode(world, target);
    }

    private static Node fileOrNode(World world, String target) throws URISyntaxException, NodeInstantiationException {
        FileNode file;

        if (!target.contains(":")) {
            file = world.file(target);
            if (file.getParent().isDirectory()) {
                return file;
            }
        }
        return world.node(target);
    }

    private static int count(boolean ... booleans) {
        int count;

        count = 0;
        for (boolean b : booleans) {
            if (b) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void run(Database database) throws Exception {
        List<Document> documents;

        documents = database.query(query, environment);
        console.verbose.println("Matching documents: " + documents.size());
        try (Writer dest = target.newWriter()) {
            if (json || dump) {
                json(Field.poms(documents), dest);
            } else {
                text(documents, dest);
            }
        }
    }

    private void text(List<Document> documents, Writer dest) throws IOException {
        List<String> done;
        String line;

        done = new ArrayList<>();
        for (Document document : documents) {
            line = format(document);
            if (!done.contains(line)) {
                done.add(line);
                dest.write(line);
                dest.write('\n');
            }
        }
    }

    private void json(List<Pom> matches, Writer dest) throws Exception {
        GsonBuilder builder;
        Gson gson;

        builder = new GsonBuilder();
        if (json) {
            builder.setPrettyPrinting();
        }
        gson = builder.create();
        gson.toJson(matches, dest);
    }

    private String format(Document document) throws IOException {
        char c;
        StringBuilder result;
        int end;
        String variable;
        String filter;
        List<String> values;
        boolean first;

        result = new StringBuilder();
        for (int i = 0, max = format.length(); i < max; i++) {
            c = format.charAt(i);
            if (c == '%' && i + 1 < max) {
                i++;
                c = format.charAt(i);
                values = new ArrayList<>();
                switch (c) {
                    case '%':
                        result.append(c);
                        values.add(Character.toString(c));
                        break;
                    case 'c':
                        FileNode directory = environment.mount().directory(Field.pom(document));
                        if (directory.exists()) {
                            values.add(directory.getAbsolute());
                        }
                        break;
                    default:
                        Field.forId(c).copy(document, values);
                }
                if (i + 1 < max && format.charAt(i + 1) == '[') {
                    end = format.indexOf(']', i + 2);
                    if (end == -1) {
                        throw new IllegalStateException("invalid format: " + format);
                    }
                    filter = format.substring(i + 2, end);
                    if (filter.startsWith("=")) {
                        if (filter.length() == 1 || !filter.endsWith("=")) {
                            throw new IllegalStateException("variable is not terminated: " + filter);
                        }
                        variable = filter.substring(1, filter.length() - 1);
                        filter = environment.lookup(variable);
                        if (filter == null) {
                            throw new IllegalStateException("unknown variable in format: " + variable);
                        }
                    }
                    i = end;
                } else {
                    filter = "";
                }
                first = true;
                for (String value : values) {
                    if (value.contains(filter)) {
                        if (first) {
                            first = false;
                        } else {
                            result.append(' ');
                        }
                        result.append(value);
                    }
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
