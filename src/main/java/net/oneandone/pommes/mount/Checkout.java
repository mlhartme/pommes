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
package net.oneandone.pommes.mount;

import net.oneandone.pommes.cli.Base;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;

import java.io.IOException;

public class Checkout extends Action {
    public static net.oneandone.pommes.mount.Checkout createOpt(FileNode directory, String svnurl) throws IOException {
        String scannedUrl;

        if (directory.exists()) {
            scannedUrl = Base.scanUrlOpt(directory);
            if (svnurl.equals(scannedUrl)) {
                return null;
            } else {
                throw new StatusException("C " + directory + " (" + svnurl + " vs " + scannedUrl + ")");
            }
        } else {
            return new net.oneandone.pommes.mount.Checkout(directory, svnurl);
        }
    }

    public Checkout(FileNode directory, String svnurl) {
        super(directory, svnurl);
    }

    public char status() {
        return 'A';
    }

    public void run(Console console) throws MkdirException, Failure {
        Launcher svn;

        directory.getParent().mkdirsOpt();
        svn = Base.svn(directory.getParent(), "co", svnurl, directory.getName());
        if (console.getVerbose()) {
            console.verbose.println(svn.toString());
        } else {
            console.info.println("svn co " + svnurl + " " + directory.getAbsolute());
        }
        if (console.getVerbose()) {
            svn.exec(console.verbose);
        } else {
            // exec into string (and ignore it) - otherwise, Failure Exceptions cannot contains the output
            svn.exec();
        }
    }
}
