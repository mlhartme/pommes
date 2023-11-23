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
import net.oneandone.pommes.descriptor.RawDescriptor;
import net.oneandone.pommes.scm.GitUrl;
import net.oneandone.pommes.scm.ScmUrlException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.http.HttpNode;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GitlabRepository extends NextRepository<GitlabRepository.GitlabProject> {
    public static GitlabRepository create(Environment environment, String repository, String url, PrintWriter log) throws URISyntaxException, IOException {
        return new GitlabRepository(environment, repository, url);
    }


    private final Environment environment;
    private final ObjectMapper mapper;
    private final HttpNode root;

    private final List<String> groupsOrUsers;

    public GitlabRepository(Environment environment, String name, String url) throws NodeInstantiationException {
        super(name);
        this.environment = environment;
        this.mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        root = (HttpNode) environment.world().validNode(url).join("api/v4");
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

    public String getTokenHost() {
        return root.getRoot().getHostname();
    }

    public void setToken(String token) {
        // https://docs.gitlab.com/ee/api/rest/#oauth-20-tokens
        // https://docs.gitlab.com/ee/api/rest/#personalprojectgroup-access-tokens
        root.getRoot().addExtraHeader("Authorization", "Bearer " + token);
    }

    @Override
    public List<GitlabProject> doScan() throws IOException {
        List<GitlabProject> result;

        if (groupsOrUsers.isEmpty()) {
            result = listAllProjects();
        } else {
            result = new ArrayList<>();
            for (String groupOrUser : groupsOrUsers) {
                result.addAll(listGroupOrUserProjects(groupOrUser));
            }
        }
        return result.stream().filter(p -> !Boolean.TRUE.equals(p.archived)).toList();
    }


    private GitUrl repoUrl(GitlabProject project) throws ScmUrlException {
        return GitUrl.create(project.http_url_to_repo()); // TODO: configurable
    }

    @Override
    public Descriptor scanOpt(GitlabProject project) throws IOException {
        Descriptor.Creator m;
        Node<?> node;

        for (String name : files(project)) {
            m = Descriptor.match(name);
            if (m != null) {
                // TODO: when to delete node?
                node = environment.world().getTemp().createTempFile();
                HttpNode url = root.join("projects", Long.toString(project.id()), "repository/files", name, "raw");
                url = url.withParameter("ref", project.default_branch());
                url.copyFile(node);
                return m.create(environment, node, this.name, project.path_with_namespace() + "/" + name,
                        branchRevision(project, project.default_branch()),  // TODO: could be more accurate with the revision of this very file ...
                        repoUrl(project));
            }
        }

        return new RawDescriptor(name, project.path_with_namespace(), "TODO", repoUrl(project), project.web_url);
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
