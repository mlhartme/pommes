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
package net.oneandone.pommes.model;

import net.oneandone.sushi.util.Separator;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PommesQuery {
    private static final Variables EMPTY = new Variables() {
        @Override
        public String lookup(String var) throws IOException {
            return null;
        }
    };

    private static final Separator PLUS = Separator.on('+');

    public static PommesQuery create(String str) throws IOException, QueryNodeException {
        return create(Collections.singletonList(str), EMPTY);
    }

    public static PommesQuery create(List<String> initialOr, Variables variables) throws IOException, QueryNodeException {
        int queryIndex;
        List<String> or;
        BooleanQuery.Builder orBuilder;
        BooleanQuery.Builder andBuilder;
        Query termQuery;
        List<String> terms;
        Match match;
        int idx;
        String string;
        List<Field> fields;
        Object[] tmp;
        boolean not;
        String term;

        queryIndex = -1;
        or = initialOr;
        if (initialOr.size() > 0) {
            try {
                queryIndex = Integer.parseInt(initialOr.get(initialOr.size() - 1));
                queryIndex--;
                or = new ArrayList<>(initialOr);
                or.remove(initialOr.size() - 1);
            } catch (NumberFormatException e) {
                // fall-through - not and index
            }
        }
        orBuilder= new BooleanQuery.Builder();
        for (String and : or.isEmpty() ? Collections.singletonList("") : or) {
            and = variables(and, variables);
            andBuilder = new BooleanQuery.Builder();
            // make sure we have at least one entry. Because terms might be empty. Or, if it's a single "!", we need another entry to make it work.
            andBuilder.add(any(), BooleanClause.Occur.MUST);
            terms = PLUS.split(and);
            for (String termWithNot : terms) {
                not = termWithNot.startsWith("!");
                term = not ? termWithNot.substring(1) : termWithNot;
                if (term.startsWith("§")) {
                    // CAUTION: don't merge this into + separates terms below, because lucene query may contain '+' themselves
                    termQuery = new StandardQueryParser().parse(and.substring(1), Field.ARTIFACT.dbname());
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
                    string = variables(string, variables);
                    termQuery = or(fields, match, string);
                }
                andBuilder.add(termQuery, not ? BooleanClause.Occur.MUST_NOT : BooleanClause.Occur.MUST);
            }
            orBuilder.add(andBuilder.build(), BooleanClause.Occur.SHOULD);
        }
        return new PommesQuery(queryIndex, orBuilder.build());
    }

    private static final String prefix = "{";
    private static final String suffix = "}";

    private static String variables(String string, Variables variables) throws IOException {
        StringBuilder builder;
        int start;
        int end;
        int last;
        String var;
        String replaced;

        builder = new StringBuilder();
        last = 0;
        while (true) {
            start = string.indexOf(prefix, last);
            if (start == -1) {
                if (last == 0) {
                    return string;
                } else {
                    builder.append(string.substring(last));
                    return builder.toString();
                }
            }
            end = string.indexOf(suffix, start + prefix.length());
            if (end == -1) {
                throw new IllegalArgumentException("missing end marker");
            }
            var = string.substring(start + prefix.length(), end);
            replaced = variables.lookup(var);
            if (replaced == null) {
                throw new IOException("undefined variable: " + var);
            }
            builder.append(string.substring(last, start));
            builder.append(replaced);
            last = end + suffix.length();
        }
    }

    private static Query any() {
        BooleanQuery.Builder result;

        result = new BooleanQuery.Builder();
        result.add(Field.ID.query(Match.SUBSTRING, ""), BooleanClause.Occur.SHOULD);
        return result.build();
    }

    private static Query or(List<Field> fields, Match match, String string) {
        BooleanQuery.Builder result;

        result = new BooleanQuery.Builder();
        for (Field field : fields) {
            result.add(field.query(match, string), BooleanClause.Occur.SHOULD);
        }
        return result.build();
    }

    //--

    private final int idx;
    private final Query query;

    public PommesQuery(int idx, Query query) {
        this.idx = idx;
        this.query = query;
    }

    public List<Document> find(IndexSearcher searcher) throws IOException {
        TopDocs search;
        List<Document> list;

        search = searcher.search(query, Integer.MAX_VALUE);
        list = new ArrayList<>();
        if (idx < 0) {
            for (ScoreDoc scoreDoc : search.scoreDocs) {
                list.add(searcher.getIndexReader().document(scoreDoc.doc));
            }
        } else {
            if (idx < search.scoreDocs.length) {
                list.add(searcher.getIndexReader().document(search.scoreDocs[idx].doc));
            }
        }
        return list;
    }

    public void delete(IndexWriter writer) throws IOException {
        if (idx >= 0) {
            throw new UnsupportedOperationException();
        }
        writer.deleteDocuments(query);
    }
}
