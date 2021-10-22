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
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.util.Strings;

import javax.json.Json;
import javax.json.stream.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;

public class ArtifactoryRepository implements Repository {
    private static final String PROTOCOL = "artifactory:";

    public static ArtifactoryRepository createOpt(Environment environment, String url) {
        if (url.startsWith(PROTOCOL)) {
            return new ArtifactoryRepository(environment, url.substring(PROTOCOL.length()));
        } else {
            return null;
        }
    }

    private final String url;
    private final Environment environment;
    private final World world;
    private String contextPath;

    public ArtifactoryRepository(Environment environment, String url) {
        this.environment = environment;
        this.world = environment.world();
        this.url = url;
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

    public void addExclude(String exclude) {
        throw new ArgumentException(url + ": excludes not supported: " + exclude);
    }

    @Override
    public void scan(BlockingQueue<Descriptor> dest) throws IOException, URISyntaxException {
        Node listing;
        Node root;

        root = world.node(url);
        listing = world.node(artifactory() + "api/storage/" + repositoryAndPath() + "?list&deep=1&mdTimestamps=0");
        try {
            Parser.run(environment, listing, root, dest);
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("scanning failed: " + e.getMessage(), e);
        }
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

        public static void run(Environment environment, Node listing, Node root, BlockingQueue<Descriptor> dest) throws Exception {
            String uri;
            long size;
            Date lastModified;
            String sha1;
            int count;
            Node node;
            Descriptor project;

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
                    project = Descriptor.probeChecked(environment, node);
                    if (project != null) {
                        project.setOrigin("artifactory:" + node.getUri().toString());
                        project.setRevision(sha1);
                        dest.put(project);
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
