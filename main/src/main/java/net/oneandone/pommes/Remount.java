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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Remount extends Base {
    private String dirString;

    @Remaining
    public void remaining(String str) {
        if (dirString == null) {
            dirString = str;
        } else {
            throw new ArgumentException("too many arguments");
        }
    }

    public Remount(Console console, Maven maven) {
        super(console, maven);
    }

    @Override
    public void invoke() throws Exception {
        FileNode root;
        FileMap mounts;
        FileMap checkouts;
        Map<FileNode, String> adds;
        Map<FileNode, String> removes;

        root = dirString == null ? (FileNode) console.world.getWorking() : console.world.file(dirString);
        mounts = FileMap.loadMounts(console.world);
        checkouts = FileMap.loadCheckouts(console.world);
        adds = new LinkedHashMap<>();
        removes = new LinkedHashMap<>();
        for (FileNode mount : mounts.under(root)) {
            scanUpdate(checkouts, mounts.lookupUrl(mount), mount, adds, removes);
        }
        performUpdate(mounts, Collections.<FileNode, String>emptyMap(), Collections.<FileNode, String>emptyMap(),
                checkouts, adds, removes);
    }
}
