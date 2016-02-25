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
import net.oneandone.pommes.mount.Fstab;
import net.oneandone.pommes.mount.Point;
import net.oneandone.sushi.fs.file.FileNode;

import java.util.ArrayList;

public class FstabAdd extends Base {
    private String url;
    private FileNode directory;

    public FstabAdd(Globals globals, String url, FileNode directory) {
        super(globals);
        this.url = url;
        this.directory = directory;
    }

    @Override
    public void run(Database database) throws Exception {
        Fstab fstab;

        url = Database.withSlash(url);
        fstab = Fstab.load(world);
        directory.getParent().checkDirectory();
        directory.mkdirOpt();
        fstab.add(new Point(url, directory, new ArrayList<>()));
        fstab.save(Fstab.file(world));
    }
}
