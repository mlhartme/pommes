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

import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.Node;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.TreeItem;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiFunction;

public class GitlabRepository extends Repository {
    private static final String PROTOCOL = "gitlab:";

    private final Environment environment;
    private final GitLabApi api;

    public GitlabRepository(Environment environment, String name, String url) {
        super(name);
        this.environment = environment;
        api = new GitLabApi(url, null);
    }

    public void addOption(String option) {
        throw new ArgumentException("unknown option: " + option);
    }

    public Project findProject(String namespace, String name) throws GitLabApiException {
        return api.getProjectApi().getProject(namespace, name);
    }

    public List<String> list(Project project) throws GitLabApiException {
        List<TreeItem> lst;

        lst = api.getRepositoryApi().getTree(project.getId(), "", project.getDefaultBranch(), false);
        return lst.stream().map((item) -> item.getType() == TreeItem.Type.BLOB ? item.getName() : null).filter(Objects::nonNull).toList();
    }

    public void addExclude(String exclude) {
        throw new ArgumentException("excludes not supported: " + exclude);
    }

    @Override
    public void scan(BlockingQueue<Descriptor> dest) throws IOException, URISyntaxException, InterruptedException {
    }

    public Descriptor scanOpt(Project project) throws GitLabApiException, IOException {
        BiFunction<Environment, Node<?>, Descriptor> m;
        Node<?> node;
        Descriptor result;

        for (String name : list(project)) {
            m = Descriptor.match(name);
            if (m != null) {
                System.out.println("name " + name);
                // TODO: when to delete?
                node = environment.world().getTemp().createTempFile();
                node.writeBytes(api.getRepositoryFileApi().getFile(project.getId(), name, project.getDefaultBranch(), true).getDecodedContentAsBytes());
                result = m.apply(environment, node);
                result.setRepository(this.name);
                result.setPath(project.getPathWithNamespace() + "/" + name);
                result.setRevision("TODO");
                result.setScm(project.getSshUrlToRepo());
                return result;
            }
        }
        return null;
    }
}
