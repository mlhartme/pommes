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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.oneandone.inline.ArgumentException;
import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;

public class GithubRepository extends Repository {
    private static final String PROTOCOL = "github:";

    public static GithubRepository createOpt(Environment environment, String name, String url, PrintWriter log) {
        if (url.startsWith(PROTOCOL)) {
            return new GithubRepository(environment, name, url.substring(PROTOCOL.length()), log);
        } else {
            return null;
        }
    }

    private final Environment environment;
    private final World world;
    private final String user;
    private boolean branches;
    private boolean tags;
    private final PrintWriter log;

    public GithubRepository(Environment environment, String name, String user, PrintWriter log) {
        super(name);
        this.environment = environment;
        this.world = environment.world();
        this.user = user;
        this.branches = false;
        this.tags = false;
        this.log = log;
    }

    public void addOption(String option) {
        if (option.equals("branches")) {
            branches = true;
        } else if (option.equals("tags")) {
            tags = true;
        } else {
            throw new ArgumentException(user + ": unknown option: " + option);
        }
    }

    @Override
    public void scan(BlockingQueue<Descriptor> dest, Console console) throws IOException, URISyntaxException, InterruptedException {
        String str;
        JsonArray repositories;
        JsonObject r;
        String name;
        Repository repository;

        str = world.validNode("https://api.github.com/users/" + user + "/repos").readString();
        repositories = (JsonArray) JsonParser.parseString(str);
        for (JsonElement e : repositories) {
            r = e.getAsJsonObject();
            name = r.get("name").getAsString();
            repository = new NodeRepository(environment, this.name, world.validNode("svn:https://github.com/" + user + "/" + name), branches, tags, log);
            repository.scan(dest, console);
        }
    }
}
