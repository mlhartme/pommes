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

import io.gitea.ApiClient;
import io.gitea.ApiException;
import io.gitea.Configuration;
import io.gitea.api.OrganizationApi;
import io.gitea.api.RepositoryApi;
import io.gitea.auth.ApiKeyAuth;
import io.gitea.model.ContentsResponse;
import io.gitea.model.Organization;
import net.oneandone.inline.ArgumentException;
import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.memory.MemoryNode;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiFunction;

/** https://developer.atlassian.com/static/rest/bitbucket-server/4.6.2/bitbucket-rest.html */
public class GiteaRepository implements Repository {
    private static final String PROTOCOL = "gitea:";

    public static void main(String[] args) throws IOException, URISyntaxException {
        Environment env;
        GiteaRepository gitea;

        env = new Environment(Console.create(), World.create());
        gitea = GiteaRepository.create(env, "https://git.ionos.org/CPOPS");
        BlockingQueue<Descriptor> result = new ArrayBlockingQueue<>(1000);
        gitea.scan(result);
        for (var d : result) {
            System.out.println(" " + d.load(env, "zone"));
        }
    }

    public static GiteaRepository createOpt(Environment environment, String url) throws URISyntaxException, NodeInstantiationException {
        if (url.startsWith(PROTOCOL)) {
            return create(environment, url.substring(PROTOCOL.length()));
        } else {
            return null;
        }
    }

    public static GiteaRepository create(Environment environment, String uriStr) throws URISyntaxException {
        ApiClient gitea;
        URI uri;
        String path;
        String selectedOrganization;

        uri = new URI(uriStr);
        path = uri.getPath();
        if (path != null) {
            path = Strings.removeLeft(path, "/");
            if (path.contains("/")) {
                throw new IllegalArgumentException(uriStr);
            }
            selectedOrganization = path;
        } else {
            selectedOrganization = null;
        }
        gitea = Configuration.getDefaultApiClient();
        gitea.setBasePath(uri.resolve(URI.create("/api/v1")).toString());
        gitea.setReadTimeout(120000);

        ApiKeyAuth accessToken = (ApiKeyAuth) gitea.getAuthentication("AccessToken");
        accessToken.setApiKey(environment.home.properties().giteaKey);

        return new GiteaRepository(environment, gitea, selectedOrganization);
    }

    private final Environment environment;
    private final ApiClient gitea;
    private final String selectedOrganization;
    private final OrganizationApi organizationApi;
    private final RepositoryApi repositoryApi;

    public GiteaRepository(Environment environment, ApiClient gitea, String selectedOrganization) {
        this.environment = environment;
        this.gitea = gitea;
        this.selectedOrganization = selectedOrganization;
        this.organizationApi = new OrganizationApi(gitea);
        this.repositoryApi = new RepositoryApi(gitea);
    }

    public void addOption(String option) {
        throw new ArgumentException(gitea.getBasePath() + ": unknown option: " + option);
    }

    public void addExclude(String exclude) {
        throw new ArgumentException(gitea.getBasePath() + ": excludes not supported: " + exclude);
    }

    @Override
    public void scan(BlockingQueue<Descriptor> dest) throws IOException {
        List<String> orgs;
        Descriptor descriptor;
        orgs = listCurrentUserOrgs();
        orgs.addAll(listOrganizations());

        if (selectedOrganization == null) {
            // TODO: duplicates orgs
            orgs = new ArrayList<>(new HashSet<>(orgs));
            Collections.sort(orgs);
        } else {
            orgs = Collections.singletonList(selectedOrganization);
        }
        for (String org : orgs) {
            for (var r : listRepos(org)) {
                descriptor = scanOrganizationOpt(org, r.getName(), r.getDefaultBranch());
                if (descriptor != null) {
                    dest.add(descriptor);
                }
            }
        }
    }

    private Descriptor scanOrganizationOpt(String org, String repo, String ref) throws IOException {
        List<ContentsResponse> lst;

        try {
            lst = repositoryApi.repoGetContentsList(org, repo, ref);
            for (ContentsResponse contents : lst) {
                if ("file".equals(contents.getType())) {
                    BiFunction<Environment, Node<?>, Descriptor> m = Descriptor.match(contents.getName());
                    if (m != null) {
                        contents = repositoryApi.repoGetContents(org, repo, contents.getPath(), ref);
                        String str = contents.getContent();
                        if ("base64".equals(contents.getEncoding())) {
                            str = new String(Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
                        } else if (contents.getEncoding() != null) {
                            throw new IllegalStateException(contents.getEncoding());
                        }
                        MemoryNode tmp = environment.world().memoryNode(str);
                        Descriptor descriptor = m.apply(environment, tmp);
                        if (descriptor != null) {
                            String hostname = "git.ionos.org"; // TODO
                            descriptor.setOrigin("gitea://" + hostname + "/" + org + "/" + repo + "/" + contents.getPath());
                            descriptor.setRevision(tmp.sha());
                            descriptor.setScm("git:ssh://git@" + hostname + "/" + org + "/" + repo + ".git");
                            return descriptor;
                        }
                    }
                }
            }
        } catch (ApiException e) {
            throw new IOException(e);
        }
        return null;
    }

    public List<io.gitea.model.Repository> listRepos(String org) throws IOException {
        List<io.gitea.model.Repository> repos;
        List<io.gitea.model.Repository> result;

        result = new ArrayList<>();
        try {
            for (int page = 0; true; page++) {
                repos = organizationApi.orgListRepos(org, page, 50);
                if (repos.isEmpty()) {
                    break;
                }
                for (var r: repos) {
                    result.add(r);
                }
            }
        } catch (ApiException e) {
            throw new IOException(e);
        }
        Collections.sort(result, Comparator.comparing(io.gitea.model.Repository::getName));
        return result;
    }

    public List<String> listOrganizations() throws IOException {
        List<Organization> orgas;
        List<String> result;

        result = new ArrayList<>();
        try {
            for (int page = 0; true; page++) {
                orgas = organizationApi.orgGetAll(page, 50);
                if (orgas.isEmpty()) {
                    break;
                }
                for (var o : orgas) {
                    result.add(o.getUsername());
                }
            }
        } catch (ApiException e) {
            throw new IOException(e);
        }
        return result;
    }

    public List<String> listCurrentUserOrgs() throws IOException {
        List<Organization> orgas;
        List<String> result;

        result = new ArrayList<>();
        try {
            for (int page = 0; true; page++) {
                orgas = organizationApi.orgListCurrentUserOrgs(page, 50);
                if (orgas.isEmpty()) {
                    break;
                }
                for (var o : orgas) {
                    result.add(o.getUsername());
                }
            }
        } catch (ApiException e) {
            throw new IOException(e);
        }
        return result;
    }



}
