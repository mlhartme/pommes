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
package net.oneandone.pommes.repository;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.project.Project;
import net.oneandone.sushi.fs.NewInputStreamException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.http.HttpNode;
import net.oneandone.sushi.fs.http.MovedTemporarilyException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;

/** https://developer.atlassian.com/static/rest/bitbucket-server/4.6.2/bitbucket-rest.html */
public class BitbucketRepository implements Repository {
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
    public void scan(BlockingQueue<Project> dest) throws IOException, URISyntaxException, InterruptedException {
        String bbProject;
        int perPage = 10;
        String path;
        String str;
        Node node;
        JsonParser parser;
        JsonObject paged;
        Node pom;
        Project project;
        String location;

        bbProject = bitbucket.getName();
        path = "rest/api/1.0/projects/" + bbProject + "/repos";
        parser = new JsonParser();
        for (int i = 0; true; i += perPage) {
            node = bitbucket.getRoot().node(path, "limit=" + perPage + "&start=" + i);
            str = node.readString();
            paged = parser.parse(str).getAsJsonObject();
            for (JsonElement repo : paged.get("values").getAsJsonArray()) {
                String name = repo.getAsJsonObject().get("slug").getAsString();

                // TODO: offer all files, not only poms. Recursion.
                // https://bitbucket.1and1.org:443/rest/api/1.0/projects/CISOOPS/repos/change/files/

                pom = node.getRoot().node("projects/" + bbProject + "/repos/" + name + "/browse/pom.xml", "raw");
                while (true) {
                    try {
                        // try to load. To see if we ne a redirected location
                        pom.readString();
                        break;
                    } catch (NewInputStreamException e) {
                        if (e.getCause() instanceof MovedTemporarilyException) {
                            location = ((MovedTemporarilyException) e.getCause()).location;
                            pom = bitbucket.getWorld().node(location);
                            continue;
                        } else {
                            throw e;
                        }
                    }
                }
                project = Project.probe(environment, pom);
                if (project != null) {
                    project.setRevision("todo");
                    project.setOrigin("todo");
                    dest.put(project);
                }
            }
            if (paged.get("isLastPage").getAsBoolean()) {
                break;
            }
        }
    }
}