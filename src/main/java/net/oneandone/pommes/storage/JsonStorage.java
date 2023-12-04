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
package net.oneandone.pommes.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.cli.Find;
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.descriptor.ErrorDescriptor;
import net.oneandone.pommes.descriptor.JsonDescriptor;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPInputStream;

public class JsonStorage extends Storage {
    public static JsonStorage createJson(Environment environment, String name, String url) throws URISyntaxException, NodeInstantiationException {
        return new JsonStorage(name, Find.fileOrNode(environment.world(), url));
    }

    public static JsonStorage createInline(Environment environment, String name, String url) {
        return new JsonStorage(name, environment.world().memoryNode("[ " + url + " ]"));
    }

    private final Node<?> node;

    public JsonStorage(String name, Node<?> node) {
        super(name);
        this.node = node;
    }

    public void scan(BlockingQueue<Descriptor> dest, Console console) throws IOException, InterruptedException {
        for (Descriptor descriptor : list()) {
            dest.put(descriptor);
        }
    }

    public List<Descriptor> list() throws IOException {
        List<Descriptor> result = new ArrayList<>();
        JsonArray array;
        Descriptor descriptor;
        Project project;

        try (InputStream src = node.getName().endsWith(".gz") ? new GZIPInputStream(node.newInputStream()) : node.newInputStream()) {
            array = JsonParser.parseReader(new InputStreamReader(src)).getAsJsonArray();
            for (JsonElement entry : array) {
                try {
                    project = Project.fromJson(entry.getAsJsonObject());
                    descriptor = new JsonDescriptor(project, name, project.getPath(), node.getWorld().memoryNode(project.toJson().toString()).sha(), null);
                } catch (Exception e) {
                    descriptor = new ErrorDescriptor(new IOException("json error: " + e.getMessage(), e), this.name, node.getPath(), "TODO", null);
                }
                result.add(descriptor);
            }
        }
        return result;
    }
}
