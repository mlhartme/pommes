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
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.pommes.descriptor.RawDescriptor;
import net.oneandone.pommes.scm.GitUrl;
import net.oneandone.pommes.scm.ScmUrlException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.http.HttpNode;
import net.oneandone.sushi.fs.http.model.HeaderList;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/** https://docs.github.com/de/rest/guides/getting-started-with-the-rest-api */
public class GithubRepository extends Repository<GithubRepository.GithubRepo> {
    private static final int PAGE_SIZE = 30;

    public static GithubRepository create(Environment environment, String name, String url, PrintWriter log)
            throws IOException, URISyntaxException {
        return new GithubRepository(environment, name, url);
    }


    private final Environment environment;
    private final String host;
    private final ObjectMapper mapper;
    private final HttpNode root;

    private final List<String> groupsOrUsers;

    public GithubRepository(Environment environment, String name, String urlstr) throws NodeInstantiationException, URISyntaxException {
        super(name);
        // currently hard-coded, other server might need something else ...
        String prefix = "api.";
        URI url = new URI(urlstr);

        this.environment = environment;
        this.mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.host = url.getHost();
        this.root = (HttpNode) environment.world().node(new URI(url.getScheme(), url.getUserInfo(),
                prefix + host, url.getPort(), url.getPath(), url.getQuery(), url.getFragment()));
        this.groupsOrUsers = new ArrayList<>();
    }

    public String getTokenHost() {
        return host;
    }
    public void setToken(String token) {
        // https://docs.gitlab.com/ee/api/#personalprojectgroup-access-tokens
        root.getRoot().addExtraHeader("Authorization", "Bearer " + token);
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
    public List<GithubRepo> doScan() throws IOException {
        if (groupsOrUsers.isEmpty()) {
            throw new IOException("cannot collect all repos ...");
        }
        List<GithubRepo> result = new ArrayList<>();
        for (String groupOrUser : groupsOrUsers) {
            result.addAll(listOrganizationOrUserRepos(groupOrUser));
        }
        return result;
    }


    public HttpNode fileNode(GithubRepo repo, String path) {
        HttpNode url = root.join("repos", repo.full_name, "contents", path);
        url = url.withParameter("ref", repo.default_branch());
        url = url.withHeaders(HeaderList.of("Accept", "application/vnd.github.v3.raw"));
        return url;
    }

    @Override
    public Descriptor scanOpt(GithubRepo repo) throws IOException {
        Descriptor.Creator m;
        Node<?> node;

        for (String name : files(repo)) {
            m = Descriptor.match(name);
            if (m != null) {
                // TODO: when to delete node?
                node = environment.world().getTemp().createTempFile();
                fileNode(repo, name).copyFile(node);
                return m.create(environment, node, this.name, repo.full_name(), branchRevision(repo, repo.default_branch()), repoUrl(repo));
            }
        }

        return new RawDescriptor(name, repo.full_name(), "TODO", repoUrl(repo), repo.url);
    }


    private GitUrl repoUrl(GithubRepo repo) throws ScmUrlException {
        return GitUrl.create(repo.clone_url);
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
