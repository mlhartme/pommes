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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParserTest {
    @Test
    public void test() throws IOException {
        check("as:someString", "/", "someString");
        check("-a:someString", "/", "!a:someString");
        check("as:a as:b", "/", "a", "b");
        check("o=local:+-a:someString", "!a:someString");
        check("o=local:+as:x+as:y", "x+y");
        check("o=local:+as:x o=local:+as:y", "x", "y");
    }

    private void check(String expected, String... expr) throws IOException {
        assertEquals(expected, PommesQuery.parse(Arrays.asList(expr), "local").toString());
    }
}
