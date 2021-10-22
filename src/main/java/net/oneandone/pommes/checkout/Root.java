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

import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.net.URISyntaxException;

/** Root directory for mounting; typically ~/Projects */
public class Root {
    public final FileNode directory;

    public Root(FileNode directory) {
        this.directory = directory;
    }

    public FileNode directory(Project project) throws IOException {
        Scm scm;
        String path;

        if (project.scm == null) {
            throw new IOException(project + ": missing scm");
        }
        scm = Scm.probeUrl(project.scm);
        if (scm == null) {
            throw new IOException(project + ": unknown scm: " + project.scm);
        }
        try {
            path = scm.path(project.scm);
        } catch (URISyntaxException e) {
            throw new IOException(project.scm + ": invalid scm uri", e);
        }
        return directory.join(path);
    }
}
