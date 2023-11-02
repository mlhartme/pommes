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
package net.oneandone.pommes.scm;

import net.oneandone.pommes.database.Gav;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubversionTest {
    @Test
    public void path() throws URISyntaxException {
        Subversion svn;

        svn = new Subversion();
        assertEquals("svn.1and1.org/controlpanel/tools/maven/plugins/check-characters", svn.directory("svn:https://svn.1and1.org/svn/controlpanel/tools/maven/plugins/check-characters/trunk"));
        assertEquals("svn.1and1.org/controlpanel/tools/maven/plugins/check-characters", svn.directory("svn:https://svn.1and1.org/svn/controlpanel/tools/maven/plugins/check-characters/trunk/"));
        assertEquals("svn.1and1.org/controlpanel/tools/maven", svn.directory("svn:https://svn.1and1.org/controlpanel/tools/maven/branches/demo"));
    }

    @Test
    public void dflt() throws URISyntaxException {
        Subversion svn;

        svn = new Subversion();
        assertEquals(new Gav("", "puppet_ciso", "1-SNAPSHOT"), svn.defaultGav("svn:https://svn.1and1.org/svn/puppet_ciso/"));
    }
}
