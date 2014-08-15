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
package net.oneandone.sales.tools.pommes;

import net.oneandone.pommes.maven.Maven;
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.fs.file.FileNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Umount extends Base {
    private String baseDirectoryString;

    @Remaining
    public void remaining(String str) {
        if (baseDirectoryString == null) {
            baseDirectoryString = str;
        } else {
            throw new ArgumentException("too many arguments");
        }
    }

    public Umount(Console console, Maven maven) {
        super(console, maven);
    }

    @Override
    public void invoke() throws Exception {
        FileNode baseDirectory;
        FileMap mounts;
        FileMap checkouts;
        Map<FileNode, String> mountRemoves;
        Map<FileNode, String> checkoutRemoves;
        String url;

        baseDirectory = baseDirectoryString == null ? (FileNode) console.world.getWorking() : console.world.file(baseDirectoryString);
        mounts = FileMap.loadMounts(console.world);
        checkouts = FileMap.loadCheckouts(console.world);
        mountRemoves = new LinkedHashMap<>();
        checkoutRemoves = new LinkedHashMap<>();
        for (FileNode mount : mounts.under(baseDirectory)) {
            url = mounts.lookupUrl(mount);
            mountRemoves.put(mount, url);
            for (FileNode checkout : checkouts.under(mount)) {
                checkoutRemoves.put(checkout, checkouts.lookupUrl(checkout));
            }
        }
        if (mountRemoves.size() == 0) {
            throw new ArgumentException("nothing to unmount under " + baseDirectory);
        } else {
            performUpdate(mounts, Collections.<FileNode, String>emptyMap(), mountRemoves,
                    checkouts, Collections.<FileNode, String>emptyMap(), checkoutRemoves);
        }
    }
}
