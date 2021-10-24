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
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiFunction;

/** https://developer.atlassian.com/static/rest/bitbucket-server/4.6.2/bitbucket-rest.html */
public class GiteaRepository implements Repository {
    private static final String PROTOCOL = "gitea:";

    public static void main(String[] args) throws IOException {
        GiteaRepository gitea;

        gitea = GiteaRepository.create(new Environment(Console.create(), World.create()));
        gitea.scanOpt("CPOPS", "puc", "main");
    }

    public static GiteaRepository create(Environment environment) {
        ApiClient gitea;

        gitea = Configuration.getDefaultApiClient();
        gitea.setBasePath("https://git.ionos.org/api/v1");
        gitea.setReadTimeout(120000);

        ApiKeyAuth accessToken = (ApiKeyAuth) gitea.getAuthentication("AccessToken");
        accessToken.setApiKey(environment.home.properties().giteaKey);

        return new GiteaRepository(gitea);
    }

    private final ApiClient gitea;
    private final OrganizationApi organizationApi;
    private final RepositoryApi repositoryApi;

    public GiteaRepository(ApiClient gitea) {
        this.gitea = gitea;
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

        // TODO: duplicates orgs
        orgs = new ArrayList<>(new HashSet<>(orgs));
        Collections.sort(orgs);

        for (String org : orgs) {
            for (var r : listRepos(org)) {
                descriptor = scanOpt(org, r.getName(), r.getDefaultBranch());
                if (descriptor != null) {
                    dest.add(descriptor);
                }
            }
        }
    }

    public Descriptor scanOpt(String org, String repo, String ref) throws IOException {
        List<ContentsResponse> lst;

        try {
            lst = repositoryApi.repoGetContentsList(org, repo, ref);
            for (ContentsResponse contents : lst) {
                if ("file".equals(contents.getType())) {
                    BiFunction m = Descriptor.match(contents.getName());
                    if (m != null) {
                        var c = repositoryApi.repoGetContents(org, repo, contents.getPath(), ref);
                        System.out.println("" + c.getContent());
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
