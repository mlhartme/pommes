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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Json {
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
}
