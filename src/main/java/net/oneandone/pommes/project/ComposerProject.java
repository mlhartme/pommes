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
import net.oneandone.sushi.util.Strings;

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
        String path;
        int last;
        int before;
        String group;
        String artifact;

        path = Strings.removeRightOpt(origin, "/composer.json");
        last = path.lastIndexOf('/');
        if (last == -1) {
            group = "";
            artifact = path;
        } else {
            before = path.lastIndexOf('/', last - 1);
            if (before == -1) {
                group = "";
                artifact = "unknown";
            } else {
                group = path.substring(before + 1, last);
                artifact = path.substring(last + 1);
            }
        }
        return new Gav(group, artifact, "0");
    }
}
