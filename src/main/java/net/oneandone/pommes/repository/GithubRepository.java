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
import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.database.Gav;
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.pommes.scm.GitUrl;
import net.oneandone.pommes.scm.ScmUrl;
import net.oneandone.pommes.scm.ScmUrlException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.http.HttpNode;
import net.oneandone.sushi.fs.http.model.HeaderList;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiFunction;

/** https://docs.github.com/de/rest/guides/getting-started-with-the-rest-api */
public class GithubRepository extends Repository {
    private static final int PAGE_SIZE = 30;

    private static final String PROTOCOL = "github:";

    public static GithubRepository createOpt(Environment environment, String repository, String url, PrintWriter log) throws IOException {
        if (url.startsWith(PROTOCOL)) {
            return new GithubRepository(environment, repository, url.substring(PROTOCOL.length()), environment.lib.tokenOpt(repository, PROTOCOL));
        } else {
            return null;
        }
    }


    private final Environment environment;
    private final ObjectMapper mapper;
    private final HttpNode root;

    private final List<String> groupsOrUsers;

    public GithubRepository(Environment environment, String name, String url, String token) throws NodeInstantiationException {
        super(name);
        this.environment = environment;
        this.mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        root = (HttpNode) environment.world().validNode(url);
        if (token != null) {
            // https://docs.gitlab.com/ee/api/#personalprojectgroup-access-tokens
            root.getRoot().addExtraHeader("Authorization", "Bearer " + token);
        }
        this.groupsOrUsers = new ArrayList<>();
    }

    public void addOption(String option) {
        groupsOrUsers.add(option);
    }

    // curl "https://api.github.com/repos/mlhartme/pommes"
    public GithubRepo getRepo(String org, String name) throws IOException {
        HttpNode url;

        url = root.join("repos", org, name);
        return mapper.readValue(url.readString(), GithubRepo.class);
    }

    public String branchRevision(GithubRepo repo, String branch) throws IOException {
        HttpNode url;
        Branch obj;

        url = root.join("repos", repo.full_name, "branches", branch);
        obj = mapper.readValue(url.readString(), Branch.class);
        return obj.commit.sha;
    }


    // curl "https://api.github.com/orgs/1and1/repos"
    public List<GithubRepo> listOrganizationOrUserRepos(String orgOrUser) throws IOException {
        String str;
        HttpNode url;
        List<GithubRepo> result;
        List<GithubRepo> step;

        result = new ArrayList<>();
        if (orgOrUser.startsWith("~")) {
            url = root.join("users", orgOrUser.substring(1), "repos");
        } else {
            url = root.join("orgs", orgOrUser, "repos");
        }
        for (int page = 1; true; page++) {
            str = page(url, page).readString();
            step = mapper.readValue(str, new TypeReference<>() {});
            result.addAll(step);
            if (step.size() < PAGE_SIZE) {
                return result;
            }
        }
    }

    private HttpNode page(HttpNode url, int page) {
        return url.withParameter("page", page).withParameter("per_page", PAGE_SIZE);
    }
    public List<String> files(GithubRepo repo) throws IOException {
        HttpNode url;
        String str;
        List<GithubFile> files;
        List<String> result;

        result = new ArrayList<>();
        url = root.join("repos", repo.full_name, "contents"); // caution: not paginated!
        str = url.readString();
        files = mapper.readValue(str, new TypeReference<>() {});
        for (GithubFile file : files) {
            result.add(file.path);
        }
        return result;
    }

    @Override
    public void scan(BlockingQueue<Descriptor> dest, Console console) throws IOException, URISyntaxException, InterruptedException {
        if (groupsOrUsers.isEmpty()) {
            throw new IOException("cannot collect all repos ...");
        } else {
            for (String groupOrUser : groupsOrUsers) {
                for (GithubRepo repo : listOrganizationOrUserRepos(groupOrUser)) {
                    scan(repo, dest, console);
                }
            }
        }
    }

    private void scan(GithubRepo repo, BlockingQueue<Descriptor> dest, Console console) throws InterruptedException {
        try {
            var descriptor = scanOpt(repo);
            if (descriptor != null) {
                dest.put(descriptor);
            }
        } catch (IOException e) {
            console.error.println("cannot load " + repo.full_name + " (id=" + repo.id + "): " + e.getMessage());
            e.printStackTrace(console.verbose);
        }
    }

    public HttpNode fileNode(GithubRepo repo, String path) {
        HttpNode url = root.join("repos", repo.full_name, "contents", path);
        url = url.withParameter("ref", repo.default_branch());
        url = url.withHeaders(HeaderList.of("Accept", "application/vnd.github.v3.raw"));
        return url;
    }

    public Descriptor scanOpt(GithubRepo repo) throws IOException {
        BiFunction<Environment, Node<?>, Descriptor> m;
        Node<?> node;
        Descriptor result;

        for (String name : files(repo)) {
            m = Descriptor.match(name);
            if (m != null) {
                // TODO: when to delete node?
                node = environment.world().getTemp().createTempFile();
                fileNode(repo, name).copyFile(node);
                result = m.apply(environment, node);
                result.setRepository(this.name);
                result.setPath(repo.full_name());
                result.setRevision(branchRevision(repo, repo.default_branch()));
                result.setScm(GitUrl.create(repoUrl(repo)));
                return result;
            }
        }

        Gav gav = GitUrl.create(repoUrl(repo)).defaultGav();
        result = new Descriptor() {
            @Override
            protected Project doLoad(Environment environmentNotUsed, String withRepository, String withOrigin, String withRevision, ScmUrl withScm) throws ScmUrlException {
                return new Project(name, repo.full_name(), "TODO", null, gav,
                        GitUrl.create(repoUrl(repo)), repo.url);
            }
        };
        // TODO: kind of duplication ...
        result.setRepository(name);
        result.setPath(repo.full_name());
        result.setRevision("TODO");
        result.setScm(GitUrl.create(repoUrl(repo)));
        return result;
    }


    private String repoUrl(GithubRepo repo) {
        return repo.clone_url;
    }
    public record GithubRepo(int id, String name, String full_name, String url, String clone_url, String git_url, String default_branch) {
    }

    public record GithubFile(String path) {
    }
    public record Branch(String name, Commit commit) {
    }
    public record Commit(String sha) {
    }

}
