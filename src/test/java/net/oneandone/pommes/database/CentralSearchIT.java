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

import net.oneandone.maven.embedded.Maven;
import net.oneandone.sushi.fs.World;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CentralSearchIT {
    @Test
    public void json() throws IOException {
        World world;
        CentralSearch search;
        List<Document> documents;

        world = World.create();
        search = new CentralSearch(world, Maven.withSettings(world));
        documents = search.query(Arrays.asList("sushi"));
        for (var document : documents) {
            System.out.println(" " + document);
        }
    }
}