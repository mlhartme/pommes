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
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.ValueInstantiators;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.pommes.scm.Git;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.http.HttpNode;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.TreeItem;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GitlabRepository extends Repository {
    private static final String PROTOCOL = "gitlab:";

    private final Environment environment;
    private final GitLabApi api;

    private final HttpNode root;

    public GitlabRepository(Environment environment, String name, String url) throws NodeInstantiationException {
        super(name);
        this.environment = environment;
        api = new GitLabApi(url, null);
        root = (HttpNode) environment.world().validNode(url).join("api/v4");
    }

    public void addOption(String option) {
        throw new ArgumentException("unknown option: " + option);
    }

    // TODO
    public class RecordNamingStrategyPatchModule extends SimpleModule {

        @Override
        public void setupModule(SetupContext context) {
            context.addValueInstantiators(new ValueInstantiatorsModifier());
            super.setupModule(context);
        }

        /**
         * Remove when the following issue is resolved:
         * <a href="https://github.com/FasterXML/jackson-databind/issues/2992">Properties naming strategy do not work with Record #2992</a>
         */
        private static class ValueInstantiatorsModifier extends ValueInstantiators.Base {
            @Override
            public ValueInstantiator findValueInstantiator(
                    DeserializationConfig config, BeanDescription beanDesc, ValueInstantiator defaultInstantiator
            ) {
                if (!beanDesc.getBeanClass().isRecord() || !(defaultInstantiator instanceof StdValueInstantiator) || !defaultInstantiator.canCreateFromObjectWith()) {
                    return defaultInstantiator;
                }
                Map<String, BeanPropertyDefinition> map = beanDesc.findProperties().stream().collect(Collectors.toMap(p -> p.getInternalName(), Function.identity()));
                SettableBeanProperty[] renamedConstructorArgs = Arrays.stream(defaultInstantiator.getFromObjectArguments(config))
                        .map(p -> {
                            BeanPropertyDefinition prop = map.get(p.getName());
                            return prop != null ? p.withName(prop.getFullName()) : p;
                        })
                        .toArray(SettableBeanProperty[]::new);

                return new PatchedValueInstantiator((StdValueInstantiator) defaultInstantiator, renamedConstructorArgs);
            }
        }

        private static class PatchedValueInstantiator extends StdValueInstantiator {

            protected PatchedValueInstantiator(StdValueInstantiator src, SettableBeanProperty[] constructorArguments) {
                super(src);
                _constructorArguments = constructorArguments;
            }
        }
    }
    public List<GitlabProject> listProjects() throws IOException {
        List<GitlabProject> result;
        List<GitlabProject> step;
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        m.registerModule(new RecordNamingStrategyPatchModule());
        String str;
        HttpNode url;

        result = new ArrayList<>();
        int pageSize = 80;
        url = root.join("projects");
        for (int page = 1; true; page++) {
            str = url.withParameter("page", page).withParameter("per_page", pageSize).readString();
            step = m.readValue(str, new TypeReference<>() {});
            result.addAll(step);
            if (step.size() < pageSize) {
                break;
            }
        }
        return result;
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
