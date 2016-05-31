package net.oneandone.pommes.model;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mhm on 31.05.16.
 */
public enum Field {
    /**
     * Mandatory. The full uri used to load the pom for indexing. Full means the uri pointing to the pom file, not to trunk or a branch directory.
     * Used as a unique identifier for the document.
     */
    ORIGIN,
    REVISION,
    PARENT(true, false),
    GAV,
    SCM(true, false),
    DEP(true, true),
    URL(true, false);

    private final boolean optional;
    private final boolean list;
    private final String dbname;

    Field() {
        this(false, false);
    }

    Field(boolean optional, boolean list) {
        this.optional = optional;
        this.list = list;
        this.dbname = name().toLowerCase();
    }

    public void add(Document document, String value) {
        document.add(new StringField(dbname, value, org.apache.lucene.document.Field.Store.YES));
    }

    public void addOpt(Document document, String value) {
        if (value != null) {
            add(document, value);
        }
    }

    public String get(Document document) {
        return document.get(dbname);
    }

    public String[] getList(Document document) {
        return document.getValues(dbname);
    }

    public Query substring(String substring) {
        return new WildcardQuery(new Term(dbname, "*" + substring + "*"));
    }

    public String dbname() {
        return dbname;
    }

    public Term term(String str) {
        return new Term(dbname, str);
    }

    public void copy(Document src, List<String> dest) {
        String str;

        if (list) {
            for (String value : getList(src)) {
                dest.add(value);
            }
        } else {
            str = get(src);
            if (str != null) {
                dest.add(str);
            }
        }
    }

    //--

    public static Field forLetter(char c) {
        for (Field field : values()) {
            if (field.dbname.charAt(0) == c) {
                return field;
            }
        }
        throw new IllegalStateException("unknown field letter: " + c);
    }

    public static Document document(Pom pom) {
        Document doc;

        doc = new Document();
        ORIGIN.add(doc, pom.origin);
        REVISION.add(doc, pom.revision);
        if (pom.parent != null) {
            PARENT.add(doc, pom.parent.toGavString());
        }
        GAV.add(doc, pom.coordinates.toGavString());
        for (Gav dep : pom.dependencies) {
            DEP.add(doc, dep.toGavString());
        }
        SCM.addOpt(doc, pom.scm);
        URL.addOpt(doc, pom.url);
        return doc;
    }

    public static Pom pom(Document document) {
        Pom result;
        String parent;

        parent = PARENT.get(document);
        result = new Pom(ORIGIN.get(document), REVISION.get(document),
                parent == null ? null : Gav.forGav(parent), Gav.forGav(GAV.get(document)),
                SCM.get(document), URL.get(document));
        for (String dep : DEP.getList(document)) {
            result.dependencies.add(Gav.forGav(dep));
        }
        return result;
    }

    public static List<Pom> poms(List<Document> documents) {
        List<Pom> result;

        result = new ArrayList<>();
        for (Document document : documents) {
            result.add(Field.pom(document));
        }
        return result;
    }
}
