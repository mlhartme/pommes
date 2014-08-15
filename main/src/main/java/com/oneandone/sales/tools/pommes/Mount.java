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
package com.oneandone.sales.tools.pommes;

import com.oneandone.sales.tools.maven.Maven;
import com.oneandone.sales.tools.pommes.lucene.Database;
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Mount extends Base {
    private String baseUrl;

    private String baseDirectoryStr;

    @Remaining
    public void remaining(String str) {
        if (baseUrl == null) {
            baseUrl = str;
        } else if (baseDirectoryStr == null) {
            baseDirectoryStr = str;
        } else {
            throw new ArgumentException("too many arguments");
        }
    }

    public Mount(Console console, Maven maven) {
        super(console, maven);
    }

    @Override
    public void invoke() throws Exception {
        if (baseUrl == null) {
            list();
        } else {
            baseUrl = Database.withSlash(baseUrl);
            add(baseDirectoryStr == null ? (FileNode) console.world.getWorking() : console.world.file(baseDirectoryStr));
        }
    }

    private void list() throws IOException {
        FileMap mounts;

        mounts = FileMap.loadMounts(console.world);
        for (FileNode mount : mounts.directories()) {
            console.info.println(mount.getAbsolute() + " -> " + mounts.lookupUrl(mount));
        }
    }

    private void add(FileNode baseDirectory) throws IOException {
        FileMap mounts;
        FileMap checkouts;
        String existing;
        Map<FileNode, String> mountAdds;
        Map<FileNode, String> checkoutAdds;
        Map<FileNode, String> checkoutRemoves;

        mounts = FileMap.loadMounts(console.world);
        existing = mounts.lookupUrl(baseDirectory);
        if (existing != null) {
            throw new ArgumentException(baseDirectory + " already mount point for " + existing);
        }
        mountAdds = new LinkedHashMap<>();
        mountAdds.put(baseDirectory, baseUrl);
        checkouts = FileMap.loadCheckouts(console.world);
        checkoutAdds = new LinkedHashMap<>();
        checkoutRemoves = new LinkedHashMap<>();
        scanUpdate(checkouts, baseUrl, baseDirectory, checkoutAdds, checkoutRemoves);
        if (!checkoutRemoves.isEmpty()) {
            throw new IllegalStateException();
        }
        performUpdate(mounts, mountAdds, Collections.<FileNode, String>emptyMap(), checkouts, checkoutAdds, checkoutRemoves);
    }
}
