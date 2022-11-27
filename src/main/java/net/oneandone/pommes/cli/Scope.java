package net.oneandone.pommes.cli;

import net.oneandone.pommes.database.Database;
import net.oneandone.pommes.database.Field;
import net.oneandone.pommes.database.PommesQuery;
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.database.Variables;
import net.oneandone.pommes.search.CentralSearch;

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

    public List<Project> queryDatabase(List<String> queryStrings) throws IOException {
        PommesQuery query;

        query = PommesQuery.parse(queryStrings, variables);
        return Field.projects(database.query(query));
    }

    public List<Project> queryCentral(List<String> queryStrings) throws IOException {
        PommesQuery query;

        query = PommesQuery.parse(queryStrings, variables);
        return centralSearch.query(query);
    }
}
