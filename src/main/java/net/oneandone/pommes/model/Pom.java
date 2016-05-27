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
import net.oneandone.sushi.util.Strings;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;

public class Pom {
    public static Pom forComposer(String origin, String revision, Node composer) {
        return new Pom(origin, revision, new GAV("1and1-sales", composer.getParent().getParent().getName(), "0"), "");
    }

    public static Pom forProject(String origin, String revision, MavenProject project) {
        Pom result;
        String scm;

        scm = null;
        if (project.getScm() != null) {
            scm = project.getScm().getConnection();
        }
        if (scm == null) {
            scm = "";
        } else {
            // removeOpt because I've seen project that omit the prefix ...
            scm = Strings.removeLeftOpt(scm, "scm:");
        }
        result = new Pom(origin, revision, new GAV(project.getGroupId(), project.getArtifactId(), project.getVersion()), scm);
        for (Dependency dependency : project.getDependencies()) {
            result.dependencies.add(GAV.forDependency(dependency));
        }
        return result;
    }

    //--

    /** currently always starts with svn:https:// */
    public final String origin;
    public final String revision;

    public final GAV coordinates;

    public final String scm;

    public final List<GAV> dependencies;

    public Pom(String origin, String revision, GAV coordinates, String scm) {
        if (origin == null || origin.endsWith("/")) {
            throw new IllegalArgumentException(origin);
        }
        this.origin = origin;
        this.revision = revision;
        this.coordinates = coordinates;
        this.scm = scm;
        this.dependencies = new ArrayList<>();
    }

    public String toLine() {
        return coordinates.toGavString() + " @ " + scm;
    }

    @Override
    public String toString() {
        return toLine();
    }
}
