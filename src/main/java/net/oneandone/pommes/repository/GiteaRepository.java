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
import io.gitea.auth.ApiKeyAuth;
import io.gitea.model.Organization;
import net.oneandone.inline.ArgumentException;
import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/** https://developer.atlassian.com/static/rest/bitbucket-server/4.6.2/bitbucket-rest.html */
public class GiteaRepository implements Repository {
    private static final String PROTOCOL = "gitea:";

    public static void main(String[] args) throws IOException {
        GiteaRepository gitea;
        List<io.gitea.model.Repository> lst;

        gitea = GiteaRepository.create(new Environment(Console.create(), World.create()));
        gitea.scan(null);
    }

    public static GiteaRepository create(Environment environment) {
        ApiClient gitea;
        List<io.gitea.model.Repository> lst;

        gitea = Configuration.getDefaultApiClient();
        gitea.setBasePath("https://git.ionos.org/api/v1");
        gitea.setReadTimeout(120000);

        ApiKeyAuth accessToken = (ApiKeyAuth) gitea.getAuthentication("AccessToken");
        accessToken.setApiKey(environment.home.properties().giteaKey);

        return new GiteaRepository(gitea);
    }

    private final ApiClient gitea;

    public GiteaRepository(ApiClient gitea) {
        this.gitea = gitea;
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

        orgs = listCurrentUserOrgs();
        orgs.addAll(listOrganizations());
        // TODO: duplicates orgs
        orgs = new ArrayList<>(new HashSet<>(orgs));
        Collections.sort(orgs);
            for (String org : orgs) {
               System.out.println(org);
/*                var lst = organizationApi.orgListRepos(o.getUsername(), 0, 200);
                for (var r : lst) {
                    System.out.println("  " + r.getName() + " " + r.getDescription());
                }
  */
            }
    }

    public List<String> listOrganizations() throws IOException {
        OrganizationApi organizationApi = new OrganizationApi(gitea);
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
        OrganizationApi organizationApi = new OrganizationApi(gitea);
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
