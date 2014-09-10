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

import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.io.OS;
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
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.Restriction;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
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
            throw new ArgumentException("invalid url for global pommes database file: " + global, e);
        }
    }

    //--

    public static final String SCM_SVN = "scm:svn:";

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
    public static final String GAV = "gav";

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

    public Database updateOpt() throws IOException {
        if (global != null) {
            update();
        }
        return this;
    }

    public void update() throws IOException {
        FileNode zip;

        if (global == null) {
            throw new IllegalStateException();
        }
        if (!directory.exists() || directory.getLastModified() - System.currentTimeMillis() > 1000L * 60 * 60 * 24) {
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

    public static Document document(String origin, Pom pom) throws InvalidVersionSpecificationException {
        Document doc;

        doc = new Document();
        doc.add(new StringField(ORIGIN, origin, Field.Store.YES));
        doc.add(new StringField(GROUP, pom.coordinates.groupId, Field.Store.YES));
        doc.add(new StringField(ARTIFACT, pom.coordinates.artifactId, Field.Store.YES));
        doc.add(new StringField(VERSION, pom.coordinates.version, Field.Store.YES));
        doc.add(new StringField(GA, pom.coordinates.toGaString(), Field.Store.YES));
        doc.add(new StringField(GAV, pom.coordinates.toGavString(), Field.Store.YES));
        return doc;
    }

    public static Document document(String origin, MavenProject mavenProject) throws InvalidVersionSpecificationException {
        Document doc;
        MavenProject parent;
        Pom parPom;

        doc = document(origin, Pom.forProject(origin, mavenProject));
        for (Dependency dependency : mavenProject.getDependencies()) {
            net.oneandone.pommes.model.GAV dep = net.oneandone.pommes.model.GAV.forDependency(dependency);

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

    //--

    /**
     * TODO: git support
     * @return urls with tailing slash
     */
    public List<String> list(String urlPrefix) throws IOException {
        List<String> result;
        String origin;
        String prefix;
        Document doc;

        result = new ArrayList<>();
        prefix = Database.SCM_SVN + urlPrefix;
        try (IndexReader reader = DirectoryReader.open(getIndexLuceneDirectory())) {
            int numDocs = reader.numDocs();
            for (int i = 0; i < numDocs; i++) {
                doc = reader.document(i);
                origin = doc.get(Database.ORIGIN);
                if (origin.startsWith(prefix)) {
                    result.add(origin);
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

    public List<Pom> search(String substring) throws IOException {
        List<Pom> result;
        Document document;
        Pom pom;
        IndexReader reader;

        result = new ArrayList<>();
        reader = DirectoryReader.open(getIndexLuceneDirectory());
        int numDocs = reader.numDocs();
        for (int i = 0; i < numDocs; i++) {
            document = reader.document(i);
            pom = toPom(document);
            if (pom.toLine().contains(substring)) {
                result.add(pom);
            }
        }
        reader.close();
        return result;
    }

    public static Pom toPom(Document document) {
        net.oneandone.pommes.model.GAV c;

        c = net.oneandone.pommes.model.GAV.forGav(document.get(Database.GAV));
        return new Pom(document.get(Database.ORIGIN), new GAV(c.groupId, c.artifactId, c.version));
    }

    //-- searching

    public net.oneandone.pommes.model.GAV findLatestVersion(net.oneandone.pommes.model.GAV gav) throws IOException {
        List<Reference> list;

        list = queryGa(Database.GA, gav.toGaString(), null);
        if (list.isEmpty()) {
            throw new IllegalStateException("Artifact " + gav.toGavString() + " not found.");
        }
        return Collections.max(list).from;
    }

    public List<Reference> findDependeesIgnoringVersion(String groupId, String artifactId) throws IOException {
        GAV gav;

        gav = new GAV(groupId, artifactId, "");
        return queryGa(Database.DEP_GA, gav.toGaString(), Database.DEP_GAV);
    }

    public List<Reference> findChildrenIgnoringVersion(String groupId, String artifactId) throws IOException {
        GAV gav;

        gav = new GAV(groupId, artifactId, "");
        return queryGa(Database.PAR_GA, gav.toGaString(), Database.PAR_GAV);
    }

    private List<Reference> queryGa(String gaField, String gaValue, String gavField) throws IOException {
        List<Reference> list;
        GAV from;
        GAV to;

        list = new ArrayList<>();
        for (Document doc : doQuery(new TermQuery(new Term(gaField, gaValue)))) {
            from = toPom(doc).coordinates;
            to = gavField == null ? null : getReference(doc, gavField, gaValue);
            list.add(new Reference(doc, from, to));
        }
        return list;
    }

    private net.oneandone.pommes.model.GAV getReference(Document doc, String gavField, String gaValue) {
        net.oneandone.pommes.model.GAV result;
        net.oneandone.pommes.model.GAV gav;

        result = null;
        for (IndexableField field : doc.getFields(gavField)) {
            gav = net.oneandone.pommes.model.GAV.forGav(field.stringValue());
            if (gav.toGaString().equals(gaValue)) {
                if (result != null) {
                    throw new IllegalStateException("ambiguous: " + result + " vs " + gav);
                }
                result = gav;
            }
        }
        if (result == null) {
            throw new IllegalStateException();
        }
        return result;
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
            net.oneandone.pommes.model.GAV gav = ubi.from;
            net.oneandone.pommes.model.GAV latest = findLatestVersion(gav);
            if (!gav.gavEquals(latest)) {
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
        String lowerVersion = artifacts.get(0).from.version;
        String upperVersion = lowerVersion;

        Iterator<Reference> iterator = artifacts.iterator();
        while (iterator.hasNext()) {
            Reference ubi = iterator.next();

            boolean addPrevious = false;
            if (ubi.from.toGaString().equals(previous.from.toGaString())
                    && ubi.to.toGavString().equals(previous.to.toGavString())) {
                // as long as artifact.ga is equal and reference.gav is equal
                upperVersion = ubi.from.version;
            } else {
                addPrevious = true;
            }

            if (addPrevious) {
                addAggregated(results, previous, lowerVersion, upperVersion);

                // reset versions
                lowerVersion = ubi.from.version;
                upperVersion = lowerVersion;
            }

            if (!iterator.hasNext()) {
                addAggregated(results, ubi, lowerVersion, ubi.from.version);
            }

            previous = ubi;
        }

        return results;
    }

    private void addAggregated(List<Reference> results, Reference ubi, String lowerVersion, String upperVersion) {
        net.oneandone.pommes.model.GAV previous;
        String version;
        net.oneandone.pommes.model.GAV aggregated;

        previous = ubi.from;
        version = lowerVersion;
        if (!lowerVersion.equals(upperVersion)) {
            version = "[" + lowerVersion + "," + upperVersion + "]";
        }
        aggregated = new net.oneandone.pommes.model.GAV(previous.groupId, previous.artifactId, version);
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
        List<Reference> results = new ArrayList<>();
        for (Reference artifact : artifacts) {
            VersionRange subjectVersionRange = VersionRange.createFromVersionSpec(artifact.to.version);
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

    public List<Pom> substring(Origin origin, String substring) throws IOException {
        BooleanQuery q;

        q = new BooleanQuery();
        q.setMinimumNumberShouldMatch(1);
        switch (origin) {
            case ANY:
                // do nothing
                break;
            case TRUNC:
                q.add(new WildcardQuery(new Term(Database.ORIGIN, "*/trunk/*")), BooleanClause.Occur.MUST);
                break;
            case BRANCH:
                q.add(new WildcardQuery(new Term(Database.ORIGIN, "*/branches/*")), BooleanClause.Occur.MUST);
                break;
            default:
                throw new IllegalStateException(origin.toString());
        }
        q.add(new WildcardQuery(new Term(Database.GAV, "*" + substring + "*")), BooleanClause.Occur.SHOULD);
        q.add(new WildcardQuery(new Term(Database.ORIGIN, "*" + substring + "*")), BooleanClause.Occur.SHOULD);
        return query(q);
    }

    //--

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

    public List<Pom> query(String queryString) throws IOException, QueryNodeException {
        return query(new StandardQueryParser().parse(queryString, Database.GAV));
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

}
