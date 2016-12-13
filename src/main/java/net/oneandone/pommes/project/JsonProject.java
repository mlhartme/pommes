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
package net.oneandone.pommes.project;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;

public class JsonProject extends Project {
    public static boolean matches(String name) throws IOException {
        return name.equals(".pommes.json") || name.equals("pommes.json");
    }

    public static JsonProject create(Environment environment, Node node) {
        JsonObject json;

        try {
            json = new JsonParser().parse(node.readString()).getAsJsonObject();
        } catch (IOException e) {
            throw new RuntimeException("TODO", e);
        }
        return new JsonProject(environment.gson().fromJson(json, Pom.class));
    }

    //--

    private final Pom orig;

    public JsonProject(Pom orig) {
        this.orig = orig;
    }

    @Override
    protected Pom doLoad(Environment environment, String zone, String origin, String revision) throws IOException {
        Pom pom;

        pom = new Pom(zone, origin, revision, orig.parent, orig.artifact, orig.scm, orig.url);
        pom.dependencies.addAll(orig.dependencies);
        return pom;
    }
}
