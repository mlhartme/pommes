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

import net.oneandone.maven.embedded.Maven;
import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Value;
import net.oneandone.sushi.fs.file.FileNode;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;

import java.io.IOException;
import java.util.List;

/**
 * Lists all indexed POMs in index *without* updating from the server. Warning: this will be a long list, only useful with grep.
 */
public class Find extends SearchBase<Pom> {
    private final FileMap checkouts;

    private final boolean query;

    public Find(boolean query, Console console, Maven maven) throws IOException {
        super(console, maven);
        this.query = query;
        this.checkouts = FileMap.loadCheckouts(console.world);
    }

    @Value(name = "substring", position = 1)
    private String substring;

    public List<Pom> search(Database database) throws IOException, QueryNodeException {
        return query ? database.query(substring) : database.substring(substring);
    }

    @Override
    public Pom toPom(Pom pom) {
        return pom;
    }

    @Override
    public String toLine(Pom pom) {
        StringBuilder result;
        String url;
        List<FileNode> directories;

        result = new StringBuilder(pom.toLine());
        url = pom.svnUrl();
        if (url != null) {
            directories = checkouts.lookupDirectories(url);
            for (FileNode directory : directories) {
                result.append(' ').append(directory.getAbsolute());
            }
        } else {
            // TODO: git support
        }
        return result.toString();
    }
}
