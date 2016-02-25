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

import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.mount.Action;
import net.oneandone.pommes.mount.Checkout;
import net.oneandone.pommes.mount.StatusException;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Mount extends Base {
    private final String query;

    public Mount(Globals globals, String query) {
        super(globals);
        this.query = query;
    }

    @Override
    public void run(Database database) throws Exception {
        String svnurl;
        List<Action> adds;
        Action action;
        int problems;
        List<FileNode> directories;

        adds = new ArrayList<>();
        problems = 0;
        for (Pom pom : database.query(query, environment)) {
            svnurl = pom.projectUrl();
            directories = environment.fstab().directories(svnurl);
            if (directories.isEmpty()) {
                console.error.println("no mount point for " + svnurl);
                problems++;
            } else for (FileNode directory : directories) {
                try {
                    action = Checkout.createOpt(directory, svnurl);
                    if (action != null) {
                        adds.add(action);
                    } else {
                        console.verbose.println("already mounted: " + directory);
                    }
                } catch (StatusException e) {
                    console.error.println(e.getMessage());
                    problems++;
                }
            }
        }
        if (problems > 0) {
            throw new IOException("aborted - fix the above problem(s) first: " + problems);
        }
        runAll(adds);
    }
}
