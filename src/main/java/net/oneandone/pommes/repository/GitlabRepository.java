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
import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.database.Gav;
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.pommes.scm.Git;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.http.HttpNode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiFunction;

public class GitlabRepository extends Repository {
    private static final String PROTOCOL = "gitlab:";

    public static GitlabRepository createOpt(Environment environment, String repository, String url) throws URISyntaxException, IOException {
        if (url.startsWith(PROTOCOL)) {
            return new GitlabRepository(environment, repository, url.substring(PROTOCOL.length()), environment.lib.tokenOpt(PROTOCOL));
        } else {
            return null;
        }
    }


    private final Environment environment;
    private final ObjectMapper mapper;
    private final HttpNode root;

    private final List<String> groupsOrUsers;

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
        this.groupsOrUsers = new ArrayList<>();
    }

    public void addOption(String option) {
        groupsOrUsers.add(option);
    }

    // https://docs.gitlab.com/ee/api/groups.html#list-a-groups-projects
    public List<GitlabProject> listGroupOrUserProjects(String groupOrUsers) throws IOException {
        List<GitlabProject> result;
        List<GitlabProject> step;
        String str;
        HttpNode url;

        result = new ArrayList<>();
        int pageSize = 80;
        if (groupOrUsers.startsWith("~")) {
            url = root.join("users", groupOrUsers.substring(1), "projects");
        } else {
            url = root.join("groups", groupOrUsers, "projects");
        }
        url = url.withParameter("include_subgroups", "true")
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

    public List<GitlabProject> listAllProjects() throws IOException {
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

    public String branchRevision(GitlabProject project, String branch) throws IOException {
        HttpNode url;
        Branch obj;

        url = root.join("projects", Long.toString(project.id()), "repository/branches", branch);
        obj = mapper.readValue(url.readString(), Branch.class);
        return obj.commit.id;
    }

    public GitlabProject getProject(long id) throws IOException {
        HttpNode url;

        url = root.join("projects", Long.toString(id));
        return mapper.readValue(url.readString(), GitlabProject.class);
    }

    public List<String> files(GitlabProject project) throws IOException {
        HttpNode url;
        List<TreeItem> items;

        url = root.join("projects", Long.toString(project.id()), "repository/tree");
        url.withParameter("per_page", 100);
        url.withParameter("ref", or(project.default_branch(), "main"));
        items = mapper.readValue(url.readString(), new TypeReference<>() {});
        return items.stream().map((item) -> "blob".equals(item.type()) ? item.name() : null).filter(Objects::nonNull).toList();
    }

    private static String or(String left, String right) {
        return left != null ? left : right;
    }

    public void addExclude(String exclude) {
        throw new ArgumentException("excludes not supported: " + exclude);
    }

    @Override
    public void scan(BlockingQueue<Descriptor> dest, Console console) throws IOException, URISyntaxException, InterruptedException {
        List<GitlabProject> all;

        if (groupsOrUsers.isEmpty()) {
            console.info.println("collecting projects ...");
            all = listAllProjects();
            console.info.println("scan " + all.size() + " projects");
            for (GitlabProject project : all) {
                scan(project, dest, console);
            }
        } else {
            for (String groupOrUser : groupsOrUsers) {
                for (GitlabProject project : listGroupOrUserProjects(groupOrUser)) {
                    scan(project, dest, console);
                }
            }
        }
    }

    private void scan(GitlabProject project, BlockingQueue<Descriptor> dest, Console console) throws InterruptedException {
        if (Boolean.TRUE.equals(project.archived)) {
            console.verbose.println("skip archived: " + project.path_with_namespace);
            return;
        }
        try {
            var descriptor = scanOpt(project);
            if (descriptor != null) {
                dest.put(descriptor);
            }
        } catch (IOException e) {
            console.error.println("cannot load " + project.path_with_namespace + " (id=" + project.id + "): " + e.getMessage());
            e.printStackTrace(console.verbose);
        }
    }

    private String repoUrl(GitlabProject project) {
        return project.http_url_to_repo(); // TODO: configurable
    }

    public Descriptor scanOpt(GitlabProject project) throws IOException {
        BiFunction<Environment, Node<?>, Descriptor> m;
        Node<?> node;
        Descriptor result;

        for (String name : files(project)) {
            m = Descriptor.match(name);
            if (m != null) {
                // TODO: when to delete node?
                node = environment.world().getTemp().createTempFile();
                HttpNode url = root.join("projects", Long.toString(project.id()), "repository/files", name, "raw");
                url = url.withParameter("ref", project.default_branch());
                url.copyFile(node);
                result = m.apply(environment, node);
                result.setRepository(this.name);
                result.setPath(project.path_with_namespace() + "/" + name);
                result.setRevision(branchRevision(project, project.default_branch())); // TODO: could be more accurate with the revision of this very file ...
                result.setScm(Git.PROTOCOL + repoUrl(project));
                return result;
            }
        }

        Gav gav;
        try {
            gav = Scm.GIT.defaultGav(repoUrl(project));
        } catch (URISyntaxException e) {
            throw new IOException("invalid url", e);
        }
        result = new Descriptor() {
            @Override
            protected Project doLoad(Environment environmentNotUsed, String withRepository, String withOrigin, String withRevision, String withScm) {
                return new Project(name, project.path_with_namespace(), "TODO", null, gav, Git.PROTOCOL + repoUrl(project), project.web_url);
            }
        };
        // TODO: kind of duplication ...
        result.setRepository(name);
        result.setPath(project.path_with_namespace());
        result.setRevision("TODO");
        result.setScm(Git.PROTOCOL + repoUrl(project));
        return result;
    }

    //--

    public record TreeItem(String id, String name, String type, String path, String mode) { }

    public record GitlabProject(String default_branch, int id, String path, String path_with_namespace,
                                String ssh_url_to_repo, String http_url_to_repo, String web_url, String visibility, Boolean archived) {
    }
    public record Branch(String name, Commit commit) {
    }

    public record Commit(String id, String short_id) {
    }
}
