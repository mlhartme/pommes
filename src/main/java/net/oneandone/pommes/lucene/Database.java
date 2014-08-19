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

import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.io.OS;
import net.oneandone.sushi.util.Strings;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.Restriction;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Database implements AutoCloseable {
    public static Database load(World world) throws NodeInstantiationException {
        String global;
        String localStr;
        FileNode local;

        global = System.getenv("POMMES_GLOBAL");
        localStr = System.getenv("POMMES_LOCAL");
        if (localStr == null) {
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
            throw new ArgumentException("invalid url for global pommes database file: " + global, e);
        }
    }

    public static Database loadUpdated(World world) throws IOException {
        Database result;

        result = load(world);
        result.updateOpt();
        return result;
    }

    //--

    public static final String SCM_SVN = "scm:svn:";

    //-- field names

    public static final String ID = "id";

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

    /** created on demand */
    private Directory indexLuceneDirectory;

    /** created on demand */
    private IndexSearcher searcher;

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

    public void updateOpt() throws IOException {
        FileNode zip;

        if (global != null) {
            if (!directory.exists() || directory.getLastModified() - System.currentTimeMillis() > 1000L * 60 * 60 * 24) {
                zip = directory.getWorld().getTemp().createTempFile();
                global.copyFile(zip);
                clear();
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

    //-- change the index

    public void clear() throws IOException {
        close();
        directory.deleteTreeOpt();
        directory.mkdir();
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
            writer.updateDocument(new Term(ID, doc.get(ID)), doc);
        }
        writer.close();
    }

    public static Document document(String id, MavenProject mavenProject) throws InvalidVersionSpecificationException {
        Document doc;
        GroupArtifactVersion gav;
        MavenProject parent;

        doc = new Document();
        gav = new GroupArtifactVersion(mavenProject);

        doc.add(new StringField(ID, id, Field.Store.YES));

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
        IndexReader reader;

        result = new ArrayList<>();
        reader = DirectoryReader.open(getIndexLuceneDirectory());
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

    //-- searching

    public GroupArtifactVersion findLatestVersion(GroupArtifactVersion gav) throws IOException {

        List<Reference> list = query(Database.GA, gav.toGaString(), null, null);
        if (list.isEmpty()) {
            throw new IllegalStateException("Artifact " + gav.toGavString() + " not found in index.");
        }
        return Collections.max(list).from;
    }

    public List<Reference> findDependeesIgnoringVersion(String groupId, String artifactId) throws IOException {
        GroupArtifactVersion gav = new GroupArtifactVersion(groupId, artifactId, "");
        List<Reference> dependees = query(Database.DEP_GA, gav.toGaString(), Database.DEP_GAV, gav.toGaString());
        return dependees;
    }

    public List<Reference> findChildrenIgnoringVersion(String groupId, String artifactId) throws IOException {
        GroupArtifactVersion gav = new GroupArtifactVersion(groupId, artifactId, "");
        List<Reference> children = query(Database.PAR_GA, gav.toGaString(), Database.PAR_GAV,
                gav.toGaString());
        return children;
    }

    private List<Reference> query(String searchField, String searchValue, String refField, String refValue) throws IOException {
        Term term = new Term(searchField, searchValue);
        Query query = new TermQuery(term);
        List<Reference> list = new ArrayList<>();
        for (Document doc : query(query)) {
            GroupArtifactVersion artifact = new GroupArtifactVersion(doc.get(Database.GAV));
            GroupArtifactVersion reference = getReference(doc, refField, refValue);
            list.add(new Reference(doc, artifact, reference));
        }
        return list;
    }

    private GroupArtifactVersion getReference(Document doc, String fieldName, String ga) {
        if (fieldName != null && ga != null) {
            for (IndexableField field : doc.getFields(fieldName)) {
                GroupArtifactVersion reference = new GroupArtifactVersion(field.stringValue());
                if (reference.toGaString().equals(ga)) {
                    return reference;
                }
            }
        }
        return null;
    }

    /**
     * Filters the list of artifacts, retaining only the latest version of each artifact. The artifact is removed is the
     * latest version doesn't use the referenced artifact any more. Note that the list must be sorted!
     * @param artifacts
     *            the sorted list of artifacts, the list is not modified
     * @return the filtered list
     */
    public List<Reference> filterLatest(List<Reference> artifacts) throws IOException {

        List<Reference> results = new ArrayList<>();

        if (artifacts.isEmpty()) {
            return results;
        }

        // 1st filter: only retain latest version in the list
        Reference previous = artifacts.get(0);
        Iterator<Reference> iterator = artifacts.iterator();
        while (iterator.hasNext()) {
            Reference ubi = iterator.next();

            boolean addPrevious = false;
            if (!ubi.from.toGaString().equals(previous.from.toGaString())) {
                addPrevious = true;
            }
            // else {
            // DefaultArtifactVersion currentVersion = new DefaultArtifactVersion(ubi.getArtifact().getVersion());
            // DefaultArtifactVersion previousVersion = new DefaultArtifactVersion(previous.getArtifact().getVersion());
            // if (currentVersion.getMajorVersion() != previousVersion.getMajorVersion()
            // || currentVersion.getMinorVersion() != previousVersion.getMinorVersion()) {
            // addPrevious = true;
            // }
            // }

            if (addPrevious) {
                results.add(previous);
            }
            if (!iterator.hasNext()) {
                results.add(ubi);
            }

            previous = ubi;
        }

        // 2nd filter: remove the artifact if the latest version in the list isn't the latest available version
        iterator = results.iterator();
        while (iterator.hasNext()) {
            Reference ubi = iterator.next();
            GroupArtifactVersion gav = ubi.from;
            GroupArtifactVersion latest = findLatestVersion(gav);
            if (!gav.equals(latest)) {
                iterator.remove();
            }
        }

        return results;
    }

    /**
     * Aggregates the list of artifacts. Multiple version of an artifact that use the same dependency are aggregated to
     * one line.
     * <p/>
     * Example:
     *
     * <pre>
     * com.example:foo:0.1.0 -&gt; com.example:bar:1.2.5
     * com.example:foo:0.1.1 -&gt; com.example:bar:1.2.5
     * com.example:foo:0.1.2 -&gt; com.example:bar:1.2.5
     * </pre>
     *
     * becomes
     *
     * <pre>
     * com.example:foo:[0.1.0,0.1.2] -&gt; com.example:bar:1.2.5
     * </pre>
     *
     * Note that the list must be sorted!
     * @param artifacts
     *            the sorted list of artifact, the list is not modified
     * @return the aggregated list
     */
    public List<Reference> aggregateArtifacts(List<Reference> artifacts) {

        List<Reference> results = new ArrayList<>();

        if (artifacts.isEmpty()) {
            return results;
        }

        Reference previous = artifacts.get(0);
        String lowerVersion = artifacts.get(0).from.getVersion();
        String upperVersion = lowerVersion;

        Iterator<Reference> iterator = artifacts.iterator();
        while (iterator.hasNext()) {
            Reference ubi = iterator.next();

            boolean addPrevious = false;
            if (ubi.from.toGaString().equals(previous.from.toGaString())
                    && ubi.to.toGavString().equals(previous.to.toGavString())) {
                // as long as artifact.ga is equal and reference.gav is equal
                upperVersion = ubi.from.getVersion();
            } else {
                addPrevious = true;
            }

            if (addPrevious) {
                addAggregated(results, previous, lowerVersion, upperVersion);

                // reset versions
                lowerVersion = ubi.from.getVersion();
                upperVersion = lowerVersion;
            }

            if (!iterator.hasNext()) {
                addAggregated(results, ubi, lowerVersion, ubi.from.getVersion());
            }

            previous = ubi;
        }

        return results;
    }

    private void addAggregated(List<Reference> results, Reference ubi, String lowerVersion, String upperVersion) {
        GroupArtifactVersion previous;
        String version;
        GroupArtifactVersion aggregated;

        previous = ubi.from;
        version = lowerVersion;
        if (!lowerVersion.equals(upperVersion)) {
            version = "[" + lowerVersion + "," + upperVersion + "]";
        }
        aggregated = new GroupArtifactVersion(previous.getGroupId(), previous.getArtifactId(), version);
        results.add(new Reference(ubi.document, aggregated, ubi.to));
    }

    /**
     * Extracts and returns all artifacts in the given list which referenced artifact (dependency or parent) matches the
     * given version.
     * @param artifacts
     *            the unfiltered list of artifacts, the list is not modified
     * @param version
     *            the version
     * @param olderIncludes
     *            older version range to include
     * @param newerIncludes
     *            newer version range to include
     * @return the list of extracted artifacts
     */
    public List<Reference> filterByReferencedArtifacts(List<Reference> artifacts, String version,
                                                       Includes olderIncludes, Includes newerIncludes) throws InvalidVersionSpecificationException {
        // build a version range, based on the requested artifact version and the specified includes
        ArtifactVersion requestedVersion = new DefaultArtifactVersion(version);
        ArtifactVersion lowerBound;
        ArtifactVersion upperBound;

        switch (olderIncludes) {
            case MAJOR:
                lowerBound = new DefaultArtifactVersion("0.0.0");
                break;
            case MINOR:
                lowerBound = new DefaultArtifactVersion(requestedVersion.getMajorVersion() + ".0.0");
                break;
            case INCREMENTAL:
                lowerBound = new DefaultArtifactVersion(requestedVersion.getMajorVersion() + "."
                        + requestedVersion.getMinorVersion() + ".0");
                break;
            case NONE:
                lowerBound = new DefaultArtifactVersion(requestedVersion.getMajorVersion() + "."
                        + requestedVersion.getMinorVersion() + "." + requestedVersion.getIncrementalVersion());
                break;
            default:
                throw new IllegalStateException("" + olderIncludes);
        }
        switch (newerIncludes) {
            case MAJOR:
                upperBound = null;
                break;
            case MINOR:
                upperBound = new DefaultArtifactVersion((requestedVersion.getMajorVersion() + 1) + ".0.0");
                break;
            case INCREMENTAL:
                upperBound = new DefaultArtifactVersion(requestedVersion.getMajorVersion() + "."
                        + (requestedVersion.getMinorVersion() + 1) + ".0");
                break;
            case NONE:
                upperBound = new DefaultArtifactVersion(requestedVersion.getMajorVersion() + "."
                        + requestedVersion.getMinorVersion() + "." + (requestedVersion.getIncrementalVersion() + 1));
                break;
            default:
                throw new IllegalStateException("" + newerIncludes);
        }
        Restriction restriction = new Restriction(lowerBound, true, upperBound, false);
        VersionRange restrictionRange = VersionRange.createFromVersionSpec(restriction.toString());

        // filter the list
        List<Reference> results = new ArrayList<Reference>();
        for (Reference artifact : artifacts) {
            VersionRange subjectVersionRange = VersionRange.createFromVersionSpec(artifact.to.getVersion());
            VersionRange intersectionVersionRange = subjectVersionRange.restrict(restrictionRange);

            boolean isIncluded = false;
            if (!intersectionVersionRange.getRestrictions().isEmpty()) {
                if (subjectVersionRange.getRecommendedVersion() != null) {
                    isIncluded = intersectionVersionRange.containsVersion(subjectVersionRange.getRecommendedVersion());
                } else {
                    isIncluded = true;
                }
            }

            if (isIncluded) {
                results.add(artifact);
            }
        }

        return results;
    }

    public enum Includes {
        NONE, INCREMENTAL, MINOR, MAJOR;

        public String toString() {
            if (this == INCREMENTAL) {
                return "incremental";
            }
            if (this == MINOR) {
                return "incremental/minor";
            }
            if (this == MAJOR) {
                return "incremental/minor/major";
            }
            return super.toString();
        }
    }

    //--

    public List<Document> substring(String substring) throws IOException {
        return query(new WildcardQuery(new Term(Database.GAV, "*" + substring + "*")));
    }

    public List<Document> query(String queryString) throws IOException, QueryNodeException {
        return query(new StandardQueryParser().parse(queryString, Database.GAV));
    }

    public List<Document> query(Query query) throws IOException {
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

}
