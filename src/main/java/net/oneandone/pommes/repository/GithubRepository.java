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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.project.Project;
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;

public class GithubRepository implements Repository {
    private static final String PROTOCOL = "github:";

    public static GithubRepository createOpt(World world, String url) {
        if (url.startsWith(PROTOCOL)) {
            return new GithubRepository(world, url.substring(PROTOCOL.length()));
        } else {
            return null;
        }
    }

    private final World world;
    private final String user;

    public GithubRepository(World world, String user) {
        this.world = world;
        this.user = user;
    }

    public void addOption(String option) {
        throw new ArgumentException(user + ": unknown option: " + option);
    }

    public void addExclude(String exclude) {
        throw new ArgumentException(user + ": excludes not supported: " + exclude);
    }

    @Override
    public void scan(BlockingQueue<Project> dest) throws IOException, URISyntaxException, InterruptedException {
        String str;
        JsonParser parser;
        JsonArray repositories;
        JsonObject r;
        String name;
        Repository repository;

        str = world.validNode("https://api.github.com/users/" + user + "/repos").readString();
        parser = new JsonParser();
        repositories = (JsonArray) parser.parse(str);
        for (JsonElement e : repositories) {
            r = e.getAsJsonObject();
            name = r.get("name").getAsString();
            repository = new NodeRepository(world.validNode("svn:https://github.com/" + user + "/" + name));
            repository.scan(dest);
        }
    }
}