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

import net.oneandone.maven.embedded.Maven;
import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Value;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;

import java.util.ArrayList;
import java.util.List;

public class FstabCreate extends Base {
    @Value(name = "root", position = 1)
    private FileNode root;

    public FstabCreate(Console console, Maven maven) {
        super(console, maven);
    }

    @Override
    public void invoke() throws Exception {
        /*
        Fstab fstab;
        String prefix;
        FileNode directory;
        List<String> prefixes;

        fstab = new Fstab();
        root.checkDirectory();
        try (Database database = Database.load(console.world).updateOpt()) {
            for (Pom pom : database.substring("")) {
                if (pom.scm != null) {
                    addPrefix(pom.svnUrl(), prefixes);
                }
            }
        }*/
    }

    /*
    private static String addPrefix(String svnurl) {
    }
*/
    private static String name(String prefix) {
        int idx;

        prefix = Strings.removeRightOpt(prefix, "/");
        idx = prefix.lastIndexOf('/');
        if (idx == -1) {
            throw new IllegalStateException();
        }
        return prefix.substring(idx + 1);
    }
}
