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
import net.oneandone.pommes.model.PommesQuery;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.apache.lucene.document.Document;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class Find extends Base {
    private static final String JSON = "-json";
    private static final String DUMP = "-dump";

    private final Node output;
    private final boolean fold;
    private final List<String> query;
    private StringBuilder formatBuilder;

    public Find(Environment environment, String output, boolean fold) throws URISyntaxException, NodeInstantiationException {
        super(environment);

        this.output = output == null ? null : fileOrNode(world, output);
        this.fold = fold;
        this.query = new ArrayList<>();
        this.formatBuilder = null;
    }

    public void arg(String arg) {
        switch (arg) {
            case "-":
                formatBuilder = new StringBuilder();
                return;
            case JSON:
                formatBuilder = new StringBuilder(JSON);
                return;
            case DUMP:
                formatBuilder = new StringBuilder(DUMP);
                return;
            default:
                if (formatBuilder == null) {
                    if (arg.startsWith("-")) {
                        formatBuilder = new StringBuilder(arg);
                    } else {
                        query.add(arg);
                    }
                } else {
                    if (formatBuilder.length() > 0) {
                        formatBuilder.append(' ');
                    }
                    formatBuilder.append(arg);
                }
        }
    }

    public static Node fileOrNode(World world, String target) throws URISyntaxException, NodeInstantiationException {
        FileNode file;

        if (!target.contains(":")) {
            file = world.file(target);
            if (file.getParent().isDirectory()) {
                return file;
            }
        }
        return world.node(target);
    }

    @Override
    public void run(Database database) throws Exception {
        List<Document> documents;
        String format;
        String macroName;
        List<String> macro;
        String tmpFormat;
        PrintStream dest;

        dest = null;
        try {
            if (query.size() > 0 && query.get(0).startsWith("@")) {
                for (int i = 1; i < query.size(); i++) {
                    environment.defineArgument(i, query.get(i));
                }
                macroName = query.get(0).substring(1);
                macro = environment.properties().lookupQuery(macroName);
                if (macro == null) {
                    throw new ArgumentException("query macro not found: " + macroName);
                }
                query.clear();
                query.addAll(macro);
            } else {
                macroName = null;
            }
            documents = database.query(PommesQuery.create(query, environment));
            console.verbose.println("Matching documents: " + documents.size());
            if (formatBuilder != null) {
                format = formatBuilder.toString();
                if (format.startsWith("-")) {
                    tmpFormat = environment.lookup(format.substring(1));
                    if (tmpFormat != null) {
                        format = tmpFormat;
                    }
                }
            } else {
                if (macroName == null) {
                    macroName = "default";
                }
                format = environment.properties().lookupFormat(macroName);
                if (format == null) {
                    format = "%o";
                }
            }
            console.verbose.println("query: " + query);
            console.verbose.println("format: " + format);

            if (output == null) {
                dest = System.out;
            } else if (output.getName().endsWith(".gz")) {
                dest = new PrintStream(new GZIPOutputStream(output.newOutputStream()));
            } else {
                dest = new PrintStream(output.newOutputStream());
            }
            switch (format) {
                case JSON:
                    json(Field.poms(documents), true, dest);
                    break;
                case DUMP:
                    json(Field.poms(documents), false, dest);
                    break;
                default:
                    text(documents, format, dest);
            }
        } finally {
            if (dest != null) {
                dest.close();
            }
            environment.clearArguments();
        }
    }

    private void text(List<Document> documents, String format, PrintStream dest) throws IOException {
        List<String> done;
        String line;

        done = new ArrayList<>();
        for (Document document : documents) {
            line = format(document, format);
            if (fold) {
                if (done.contains(line)) {
                    continue;
                }
                done.add(line);
            }
            dest.println(line);
        }
    }

    private void json(List<Pom> matches, boolean prettyprint, PrintStream dest) {
        GsonBuilder builder;
        Gson gson;

        builder = new GsonBuilder();
        if (prettyprint) {
            builder.setPrettyPrinting();
        }
        gson = builder.create();
        gson.toJson(matches, dest);
        dest.print('\n');
    }

    private String format(Document document, String format) throws IOException {
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
                        FileNode directory = environment.properties().root.directory(Field.pom(document));
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
