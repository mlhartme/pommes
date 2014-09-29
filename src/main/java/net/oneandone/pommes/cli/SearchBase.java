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
import net.oneandone.pommes.mount.Fstab;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lists all indexed POMs in index *without* updating from the server. Warning: this will be a long list, only useful with grep.
 */
public abstract class SearchBase<T> extends Base {
    public SearchBase(Console console, Environment environment) {
        super(console, environment);
    }

    @Option("checkout")
    private boolean checkout;

    public void invoke(Database database) throws Exception {
        List<T> matches;
        List<FileNode> directories;

        matches = search(database);
        if (checkout) {
            directories = extractLocal(matches);
            for (FileNode directory : directories) {
                console.info.println(directory);
            }
            if (!matches.isEmpty()) {
                for (T match : matches) {
                    console.info.println(toLine(match));
                }
                throw new IOException("aborted with " + matches.size() + " remote matches");
            }
        } else {
            for (T match : matches) {
                console.info.println(toLine(match));
            }
        }
    }

    public abstract List<T> search(Database database) throws Exception;
    public abstract Pom toPom(T t);
    public abstract String toLine(T t) throws IOException;

    private List<FileNode> extractLocal(List<T> matches) throws IOException {
        List<FileNode> result;
        Pom pom;
        Fstab fstab;
        Iterator<T> iter;
        String url;
        boolean found;

        result = new ArrayList<>();
        fstab = Fstab.load(console.world);
        iter = matches.iterator();
        while (iter.hasNext()) {
            pom = toPom(iter.next());
            url = pom.projectUrl();
            found = false;
            for (FileNode directory : fstab.directories(url)) {
                if (directory.exists()) {
                    result.add(directory);
                    found = true;
                }
            }
            if (found) {
                iter.remove();
            }
        }
        return result;
    }
}
