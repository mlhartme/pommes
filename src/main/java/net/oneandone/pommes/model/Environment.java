package net.oneandone.pommes.model;

import net.oneandone.maven.embedded.Maven;
import net.oneandone.pommes.mount.Fstab;
import net.oneandone.pommes.mount.Point;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.io.IOException;

public class Environment {
    private final World world;
    private final Maven maven;

    private MavenProject lazyProject;
    private Fstab lazyFstab;

    public Environment(World world, Maven maven) {
        this.world = world;
        this.maven = maven;
    }

    public String get(String name) throws IOException {
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
                throw new IllegalArgumentException(name);
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
