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

import net.oneandone.inline.ArgumentException;
import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/** A place to get descriptors from. Can list repositories, which in turn can list files. */
public abstract class Storage {
    private static Map<String, Constructor> types;
    static {
        types = new HashMap<>();
        types.put("artifactory", ArtifactoryStorage::create);
        types.put("bitbucket", BitbucketStorage::create);
        types.put("file", FileStorage::create);
        types.put("gitea", GiteaStorage::create);
        types.put("github", GithubStorage::create);
        types.put("gitlab", GitlabStorage::create);
        types.put("json", JsonStorage::createJson);
        types.put("inline", JsonStorage::createInline);
    }

    public static Storage createStorage(Environment environment, String name, final String scmurl) throws URISyntaxException, IOException {
        int idx;
        FileNode file;
        Constructor constructor;

        idx = scmurl.indexOf(':');
        if (idx >= 0) {
            constructor = types.get(scmurl.substring(0, idx));
            if (constructor != null) {
                return constructor.create(environment, name, scmurl.substring(idx + 1));
            }
        }
        file = environment.world().file(scmurl);
        if (file.exists()) {
            return new FileStorage(environment, name, file);
        }
        throw new ArgumentException("storage " + name + ": unknown storage type: " + scmurl);
    }

    protected final String name;

    public Storage(String name) {
        this.name = name;
    }


    // override to return a host name if storage can handle token
    public String getTokenHost() {
        return null;
    }
    // override if getTokenHost is overridden
    public void setToken(String token) {
        throw new IllegalStateException("token not supported");
    }

    public void addOption(String option) {
        throw new ArgumentException(name + ": unknown option: " + option);
    }

    public void addExclude(String exclude) {
        throw new ArgumentException(name + ": excludes not supported: " + exclude);
    }

    //--

    public abstract void scan(BlockingQueue<Descriptor> dest, Console console) throws IOException, InterruptedException;
}
