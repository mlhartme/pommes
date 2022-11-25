package net.oneandone.pommes.database;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.oneandone.maven.embedded.Maven;
import net.oneandone.pommes.descriptor.MavenDescriptor;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.http.HttpNode;
import org.apache.lucene.document.Document;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class CentralSearch {
    private final World world;
    private final Maven maven;

    public CentralSearch(World world, Maven maven) {
        this.world = world;
        this.maven = maven;
    }

    public List<Document> query(List<String> query) throws IOException {
        HttpNode search;
        JsonObject json;
        List<Document> result;

        search = (HttpNode) world.validNode("https://search.maven.org/solrsearch/select");
        search = search.withParameter("q", centralQueryStr(query));
        search = search.withParameter("rows", "20");
        search = search.withParameter("wt", "json");
        try (Reader src = search.newReader()) {
            json = JsonParser.parseReader(src).getAsJsonObject();
        }
        json = getObject(json, "response");
        result = new ArrayList();
        for (JsonElement element : get(json, "docs").getAsJsonArray()) {
            result.add(document(element.getAsJsonObject()));
        }
        return result;
    }

    // TODO: return project instead -- and merge with MavenDescriotor code
    private Document document(JsonObject obj) throws IOException {
        Artifact artifact;
        MavenProject p;
        Project pommes;

        artifact = new DefaultArtifact(getString(obj, "g"), getString(obj, "a"), "pom", getString(obj, "latestVersion"));
        try {
            p = maven.loadPom(artifact);
        } catch (ProjectBuildingException | RepositoryException e) {
            throw new IOException("failed to resolve " + artifact + ": " + e.getMessage(), e);
        }
        pommes = MavenDescriptor.mavenToPommesProject(p, "repoTODO", "originTODO", "revisionTODO", MavenDescriptor.scmOpt(p));
        return Field.document(pommes);
    }

    private static String getString(JsonObject obj, String member) throws IOException {
        return get(obj, member).getAsString();
    }

    private static JsonObject getObject(JsonObject obj, String member) throws IOException {
        return get(obj, member).getAsJsonObject();
    }

    private static JsonElement get(JsonObject obj, String member) throws IOException {
        JsonElement result = obj.get(member);
        if (result == null) {
            throw new IOException("member not found: " + member);
        }
        return result;
    }

    private static String centralQueryStr(List<String> query) {
        StringBuilder result;

        result = new StringBuilder();
        for (String q : query) {
            if (!result.isEmpty()) {
                result.append(" AND ");
            }
            result.append(q);
        }
        return result.toString();
    }
}
