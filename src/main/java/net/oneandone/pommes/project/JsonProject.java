package net.oneandone.pommes.project;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;

public class JsonProject extends Project {
    public static boolean matches(String name) throws IOException {
        return name.equals(".pommes.json") || name.equals("pommes.json");
    }

    public static JsonProject create(Environment environment, Node node) {
        JsonObject json;

        try {
            json = new JsonParser().parse(node.readString()).getAsJsonObject();
        } catch (IOException e) {
            throw new RuntimeException("TODO", e);
        }
        return new JsonProject(environment.gson().fromJson(json, Pom.class));
    }

    //--

    private final Pom orig;

    public JsonProject(Pom orig) {
        this.orig = orig;
    }

    @Override
    protected Pom doLoad(Environment environment, String zone, String origin, String revision) throws IOException {
        Pom pom;

        pom = new Pom(zone, origin, revision, orig.parent, orig.artifact, orig.scm, orig.url);
        pom.dependencies.addAll(orig.dependencies);
        return pom;
    }
}
