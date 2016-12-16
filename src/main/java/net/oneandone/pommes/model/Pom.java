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
package net.oneandone.pommes.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

public class Pom {
    public static Pom fromJson(JsonObject object) {
        JsonElement array;
        Pom result;

        result = new Pom(string(object, "id"),
                string(object, "revision"),
                Gav.forGavOpt(stringOpt(object, "parent")),
                Gav.forGav(string(object, "artifact")),
                string(object, "scm"),
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

    /** id = zone + ":" + origin */
    public final String id;
    public final String revision;

    /** may be null */
    public final Gav parent;
    public final Gav artifact;
    /** may be null */
    public final String scm;
    /** may be null */
    public final String url;

    public final List<Gav> dependencies;

    public Pom(String zone, String origin, String revision, Gav parent, Gav artifact, String scm, String url) {
        this(zone + ":" + origin, revision, parent, artifact, scm, url);
    }

    public Pom(String id, String revision, Gav parent, Gav artifact, String scm, String url) {
        if (id == null) {
            throw new IllegalArgumentException("id: " + id);
        }
        if (scm == null || scm.endsWith("/")) { // forbid tailing / to normalize svn urls - some end with / and some not
            throw new IllegalArgumentException("scm: " + scm);
        }
        this.id = id;
        this.revision = revision;
        this.parent = parent;
        this.artifact = artifact;
        this.scm = scm;
        this.url = url;
        this.dependencies = new ArrayList<>();
    }

    public String toLine() {
        return artifact.toGavString() + " @ " + scm;
    }

    @Override
    public String toString() {
        return toLine();
    }

    public String getOrigin() {
        return id.substring(id.indexOf(':') + 1);
    }

    public String getZone() {
        return id.substring(0, id.indexOf(':'));
    }

    public JsonObject toJson() {
        JsonObject obj;
        JsonArray array;

        obj = new JsonObject();
        obj.add("id", new JsonPrimitive(id));
        obj.add("revision", new JsonPrimitive(revision));
        if (parent != null) {
            obj.add("parent", new JsonPrimitive(parent.toGavString()));
        }
        obj.add("artifact", new JsonPrimitive(artifact.toGavString()));
        obj.add("scm", new JsonPrimitive(scm));
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
