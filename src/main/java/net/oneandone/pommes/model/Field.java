package net.oneandone.pommes.model;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;

import java.util.ArrayList;
import java.util.List;

public enum Field {
    /**
     * Mandatory. The full uri used to load the pom for indexing. Full means the uri pointing to the pom file, not to trunk or a branch directory.
     * Used as a unique identifier for the document.
     */
    ID("Unique identifier for this pom. Zone + ':' + origin."),
    REVISION("Last modified timestamp or content hash of this pom."),
    PARENT(true, false, "Coordinates of the parent project."),
    ARTIFACT("Coordinates of this project."),
    SCM(true, false, "Scm location for this project."),
    DEP(true, true, "Coordinates of project dependencies."),
    URL(true, false, "Url for this project.");

    private final boolean optional;
    private final boolean list;
    public final String description;
    private final String dbname;

    Field(String description) {
        this(false, false, description);
    }

    Field(boolean optional, boolean list, String description) {
        this.optional = optional;
        this.list = list;
        this.description = description;
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

    public static Field forId(char c) {
        for (Field field : values()) {
            if (field.dbname.charAt(0) == c) {
                return field;
            }
        }
        throw new IllegalStateException("unknown field id: " + c);
    }

    public static List<Field> forIds(String str) {
        List<Field> result;

        result = new ArrayList<>();
        for (int i = 0, max = str.length(); i < max; i++) {
            result.add(Field.forId(str.charAt(i)));
        }
        return result;
    }

    public static Document document(Pom pom) {
        Document doc;

        doc = new Document();
        ID.add(doc, pom.id);
        REVISION.add(doc, pom.revision);
        if (pom.parent != null) {
            PARENT.add(doc, pom.parent.toGavString());
        }
        ARTIFACT.add(doc, pom.artifact.toGavString());
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
        result = new Pom(ID.get(document), REVISION.get(document),
                parent == null ? null : Gav.forGav(parent), Gav.forGav(ARTIFACT.get(document)),
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
