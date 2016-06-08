package net.oneandone.pommes.project;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;

public class JsonProject extends Project {
    public static JsonProject probe(Gson gson, Node node) throws IOException {
        String name;
        JsonObject json;

        name = node.getName();
        if (name.equals(".pommes.json") || name.equals("pommes.json")) {
            json = new JsonParser().parse(node.readString()).getAsJsonObject();
            return new JsonProject(gson.fromJson(json, Pom.class));
        }
        return null;
    }

    private final Pom orig;

    public JsonProject(Pom orig) {
        this.orig = orig;
    }

    @Override
    protected Pom doLoad(Environment environment, String zone, String origin, String revision) throws IOException {
        Pom pom;

        pom = new Pom(zone, origin, revision, orig.parent, orig.coordinates, orig.scm, orig.url);
        pom.dependencies.addAll(orig.dependencies);
        return pom;
    }
}
