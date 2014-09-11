/**
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
package net.oneandone.pommes.cli;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FstabTest {
    @Test
    public void fold() {
        assertEquals("/", Fstab.fold(("/")));
        assertEquals("a/b/c/", Fstab.fold(("a/b/c/")));
        assertEquals("a/b/", Fstab.fold(("a/b/trunk/")));

        assertEquals("a/puc/", Fstab.fold(("a/puc/trunk/")));
        assertEquals("a/pucwar/", Fstab.fold(("a/puc/branches/pucwar/")));
        assertEquals("a/puc-4/", Fstab.fold(("a/puc/branches/puc-4/")));
        assertEquals("a/puc-VTR.42/", Fstab.fold(("a/puc/branches/VTR.42/")));
    }
}
