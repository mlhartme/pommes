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

import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.cli.Find;
import net.oneandone.pommes.project.Project;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class NodeRepository implements Repository {
    public static final String FILE = "file:";
    public static final String SVN = "svn:";

    public static NodeRepository createOpt(World world, String url, PrintWriter log) throws URISyntaxException, NodeInstantiationException {
        if (url.startsWith(SVN) || url.startsWith(FILE)) {
            return new NodeRepository(Find.fileOrNode(world, url), log);
        } else {
            return null;
        }
    }

    //--

    private final PrintWriter log;
    private final Node node;
    private boolean branches;
    private boolean tags;
    private final Filter exclude;

    public NodeRepository(Node node, PrintWriter log) {
        this(node, false, false, log);
    }

    public NodeRepository(Node node, boolean branches, boolean tags, PrintWriter log) {
        this.node = node;
        this.exclude = new Filter();
        this.branches = branches;
        this.tags = tags;
        this.log = log;
    }

    @Override
    public void addOption(String option) {
        if (option.equals("branches")) {
            branches = true;
        } else if (option.equals("tags")) {
            tags = true;
        } else {
            throw new ArgumentException("unknown option: " + option);
        }
    }

    @Override
    public void addExclude(String str) {
        if (str.startsWith("/") || str.endsWith("/")) {
            throw new ArgumentException("do not use '/' before or after excludes: " + str);
        }
        exclude.include("**/" + str);
    }

    @Override
    public void scan(BlockingQueue<Project> dest) throws IOException, InterruptedException {
        scan(node, true, dest);
    }

    public void scan(Node root, boolean recurse, BlockingQueue<Project> dest) throws IOException, InterruptedException {
        List<? extends Node> children;
        Project project;
        Node trunkNode;
        Node branchesNode;
        Node tagsNode;
        List<? extends Node> grandChildren;

        if (exclude.matches(root.getPath())) {
            return;
        }
        children = root.list();
        if (children == null) {
            return;
        }
        log.println(root.getPath() + "\n");
        for (Node node : children) {
            project = Project.probe(node);
            if (project != null) {
                project.setRevision(Long.toString(node.getLastModified()));
                dest.put(project);
                return;
            }
        }
        trunkNode = child(children, "trunk");
        if (trunkNode != null) {
            scan(trunkNode, false, dest);
        }
        branchesNode = child(children, "branches");
        if (branchesNode != null && branches) {
            grandChildren = branchesNode.list();
            if (grandChildren != null) {
                for (Node grandChild : grandChildren) {
                    scan(grandChild, false, dest);
                }
            }
        }
        tagsNode = child(children, "tags");
        if (tagsNode != null && tags) {
            grandChildren = tagsNode.list();
            if (grandChildren != null) {
                for (Node grandChild : grandChildren) {
                    scan(grandChild, false, dest);
                }
            }
        }
        if (trunkNode != null || branchesNode != null || tagsNode != null) {
            // found project with standard svn layout; no need for further recursion
            return;
        }

        if (child(children, "src") != null) {
            // probably an old ant build
            return;
        }

        if (recurse) {
            // recurse
            for (Node node : children) {
                scan(node, true, dest);
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
