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

import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;

import java.io.IOException;

public class Checkout extends Action {
    public static Checkout createOpt(Environment environment, FileNode directory, Pom pom) throws IOException {
        Pom scannedPom;
        Scm scm;

        if (directory.exists()) {
            scannedPom = environment.scanPomOpt(directory);
            if (scannedPom.equals(pom)) {
                return null;
            } else {
                throw new StatusException("C " + directory + " (" + pom + " vs " + scannedPom + ")");
            }
        } else {
            scm = Scm.probeUrl(pom.scm);
            return new Checkout(scm, directory, pom.scm);
        }
    }

    public Checkout(Scm scm, FileNode directory, String url) {
        super(scm, directory, url);
    }

    public char status() {
        return 'A';
    }

    public void run(Console console) throws MkdirException, Failure {
        directory.getParent().mkdirsOpt();
        scm.checkout(directory, url, console);
    }
}
