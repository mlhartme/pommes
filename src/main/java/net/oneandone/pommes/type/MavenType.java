package net.oneandone.pommes.type;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import org.apache.lucene.document.Document;
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

    private final Node node;

    public MavenType(Node node) {
        this.node = node;
    }


    //--

    @Override
    public Document createDocument(String origin, String revision, Environment environment) throws IOException {
        FileNode local;
        MavenProject project;

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
            return Database.document(origin, revision, project);
        } finally {
            if (local != node) {
                local.deleteFile();
            }
        }
    }
}
