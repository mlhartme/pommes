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
import net.oneandone.sushi.fs.filter.Filter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class DatabaseAddSvn extends BaseDatabaseAdd {
    public DatabaseAddSvn(Environment environment) {
        super(environment);
    }

    @Override
    public void collect(Source source, BlockingQueue<Node> dest) throws IOException, InterruptedException, URISyntaxException {
        Node node;
        Filter exclude;
        boolean noBranches;

        node = world.node("svn:" + source.url);
        noBranches = false;
        exclude = new Filter();
        for (String str : source.excludes) {
            if (str.equals("%BRANCHES")) {
                noBranches = true;
            } else {
                if (str.startsWith("/") || str.endsWith("/")) {
                    throw new ArgumentException("do not use '/' before or after excludes: " + str);
                }
                exclude.include("**/" + str);
            }
        }
        if (exclude.getIncludes().length == 0) {
            exclude = null;
        }
        scan(node, exclude, true, noBranches, dest);
    }

    public void scan(Node root, Filter excludes, boolean recurse, boolean noBranches, BlockingQueue<Node> dest) throws IOException, InterruptedException {
        List<? extends Node> children;
        Node project;
        Node trunk;
        Node branches;
        List<? extends Node> grandChildren;

        if (excludes != null && excludes.matches(root.getPath())) {
            return;
        }
        console.verbose.println("scanning ... " + root.getURI());
        children = root.list();
        if (children == null) {
            return;
        }
        project = child(children, "pom.xml");
        if (project != null) {
            dest.put(project);
            return;
        }
        project = child(children, "composer.json");
        if (project != null) {
            dest.put(project);
            return;
        }
        trunk = child(children, "trunk");
        if (trunk != null) {
            scan(trunk, excludes, false, noBranches, dest);
        }
        branches = child(children, "branches");
        if (branches != null && !noBranches) {
            grandChildren = branches.list();
            if (grandChildren != null) {
                for (Node grandChild : grandChildren) {
                    scan(grandChild, excludes, false, noBranches, dest);
                }
            }
        }
        if (trunk != null || branches != null) {
            return;
        }

        if (child(children, "src") != null) {
            // probably an old ant build
            return;
        }

        if (recurse) {
            // recurse
            for (Node node : children) {
                scan(node, excludes, true, noBranches, dest);
            }
        }
    }

    private static Node child(List<? extends Node> childen, String name) {
        for (Node node : childen) {
            if (node.getName().equals(name)) {
                return node;
            }
        }
        return null;
    }
}