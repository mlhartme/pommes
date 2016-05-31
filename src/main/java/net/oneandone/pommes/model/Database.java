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
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    //--

    /** Lucene index directory */
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
            indexLuceneDirectory = FSDirectory.open(directory.toPath());
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

    public void remove(String query, Variables variables) throws IOException, QueryNodeException {
        IndexWriter writer;
        IndexWriterConfig config;

        close();
        config =  new IndexWriterConfig(new StandardAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        writer = new IndexWriter(getIndexLuceneDirectory(), config);
        writer.deleteDocuments(pommesQuery(query, variables));
        writer.close();
    }

    public void list(Map<String, String> result) throws IOException {
        TopDocs search;
        Document document;
        Query query;

        if (searcher == null) {
            searcher = new IndexSearcher(DirectoryReader.open(getIndexLuceneDirectory()));
        }
        query = new WildcardQuery(Field.ORIGIN.term("*"));
        search = searcher.search(query, Integer.MAX_VALUE);
        for (ScoreDoc scoreDoc : search.scoreDocs) {
            document = searcher.getIndexReader().document(scoreDoc.doc);
            result.put(Field.ORIGIN.get(document), Field.REVISION.get(document));
        }
    }

    public void index(Iterator<Document> iterator) throws IOException {
        IndexWriter writer;
        IndexWriterConfig config;
        Document doc;

        close();
        // no analyzer, I have String fields only
        config =  new IndexWriterConfig(new StandardAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        writer = new IndexWriter(getIndexLuceneDirectory(), config);
        while (iterator.hasNext()) {
            doc = iterator.next();
            writer.updateDocument(Field.ORIGIN.term(Field.ORIGIN.get(doc)), doc);
        }
        writer.close();
    }

    //-- query

    private static final Separator PLUS = Separator.on('+');

    public List<Pom> query(String queryString, Variables variables) throws IOException, QueryNodeException {
        return query(pommesQuery(queryString, variables));
    }

    private Query pommesQuery(String queryString, Variables variables) throws IOException, QueryNodeException {
        BooleanQuery.Builder query;
        List<String> terms;
        Query term;
        char marker;
        String string;

        queryString = variables(queryString, variables);
        if (queryString.startsWith("%")) {
            // CAUTION: don't merge this into + separates terms below, because lucene query may contain '+' themselves
            return new StandardQueryParser().parse(queryString.substring(1), Field.GAV.dbname());
        } else {
            query = new BooleanQuery.Builder();
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
                            term = or(Field.PARENT.substring(string), Field.DEP.substring(string));
                        } else if (termString.length() > 1 && termString.charAt(1) == '_') {
                            string = variables(termString.substring(2), variables);
                            term = Field.DEP.substring(string);
                        } else {
                            string = variables(termString.substring(1), variables);
                            term = Field.GAV.substring(string);
                        }
                        break;
                    case '^':
                        string = variables(termString.substring(1), variables);
                        term = Field.PARENT.substring(string);
                        break;
                    case '@':
                        string = variables(termString.substring(1), variables);
                        term = Field.SCM.substring(string);
                        break;
                    case '§':
                        string = variables(termString.substring(1), variables);
                        term = Field.ORIGIN.substring(string);
                        break;
                    default:
                        string = variables(termString, variables);
                        term = or(Field.GAV.substring(string), Field.SCM.substring(string),
                                Field.PARENT.substring(string));
                        break;
                }
                query.add(term, BooleanClause.Occur.MUST);
            }
            return query.build();
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

    private static Query or(Query left, Query ... rights) {
        BooleanQuery.Builder result;

        result = new BooleanQuery.Builder();
        result.add(left, BooleanClause.Occur.SHOULD);
        for (Query right : rights) {
            result.add(right, BooleanClause.Occur.SHOULD);
        }
        return result.build();
    }

    public List<Pom> query(Query query) throws IOException {
        List<Pom> result;

        result = new ArrayList<>();
        for (Document document : doQuery(query)) {
            result.add(Field.pom(document));
        }
        return result;
    }

    private List<Document> doQuery(Query query) throws IOException {
        TopDocs search;
        List<Document> list;

        if (searcher == null) {
            searcher = new IndexSearcher(DirectoryReader.open(getIndexLuceneDirectory()));
        }
        search = searcher.search(query, Integer.MAX_VALUE);
        list = new ArrayList<>();
        for (ScoreDoc scoreDoc : search.scoreDocs) {
            list.add(searcher.getIndexReader().document(scoreDoc.doc));
        }
        return list;
    }
}
