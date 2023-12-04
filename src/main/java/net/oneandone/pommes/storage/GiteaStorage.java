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
package net.oneandone.pommes.storage;

import io.gitea.ApiClient;
import io.gitea.ApiException;
import io.gitea.Configuration;
import io.gitea.api.OrganizationApi;
import io.gitea.api.RepositoryApi;
import io.gitea.auth.ApiKeyAuth;
import io.gitea.model.ContentsResponse;
import io.gitea.model.Organization;
import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.pommes.scm.GitUrl;
import net.oneandone.pommes.scm.ScmUrl;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.memory.MemoryNode;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/** https://developer.atlassian.com/static/rest/bitbucket-server/4.6.2/bitbucket-rest.html */
public class GiteaStorage extends TreeStorage<GiteaProject, ContentsResponse> {
    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        Environment env;
        GiteaStorage gitea;

        env = new Environment(Console.create(), World.create());
        gitea = GiteaStorage.create(env, "storagename", "https://git.ionos.org/CPOPS");
        BlockingQueue<Descriptor> result = new ArrayBlockingQueue<>(1000);
        gitea.scan(result, env.console());
        for (var d : result) {
            System.out.println(" " + d.load());
        }
    }

    public static GiteaStorage create(Environment environment, String storage, String uriStr) throws URISyntaxException {
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
        accessToken.setApiKey(environment.lib.properties().giteaKey);

        return new GiteaStorage(environment, storage, uri.getHost(), gitea, selectedOrganization);
    }

    private final Environment environment;
    private final String hostname;
    private final ApiClient gitea;
    private final String selectedOrganization;
    private final OrganizationApi organizationApi;
    private final RepositoryApi repositoryApi;

    public GiteaStorage(Environment environment, String storage, String hostname, ApiClient gitea, String selectedOrganization) {
        super(environment, storage);
        this.environment = environment;
        this.hostname = hostname;
        this.gitea = gitea;
        this.selectedOrganization = selectedOrganization;
        this.organizationApi = new OrganizationApi(gitea);
        this.repositoryApi = new RepositoryApi(gitea);
    }

    @Override
    public List<GiteaProject> list() throws IOException {
        List<String> orgs;
        orgs = listCurrentUserOrgs();
        orgs.addAll(listOrganizations());

        if (selectedOrganization == null) {
            // TODO: duplicates orgs
            orgs = new ArrayList<>(new HashSet<>(orgs));
            Collections.sort(orgs);
        } else {
            orgs = Collections.singletonList(selectedOrganization);
        }
        List<GiteaProject> result = new ArrayList<>();
        for (String org : orgs) {
            for (var r : listRepos(org)) {
                result.add(new GiteaProject(org, r.getName(), r.getDefaultBranch()));
            }
        }
        return result;
    }

    @Override
    public List<ContentsResponse> listRoot(GiteaProject entry) throws IOException {
        try {
            return entry.list(repositoryApi);
        } catch (ApiException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String fileName(ContentsResponse file) {
        return file.getName();
    }

    @Override
    public ScmUrl storageUrl(GiteaProject repository) throws IOException {
        return GitUrl.create("ssh://gitea@" + hostname + "/" + repository.org() + "/" + repository + ".git");
    }

    @Override
    public String repositoryPath(GiteaProject repository, ContentsResponse file) throws IOException {
        return repository.org() + "/" + repository.repo() + "/" + file.getPath();
    }

    @Override
    public MemoryNode localFile(GiteaProject repository, ContentsResponse file) throws IOException {
        try {
            return repository.localFile(file, environment, repositoryApi);
        } catch (ApiException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public String fileRevision(GiteaProject repository, ContentsResponse contentsResponse, Node<?> local) throws IOException {
        return local.sha();
    }

    @Override
    public Descriptor createDefault(GiteaProject entry) throws IOException {
        return null; // TODO
    }

    public List<io.gitea.model.Repository> listRepos(String org) throws IOException {
        List<io.gitea.model.Repository> repos;
        Map<String, io.gitea.model.Repository> map;

        map = new TreeMap<>();
        try {
            for (int page = 0; true; page++) {
                repos = organizationApi.orgListRepos(org, page, 50);
                if (repos.isEmpty()) {
                    break;
                }
                for (var r: repos) {
                    if (!map.containsKey(r.getName())) { // TODO: avoid duplicates
                        map.put(r.getName(), r);
                    }
                }
            }
        } catch (ApiException e) {
            throw new IOException(e);
        }
        return new ArrayList<>(map.values());
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
