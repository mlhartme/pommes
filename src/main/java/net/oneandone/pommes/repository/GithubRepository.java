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
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.http.HttpNode;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/** https://docs.github.com/de/rest/guides/getting-started-with-the-rest-api */
public class GithubRepository extends Repository {
    private static final String PROTOCOL = "github:";

    public static GithubRepository createOpt(Environment environment, String repository, String url, PrintWriter log) throws URISyntaxException, IOException {
        if (url.startsWith(PROTOCOL)) {
            return new GithubRepository(environment, repository, url.substring(PROTOCOL.length()), environment.lib.tokenOpt(PROTOCOL));
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

    // curl "https://api.github.com/orgs/1and1/repos"
    public List<GithubRepo> listOrganizationRepos(String org) throws IOException {
        final int pageSize = 30;
        String str;
        HttpNode url;
        List<GithubRepo> result;
        List<GithubRepo> step;

        result = new ArrayList<>();
        url = root.join("orgs", org, "repos");
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

    @Override
    public void scan(BlockingQueue<Descriptor> dest, Console console) throws IOException, URISyntaxException, InterruptedException {
    }

    private String repoUrl(GithubRepo repo) {
        return repo.clone_url;
    }
    public record GithubRepo(int id, String name, String full_name, String url, String clone_url, String git_url, String default_branch) {
    }
}
