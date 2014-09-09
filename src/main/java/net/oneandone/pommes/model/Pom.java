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
import org.apache.maven.project.MavenProject;

public class Pom {
    public static Pom forComposer(Node composer) {
        return new Pom(composer.getURI().toString(), new GAV("1and1-sales", composer.getParent().getParent().getName(), "0"));
    }

    public static Pom forProject(MavenProject project, String origin) {
        return new Pom(origin, new GAV(project.getGroupId(), project.getArtifactId(), project.getVersion()));
    }

    //--

    public final GAV coordinates;

    public final String origin;

    public Pom(String origin, GAV coordinates) {
        if (origin == null || origin.endsWith("/")) {
            throw new IllegalArgumentException(origin);
        }
        this.origin = origin;
        this.coordinates = coordinates;
    }

    public String toLine() {
        return coordinates.toGavString() + " @ " + origin;
    }

    /**
     * URL to checkout whole project.
     * @return always with tailing slash
     */
    public String projectUrl() {
        return origin.substring(0, origin.lastIndexOf('/') + 1);
    }

    @Override
    public String toString() {
        return toLine();
    }
}
