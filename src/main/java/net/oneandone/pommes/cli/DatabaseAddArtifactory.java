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
package net.oneandone.pommes.cli;

import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.model.Database;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseAddArtifactory extends Base {
    private List<String> urls;

    public DatabaseAddArtifactory(Environment environment) {
        super(environment);
        this.urls = new ArrayList<>();
    }

    public void url(String url) {
        urls.add(url);
    }

    @Override
    public void run(Database database) throws Exception {
        Node listing;
        Node r;
        List<Node> lst;
        DatabaseAddSvn.ProjectIterator iterator;

        if (urls.size() == 0) {
            throw new ArgumentException("missing roots");
        }
        console.info.println("scanning artifactory ...");
        for (String url : urls) {
            r = world.node(url);
            listing = world.node(strip(url) + "/api/storage/" + r.getName() + "?list&deep=1&mdTimestamps=0");
            try {
                lst = Parser.run(listing, r);
            } catch (IOException | RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException("scanning failed: " + e.getMessage(), e);
            }
            System.out.println("adding " + lst.size() + " poms ...");
            iterator = new DatabaseAddSvn.ProjectIterator(console, world, environment.maven(), lst.iterator());
            database.index(iterator);
            iterator.summary();
        }
    }

    private static String strip(String url) {
        int idx;

        idx = url.lastIndexOf('/');
        return url.substring(0, idx);
    }
}