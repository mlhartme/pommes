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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.NewInputStreamException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.http.HttpNode;
import net.oneandone.sushi.fs.http.HttpRoot;
import net.oneandone.sushi.fs.http.MovedTemporarilyException;
import net.oneandone.sushi.fs.http.StatusException;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiFunction;

/** https://developer.atlassian.com/static/rest/bitbucket-server/4.6.2/bitbucket-rest.html */
public class BitbucketRepository implements Repository {
    public static void main(String[] args) throws IOException {
        World world;
        Bitbucket bb;

        world = World.create();
        bb = new Bitbucket(((HttpNode) world.validNode("https://bitbucket.1and1.org")).getRoot(), new JsonParser());
        System.out.println("rev: " + new String(bb.readBytes("CISOOPS", "puc", "pom.xml")));
    }

    private static final String PROTOCOL = "bitbucket:";

    public static BitbucketRepository createOpt(Environment environment, String url) throws URISyntaxException, NodeInstantiationException {
        if (url.startsWith(PROTOCOL)) {
            return new BitbucketRepository(environment, (HttpNode) environment.world().node(url.substring(PROTOCOL.length())));
        } else {
            return null;
        }
    }

    private final Environment environment;
    private final HttpNode bitbucket;

    public BitbucketRepository(Environment environment, HttpNode bitbucket) {
        this.environment = environment;
        this.bitbucket = bitbucket;
    }

    public void addOption(String option) {
        throw new ArgumentException(bitbucket.getUri() + ": unknown option: " + option);
    }

    public void addExclude(String exclude) {
        throw new ArgumentException(bitbucket.getUri() + ": excludes not supported: " + exclude);
    }

    @Override
    public void scan(BlockingQueue<Descriptor> dest) throws IOException, URISyntaxException, InterruptedException {
        Bitbucket bb;
        String bbProject;
        Descriptor descriptor;
        BiFunction<Environment, Node, Descriptor> m;
        byte[] bytes;
        List<String> lst;
        Node tmp;
        String hostname;

        bb = new Bitbucket(bitbucket.getRoot(), environment.jsonParser());
        bbProject = bitbucket.getName();
        for (String repo : bb.listRepos(bbProject)) {
            lst = bb.listRoot(bbProject, repo);
            for (String path : lst) {
                m = Descriptor.match(path);
                if (m != null) {
                    bytes = bb.readBytes(bbProject, repo, path);
                    tmp = environment.world().memoryNode(bytes);
                    descriptor = m.apply(environment, tmp);
                    if (descriptor != null) {
                        hostname = bitbucket.getRoot().getHostname();
                        descriptor.setOrigin("bitbucket://" + hostname + "/" + bbProject.toLowerCase() + "/" + repo + "/" + path);
                        descriptor.setRevision(tmp.sha());
                        descriptor.setScm("git:ssh://git@" + hostname + "/" + bbProject.toLowerCase() + "/" + repo + ".git");
                        dest.put(descriptor);
                    }
                }
            }
        }
    }

    private static class Bitbucket {
        private final HttpRoot root;
        private final JsonParser parser;


        Bitbucket(HttpRoot root, JsonParser parser) {
            this.root = root;
            this.parser = parser;
        }

        // TODO: always fails with 404 error ...
        public String getRevision(String project, String repo, String path) throws IOException {
            JsonObject result;

            result = getJsonObject("rest/api/1.0/projects/" + project + "/repos/" + repo + "/last-modified/" + path, "");
            return result.get("id").getAsString();
        }

        public byte[] readBytes(String project, String repo, String path) throws IOException {
            String location;
            HttpNode node;

            node = root.node("projects/" + project + "/repos/" + repo + "/browse/" + path, "raw");
            while (true) {
                try {
                    // try to load. To see if we ne a redirected location
                    return node.readBytes();
                } catch (NewInputStreamException e) {
                    if (e.getCause() instanceof MovedTemporarilyException) {
                        location = ((MovedTemporarilyException) e.getCause()).location;
                        try {
                            node = (HttpNode) node.getWorld().node(location);
                        } catch (URISyntaxException e1) {
                            throw new IOException("cannot redirect to location " + location, e);
                        }
                        continue;
                    } else {
                        throw e;
                    }
                }
            }
        }

        public List<String> listRepos(String project) throws IOException {
            List<String> result;

            result = new ArrayList<>();
            for (JsonElement repo : getPaged("rest/api/1.0/projects/" + project + "/repos")) {
                result.add(repo.getAsJsonObject().get("slug").getAsString());
            }
            return result;
        }

        public List<String> listRoot(String project, String repo) throws IOException {
            String path;
            List<JsonElement> all;
            List<String> result;


            result = new ArrayList<>();
            try {
                all = getPaged("rest/api/1.0/projects/" + project + "/repos/" + repo + "/files/");
            } catch (NewInputStreamException e) {
                if (e.getCause() instanceof StatusException) {
                    if (((StatusException) e.getCause()).getStatusLine().code == 401) {
                        // happens if the repository is still empty
                        return result;
                    }
                }
                throw e;
            }

            for (JsonElement file : all) {
                path = file.getAsString();
                if (!path.contains("/")) {
                    result.add(path);
                }
            }
            return result;
        }

        //--

        private List<JsonElement> getPaged(String path) throws IOException {
            int perPage = 25;
            JsonObject paged;
            List<JsonElement> result;

            result = new ArrayList<>();
            for (int i = 0; true; i += perPage) {
                paged = getJsonObject(path, "limit=" + perPage + "&start=" + i);
                for (JsonElement value : paged.get("values").getAsJsonArray()) {
                    result.add(value);
                }
                if (paged.get("isLastPage").getAsBoolean()) {
                    break;
                }
            }
            return result;
        }

        private JsonObject getJsonObject(String path, String params) throws IOException {
            try (Reader src = root.node(path, params).newReader()) {
                return parser.parse(src).getAsJsonObject();
            }
        }
    }
}
