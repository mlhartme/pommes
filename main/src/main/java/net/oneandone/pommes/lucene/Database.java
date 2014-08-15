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
package net.oneandone.pommes.lucene;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Database {
    public static final String SCM_SVN = "scm:svn:";

    //-- field names

    public static final String GROUP = "g";
    public static final String ARTIFACT = "a";
    public static final String VERSION = "v";
    public static final String GA = "ga";
    public static final String GAV = "gav";
    public static final String SCM = "scm";

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

    private Directory indexLuceneDirectory;

    public Database(FileNode directory, Node global) {
        this.directory = directory;
        this.global = global;
    }

    public Directory getIndexLuceneDirectory() throws IOException {
        if (indexLuceneDirectory == null) {
            indexLuceneDirectory = FSDirectory.open(directory.toPath().toFile());
        }
        return indexLuceneDirectory;
    }

    public void close() throws IOException {
        if (indexLuceneDirectory != null) {
            indexLuceneDirectory.close();
        }
    }

    //--

    public void downloadOpt() throws IOException {
        FileNode zip;

        if (global != null) {
            if (!directory.exists() || directory.getLastModified() - System.currentTimeMillis() > 1000L * 60 * 60 * 24) {
                zip = directory.getWorld().getTemp().createTempFile();
                global.copyFile(zip);
                wipe();
                zip.unzip(directory);
                zip.deleteFile();
            }
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

    //-- create an index

    public void index(Iterator<Document> iterator) throws IOException {
        IndexWriter writer;

        wipe();
        writer = new IndexWriter(getIndexLuceneDirectory(),
                new IndexWriterConfig(Version.LUCENE_4_9, new StandardAnalyzer(Version.LUCENE_4_9)));
        while (iterator.hasNext()) {
            writer.addDocument(iterator.next());
        }
        writer.close();
    }

    public static Document document(MavenProject mavenProject) throws InvalidVersionSpecificationException {
        Document doc;
        GroupArtifactVersion gav;
        MavenProject parent;

        doc = new Document();
        gav = new GroupArtifactVersion(mavenProject);

        // basic stuff
        doc.add(new StringField(GROUP, gav.getGroupId(), Field.Store.YES));
        doc.add(new StringField(ARTIFACT, gav.getArtifactId(), Field.Store.YES));
        doc.add(new StringField(VERSION, gav.getVersion(), Field.Store.YES));
        doc.add(new StringField(GA, gav.toGaString(), Field.Store.YES));
        doc.add(new StringField(GAV, gav.toGavString(), Field.Store.YES));
        doc.add(new StringField(SCM, scm(mavenProject), Field.Store.YES));

        // dependencies
        List<Dependency> dependencies = mavenProject.getDependencies();
        for (Dependency dependency : dependencies) {
            GroupArtifactVersion depGav = new GroupArtifactVersion(dependency);

            // index groupId:artifactId for non-version searches
            doc.add(new StringField(DEP_GA, depGav.toGaString(), Field.Store.YES));
            // index groupId:artifactId:version for exact-version searches
            doc.add(new StringField(DEP_GAV, depGav.toGavString(), Field.Store.YES));
            // index groupId:artifactId for searches that need to evaluate the range
            VersionRange vr = VersionRange.createFromVersionSpec(dependency.getVersion());
            if (vr.hasRestrictions()) {
                doc.add(new StringField(DEP_GA_RANGE, depGav.toGaString(), Field.Store.YES));
            }
        }

        // parent
        parent = mavenProject.getParent();
        if (parent != null) {
            GroupArtifactVersion parGav = new GroupArtifactVersion(parent);

            doc.add(new StringField(PAR_GA, parGav.toGaString(), Field.Store.YES));
            doc.add(new StringField(PAR_GAV, parGav.toGavString(), Field.Store.YES));
        }
        return doc;
    }

    //--

    private static String scm(MavenProject project) {
        Scm scm;
        String connection;

        scm = project.getScm();
        if (scm != null) {
            connection = scm.getConnection();
            if (connection != null) {
                return connection;
            }
        }
        return "";
    }

    private void wipe() throws IOException {
        directory.deleteTreeOpt();
        directory.mkdir();
    }

    //--

    /**
     * TODO: git support
     * @return urls with tailing slash
     */
    public List<String> list(String urlPrefix) throws IOException {
        List<String> result;
        String url;
        String prefix;
        Document doc;

        result = new ArrayList<>();
        prefix = Database.SCM_SVN + urlPrefix;
        try (IndexReader reader = DirectoryReader.open(getIndexLuceneDirectory())) {
            int numDocs = reader.numDocs();
            for (int i = 0; i < numDocs; i++) {
                doc = reader.document(i);
                url = doc.get(Database.SCM);
                url = withSlash(url);
                if (url.startsWith(prefix)) {
                    result.add(Strings.removeLeft(url, Database.SCM_SVN));
                }
            }
        }
        return result;
    }

    public static String withSlash(String url) {
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        return url;
    }

    //--

    public List<Asset> search(String substring) throws IOException {
        List<Asset> result;
        Document document;
        Asset asset;

        result = new ArrayList<>();
        IndexReader reader = DirectoryReader.open(getIndexLuceneDirectory());
        int numDocs = reader.numDocs();
        for (int i = 0; i < numDocs; i++) {
            document = reader.document(i);
            asset = toAsset(document);
            if (asset.toLine().contains(substring)) {
                result.add(asset);
            }
        }
        reader.close();
        return result;
    }

    public static Asset toAsset(Document document) {
        GroupArtifactVersion artifact;
        String url;

        artifact = new GroupArtifactVersion(document.get(Database.GAV));
        url = document.get(Database.SCM);
        return new Asset(artifact.getGroupId(), artifact.getArtifactId(), url);
    }
}
