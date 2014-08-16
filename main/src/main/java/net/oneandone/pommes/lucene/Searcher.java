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

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.builders.StandardQueryBuilder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.Restriction;
import org.apache.maven.artifact.versioning.VersionRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Searcher {
    private final IndexReader reader;
    private final IndexSearcher searcher;

    public Searcher(Database database) throws IOException {
        this.reader = DirectoryReader.open(database.getIndexLuceneDirectory());
        this.searcher = new IndexSearcher(reader);
    }

    public void close() throws IOException {
        reader.close();
    }

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

        search = searcher.search(query, 100000);
        list = new ArrayList<>();
        for (ScoreDoc scoreDoc : search.scoreDocs) {
            list.add(reader.document(scoreDoc.doc));
        }
        return list;
    }

}
