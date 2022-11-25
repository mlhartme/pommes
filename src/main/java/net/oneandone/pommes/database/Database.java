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
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
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

    public void removeOrigins(Collection<String> origins) throws IOException {
        IndexWriter writer;
        IndexWriterConfig config;
        Term[] terms;

        if (origins.isEmpty()) {
            return;
        }
        terms = new Term[origins.size()];
        int i = 0;
        for (String origin : origins) {
            terms[i++] = Field.ORIGIN.term(origin);
        }

        close();
        config =  new IndexWriterConfig(new StandardAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        writer = new IndexWriter(getIndexLuceneDirectory(), config);
        writer.deleteDocuments(terms);
        writer.close();
    }

    public void list(String repository, Map<String, String> result) throws IOException {
        TopDocs search;
        Document document;

        if (searcher == null) {
            searcher = new IndexSearcher(DirectoryReader.open(getIndexLuceneDirectory()));
        }
        search = searcher.search(Field.ORIGIN.query(Match.PREFIX, repository + Field.ORIGIN_DELIMITER), Integer.MAX_VALUE);
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

    public List<Document> query(PommesQuery pq) throws IOException {
        if (searcher == null) {
            searcher = new IndexSearcher(DirectoryReader.open(getIndexLuceneDirectory()));
        }
        return pq.find(searcher);
    }

    public List<Project> projectsByScm(String url) throws IOException {
        List<Document> poms;

        // TODO: to normalize subversion urls
        url = Strings.removeRightOpt(url, "/");
        poms = query(PommesQuery.parse("s:" + url));
        return poms.stream().map(pom -> Field.project(pom)).toList();
    }
}
