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
import net.oneandone.pommes.checkout.Action;
import net.oneandone.pommes.checkout.Problem;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.file.FileNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Remove extends Base {
    private final FileNode directory;

    public Remove(Environment environment, FileNode directory) {
        super(environment);
        this.directory = directory;
    }

    @Override
    public void run(SearchEngine search) throws Exception {
        Map<FileNode, Scm> checkouts;
        List<Action> removes;
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
            if (scm.isAlive(checkout)) {
                removes.add(net.oneandone.pommes.checkout.Remove.create(checkout));
            } else {
                removes.add(new Problem(checkout, checkout + ": checkout is not alive"));
            }
        }
        runAll(removes);
    }
}
