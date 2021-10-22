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

import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Database implements AutoCloseable {
    public static Database load(FileNode directory) throws IOException {
        boolean create;
        Database result;

        create = !directory.exists();
        if (create) {
            directory.mkdirs();
        }
        result = new Database(directory);
        if (create) {
            result.index(Collections.emptyIterator());
        }
        return result;
    }

    /** Lucene index directory */
    private final FileNode directory;

    /** created on demand */
    private Directory indexLuceneDirectory;

    /** created on demand */
    private IndexSearcher searcher;

    /**
     * @param directory valid lucene directory or none-existing directory
     */
    public Database(FileNode directory) {
        this.directory = directory;
        this.searcher = null;
    }

    public FileNode importMarker() {
        return directory.getParent().join(directory.getName() + ".imported");
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

    //-- change the index

    public void reset() throws IOException {
        close();
        directory.deleteTreeOpt();
        importMarker().deleteFileOpt();
        directory.mkdir();
        index(Collections.emptyIterator());
    }

    public void remove(PommesQuery query) throws IOException, QueryNodeException {
        IndexWriter writer;
        IndexWriterConfig config;

        close();
        config =  new IndexWriterConfig(new StandardAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        writer = new IndexWriter(getIndexLuceneDirectory(), config);
        query.delete(writer);
        writer.close();
    }

    public void removeIds(Collection<String> ids) throws IOException, QueryNodeException {
        IndexWriter writer;
        IndexWriterConfig config;
        Term[] terms;
        int i;

        if (ids.isEmpty()) {
            return;
        }
        terms = new Term[ids.size()];
        i = 0;
        for (String id : ids) {
            terms[i++] = Field.ID.term(id);
        }
        close();
        config =  new IndexWriterConfig(new StandardAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        writer = new IndexWriter(getIndexLuceneDirectory(), config);
        writer.deleteDocuments(terms);
        writer.close();
    }

    public void list(Map<String, String> result, String zone) throws IOException {
        TopDocs search;
        Document document;
        Query query;

        if (searcher == null) {
            searcher = new IndexSearcher(DirectoryReader.open(getIndexLuceneDirectory()));
        }
        query = new WildcardQuery(Field.ID.term(zone + ":*"));
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

    public List<Document> query(PommesQuery pq) throws IOException {
        if (searcher == null) {
            searcher = new IndexSearcher(DirectoryReader.open(getIndexLuceneDirectory()));
        }
        return pq.find(searcher);
    }

    public Project projectByScm(String url) throws IOException {
        List<Document> poms;

        // TODO: to normalize subversion urls
        url = Strings.removeRightOpt(url, "/");
        try {
            poms = query(PommesQuery.create("s:" + url));
        } catch (QueryNodeException e) {
            throw new IllegalStateException();
        }
        switch (poms.size()) {
            case 0:
                return null;
            case 1:
                return Field.project(poms.get(0));
            default:
                throw new IOException("scm ambiguous: " + url);
        }
    }
}
