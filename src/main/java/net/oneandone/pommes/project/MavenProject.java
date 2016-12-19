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
package net.oneandone.pommes.project;

import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.database.Gav;
import net.oneandone.pommes.database.Pom;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.ProjectBuildingException;

import java.io.IOException;

public class MavenProject extends Project {
    protected final Node descriptor;

    public static boolean matches(String name) {
        return name.equals("pom.xml") || name.endsWith(".pom");
    }

    public static MavenProject create(Environment environment, Node node) {
        return new MavenProject(node);
    }

    public MavenProject(Node descriptor) {
        this.descriptor = descriptor;
    }

    //--

    @Override
    protected Pom doLoad(Environment environment, String zone, String origin, String revision, String scm) throws IOException {
        FileNode local;
        org.apache.maven.project.MavenProject project;
        Artifact pa;
        Gav paGav;
        Pom pom;

        local = null;
        try {
            if (descriptor instanceof FileNode) {
                local = (FileNode) descriptor;
            } else {
                local = environment.world().getTemp().createTempFile();
                descriptor.copyFile(local);
            }
            try {
                project = environment.maven().loadPom(local);
            } catch (ProjectBuildingException e) {
                throw new IOException(descriptor + ": cannot load maven project: " + e.getMessage(), e);
            }

            pa = project.getParentArtifact();
            paGav = pa != null ? Gav.forArtifact(pa) : null;
            pom = new Pom(zone, origin, revision, paGav, Gav.forArtifact(project.getArtifact()), scm(environment.console(), scm, project), project.getUrl());
            for (Dependency dependency : project.getDependencies()) {
                pom.dependencies.add(Gav.forDependency(dependency));
            }
            return pom;
        } finally {
            if (local != descriptor) {
                local.deleteFile();
            }
        }
    }

    private String scm(Console console, String repositoryScm, org.apache.maven.project.MavenProject project) throws IOException {
        String pomScm;

        if (project.getScm() != null) {
            pomScm = project.getScm().getConnection();
            if (pomScm != null) {
                // removeOpt because I've seen projects that omit the prefix ...
                pomScm = Strings.removeLeftOpt(pomScm, "scm:");
            }
        } else {
            pomScm = null;
        }
        if (repositoryScm != null) {
            if (pomScm != null && !pomScm.equals(repositoryScm)) {
                console.error.println("overriding pom scm " + pomScm + " with " + repositoryScm);
            }
            return repositoryScm;
        }
        if (pomScm == null) {
            throw new IOException("missing scm in pom.xml");
        }
        return pomScm;
    }
}
