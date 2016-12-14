/**
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

import com.google.gson.Gson;
import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.project.JsonProject;
import net.oneandone.pommes.project.Project;
import net.oneandone.sushi.fs.NodeInstantiationException;

import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;

public class InlineRepository implements Repository {
    private static final String PROTOCOL = "inline:";

    public static InlineRepository createOpt(Gson gson, String url) throws URISyntaxException, NodeInstantiationException {
        if (url.startsWith(PROTOCOL)) {
            return new InlineRepository(gson, url.substring(PROTOCOL.length()));
        } else {
            return null;
        }
    }

    private final Gson gson;
    private final String str;

    public InlineRepository(Gson gson, String str) {
        this.gson = gson;
        this.str = str;
    }

    public void addOption(String option) {
        throw new ArgumentException("unknown option: " + option);
    }

    public void addExclude(String exclude) {
        throw new ArgumentException("excludes not supported: " + exclude);
    }

    @Override
    public void scan(BlockingQueue<Project> dest) throws InterruptedException {
        Pom pom;
        JsonProject project;

        pom = gson.fromJson(str, Pom.class);
        project = new JsonProject(pom);
        project.setOrigin(pom.getOrigin());
        project.setRevision(pom.revision);
        dest.put(project);
    }
}
