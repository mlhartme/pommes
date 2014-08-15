package com.oneandone.sales.tools.pommes;

import com.oneandone.sales.tools.maven.Maven;
import com.oneandone.sales.tools.pommes.lucene.GroupArtifactVersion;
import com.oneandone.sales.tools.pommes.lucene.Database;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Value;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;

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
        Document document;
        Database database;
        String line;

        result = new ArrayList<>();
        database = updatedDatabase();
        IndexReader reader = DirectoryReader.open(database.getIndexLuceneDirectory());
        int numDocs = reader.numDocs();
        for (int i = 0; i < numDocs; i++) {
            document = reader.document(i);
            line = toLine(document);
            if (line.contains(substring)) {
                result.add(document);
            }
        }
        reader.close();
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
