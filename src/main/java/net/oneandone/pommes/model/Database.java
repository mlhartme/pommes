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
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Database implements AutoCloseable {
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
        index(Collections.emptyIterator());
    }

    public void remove(List<String> query, Variables variables) throws IOException, QueryNodeException {
        IndexWriter writer;
        IndexWriterConfig config;

        close();
        config =  new IndexWriterConfig(new StandardAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        writer = new IndexWriter(getIndexLuceneDirectory(), config);
        writer.deleteDocuments(PommesQuery.build(query, variables));
        writer.close();
    }

    public void list(Map<String, String> result) throws IOException {
        TopDocs search;
        Document document;
        Query query;

        if (searcher == null) {
            searcher = new IndexSearcher(DirectoryReader.open(getIndexLuceneDirectory()));
        }
        query = new WildcardQuery(Field.ID.term("*"));
        search = searcher.search(query, Integer.MAX_VALUE);
        for (ScoreDoc scoreDoc : search.scoreDocs) {
            document = searcher.getIndexReader().document(scoreDoc.doc);
            result.put(Field.ID.get(document), Field.REVISION.get(document));
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
            writer.updateDocument(Field.ID.term(Field.ID.get(doc)), doc);
        }
        writer.close();
    }

    public List<Document> query(List<String> query, Variables variables) throws IOException, QueryNodeException {
        return query(PommesQuery.build(query, variables));
    }

    public List<Document> query(Query query) throws IOException {
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
