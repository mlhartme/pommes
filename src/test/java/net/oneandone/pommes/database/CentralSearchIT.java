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

import net.oneandone.maven.summon.api.Maven;
import net.oneandone.pommes.search.CentralSearch;
import net.oneandone.sushi.fs.World;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CentralSearchIT {
    private static final CentralSearch SEARCH;
    static {
        try {
            World world = World.create();
            SEARCH = new CentralSearch(world, Maven.create());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test() throws IOException {
        check("de.schmizzolin:yogi:1.4.0", "yogi", "/", "yogi");
        // TODO check("net.sf.beezle.sushi:sushi:2.7.0", "sushi AND beezle", "sushi+beezle");
        // TODO check("de.schmizzolin:yogi:1.4.0", "yogi AND -beezle", "yogi+!beezle");
    }

    public void check(String expectedGav, String expectedParsed, String... queryStrings) throws IOException {
        PommesQuery query;
        List<Project> projects;
        Project p;

        query = PommesQuery.parse(Arrays.asList(queryStrings));
        assertEquals(Arrays.asList(expectedParsed), query.toCentral());
        projects = SEARCH.query(query);
        assertEquals(1, projects.size());
        p = projects.get(0);
        assertEquals(Gav.forGav(expectedGav), p.artifact);
    }

    @Test
    public void testRaw() throws IOException {
        checkRaw("+sushi -metridoc -beezle", "net.oneandone:sushi:3.3.0");
        checkRaw("+metri"); // does not match substring
        checkRaw("metri"); // does not match substring
        checkRaw("beezle parent", "net.sf.beezle.maven.poms:parent:1.0.10"); // without +
        // checkRaw("text:beezle" -> 5+ results); // word-match only for text field, not for id
        checkRaw("g:beezle"); // word-match only for text field, not for group
        checkRaw("id:beezle"); // word-match only for text field, not for id
        checkRaw("+org +apache +maven +plugins +eclipse",
                "org.apache.maven.plugins:maven-eclipse-plugin:2.10",
                "org.apache.tuscany.maven.plugins:maven-eclipse-compiler:1.0.2");
        checkRaw("org apache maven plugins eclipse",
                "org.apache.maven.plugins:maven-eclipse-plugin:2.10",
                "org.apache.tuscany.maven.plugins:maven-eclipse-compiler:1.0.2");
    }

    private void checkRaw(String queryRaw, String... expectedGavs) throws IOException {
        List<Project> projects;

        projects = SEARCH.query(queryRaw);
        assertEquals(Arrays.asList(expectedGavs).stream().map(Gav::forGav).toList(),
                projects.stream().map((p) -> p.artifact).toList());
    }
}
