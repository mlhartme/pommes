package net.oneandone.pommes.project;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;

public class JsonProject extends Project {
    public static JsonProject probe(Node node) throws IOException {
        String name;

        name = node.getName();
        if (name.equals(".pommes.json") || name.equals("pommes.json")) {
            return new JsonProject(new JsonParser().parse(node.readString()).getAsJsonObject());
        }
        return null;
    }

    private final JsonObject json;

    public JsonProject(JsonObject json) {
        super(null);
        this.json = json;
    }

    @Override
    public Pom load(Environment environment) throws IOException {
        return environment.gson().fromJson(json, Pom.class);
    }
}
