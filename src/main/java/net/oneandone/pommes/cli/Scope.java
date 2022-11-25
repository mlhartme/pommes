package net.oneandone.pommes.cli;

import net.oneandone.pommes.database.Database;
import net.oneandone.pommes.database.Field;
import net.oneandone.pommes.database.PommesQuery;
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.database.Variables;
import net.oneandone.pommes.search.CentralSearch;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;

import java.io.IOException;
import java.util.List;

public class Scope {
    private final Database database;
    private final CentralSearch centralSearch;
    private final Variables variables;

    public Scope(Database database, Variables variables, CentralSearch centralSearch) {
        this.database = database;
        this.variables = variables;
        this.centralSearch = centralSearch;
    }

    public Database getDatabase() {
        return database;
    }

    public List<Project> query(List<String> query) throws IOException, QueryNodeException {
        List<Project> result;

        result = databaseQuery(PommesQuery.create(query, variables));
        result.addAll(centralSearch.query(query));
        return result;
    }

    private List<Project> databaseQuery(PommesQuery pq) throws IOException {
        return Field.projects(database.query(pq));
    }
}
