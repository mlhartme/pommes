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
import net.oneandone.pommes.search.CentralSearch;
import net.oneandone.sushi.fs.World;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CentralSearchIT {
    @Test
    public void test() throws IOException {
        check("de.schmizzolin:yogi:1.4.0", "yogi", "yogi");
        check("net.sf.beezle.sushi:sushi:2.7.0", "sushi AND beezle", "sushi+beezle");
        check("de.schmizzolin:yogi:1.4.0", "yogi AND -beezle", "yogi+!beezle");
    }

    public void check(String expectedGav, String expectedParsed, String... queryStrings) throws IOException {
        World world;
        PommesQuery query;
        CentralSearch search;
        List<Project> projects;
        Project p;

        world = World.create();
        search = new CentralSearch(world, Maven.withSettings(world));
        query = PommesQuery.parse(Arrays.asList(queryStrings));
        assertEquals(expectedParsed, query.toCentral());
        projects = search.query(query);
        assertEquals(1, projects.size());
        p = projects.get(0);
        assertEquals(Gav.forGav(expectedGav), p.artifact);
    }
}
