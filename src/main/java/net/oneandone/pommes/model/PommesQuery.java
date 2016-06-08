package net.oneandone.pommes.model;

import net.oneandone.sushi.util.Separator;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PommesQuery {
    private static final Separator PLUS = Separator.on('+');

    public static Query build(List<String> or, Variables variables) throws IOException, QueryNodeException {
        BooleanQuery.Builder orBuilder;
        BooleanQuery.Builder andBuilder;
        Query termQuery;
        List<String> terms;
        Match match;
        int idx;
        String string;
        List<Field> fields;
        Object[] tmp;

        orBuilder= new BooleanQuery.Builder();
        for (String and : or.isEmpty() ? Collections.singletonList("") : or) {
            and = variables(and, variables);
            andBuilder = new BooleanQuery.Builder();
            terms = PLUS.split(and);
            if (terms.isEmpty()) {
                terms.add("");
            }
            for (String term : terms) {
                if (term.startsWith("%")) {
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
                andBuilder.add(termQuery, BooleanClause.Occur.MUST);
            }
            orBuilder.add(andBuilder.build(), BooleanClause.Occur.SHOULD);
        }
        return orBuilder.build();
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

    private static Query or(List<Field> fields, Match match, String string) {
        BooleanQuery.Builder result;

        result = new BooleanQuery.Builder();
        for (Field field : fields) {
            result.add(field.query(match, string), BooleanClause.Occur.SHOULD);
        }
        return result.build();
    }

}
