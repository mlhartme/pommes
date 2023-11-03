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
package net.oneandone.pommes.descriptor;

import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.database.Gav;
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.scm.ScmUrl;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.io.IOException;

public class MavenDescriptor extends Descriptor {
    protected final Node descriptor;

    public static boolean matches(String name) {
        return name.equals("pom.xml") || name.endsWith(".pom");
    }

    public static MavenDescriptor create(Environment notUsed, Node<?> node) {
        return new MavenDescriptor(node);
    }

    public MavenDescriptor(Node<?> descriptor) {
        this.descriptor = descriptor;
    }

    //--

    @Override
    protected Project doLoad(Environment environment, String repository, String path, String revision, ScmUrl scm) throws IOException {
        FileNode local;
        MavenProject project;

        local = null;
        try {
            if (descriptor instanceof FileNode) {
                local = (FileNode) descriptor;
            } else {
                local = environment.world().getTemp().createTempFile();
                descriptor.copyFile(local);
            }
            try {
                project = environment.maven().loadPom(local.toPath().toFile());
            } catch (ProjectBuildingException e) {
                throw new IOException(descriptor + ": cannot load maven project: " + e.getMessage(), e);
            }

            return mavenToPommesProject(project, repository, path, revision, scm(environment.console(), scm.scmUrl(), project));
        } finally {
            if (local != null && local != descriptor) {
                local.deleteFile();
            }
        }
    }

    public static Project mavenToPommesProject(MavenProject project, String repository, String path, String revision, String scm) {
        Artifact pa;
        Gav paGav;
        Project pommesProject;

        pa = project.getParentArtifact();
        paGav = pa != null ? Gav.forArtifact(pa) : null;
        pommesProject = new Project(repository, path, revision, paGav, Gav.forArtifact(project.getArtifact()), scm, project.getUrl());
        for (Dependency dependency : project.getDependencies()) {
            pommesProject.dependencies.add(Gav.forDependency(dependency));
        }
        return pommesProject;

    }
    private String scm(Console console, String repositoryScm, MavenProject project) throws IOException {
        String pomScm;

        pomScm = scmOpt(project);
        if (repositoryScm != null) {
            if (pomScm != null && !pomScm.equals(repositoryScm)) {
                console.error.println("overriding pom scm " + pomScm + " with " + repositoryScm);
            }
            return repositoryScm;
        }
        if (pomScm == null) {
            throw new IOException("missing scm in pom.xml: " + project.getFile());
        }
        return pomScm;
    }

    public static String scmOpt(MavenProject project) {
        String pomScm;

        if (project.getScm() != null) {
            pomScm = project.getScm().getConnection();
            if (pomScm != null) {
                // removeOpt because I've seen projects that omit the prefix ...
                return Strings.removeLeftOpt(pomScm, "scm:");
            }
        }
        return null;
    }
}
