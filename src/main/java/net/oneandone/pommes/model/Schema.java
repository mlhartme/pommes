package net.oneandone.pommes.model;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;

/** Defines database fields and pom/document conversion */
public class Schema {
    /**
     * Mandatory. The full uri used to load the pom for indexing. Full means the uri pointing to the pom file, not to trunk or a branch directory.
     * Used as a unique identifier for the document.
     */
    public static final String ORIGIN = "origin";
    /** Mandatory. */
    public static final String REVISION = "revision";
    /** Optional */
    public static final String PARENT = "parent";
    /** Mandatory. */
    public static final String GAV = "gav";
    /** Optional. */
    public static final String SCM = "scm";
    /** List. */
    public static final String DEP = "dep";
    /** Optional. */
    public static final String URL = "url";

    //--

    public static Pom toPom(Document document) {
        Pom result;
        String parent;

        parent = document.get(PARENT);
        result = new Pom(document.get(ORIGIN), document.get(REVISION),
                parent == null ? null : Gav.forGav(parent), Gav.forGav(document.get(GAV)),
                document.get(SCM), document.get(URL));
        for (String dep : document.getValues(DEP)) {
            result.dependencies.add(Gav.forGav(dep));
        }
        return result;
    }

    public static Document document(Pom pom) {
        Document doc;

        doc = new Document();
        add(doc, ORIGIN, pom.origin);
        add(doc, REVISION, pom.revision);
        if (pom.parent != null) {
            add(doc, PARENT, pom.parent.toGavString());
        }
        add(doc, GAV, pom.coordinates.toGavString());
        for (Gav dep : pom.dependencies) {
            add(doc, DEP, dep.toGavString());
        }
        if (pom.scm != null) {
            add(doc, SCM, pom.scm);
        }
        if (pom.url != null) {
            add(doc, URL, pom.url);
        }
        return doc;
    }

    private static void add(Document doc, String field, String value) {
        doc.add(new StringField(field, value, Field.Store.YES));
    }

}
