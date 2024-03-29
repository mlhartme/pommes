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
package net.oneandone.pommes.checkout;

import net.oneandone.inline.Console;
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.pommes.scm.ScmUrl;
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;

import java.io.IOException;

public class Checkout extends Action {
    public static Action createOpt(FileNode directory, Project project) throws IOException {
        Scm scm;
        ScmUrl url;
        ScmUrl scannedScm;

        if (directory.exists()) {
            scm = Scm.probeCheckout(directory);
            if (scm == null) {
                return new Problem(directory, directory + ": cannot detect checkout");
            }
            scannedScm = scm.getUrl(directory);

            if (scannedScm.same(project.scm)) {
                return null;
            } else {
                return new Problem(directory, directory + ": checkout conflict: " + project.scm + " vs " + scannedScm);
            }
        } else {
            if (project.scm == null) {
                return new Problem(directory, project.origin() + ": missing scm: " + project.scm);
            }
            return new Checkout(project.scm, directory);
        }
    }

    private final ScmUrl scm;

    public Checkout(ScmUrl scm, FileNode directory) {
        super(directory, "A " + directory + " (" + scm.scmUrl() + ")");
        this.scm = scm;
    }

    public void run(Console console) throws MkdirException, Failure {
        Launcher launcher;

        directory.getParent().mkdirsOpt();
        launcher = scm.scm().checkout(scm, directory);
        console.info.println(launcher.toString());
        if (console.getVerbose()) {
            launcher.exec(console.verbose);
        } else {
            // exec into string (and ignore it) - otherwise, Failure Exceptions cannot contains the output
            launcher.exec();
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof Checkout c) {
            return scm.equals(c.scm);
        }
        return false;
    }

    public int hashCode() {
        return scm.hashCode();
    }
}
