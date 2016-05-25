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
import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;

public class DatabaseAddArtifactory extends BaseDatabaseAdd {
    public DatabaseAddArtifactory(Environment environment) {
        super(environment);
    }

    @Override
    public void collect(Source source, BlockingQueue<Node> dest) throws IOException, URISyntaxException {
        String url;
        Node listing;
        Node root;

        if (!source.excludes.isEmpty()) {
            throw new ArgumentException("excludes not supported for artifactory: " + source.excludes);
        }
        url = source.url;
        root = world.node(url);
        listing = world.node(strip(url) + "/api/storage/" + root.getName() + "?list&deep=1&mdTimestamps=0");
        try {
            Parser.run(listing, root, dest);
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("scanning failed: " + e.getMessage(), e);
        }
    }

    private static String strip(String url) {
        int idx;

        idx = url.lastIndexOf('/');
        return url.substring(0, idx);
    }
}