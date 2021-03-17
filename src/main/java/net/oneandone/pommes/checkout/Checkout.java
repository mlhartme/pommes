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
import net.oneandone.pommes.database.Pom;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;

public class Checkout extends Action {
    public static Action createOpt(FileNode directory, Pom pom) throws IOException {
        Scm scm;
        String scannedUrl;

        if (directory.exists()) {
            scm = Scm.probeCheckout(directory);
            if (scm == null) {
                return new Problem(directory, directory + ": cannot detect checkout");
            }
            scannedUrl = scm.getUrl(directory);
            if (normalize(scannedUrl).equals(normalize(pom.scm))) {
                return null;
            } else {
                return new Problem(directory, directory + ": checkout conflict: " + pom.scm + " vs " + scannedUrl);
            }
        } else {
            if (pom.scm == null) {
                return new Problem(directory, pom.id + ": missing scm: " + pom.scm);
            }
            scm = Scm.probeUrl(pom.scm);
            if (scm == null) {
                return new Problem(directory, pom.id + ": unknown scm: " + pom.scm);
            } else {
                return new Checkout(scm, directory, pom.scm);
            }
        }
    }

    private static String normalize(String url) {
        return Strings.removeRightOpt(url, "/");
    }

    private final Scm scm;
    private final String url;

    public Checkout(Scm scm, FileNode directory, String url) {
        super(directory, "A " + directory + " (" + url + ")");
        this.scm = scm;
        this.url = url;
    }

    public void run(Console console) throws MkdirException, Failure {
        Launcher launcher;

        directory.getParent().mkdirsOpt();
        launcher = scm.checkout(directory, url);
        console.info.println(launcher.toString());
        if (console.getVerbose()) {
            launcher.exec(console.verbose);
        } else {
            // exec into string (and ignore it) - otherwise, Failure Exceptions cannot contains the output
            launcher.exec();
        }
    }

    public boolean equals(Object obj) {
        Checkout c;

        if (obj instanceof Checkout) {
            c = (Checkout) obj;
            return scm.equals(c.scm) && url.equals(c.url);
        }
        return false;
    }

    public int hashCode() {
        return url.hashCode();
    }
}
