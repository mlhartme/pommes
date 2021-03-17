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
import net.oneandone.pommes.cli.Find;
import net.oneandone.pommes.project.Project;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.svn.SvnNode;
import org.tmatesoft.svn.core.SVNURL;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class NodeRepository implements Repository {
    public static Project probe(Environment environment, FileNode checkout) throws IOException {
        NodeRepository repo;
        BlockingQueue<Project> dest;

        repo = new NodeRepository(environment, checkout, environment.console().verbose);
        dest = new ArrayBlockingQueue<>(2);
        try {
            repo.scan(checkout, false, dest);
            if (dest.size() != 1) {
                return null;
            }
            return dest.take();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final String FILE = "file:";
    private static final String SVN = "svn:";

    public static NodeRepository createOpt(Environment environment, String url, PrintWriter log) throws URISyntaxException, NodeInstantiationException {
        if (url.startsWith(SVN) || url.startsWith(FILE)) {
            return new NodeRepository(environment, Find.fileOrNode(environment.world(), url), log);
        } else {
            return null;
        }
    }

    //--

    private final Environment environment;
    private final PrintWriter log;
    private final Node node;
    private boolean branches;
    private boolean tags;
    private final Filter exclude;

    public NodeRepository(Environment environment, Node node, PrintWriter log) {
        this(environment, node, false, false, log);
    }

    public NodeRepository(Environment environment, Node node, boolean branches, boolean tags, PrintWriter log) {
        this.environment = environment;
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
        log.println("scan " + root.getPath());
        for (Node child : children) {
            project = Project.probeChecked(environment, child);
            if (project != null) {
                project.setOrigin(child.getUri().toString());
                project.setRevision(Long.toString(child.getLastModified()));
                project.setScm(nodeScm(child));
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
            for (Node child : children) {
                scan(child, true, dest);
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

    public static String nodeScm(Node descriptor) {
        SvnNode svn;
        String path;
        SVNURL root;

        if (descriptor instanceof SvnNode) {
            svn = (SvnNode) descriptor;
            // TODO: I currently use svn to crawl github ...
            // sushi path is only trunk/pom.xml - i need svnkit path instead
            root = svn.getRoot().getRepository().getLocation();
            if (root.getHost().equals("github.com")) {
                path = root.getPath();
                if (!path.startsWith("/")) {
                    throw new IllegalStateException(path);
                }
                return "git:ssh://git@github.com" + path + ".git";
            } else {
                return svn.getParent().getUri().toString();
            }
        } else {
            return null;
        }
    }
}
