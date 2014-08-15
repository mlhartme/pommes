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
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;
import org.apache.lucene.document.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lists all indexed POMs in index *without* updating from the server. Warning: this will be a long list, only useful with grep.
 */
public abstract class SearchBase<T> extends Base {
    public SearchBase(Console console, Maven maven) {
        super(console, maven);
    }

    @Option("local")
    private boolean local;

    public void invoke() throws Exception {
        List<T> matches;
        List<FileNode> directories;

        matches = search();
        if (local) {
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

    public abstract List<T> search() throws Exception;
    public abstract Document toDocument(T t);
    public abstract String toLine(T t);

    private List<FileNode> extractLocal(List<T> matches) throws IOException {
        List<FileNode> result;
        Document document;
        FileMap checkouts;
        Iterator<T> iter;
        String url;
        List<FileNode> directories;

        result = new ArrayList<>();
        checkouts = FileMap.loadCheckouts(console.world);
        iter = matches.iterator();
        while (iter.hasNext()) {
            document = toDocument(iter.next());
            url = document.get(Database.SCM);
            url = Database.withSlash(Strings.removeLeft(url, Database.SCM_SVN));
            directories = checkouts.lookupDirectories(url);
            if (!directories.isEmpty()) {
                result.addAll(directories);
                iter.remove();
            }
        }
        return result;
    }
}
