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
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;

public class Mount extends Base {
    @Value(name = "pattern", position = 1)
    private String substring;

    public Mount(Console console, Maven maven) {
        super(console, maven);
    }

    @Override
    public void invoke() throws Exception {
        Fstab fstab;
        String svnurl;

        fstab = Fstab.load(console.world);
        try (Database database = Database.load(console.world)) {
            for (Pom pom : database.substring(substring)) {
                svnurl = pom.svnUrl();
                checkout(svnurl, fstab.locate(svnurl));
            }
        }
    }
    private void checkout(String svnurl, FileNode checkout) throws MkdirException, Failure {
        Launcher svn;

        checkout.getParent().mkdirsOpt();
        svn = svn(checkout.getParent(), "co", svnurl, checkout.getName());
        if (console.getVerbose()) {
            console.verbose.println(svn.toString());
        } else {
            console.info.println("[svn co " + svnurl + " " + checkout.getAbsolute() + "]");
        }
        svn.exec(console.verbose);
    }

}
