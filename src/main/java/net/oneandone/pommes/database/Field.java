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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;

import java.util.ArrayList;
import java.util.List;

public enum Field {
    /**
     * Mandatory. The full uri used to load the project for indexing. Full means the uri pointing to the pom file, not to trunk or a branch directory.
     * Used as a unique identifier for the document.
     */
    ORIGIN("Unique identifier for this project."),
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

    public Query query(Match match, String str) {
        switch (match) {
            case SUBSTRING:
                return new WildcardQuery(new Term(dbname, "*" + str + "*"));
            case PREFIX:
                return new WildcardQuery(new Term(dbname, str + "*"));
            case SUFFIX:
                return new WildcardQuery(new Term(dbname, "*" + str));
            case STRING:
                return new WildcardQuery(new Term(dbname, str));
            default:
                throw new IllegalStateException();
        }
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

    public static Document document(Project project) {
        Document doc;

        doc = new Document();
        ORIGIN.add(doc, project.origin);
        REVISION.add(doc, project.revision);
        if (project.parent != null) {
            PARENT.add(doc, project.parent.toGavString());
        }
        ARTIFACT.add(doc, project.artifact.toGavString());
        for (Gav dep : project.dependencies) {
            DEP.add(doc, dep.toGavString());
        }
        SCM.addOpt(doc, project.scm);
        URL.addOpt(doc, project.url);
        return doc;
    }

    public static Project project(Document document) {
        Project result;
        String parent;

        parent = PARENT.get(document);
        result = new Project(ORIGIN.get(document), REVISION.get(document),
                parent == null ? null : Gav.forGav(parent), Gav.forGav(ARTIFACT.get(document)),
                SCM.get(document), URL.get(document));
        for (String dep : DEP.getList(document)) {
            result.dependencies.add(Gav.forGav(dep));
        }
        return result;
    }

    public static List<Project> projects(List<Document> documents) {
        List<Project> result;

        result = new ArrayList<>();
        for (Document document : documents) {
            result.add(Field.project(document));
        }
        return result;
    }
}
