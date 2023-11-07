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
    public static boolean matches(String name) {
        return name.equals(".pommes.json") || name.equals("pommes.json");
    }

    public static JsonDescriptor create(Environment notUsed, Node node, String repository, String path, String revision, ScmUrl repositoryScm) {
        JsonObject json;

        try {
            json = JsonParser.parseString(node.readString()).getAsJsonObject();
            return new JsonDescriptor(Project.fromJson(json), repository, path, revision, repositoryScm);
        } catch (IOException e) {
            throw new RuntimeException(node.getUri().toString() + ": cannot create json descriptor", e);
        }
    }

    //--

    private final Project orig;

    public JsonDescriptor(Project orig, String repository, String path, String revision, ScmUrl repositoryScm) {
        super(repository, path, revision, repositoryScm);
        this.orig = orig;
    }

    @Override
    protected Project doLoad(Environment environment, String repository, String origin, String revision, ScmUrl scm) {
        Project project;

        project = new Project(repository, origin, revision, orig.parent, orig.artifact, scm != null ? scm : orig.scm, orig.url);
        project.dependencies.addAll(orig.dependencies);
        return project;
    }
}
