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

import net.oneandone.pommes.lucene.Searcher;
import net.oneandone.pommes.maven.Maven;
import net.oneandone.pommes.lucene.GroupArtifactVersion;
import net.oneandone.pommes.lucene.Database;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Value;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;
import org.apache.lucene.document.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists all indexed POMs in index *without* updating from the server. Warning: this will be a long list, only useful with grep.
 */
public class Find extends SearchBase<Document> {
    private final FileMap checkouts;

    public Find(Console console, Maven maven) throws IOException {
        super(console, maven);
        checkouts = FileMap.loadCheckouts(console.world);
    }

    @Value(name = "substring", position = 1)
    private String substring;

    public List<Document> search() throws IOException {
        List<Document> result;
        Database database;
        String line;

        result = new ArrayList<>();
        database = updatedDatabase();
        for (Document document : new Searcher(database).query(substring)) {
            line = toLine(document);
            if (line.contains(substring)) {
                result.add(document);
            }
        }
        return result;
    }

    @Override
    public Document toDocument(Document document) {
        return document;
    }

    @Override
    public String toLine(Document document) {
        GroupArtifactVersion artifact;
        StringBuilder result;
        String url;
        List<FileNode> directories;

        artifact = new GroupArtifactVersion(document.get(Database.GAV));
        result = new StringBuilder(artifact.toGavString()).append(" @ ").append(document.get(Database.SCM));
        url = document.get(Database.SCM);
        if (url.startsWith(Database.SCM_SVN)) {
            url = Database.withSlash(Strings.removeLeft(url, Database.SCM_SVN));
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
