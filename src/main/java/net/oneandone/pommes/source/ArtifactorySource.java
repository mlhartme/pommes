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
package net.oneandone.pommes.source;

import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.cli.Parser;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;

public class ArtifactorySource implements Source {
    private static final String PROTOCOL = "artifactory:";

    public static ArtifactorySource createOpt(World world, String url) {
        if (url.startsWith(PROTOCOL)) {
            return new ArtifactorySource(world, url.substring(PROTOCOL.length()));
        } else {
            return null;
        }
    }

    private final String url;
    private final World world;

    public ArtifactorySource(World world, String url) {
        this.world = world;
        this.url = url;
    }

    public void addOption(String option) {
        throw new ArgumentException(url + ": unknown option: " + option);
    }

    public void addExclude(String exclude) {
        throw new ArgumentException(url + ": excludes not supported: " + exclude);
    }

    @Override
    public void scan(BlockingQueue<Node> dest) throws IOException, URISyntaxException {
        Node listing;
        Node root;

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