package net.oneandone.pommes.type;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Gav;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.util.Strings;
import org.apache.lucene.document.Document;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import java.io.IOException;

public abstract class Type {
    // TODO
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
        result = new Pom(origin, revision, new Gav(project.getGroupId(), project.getArtifactId(), project.getVersion()), scm);
        for (Dependency dependency : project.getDependencies()) {
            result.dependencies.add(Gav.forDependency(dependency));
        }
        return result;
    }

    //--

    public static Type probe(Node node) {
        Type result;

        result = MavenType.probe(node);
        if (result != null) {
            return result;
        }
        result = ComposerType.probe(node);
        if (result != null) {
            return result;
        }
        return null;
    }

    public abstract Document createDocument(String origin, String revision, Environment environment) throws IOException;
}
