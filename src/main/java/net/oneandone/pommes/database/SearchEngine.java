package net.oneandone.pommes.database;

import org.apache.lucene.document.Document;

import java.io.IOException;
import java.util.List;

public class SearchEngine {
    private final Database database;

    public SearchEngine(Database database) {
        this.database = database;
    }

    public Database getDatabase() {
        return database;
    }

    public List<Document> query(PommesQuery pq) throws IOException {
        return database.query(pq);
    }
}
