package net.oneandone.pommes.model;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;

/** Defines database fields and pom/document conversion */
public class Schema {
    public enum Field {
        /**
         * Mandatory. The full uri used to load the pom for indexing. Full means the uri pointing to the pom file, not to trunk or a branch directory.
         * Used as a unique identifier for the document.
         */
        ORIGIN,

        /** Mandatory. */
        REVISION,

        /** Optional */
        PARENT,

        /** Mandatory. */
        GAV,

        /** Optional. */
        SCM,

        /** List. */
        DEP,

        /** Optional. */
        URL;

        private String dbname = name().toLowerCase();

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

        //--

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

        public static Pom toPom(Document document) {
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
    }
}
