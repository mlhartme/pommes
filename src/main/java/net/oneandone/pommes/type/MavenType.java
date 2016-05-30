package net.oneandone.pommes.type;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Gav;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.io.IOException;

public class MavenType extends Type {
    public static MavenType probe(Node node) {
        String name;

        name = node.getName();
        if (name.equals("pom.xml") || name.endsWith(".pom")) {
            return new MavenType(node);
        }
        return null;
    }

    public MavenType(Node node) {
        super(node);
    }

    //--

    @Override
    public Pom createPom(Environment environment) throws IOException {
        FileNode local;
        MavenProject project;
        Artifact pa;
        Gav paGav;
        Pom pom;

        local = null;
        try {
            if (node instanceof FileNode) {
                local = (FileNode) node;
            } else {
                local = environment.world().getTemp().createTempFile();
            }
            node.copyFile(local);
            try {
                project = environment.maven().loadPom(local);
            } catch (ProjectBuildingException e) {
                throw new IOException(node + ": cannot load maven project: " + e.getMessage(), e);
            }

            pa = project.getParentArtifact();
            paGav = pa != null ? Gav.forArtifact(pa) : null;
            pom = new Pom(getOrigin(), getRevision(), paGav, Gav.forArtifact(project.getArtifact()), scm(project));
            for (Dependency dependency : project.getDependencies()) {
                pom.dependencies.add(Gav.forDependency(dependency));
            }
            return pom;
        } finally {
            if (local != node) {
                local.deleteFile();
            }
        }
    }

    private static String scm(MavenProject project) {
        String scm;

        if (project.getScm() != null) {
            scm = project.getScm().getConnection();
            if (scm != null) {
                // removeOpt because I've seen project that omit the prefix ...
                scm = Strings.removeLeftOpt(scm, "scm:");
                return scm;
            }
        }
        return null;
    }
}
