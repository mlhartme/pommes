/*
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
package net.oneandone.pommes.database;

import net.oneandone.sushi.util.Separator;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PommesQuery {
    private static final Variables EMPTY = var -> null;

    private static final Separator PLUS = Separator.on('+');

    public static PommesQuery parse(String str) throws IOException {
        return parse(Collections.singletonList(str));
    }

    public static PommesQuery parse(List<String> initialOr) throws IOException {
        return parse(initialOr, EMPTY);
    }

    public static PommesQuery parse(List<String> initialOr, Variables variables) throws IOException {
        int queryIndex;
        List<String> or;
        Or orBuilder;
        And andBuilder;
        List<String> terms;
        String repo;

        queryIndex = -1;
        or = new ArrayList<>(initialOr);
        if (or.size() > 0) {
            try {
                queryIndex = Integer.parseInt(initialOr.get(initialOr.size() - 1));
                queryIndex--;
                or.remove(initialOr.size() - 1);
            } catch (NumberFormatException e) {
                // fall-through - not and index
            }
        }
        if (or.size() > 0 && or.get(0).startsWith("/")) {
            repo = or.remove(0);
            if (repo.length() == 1) {
                repo = null;
            } else {
                repo = repo.substring(1);
            }
        } else {
            repo = "local";
        }
        orBuilder = new Or();
        for (String and : or.isEmpty() ? Collections.singletonList("") : or) {
            andBuilder = new And();
            if (repo != null) {
                andBuilder.add(new Atom(false, Arrays.asList(Field.ORIGIN), Match.PREFIX, repo + ":"));
            }
            terms = PLUS.split(variables.substitute(and));
            for (String termWithNot : terms) {
                andBuilder.add(Atom.parse(termWithNot, variables));
            }
            orBuilder.add(andBuilder);
        }
        return new PommesQuery(queryIndex, orBuilder);
    }


    //--

    public abstract static class Expr {
        public abstract Query toLucene();

        public abstract List<String> toCentral();

        // TODO: proper precendence handling
        public abstract String toString();
    }

    public static class Or extends Expr {
        private final List<Expr> expressions = new ArrayList<>();

        public void add(Expr expression) {
            expressions.add(expression);
        }

        public Query toLucene() {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            for (Expr expression : expressions) {
                builder.add(expression.toLucene(), BooleanClause.Occur.SHOULD);
            }
            return builder.build();
        }

        public List<String> toCentral() {
            List<String> result;

            result = new ArrayList<>();
            for (Expr expression : expressions) {
                result.add(one(expression.toCentral()));
            }
            return result;
        }

        public String toString() {
            StringBuilder result;

            result = new StringBuilder();
            for (Expr expression : expressions) {
                if (!result.isEmpty()) {
                    result.append(" ");
                }
                result.append(expression.toString());
            }
            return result.toString();
        }
    }


    private static String one(List<String> lst) {
        if (lst.size() != 1) {
            return lst.toString();
        }
        return lst.get(0);
    }

    public static class And extends Expr {
        private final List<Expr> expressions = new ArrayList<>();

        public void add(Expr expression) {
            // TODO: not ? BooleanClause.Occur.MUST_NOT : BooleanClause.Occur.MUST);
            expressions.add(expression);
        }

        public Query toLucene() {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();

            // make sure we have at least one entry. Because terms might be empty. Or, if it's a single "!", we need another entry to make it work.
            builder.add(any(), BooleanClause.Occur.MUST);

            for (Expr expression : expressions) {
                builder.add(expression.toLucene(), BooleanClause.Occur.MUST);
            }
            return builder.build();
        }

        private static Query any() {
            BooleanQuery.Builder result;

            result = new BooleanQuery.Builder();
            result.add(Field.ORIGIN.query(Match.SUBSTRING, ""), BooleanClause.Occur.SHOULD);
            return result.build();
        }


        public List<String> toCentral() {
            StringBuilder result;

            result = new StringBuilder();
            for (Expr expression : expressions) {
                if (!result.isEmpty()) {
                    result.append(" ");
                }
                result.append(one(expression.toCentral()));
            }
            return Arrays.asList(result.toString());
        }

        public String toString() {
            StringBuilder result;

            result = new StringBuilder();
            for (Expr expression : expressions) {
                if (!result.isEmpty()) {
                    result.append("+");
                }
                result.append(expression.toString());
            }
            return result.toString();
        }
    }

    public static class Atom extends Expr {
        public static Atom parse(String termWithNot, Variables variables) throws IOException {
            boolean not;
            String term;
            Object[] tmp;
            Match match;
            int idx;
            List<Field> fields;
            String string;

            not = termWithNot.startsWith("!");
            term = not ? termWithNot.substring(1) : termWithNot;
            if (term.startsWith("§")) {
                // CAUTION: don't merge this into + separates terms below, because lucene query may contain '+' themselves
                //termQuery = new StandardQueryParser().parse(and.substring(1), Field.ARTIFACT.dbname());
                throw new RuntimeException("TODO: support §");
            } else {
                tmp = Match.locate(term);
                if (tmp == null) {
                    match = Match.SUBSTRING;
                    idx = -1;
                } else {
                    match = (Match) tmp[0];
                    idx = (Integer) tmp[1];
                }
                // search origin, not trunk. Because scm url is regularly not adjusted
                fields = Field.forIds(idx < 1 ? "as" : term.substring(0, idx));
                string = term.substring(idx + 1); // ok for -1
                string = variables.substitute(string);
                return new Atom(not, fields, match, string);
            }
        }

        public final boolean not;
        private final List<Field> fields;
        private final Match match;
        private final String string;

        public Atom(boolean not, List<Field> fields, Match match, String string) {
            if (fields.isEmpty()) {
                throw new IllegalArgumentException();
            }
            this.not = not;
            this.fields = fields;
            this.match = match;
            this.string = string;
        }

        public Query toLucene() {
            BooleanQuery.Builder result;

            result = new BooleanQuery.Builder();
            for (Field field : fields) {
                result.add(field.query(match, string), BooleanClause.Occur.SHOULD);
            }
            return result.build();
        }

        public List<String> toCentral() {
            StringBuilder result = new StringBuilder();
            if (not) {
                result.append("-");
            }
            result.append(string);
            return Arrays.asList(result.toString());
        }

        public String toString() {
            StringBuilder result = new StringBuilder();
            if (not) {
                result.append("-");
            }
            for (Field field : fields) {
                result.append(field.dbname().charAt(0));
            }
            result.append(match.delimiter);
            result.append(string);
            return result.toString();
        }
    }

    //--

    private final int idx;
    private final Expr query;

    public PommesQuery(int idx, Or query) {
        this.idx = idx;
        this.query = query;
    }

    public int getIndex() {
        return idx;
    }

    public List<Document> find(IndexSearcher searcher) throws IOException {
        TopDocs search;
        List<Document> list;

        search = searcher.search(query.toLucene(), Integer.MAX_VALUE);
        list = new ArrayList<>();
        if (idx < 0) {
            for (ScoreDoc scoreDoc : search.scoreDocs) {
                list.add(searcher.getIndexReader().storedFields().document(scoreDoc.doc));
            }
        } else {
            if (idx < search.scoreDocs.length) {
                list.add(searcher.getIndexReader().storedFields().document(search.scoreDocs[idx].doc));
            }
        }
        return list;
    }

    public void delete(IndexWriter writer) throws IOException {
        if (idx >= 0) {
            throw new UnsupportedOperationException();
        }
        writer.deleteDocuments(query.toLucene());
    }

    public String toString() {
        String result;

        result = query.toString();
        if (idx > 0) {
            result = result + "[" + idx + "]";
        }
        return result;
    }

    public List<String> toCentral() {
        return query.toCentral();
    }
}
