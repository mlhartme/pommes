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
import com.google.gson.JsonParser;
import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Find;
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.descriptor.ErrorDescriptor;
import net.oneandone.pommes.descriptor.JsonDescriptor;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPInputStream;

public class JsonRepository extends Repository {
    private static final String JSON = "json:";
    private static final String INLINE = "inline:";

    public static JsonRepository createOpt(World world, String name, String url) throws URISyntaxException, NodeInstantiationException {
        if (url.startsWith(JSON)) {
            return new JsonRepository(name, Find.fileOrNode(world, url.substring(JSON.length())));
        }
        if (url.startsWith(INLINE)) {
            return new JsonRepository(name, world.memoryNode("[ " + url.substring(INLINE.length()) + " ]"));
        }
        return null;
    }

    private final Node node;

    public JsonRepository(String name, Node node) {
        super(name);
        this.node = node;
    }

    @Override
    public void scan(BlockingQueue<Descriptor> dest, Console console) throws IOException, InterruptedException {
        JsonArray array;
        Descriptor descriptor;
        Project project;

        try (InputStream src = node.getName().endsWith(".gz") ? new GZIPInputStream(node.newInputStream()) : node.newInputStream()) {
            array = JsonParser.parseReader(new InputStreamReader(src)).getAsJsonArray();
            for (JsonElement entry : array) {
                try {
                    project = Project.fromJson(entry.getAsJsonObject());
                    descriptor = new JsonDescriptor(project);
                    descriptor.setRepository(name);
                    descriptor.setPath(project.getPath());
                    descriptor.setRevision(node.getWorld().memoryNode(project.toJson().toString()).sha());
                } catch (Exception e) {
                    descriptor = new ErrorDescriptor(new IOException("json error: " + e.getMessage(), e));
                    descriptor.setRepository(name);
                    descriptor.setPath(node.getPath());
                }
                dest.put(descriptor);
            }
        }
    }
}
