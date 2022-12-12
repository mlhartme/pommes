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
package net.oneandone.pommes.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.http.HttpNode;
import net.oneandone.sushi.fs.http.model.HeaderList;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiFunction;

public class GitlabRepository extends Repository {
    private static final String PROTOCOL = "gitlab:";

    private final Environment environment;
    private final ObjectMapper mapper;
    private final HttpNode root;

    public GitlabRepository(Environment environment, String name, String url, String token) throws NodeInstantiationException {
        super(name);
        this.environment = environment;
        this.mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        root = (HttpNode) environment.world().validNode(url).join("api/v4");
        if (token != null) {
            // https://docs.gitlab.com/ee/api/#personalprojectgroup-access-tokens
            root.getRoot().addExtraHeader("PRIVATE-TOKEN", token);
        }
    }

    public void addOption(String option) {
        throw new ArgumentException("unknown option: " + option);
    }

    // https://docs.gitlab.com/ee/api/groups.html#list-a-groups-projects
    public List<GitlabProject> listGroupProjects(String group) throws IOException {
        List<GitlabProject> result;
        List<GitlabProject> step;
        String str;
        HttpNode url;

        result = new ArrayList<>();
        int pageSize = 80;
        url = root.join("groups", group, "projects")
                .withParameter("include_subgroups", "true")
                .withParameter("archived", "false")
                .withParameter("with_shared", "false");
        for (int page = 1; true; page++) {
            str = url.withParameter("page", page).withParameter("per_page", pageSize).readString();
            step = mapper.readValue(str, new TypeReference<>() {});
            result.addAll(step);
            if (step.size() < pageSize) {
                break;
            }
        }
        return result;
    }

    public List<GitlabProject> listProjects() throws IOException {
        List<GitlabProject> result;
        List<GitlabProject> step;
        String str;
        HttpNode url;

        result = new ArrayList<>();
        int pageSize = 80;
        url = root.join("projects");
        for (int page = 1; true; page++) {
            str = url.withParameter("page", page).withParameter("per_page", pageSize).readString();
            step = mapper.readValue(str, new TypeReference<>() {});
            result.addAll(step);
            if (step.size() < pageSize) {
                break;
            }
        }
        return result;
    }

    public GitlabProject getProject(long id) throws IOException {
        HttpNode url;

        url = root.join("projects", Long.toString(id));
        return mapper.readValue(url.readString(), GitlabProject.class);
    }

    public List<String> list(GitlabProject project) throws IOException {
        HttpNode url;
        List<TreeItem> items;

        url = root.join("projects", Long.toString(project.id()), "repository/tree");
        url.withParameter("per_page", 100);
        url.withParameter("ref", project.default_branch());
        items = mapper.readValue(url.readString(), new TypeReference<>() {});
        return items.stream().map((item) -> "blob".equals(item.type()) ? item.name() : null).filter(Objects::nonNull).toList();
    }

    public void addExclude(String exclude) {
        throw new ArgumentException("excludes not supported: " + exclude);
    }

    @Override
    public void scan(BlockingQueue<Descriptor> dest) throws IOException, URISyntaxException, InterruptedException {
    }

    public Descriptor scanOpt(GitlabProject project) throws IOException {
        BiFunction<Environment, Node<?>, Descriptor> m;
        Node<?> node;
        Descriptor result;

        for (String name : list(project)) {
            m = Descriptor.match(name);
            if (m != null) {
                System.out.println("name " + name);
                // TODO: when to delete?
                node = environment.world().getTemp().createTempFile();
                HttpNode url = root.join("projects", Long.toString(project.id()), "repository/files", name, "raw");
                url = url.withParameter("ref", project.default_branch());
                url.copyFile(node);
                result = m.apply(environment, node);
                result.setRepository(this.name);
                result.setPath(project.path_with_namespace() + "/" + name);
                result.setRevision("TODO");
                result.setScm(project.ssh_url_to_repo());
                return result;
            }
        }
        return null;
    }

    //--


    public record TreeItem(String id, String name, String type, String path, String mode) { }

    public record GitlabProject(String default_branch, int id, String path, String path_with_namespace,
                                String ssh_url_to_repo, String http_url_to_repo, String web_url) {
    }
}
