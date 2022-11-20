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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProjectTest {
    @Test
    public void json() {
        JsonObject obj;
        Project pom;

        obj = JsonParser.parseString(
                "  {\n" +
                "    \"repository\": \"r\",\n" +
                "    \"path\": \"p\",\n" +
                "    \"revision\": \"32\",\n" +
                "    \"parent\": \"net.oneandone.maven.poms:lazy-foss-parent:1.0.1\",\n" +
                "    \"artifact\": \"net.oneandone:pommes:3.0.2-SNAPSHOT\",\n" +
                "    \"scm\": \"git:ssh://git@github.com/mlhartme/pommes.git\",\n" +
                "    \"url\": \"https://github.com/mlhartme/pommes\",\n" +
                "    \"dependencies\": [\n" +
                "        \"net.oneandone:sushi:3.1.1\"\n" +
                "    ]\n" +
                "  }").getAsJsonObject();
        pom = Project.fromJson(obj);
        assertEquals("p", pom.path);
        assertEquals("32", pom.revision);
        assertEquals(obj, pom.toJson());
    }
}
