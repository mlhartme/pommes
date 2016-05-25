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

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.io.OS;
import net.oneandone.sushi.util.Separator;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Database implements AutoCloseable {
    public static Database load(World world) throws NodeInstantiationException {
        String global;
        String localStr;
        FileNode local;

        global = System.getenv("POMMES_GLOBAL");
        localStr = System.getenv("POMMES_LOCAL");
        if (localStr != null) {
            local = world.file(localStr);
        } else {
            if (OS.CURRENT == OS.MAC) {
                // see https://developer.apple.com/library/mac/qa/qa1170/_index.html
                local = (FileNode) world.getHome().join("Library/Caches/pommes");
            } else {
                local = world.getTemp().join("pommes-" + System.getProperty("user.name"));
            }
        }
        try {
            return new Database(local, global == null ? null : world.node(global));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("invalid url for global pommes database file: " + global, e);
        }
    }

    //-- field names

    /**
     * The full uri used to load the pom for indexing. Full means the uri pointing to the pom file, not to trunk or a branch.
     * SCM tags cannot be use instead because they are not always specified and you often forget to adjust them when creating
     * branches. And they only point to the directory containing the pom.
     */
    public static final String ORIGIN = "origin";

    public static final String GROUP = "g";
    public static final String ARTIFACT = "a";
    public static final String VERSION = "v";
    public static final String GA = "ga";
    public static final String GAV_NAME = "gav";

    public static final String DEP_GA = "dep-ga";
    public static final String DEP_GAV = "dep-gav";
    public static final String DEP_GA_RANGE = "dep-ga-range";

    public static final String PAR_GA = "par-ga";
    public static final String PAR_GAV = "par-gav";

    //--

    /** lucene index directory */
    private final FileNode directory;

    /** zipped lucene index directory; null to disable global database. */
    private final Node global;

    /** created on demand */
    private Directory indexLuceneDirectory;

    /** created on demand */
    private IndexSearcher searcher;

    /**
     * @param directory valid lucene directory or none-existing directory
     * @param global zip file or null
     */
    public Database(FileNode directory, Node global) {
        this.directory = directory;
        this.global = global;
        this.searcher = null;
    }

    private Directory getIndexLuceneDirectory() throws IOException {
        if (indexLuceneDirectory == null) {
            indexLuceneDirectory = FSDirectory.open(directory.toPath().toFile());
        }
        return indexLuceneDirectory;
    }

    public void close() throws IOException {
        if (indexLuceneDirectory != null) {
            indexLuceneDirectory.close();
            indexLuceneDirectory = null;
        }
        if (searcher != null) {
            searcher.getIndexReader().close();
            searcher = null;
        }
    }

    //--

    public Database downloadOpt() throws IOException {
        if (global != null) {
            download(false);
        }
        return this;
    }

    public void download(boolean force) throws IOException {
        FileNode zip;

        if (global == null) {
            throw new IllegalStateException();
        }
        if (force || !directory.exists() || directory.getLastModified() - System.currentTimeMillis() > 1000L * 60 * 60 * 24) {
            zip = directory.getWorld().getTemp().createTempFile();
            global.copyFile(zip);
            clear();
            zip.unzip(directory);
            zip.deleteFile();
        }
    }

    public Node upload() throws IOException {
        FileNode zip;

        if (global == null) {
            throw new IllegalStateException();
        }
        zip = directory.getWorld().getTemp().createTempFile();
        directory.zip(zip);
        zip.copyFile(global);
        return global;
    }

    //-- change the index

    public void clear() throws IOException {
        close();
        directory.deleteTreeOpt();
        directory.mkdir();
    }

    public void remove(List<String> prefixes) throws IOException {
        IndexWriter writer;
        IndexWriterConfig config;

        close();
        config =  new IndexWriterConfig(Version.LUCENE_4_9, null);
        config.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        writer = new IndexWriter(getIndexLuceneDirectory(), config);
        for (String prefix : prefixes) {
            writer.deleteDocuments(new PrefixQuery(new Term(ORIGIN, prefix)));
        }
        writer.close();
    }

    public void index(Iterator<Document> iterator) throws IOException {
        IndexWriter writer;
        IndexWriterConfig config;
        Document doc;

        close();
        // no analyzer, I have String fields only
        config =  new IndexWriterConfig(Version.LUCENE_4_9, null);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        writer = new IndexWriter(getIndexLuceneDirectory(), config);
        while (iterator.hasNext()) {
            doc = iterator.next();
            writer.updateDocument(new Term(ORIGIN, doc.get(ORIGIN)), doc);
        }
        writer.close();
    }

    //--

    public static String withSlash(String url) {
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        return url;
    }

    //--

    public static Pom toPom(Document document) {
        Pom result;
        String parent;

        result = new Pom(document.get(Database.ORIGIN), GAV.forGav(document.get(Database.GAV_NAME)));
        parent = document.get(PAR_GAV);
        if (parent != null) {
            result.dependencies.add(GAV.forGav(parent));
        }
        for (String dep : document.getValues(Database.DEP_GAV)) {
            result.dependencies.add(GAV.forGav(dep));
        }
        return result;
    }

    //-- query

    private static final Separator PLUS = Separator.on('+');

    public List<Pom> query(String queryString, Variables variables) throws IOException, QueryNodeException {
        BooleanQuery query;
        List<String> terms;
        Query term;
        char marker;
        String string;

        queryString = variables(queryString, variables);
        if (queryString.startsWith("%")) {
            // CAUTION: don't merge this into + separates terms below, because lucene query may contain '+' themselves
            return query(new StandardQueryParser().parse(queryString.substring(1), Database.GAV_NAME));
        } else {
            query = new BooleanQuery();
            terms = PLUS.split(queryString);
            if (terms.isEmpty()) {
                terms.add("");
            }
            for (String termString : terms) {
                marker = termString.isEmpty() ? ' ' : termString.charAt(0);
                switch (marker) {
                    case ':':
                        if (termString.length() > 1 && termString.charAt(1) == '-') {
                            string = variables(termString.substring(2), variables);
                            term = or(substring(Database.PAR_GAV, string), substring(Database.DEP_GAV, string));
                        } else {
                            string = variables(termString.substring(1), variables);
                            term = substring(Database.GAV_NAME, string);
                        }
                        break;
                    case '@':
                        string = variables(termString.substring(1), variables);
                        term = substring(Database.ORIGIN, string);
                        break;
                    default:
                        string = variables(termString, variables);
                        term = or(substring(Database.GAV_NAME, string), substring(Database.ORIGIN, string));
                        break;
                }
                query.add(term, BooleanClause.Occur.MUST);
            }
            return query(query);
        }
    }

    private static final String prefix = "=";
    private static final String suffix = "=";

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

    private static Query or(Query left, Query right) {
        BooleanQuery result;

        result = new BooleanQuery();
        result.add(left, BooleanClause.Occur.SHOULD);
        result.add(right, BooleanClause.Occur.SHOULD);
        return result;
    }

    private static Query substring(String field, String substring) {
        return new WildcardQuery(new Term(field, "*" + substring + "*"));
    }

    public Pom lookup(String origin) throws IOException {
        List<Pom> result;

        result = query(new TermQuery(new Term(Database.ORIGIN, origin)));
        switch (result.size()) {
            case 0:
                return null;
            case 1:
                return result.get(0);
            default:
                throw new IllegalStateException("ambiguous origin: " + origin);
        }
    }

    public List<Pom> query(Query query) throws IOException {
        List<Pom> result;

        result = new ArrayList<>();
        for (Document document : doQuery(query)) {
            result.add(toPom(document));
        }
        return result;
    }

    private List<Document> doQuery(Query query) throws IOException {
        TopDocs search;
        List<Document> list;

        if (searcher == null) {
            searcher = new IndexSearcher(DirectoryReader.open(getIndexLuceneDirectory()));
        }
        search = searcher.search(query, 100000);
        list = new ArrayList<>();
        for (ScoreDoc scoreDoc : search.scoreDocs) {
            list.add(searcher.getIndexReader().document(scoreDoc.doc));
        }
        return list;
    }

    //--

    public static Document document(String origin, Pom pom) throws InvalidVersionSpecificationException {
        Document doc;

        doc = new Document();
        doc.add(new StringField(ORIGIN, origin, Field.Store.YES));
        doc.add(new StringField(GROUP, pom.coordinates.groupId, Field.Store.YES));
        doc.add(new StringField(ARTIFACT, pom.coordinates.artifactId, Field.Store.YES));
        doc.add(new StringField(VERSION, pom.coordinates.version, Field.Store.YES));
        doc.add(new StringField(GA, pom.coordinates.toGaString(), Field.Store.YES));
        doc.add(new StringField(GAV_NAME, pom.coordinates.toGavString(), Field.Store.YES));
        return doc;
    }

    public static Document document(String origin, MavenProject mavenProject) throws InvalidVersionSpecificationException {
        Document doc;
        MavenProject parent;
        Pom parPom;

        doc = document(origin, Pom.forProject(origin, mavenProject));
        for (Dependency dependency : mavenProject.getDependencies()) {
            GAV dep = GAV.forDependency(dependency);

            // index groupId:artifactId for non-version searches
            doc.add(new StringField(DEP_GA, dep.toGaString(), Field.Store.YES));
            // index groupId:artifactId:version for exact-version searches
            doc.add(new StringField(DEP_GAV, dep.toGavString(), Field.Store.YES));
            // index groupId:artifactId for searches that need to evaluate the range
            VersionRange vr = VersionRange.createFromVersionSpec(dependency.getVersion());
            if (vr.hasRestrictions()) {
                doc.add(new StringField(DEP_GA_RANGE, dep.toGaString(), Field.Store.YES));
            }
        }

        // parent
        parent = mavenProject.getParent();
        if (parent != null) {
            parPom = Pom.forProject(origin, parent);
            doc.add(new StringField(PAR_GA, parPom.coordinates.toGaString(), Field.Store.YES));
            doc.add(new StringField(PAR_GAV, parPom.coordinates.toGavString(), Field.Store.YES));
        }
        return doc;
    }
}
