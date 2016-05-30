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
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.DeleteException;
import net.oneandone.sushi.fs.NodeNotFoundException;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;

public class Remove extends Action {
    public static Remove create(FileNode directory, Pom pom) throws IOException {
        Scm scm;

        scm = Scm.probeCheckout(directory);
        if (!scm.isCommitted(directory)) {
            throw new StatusException("M " + directory + " (" + pom + ")");
        }
        return new Remove(scm, directory, pom.scm);
    }

    public Remove(Scm scm, FileNode directory, String url) {
        super(scm, directory, url);
    }

    public char status() {
        return 'D';
    }

    public void run(Console console) throws NodeNotFoundException, DeleteException {
        console.info.println("rm -rf " + directory);
        directory.deleteTree();
    }
}
