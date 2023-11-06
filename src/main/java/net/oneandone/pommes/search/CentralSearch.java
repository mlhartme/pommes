/*
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
package net.oneandone.pommes.search;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.oneandone.maven.summon.api.Maven;
import net.oneandone.pommes.database.PommesQuery;
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.descriptor.MavenDescriptor;
import net.oneandone.pommes.scm.ScmUrl;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.http.HttpNode;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CentralSearch {
    private final World world;
    private final Maven maven;

    public CentralSearch(World world, Maven maven) {
        this.world = world;
        this.maven = maven;
    }

    public List<Project> query(PommesQuery query) throws IOException {
        List<Project> result;
        int idx;

        result = new ArrayList<>();
        for (String str : query.toCentral()) {
            result.addAll(query(str));
        }
        idx = query.getIndex();
        if (idx > 0) {
            if (idx <= result.size()) {
                return Collections.singletonList(result.get(idx - 1));
            } else {
                return Collections.emptyList();
            }
        } else {
            return result;
        }
    }

    public List<Project> query(String query) throws IOException {
        HttpNode search;
        JsonObject json;
        List<Project> result;
        Project project;

        search = (HttpNode) world.validNode("https://search.maven.org/solrsearch/select");
        search = search.withParameter("q", query);
        search = search.withParameter("rows", "20");
        search = search.withParameter("wt", "json");
        try (Reader src = search.newReader()) {
            json = JsonParser.parseReader(src).getAsJsonObject();
        }
        json = getObject(json, "response");
        result = new ArrayList<>();
        for (JsonElement element : get(json, "docs").getAsJsonArray()) {
            JsonObject obj = element.getAsJsonObject();
            Artifact artifact = new DefaultArtifact(getString(obj, "g"), getString(obj, "a"), "pom", getString(obj, "latestVersion"));
            try {
                project = projectOpt(artifact);
                if (project != null) {
                    result.add(project);
                }
            } catch (Exception e) {
                // TODO
                System.out.println("cannot load pom " + artifact + ": " + e.getMessage());
            }
        }
        return result;
    }

    private Project projectOpt(Artifact artifact) throws IOException {
        MavenProject p;
        ScmUrl scm;

        try {
            p = maven.loadPom(artifact);
        } catch (ProjectBuildingException | RepositoryException e) {
            throw new IOException("failed to resolve " + artifact + ": " + e.getMessage(), e);
        }
        scm = MavenDescriptor.scmOpt(p);
        if (scm == null) {
            System.out.println("TODO: no scm: " + artifact);
            return null;
        }
        return MavenDescriptor.mavenToPommesProject(p, "central", p.getFile().getPath(), p.getVersion(), scm);
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
}
