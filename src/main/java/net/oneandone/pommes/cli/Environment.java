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
import net.oneandone.pommes.model.Variables;
import net.oneandone.pommes.mount.Fstab;
import net.oneandone.pommes.mount.Point;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.io.IOException;

public class Environment implements Variables {
    private final World world;
    public final Maven maven;

    private MavenProject lazyProject;
    private Fstab lazyFstab;

    public Environment(World world, Maven maven) {
        this.world = world;
        this.maven = maven;
    }

    public String lookup(String name) throws IOException {
        MavenProject p;
        FileNode here;
        Point point;

        switch (name) {
            case "svn":
                here = (FileNode) world.getWorking();
                point = fstab().pointOpt(here);
                if (point == null) {
                    throw new IllegalArgumentException("no mount point for directory " + here.getAbsolute());
                }
                return point.svnurl(here);
            case "gav":
                p = project();
                return p.getGroupId() + ":" + p.getArtifactId() + ":" + p.getVersion();
            case "ga":
                p = project();
                return p.getGroupId() + ":" + p.getArtifactId();
            default:
                return null;
        }
    }

    public Fstab fstab() throws IOException {
        if (lazyFstab == null) {
            lazyFstab = Fstab.load(world);
        }
        return lazyFstab;
    }

    public MavenProject project() throws IOException {
        if (lazyProject == null) {
            try {
                lazyProject = maven.loadPom((FileNode) world.getWorking().join("pom.xml"));
            } catch (ProjectBuildingException e) {
                throw new IOException("cannot load pom: " + e.getMessage(), e);
            }
        }
        return lazyProject;
    }
}
