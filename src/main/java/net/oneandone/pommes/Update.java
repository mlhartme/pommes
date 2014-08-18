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
package net.oneandone.pommes;

import net.oneandone.pommes.maven.Maven;
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Launcher;

import java.util.List;

public class Update extends Base {
    private String baseDirectoryString;

    @Remaining
    public void remaining(String str) {
        if (baseDirectoryString == null) {
            baseDirectoryString = str;
        } else {
            throw new ArgumentException("too many arguments");
        }
    }

    public Update(Console console, Maven maven) {
        super(console, maven);
    }

    @Override
    public void invoke() throws Exception {
        FileNode baseDirectory;
        FileMap checkouts;
        List<FileNode> directories;
        Launcher svn;

        baseDirectory = baseDirectoryString == null ? (FileNode) console.world.getWorking() : console.world.file(baseDirectoryString);
        checkouts = FileMap.loadCheckouts(console.world);
        directories = checkouts.under(baseDirectory);
        for (FileNode directory : directories) {
            svn = svn(directory, "up");
            if (console.getVerbose()) {
                console.verbose.println(svn.toString());
            } else {
                console.info.println("[svn up " + directory + "]");
            }
            svn.exec(console.info);
        }
    }
}
