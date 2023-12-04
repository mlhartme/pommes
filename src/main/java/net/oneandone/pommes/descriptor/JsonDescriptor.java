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
package net.oneandone.pommes.descriptor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.scm.ScmUrl;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;

public class JsonDescriptor extends Descriptor {
    public static final String NAME = "pommes.json";

    public static boolean matches(String name) {
        return name.equals("." + NAME) || name.equals(NAME);
    }

    public static JsonDescriptor create(Environment notUsed, Node<?> node, String storage, String path, String revision, ScmUrl storageScm) {
        JsonObject json;

        try {
            json = JsonParser.parseString(node.readString()).getAsJsonObject();
            return new JsonDescriptor(Project.fromJson(json), storage, path, revision, storageScm);
        } catch (IOException e) {
            throw new RuntimeException(node.getUri().toString() + ": cannot create json descriptor", e);
        }
    }

    //--

    private final Project orig;

    public JsonDescriptor(Project orig, String storage, String path, String revision, ScmUrl storageScm) {
        super(storage, path, revision, storageScm);
        this.orig = orig;
    }

    @Override
    public Project load() {
        Project project;

        project = new Project(storage, path, revision, orig.parent, orig.artifact, storageScm != null ? storageScm : orig.scm, orig.url);
        project.dependencies.addAll(orig.dependencies);
        return project;
    }
}
