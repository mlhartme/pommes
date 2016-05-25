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
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.filter.Filter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseAddSvn extends BaseDatabaseAdd {
    private final boolean noBranches;

    private List<Node> nodes = new ArrayList<>();
    private List<Filter> excludes = new ArrayList<>();

    public DatabaseAddSvn(Environment environment, boolean noBranches) {
        super(environment);
        this.noBranches = noBranches;
    }

    public void url(String str) {
        Filter exclude;

        if (str.startsWith("-")) {
            if (excludes.isEmpty()) {
                throw new ArgumentException("missing url before exclude " + str);
            }
            if (str.startsWith("/") || str.endsWith("/")) {
                throw new ArgumentException("do not use '/' before or after excludes: " + str);
            }
            exclude = excludes.get(excludes.size() - 1);
            if (exclude == null) {
                exclude = new Filter();
                excludes.set(excludes.size() - 1, exclude);
            }
            // TODO: sushi bug? .exclude didn't work ...
            exclude.include("**/" + str.substring(1));
        } else {
            try {
                nodes.add(world.node("svn:" + str));
            } catch (URISyntaxException e) {
                throw new ArgumentException(str + ": invalid url", e);
            } catch (NodeInstantiationException e) {
                throw new ArgumentException(str + ": access failed", e);
            }
            excludes.add(null);
        }
    }

    @Override
    public List<Node> collect() throws IOException {
        if (nodes.size() == 0) {
            throw new ArgumentException("missing urls");
        }
        List<Node> result;

        result = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            scan(nodes.get(i), excludes.get(i), true, result);
        }
        return result;
    }

    public void scan(Node root, Filter excludes, boolean recurse, final List<Node> result) throws IOException {
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
            result.add(project);
            return;
        }
        project = child(children, "composer.json");
        if (project != null) {
            result.add(project);
            return;
        }
        trunk = child(children, "trunk");
        if (trunk != null) {
            scan(trunk, excludes, false, result);
        }
        branches = child(children, "branches");
        if (branches != null && !noBranches) {
            grandChildren = branches.list();
            if (grandChildren != null) {
                for (Node grandChild : grandChildren) {
                    scan(grandChild, excludes, false, result);
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
                scan(node, excludes, true, result);
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