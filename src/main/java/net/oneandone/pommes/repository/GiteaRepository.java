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
import io.gitea.model.Organization;
import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.io.PrintWriter;
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
public class GiteaRepository extends NextRepository<GiteaProject> {
    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        Environment env;
        GiteaRepository gitea;

        env = new Environment(Console.create(), World.create());
        gitea = GiteaRepository.create(env, "reponame", "https://git.ionos.org/CPOPS", null);
        BlockingQueue<Descriptor> result = new ArrayBlockingQueue<>(1000);
        gitea.scan(result, env.console());
        for (var d : result) {
            System.out.println(" " + d.load(env));
        }
    }

    public static GiteaRepository create(Environment environment, String repository, String uriStr, PrintWriter log)
            throws URISyntaxException {
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

        return new GiteaRepository(environment, repository, uri.getHost(), gitea, selectedOrganization);
    }

    private final Environment environment;
    private final String hostname;
    private final ApiClient gitea;
    private final String selectedOrganization;
    private final OrganizationApi organizationApi;
    private final RepositoryApi repositoryApi;

    public GiteaRepository(Environment environment, String repository, String hostname, ApiClient gitea, String selectedOrganization) {
        super(repository);
        this.environment = environment;
        this.hostname = hostname;
        this.gitea = gitea;
        this.selectedOrganization = selectedOrganization;
        this.organizationApi = new OrganizationApi(gitea);
        this.repositoryApi = new RepositoryApi(gitea);
    }

    @Override
    public List<GiteaProject> doScan() throws IOException {
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
    public Descriptor scanOpt(GiteaProject project) throws IOException {
        try {
            return project.scanOpt(environment, hostname, repositoryApi);
        } catch (ApiException e) {
            throw new IOException(e);
        }
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
