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
import net.oneandone.pommes.project.Project;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.http.HttpNode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;

public class BitbucketRepository implements Repository {
    public static void main(String[] args) throws IOException, URISyntaxException {
        HttpNode node;
        node = (HttpNode) World.create().node("https://bitbucket.1and1.org:443");
        node = node.getRoot().node("rest/api/1.0/projects/CISOOPS/repos", "limit=1&start=0");
        System.out.println(node);
        System.out.println(node.readString());

    }
    private static final String PROTOCOL = "bitbucket:";

    public static BitbucketRepository createOpt(World world, String url) throws URISyntaxException, NodeInstantiationException {
        if (url.startsWith(PROTOCOL)) {
            return new BitbucketRepository((HttpNode) world.node(url.substring(PROTOCOL.length())));
        } else {
            return null;
        }
    }

    private final HttpNode bitbucket;

    public BitbucketRepository(HttpNode bitbucket) {
        this.bitbucket = bitbucket;
    }

    public void addOption(String option) {
        throw new ArgumentException(bitbucket.getURI() + ": unknown option: " + option);
    }

    public void addExclude(String exclude) {
        throw new ArgumentException(bitbucket.getURI() + ": excludes not supported: " + exclude);
    }

    @Override
    public void scan(BlockingQueue<Project> dest) throws IOException, URISyntaxException, InterruptedException {
        int perPage = 10;
        String path;
        String str;
        Node node;
        JsonParser parser;
        JsonObject paged;

        path = "rest/api/1.0/projects/CISOOPS/repos";
        if (!bitbucket.getPath().isEmpty()) {
            path = bitbucket.getPath() + "/" + path;
        }
        parser = new JsonParser();
        for (int i = 0; true; i += perPage) {
            node = bitbucket.getRoot().node(path, "limit=" + perPage + "&start=" + i);
            str = node.readString();
            paged = parser.parse(str).getAsJsonObject();
            for (JsonElement repo : paged.get("values").getAsJsonArray()) {
                System.out.println(i + ": " + repo.getAsJsonObject().get("slug").getAsString() + " - " + repo.getAsJsonObject().get("name").getAsString());

                //                 name = r.get("name").getAsString();
                // repository = new NodeRepository(world.validNode("svn:https://github.com/" + user + "/" + name), branches, tags);
                // repository.scan(dest);
            }
            if (paged.get("isLastPage").getAsBoolean()) {
                break;
            }
        }
    }
}