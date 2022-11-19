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
package net.oneandone.pommes.cli;

import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.checkout.Action;
import net.oneandone.pommes.checkout.Root;
import net.oneandone.pommes.checkout.Problem;
import net.oneandone.pommes.database.SearchEngine;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.file.FileNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Remove extends Base {
    private final boolean stale;
    private final FileNode directory;

    public Remove(Environment environment, boolean stale, FileNode directory) {
        super(environment);
        this.stale = stale;
        this.directory = directory;
    }

    @Override
    public void run(SearchEngine search) throws Exception {
        Map<FileNode, Scm> checkouts;
        Project scannedPom;
        List<Action> removes;
        Root root;
        FileNode configuredDirectory;
        FileNode checkout;
        Scm scm;

        checkouts = Scm.scanCheckouts(directory, environment.excludes());
        if (checkouts.isEmpty()) {
            throw new ArgumentException("no checkouts under " + directory);
        }
        removes = new ArrayList<>();
        for (Map.Entry<FileNode, Scm> entry : checkouts.entrySet()) {
            checkout = entry.getKey();
            scm = entry.getValue();
            if (stale) {
                if (scm.isAlive(checkout)) {
                    continue;
                }
            }
            scannedPom = environment.scanPomOpt(checkout);
            if (scannedPom == null) {
                removes.add(new Problem(checkout, checkout + ": unknown project"));
            } else {
                root = environment.home.root();
                configuredDirectory = root.directory(scannedPom);
                if (checkout.equals(configuredDirectory)) {
                    removes.add(net.oneandone.pommes.checkout.Remove.create(checkout));
                } else {
                    removes.add(new Problem(checkout, checkout + ": checkout expected at " + configuredDirectory));
                }
            }
        }
        runAll(removes);
    }
}
