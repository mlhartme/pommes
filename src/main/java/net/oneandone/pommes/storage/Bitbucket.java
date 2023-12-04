package net.oneandone.pommes.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.oneandone.sushi.fs.NewInputStreamException;
import net.oneandone.sushi.fs.http.HttpNode;
import net.oneandone.sushi.fs.http.HttpRoot;
import net.oneandone.sushi.fs.http.MovedTemporarilyException;
import net.oneandone.sushi.fs.http.StatusException;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class Bitbucket {
    private final HttpRoot root;

    public Bitbucket(HttpRoot root) {
        this.root = root;
    }

    // TODO: always fails with 404 error ...
    public String getRevision(String project, String repo, String path) throws IOException {
        JsonObject result;

        result = getJsonObject("rest/api/1.0/projects/" + project + "/repos/" + repo + "/last-modified/" + path, "");
        return result.get("id").getAsString();
    }

    public byte[] readBytes(String project, String repo, String path) throws IOException {
        String location;
        HttpNode node;

        node = root.node("projects/" + project + "/repos/" + repo + "/browse/" + path, "raw");
        while (true) {
            try {
                // try to load. To see if we ne a redirected location
                return node.readBytes();
            } catch (NewInputStreamException e) {
                if (e.getCause() instanceof MovedTemporarilyException) {
                    location = ((MovedTemporarilyException) e.getCause()).location;
                    try {
                        node = (HttpNode) node.getWorld().node(location);
                    } catch (URISyntaxException e1) {
                        throw new IOException("cannot redirect to location " + location, e);
                    }
                    continue;
                } else {
                    throw e;
                }
            }
        }
    }

    public List<String> listRepos(String project) throws IOException {
        List<String> result;

        result = new ArrayList<>();
        for (JsonElement repo : getPaged("rest/api/1.0/projects/" + project + "/repos")) {
            result.add(repo.getAsJsonObject().get("slug").getAsString());
        }
        return result;
    }

    public List<String> listRoot(String project, String repo) throws IOException {
        String path;
        List<JsonElement> all;
        List<String> result;


        result = new ArrayList<>();
        try {
            all = getPaged("rest/api/1.0/projects/" + project + "/repos/" + repo + "/files/");
        } catch (NewInputStreamException e) {
            if (e.getCause() instanceof StatusException cause) {
                if (cause.getStatusLine().code == 401) {
                    // happens if the repository is still empty
                    return result;
                }
            }
            throw e;
        }

        for (JsonElement file : all) {
            path = file.getAsString();
            if (!path.contains("/")) {
                result.add(path);
            }
        }
        return result;
    }

    //--

    private List<JsonElement> getPaged(String path) throws IOException {
        int perPage = 25;
        JsonObject paged;
        List<JsonElement> result;

        result = new ArrayList<>();
        for (int i = 0; true; i += perPage) {
            paged = getJsonObject(path, "limit=" + perPage + "&start=" + i);
            for (JsonElement value : paged.get("values").getAsJsonArray()) {
                result.add(value);
            }
            if (paged.get("isLastPage").getAsBoolean()) {
                break;
            }
        }
        return result;
    }

    private JsonObject getJsonObject(String path, String params) throws IOException {
        try (Reader src = root.node(path, params).newReader()) {
            return JsonParser.parseReader(src).getAsJsonObject();
        }
    }
}
