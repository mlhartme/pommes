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
package net.oneandone.pommes.cli;

import net.oneandone.maven.embedded.Maven;
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.model.GroupArtifactVersion;
import net.oneandone.pommes.model.Reference;
import net.oneandone.pommes.model.Database;
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.util.Strings;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shows artifacts that use the artifact of interest. This includes dependees (reverse dependencies) and children
 * (reverse parent).<br/>
 * The artifact of interest can be defined by -DgroupId, -DartifactId, and -Dversion. Use -Dversion=latest to find the
 * latest version of the artifact. If started within a Maven project the coordinates are preset with the project's
 * coordinates.<br/>
 * The included versions and output format can be controlled using -Dinclude and -Dformat.
 * You can specifiy which versions differences shall be analyzed by -Dinclude=[major|minor|incremental|none]
 */
public class Users extends SearchBase<Reference> {
    public static enum OutputFormat {
        COMPACT, AGGREGATED, FULL;
    }

    private String gavString;

    @Remaining
    public void remaining(String str) {
        if (gavString != null) {
            throw new ArgumentException("too mamy arguments");
        }
        gavString = str;
    }

    @Option("include")
    protected Database.Includes includes = Database.Includes.MAJOR;

    @Option("format")
    protected OutputFormat format = OutputFormat.COMPACT;

    public Users(Console console, Maven maven) {
        super(console, maven);
    }

    public List<Reference> search() throws IOException, InvalidVersionSpecificationException, ProjectBuildingException {
        List<Reference> result;
        GroupArtifactVersion gav;
        MavenProject project;

        result = new ArrayList<>();
        try (Database database = Database.load(console.world).updateOpt()) {
            if (gavString == null) {
                project = maven.loadPom(console.world.file("pom.xml"));
                gav = new GroupArtifactVersion(project.getGroupId(), project.getArtifactId(), project.getVersion());
            } else {
                if (Strings.count(gavString, ":") == 2) {
                    gavString = gavString + ":0";
                }
                gav = new GroupArtifactVersion(gavString);
            }

            console.verbose.println("Analyzing " + gav + ", including " + includes + " versions.");

            //
            //
            // dependencies
            List<Reference> allDeps = database.findDependeesIgnoringVersion(gav.getGroupId(), gav.getArtifactId());
            Collections.sort(allDeps);

            // if only the latest using artifacts should be shown we can remove any older versions from the full list
            if (format == OutputFormat.COMPACT) {
                allDeps = database.filterLatest(allDeps);
            }

            // extract artifacts using the exact version (or within a range) of the artifact of interest
            List<Reference> exactDeps = database.filterByReferencedArtifacts(allDeps, gav.getVersion(), Database.Includes.NONE, Database.Includes.NONE);
            // for the compact output format we need to aggregate the using artifacts separately for each group
            if (format == OutputFormat.AGGREGATED) {
                exactDeps = database.aggregateArtifacts(exactDeps);
            }
            result.addAll(exactDeps);

            // extract artifacts using an older version of the artifact of interest
            List<Reference> olderDeps = database.filterByReferencedArtifacts(allDeps, gav.getVersion(), includes, Database.Includes.NONE);
            if (format == OutputFormat.AGGREGATED) {
                olderDeps = database.aggregateArtifacts(olderDeps);
            }
            olderDeps.removeAll(exactDeps);
            result.addAll(olderDeps);

            // extract artifacts using an newer version of the artifact of interest
            List<Reference> newerDeps = database.filterByReferencedArtifacts(allDeps, gav.getVersion(), Database.Includes.NONE,
                    includes);
            if (format == OutputFormat.AGGREGATED) {
                newerDeps = database.aggregateArtifacts(newerDeps);
            }
            newerDeps.removeAll(exactDeps);
            result.addAll(newerDeps);

            //
            //
            // parent-child relationship
            List<Reference> allChildren = database.findChildrenIgnoringVersion(gav.getGroupId(), gav.getArtifactId());
            Collections.sort(allChildren);

            // if only the latest using artifacts should be shown we can remove any older versions from the full list
            if (format == OutputFormat.COMPACT) {
                allChildren = database.filterLatest(allChildren);
            }

            // extract artifacts using the exact version (or within a range) of the artifact of interest
            List<Reference> exactChildren = database.filterByReferencedArtifacts(allChildren, gav.getVersion(), Database.Includes.NONE, Database.Includes.NONE);
            // for the compact output format we need to aggregate the using artifacts separately for each group
            if (format == OutputFormat.AGGREGATED) {
                exactChildren = database.aggregateArtifacts(exactChildren);
            }
            result.addAll(exactChildren);

            // extract artifacts using an older version of the artifact of interest
            List<Reference> olderChildren = database.filterByReferencedArtifacts(allChildren, gav.getVersion(), includes, Database.Includes.NONE);
            if (format == OutputFormat.AGGREGATED) {
                olderChildren = database.aggregateArtifacts(olderChildren);
            }
            olderChildren.removeAll(exactChildren);
            result.addAll(olderChildren);

            // extract artifacts using an newer version of the artifact of interest
            List<Reference> newerChildren = database.filterByReferencedArtifacts(allChildren, gav.getVersion(), Database.Includes.NONE, includes);
            if (format == OutputFormat.AGGREGATED) {
                newerChildren = database.aggregateArtifacts(newerChildren);
            }
            newerChildren.removeAll(exactChildren);
            result.addAll(newerChildren);
        }
        return result;
    }

    @Override
    public Pom toPom(Reference reference) {
        return Database.toPom(reference.document);
    }

    @Override
    public String toLine(Reference reference) {
        return reference.from.toGavString() + " -> " + reference.to.toGavString();
    }
}
