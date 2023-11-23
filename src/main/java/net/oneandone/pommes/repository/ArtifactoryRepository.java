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
package net.oneandone.pommes.repository;

import net.oneandone.inline.ArgumentException;
import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.util.Strings;

import javax.json.Json;
import javax.json.stream.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class ArtifactoryRepository extends NextRepository<Descriptor> {
    public static ArtifactoryRepository create(Environment environment, String name, String url, PrintWriter log)
            throws NodeInstantiationException, URISyntaxException {
        return new ArtifactoryRepository(environment, name, url);
    }

    private final String url;
    private final Node<?> root;
    private final Environment environment;
    private final World world;
    private String contextPath;

    public ArtifactoryRepository(Environment environment, String name, String url) throws NodeInstantiationException, URISyntaxException {
        super(name);
        this.environment = environment;
        this.world = environment.world();
        this.url = url;
        this.root = world.node(url);
        this.contextPath = "/artifactory/";
        if (!url.contains(contextPath)) {
            this.contextPath = "/";
        }
    }

    public void addOption(String option) {
        String prefix = "context=";

        if (option.startsWith(prefix)) {
            contextPath = Strings.removeLeft(option, prefix);
            if (!contextPath.endsWith("/")) {
                throw new ArgumentException("context path does not end with a slash: " + contextPath);
            }
        }
        throw new ArgumentException(url + ": unknown option: " + option);
    }

    @Override
    public void scan(BlockingQueue<Descriptor> dest, Console console) throws IOException, URISyntaxException {
    }

    @Override
    public List<Descriptor> doScan() throws IOException {
        List<Descriptor> result = new ArrayList<>();
        Node<?> listing = world.validNode(artifactory() + "api/storage/" + repositoryAndPath() + "?list&deep=1&mdTimestamps=0");
        try {
            Parser.run(environment, name, listing, root, result);
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("scanning failed: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public Descriptor scanOpt(Descriptor descriptor) {
        // TODO: currently all processing done by parser
        return descriptor;
    }

    /** @return with tailing slash */
    private String artifactory() {
        int idx;

        idx = url.indexOf(contextPath, url.indexOf("://") + 3);
        if (idx == -1) {
            throw new ArgumentException("cannot locate artifactory root. Please specify a context");
        }
        return url.substring(0, idx + contextPath.length());
    }

    private String repositoryAndPath() {
        return Strings.removeLeft(url, artifactory());
    }

    //--

    public static class Parser implements AutoCloseable {
        private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

        public static void run(Environment environment, String repository, Node listing, Node root, List<Descriptor> dest) throws Exception {
            String uri;
            long size;
            Date lastModified;
            String sha1;
            int count;
            Node node;

            count = 0;
            try (InputStream is = listing.newInputStream(); Parser parser = new Parser(Json.createParser(is))) {
                parser.next(JsonParser.Event.START_OBJECT);
                parser.eatKeyValueString("uri");
                parser.eatKeyValueString("created");
                parser.eatKey("files");
                parser.next(JsonParser.Event.START_ARRAY);
                while (parser.next() == JsonParser.Event.START_OBJECT) {
                    uri = parser.eatKeyValueString("uri");
                    size = parser.eatKeyValueNumber("size");
                    try {
                        lastModified = FMT.parse(parser.eatKeyValueString("lastModified"));
                    } catch (ParseException e) {
                        throw new IllegalStateException();
                    }
                    parser.eatKeyValueFalse("folder");
                    sha1 = parser.eatKeyValueString("sha1");
                    node = root.join(Strings.removeLeft(uri, "/"));
                    Descriptor.Creator m = Descriptor.match(((Node<?>) node).getName());
                    if (m != null) {
                        dest.add(m.create(environment, node, repository, "artifactory:" + node.getPath(), sha1, null));
                    }
                    if (parser.eatTimestampsOpt() != JsonParser.Event.END_OBJECT) {
                        throw new IllegalStateException();
                    }
                    count++;
                }
            }
        }

        private final JsonParser parser;

        public Parser(JsonParser parser) {
            this.parser = parser;
        }

        public JsonParser.Event next() {
            return parser.next();
        }

        public void next(JsonParser.Event expected) {
            JsonParser.Event event;

            event = next();
            if (event != expected) {
                throw new IllegalStateException(event + " vs " + expected);
            }
        }

        public JsonParser.Event eatTimestampsOpt() {
            JsonParser.Event result;

            result = parser.next();
            if (result != JsonParser.Event.KEY_NAME) {
                return result;
            }
            if (!"mdTimestamps".equals(parser.getString())) {
                throw new IllegalStateException();
            }
            next(JsonParser.Event.START_OBJECT);
            next(JsonParser.Event.KEY_NAME);
            next(JsonParser.Event.VALUE_STRING);
            next(JsonParser.Event.END_OBJECT);
            return parser.next();
        }

        public void eatKey(String key) {
            next(JsonParser.Event.KEY_NAME);
            if (!key.equals(parser.getString())) {
                throw new IllegalStateException();
            }
        }

        public String eatKeyValueString(String key) {
            eatKey(key);
            next(JsonParser.Event.VALUE_STRING);
            return parser.getString();
        }

        public long eatKeyValueNumber(String key) {
            eatKey(key);
            next(JsonParser.Event.VALUE_NUMBER);
            return parser.getLong();
        }

        public void eatKeyValueFalse(String key) {
            eatKey(key);
            next(JsonParser.Event.VALUE_FALSE);
        }

        @Override
        public void close() throws Exception {
            parser.close();
        }
    }
}
