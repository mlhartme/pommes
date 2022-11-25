package net.oneandone.pommes.database;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;

import java.io.IOException;
import java.util.List;

public class SearchEngine {
    private final Database database;
    private final CentralSearch centralSearch;
    private final Variables variables;

    public SearchEngine(Database database, Variables variables, CentralSearch centralSearch) {
        this.database = database;
        this.variables = variables;
        this.centralSearch = centralSearch;
    }

    public Database getDatabase() {
        return database;
    }

    public List<Document> query(List<String> query) throws IOException, QueryNodeException {
        List<Document> result;

        result = databaseQuery(PommesQuery.create(query, variables));
        result.addAll(centralSearch.query(query));
        return result;
    }

    private List<Document> databaseQuery(PommesQuery pq) throws IOException {
        return database.query(pq);
    }
}
