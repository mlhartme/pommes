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
package net.oneandone.pommes.project;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Gav;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.GetLastModifiedException;
import net.oneandone.sushi.fs.Node;

public class ComposerProject extends Project {
    public static boolean matches(String name) {
        return name.equals("composer.json");
    }

    public static ComposerProject create(Environment environment, Node descriptorCurrentlyNotUsed) {
        return new ComposerProject();
    }

    @Override
    protected Pom doLoad(Environment notUsed, String zone, String origin, String revision, String scm) throws GetLastModifiedException {
        return new Pom(zone, origin, revision, null, artifact(origin), scm, null);
    }

    private static Gav artifact(String origin) {
        int last;
        int before;
        String artifact;

        last = origin.lastIndexOf('/');
        if (last == -1) {
            artifact = "unknown";
        } else {
            before = origin.lastIndexOf('/', last - 1);
            if (before == -1) {
                artifact = "unknown";
            } else {
                artifact = origin.substring(before + 1, last);
            }
        }
        return new Gav("1and1-sales.php", artifact, "0");
    }
}
