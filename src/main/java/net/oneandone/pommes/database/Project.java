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
package net.oneandone.pommes.database;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.pommes.scm.ScmUrl;

import java.util.ArrayList;
import java.util.List;

/** This is the core class of pommes. */
public class Project {
    public static Project fromJson(JsonObject object) {
        JsonElement array;
        Project result;

        result = new Project(
                string(object, "storage"),
                string(object, "path"),
                string(object, "revision"),
                Gav.forGavOpt(stringOpt(object, "parent")),
                Gav.forGav(string(object, "artifact")),
                Scm.createValidUrl(string(object, "scm")),
                stringOpt(object, "url"));
        array = object.get("dependencies");
        if (array != null) {
            for (JsonElement element : array.getAsJsonArray()) {
                result.dependencies.add(Gav.forGav(element.getAsString()));
            }
        }
        return result;
    }

    public static String string(JsonObject obj, String name) {
        String str;

        str = stringOpt(obj, name);
        if (str == null) {
            throw new IllegalArgumentException("field '" + name + "' not found: " + obj);
        }
        return str;
    }

    public static String stringOpt(JsonObject obj, String name) {
        JsonElement e;

        e = obj.get(name);
        if (e == null) {
            return null;
        }
        if (e.isJsonPrimitive()) {
            return e.getAsString();
        } else {
            throw new IllegalArgumentException("field '" + name + "' is not a string: " + obj);
        }
    }

    //--

    public final String storage;
    public final String path;
    public final String revision;

    /** may be null */
    public final Gav parent;
    public final Gav artifact;
    /** may be null */
    public final ScmUrl scm;
    /** may be null */
    public final String url;

    public final List<Gav> dependencies;

    public Project(String storage, String path, String revision, Gav parent, Gav artifact, ScmUrl scm, String url) {
        if (storage == null || storage.contains(":")) {
            throw new IllegalArgumentException("storage: " + storage);
        }
        if (path == null) {
            throw new IllegalArgumentException("path: " + path);
        }
        if (scm == null) {
            throw new IllegalArgumentException("scm: " + scm);
        }
        this.storage = storage;
        this.path = path;
        this.revision = revision;
        this.parent = parent;
        this.artifact = artifact;
        this.scm = scm;
        this.url = url;
        this.dependencies = new ArrayList<>();
    }

    public String origin() {
        return storage + Field.ORIGIN_DELIMITER + path;
    }

    public String toLine() {
        return artifact.toGavString() + " @ " + scm;
    }

    @Override
    public String toString() {
        return toLine();
    }

    public String getPath() {
        return path;
    }

    public JsonObject toJson() {
        JsonObject obj;
        JsonArray array;

        obj = new JsonObject();
        obj.add("storage", new JsonPrimitive(storage));
        obj.add("path", new JsonPrimitive(path));
        obj.add("revision", new JsonPrimitive(revision));
        if (parent != null) {
            obj.add("parent", new JsonPrimitive(parent.toGavString()));
        }
        obj.add("artifact", new JsonPrimitive(artifact.toGavString()));
        obj.add("scm", new JsonPrimitive(scm.scmUrl()));
        if (url != null) {
            obj.add("url", new JsonPrimitive(url));
        }
        if (!dependencies.isEmpty()) {
            array = new JsonArray();
            for (Gav dep : dependencies) {
                array.add(new JsonPrimitive(dep.toGavString()));
            }
            obj.add("dependencies", array);
        }
        return obj;
    }
}
