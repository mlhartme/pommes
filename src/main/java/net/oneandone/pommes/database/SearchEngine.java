package net.oneandone.pommes.database;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;

import java.io.IOException;
import java.util.List;

public class SearchEngine {
    private final Database database;
    private final Variables variables;

    public SearchEngine(Database database, Variables variables) {
        this.database = database;
        this.variables = variables;
    }

    public Database getDatabase() {
        return database;
    }

    public List<Document> query(List<String> query) throws IOException, QueryNodeException {
        return query(PommesQuery.create(query, variables));
    }

    private List<Document> query(PommesQuery pq) throws IOException {
        return database.query(pq);
    }
}
