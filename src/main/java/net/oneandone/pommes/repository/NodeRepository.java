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
import net.oneandone.inline.Console;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.cli.Find;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.pommes.descriptor.RawDescriptor;
import net.oneandone.pommes.scm.GitUrl;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.pommes.scm.ScmUrl;
import net.oneandone.pommes.scm.SubversionUrl;
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

/** To search files system and subversion */
public class NodeRepository extends Repository {
    public static Descriptor probe(Environment environment, String repository, FileNode checkout) throws IOException {
        NodeRepository repo;
        BlockingQueue<Descriptor> dest;

        repo = new NodeRepository(environment, repository, checkout, environment.console().verbose);
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

    public static NodeRepository create(Environment environment, String name, String url, PrintWriter log) throws URISyntaxException, NodeInstantiationException {
        return new NodeRepository(environment, name, Find.fileOrNode(environment.world(), url), log);
    }

    //--

    private final Environment environment;
    private final PrintWriter log;
    private final Node root;
    private boolean branches;
    private boolean tags;
    private final Filter exclude;

    public NodeRepository(Environment environment, String name, Node root, PrintWriter log) {
        this(environment, name, root, false, false, log);
    }

    public NodeRepository(Environment environment, String name, Node root, boolean branches, boolean tags, PrintWriter log) {
        super(name);
        this.environment = environment;
        this.root = root;
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
    public void scan(BlockingQueue<Descriptor> dest, Console console) throws IOException, InterruptedException {
        scan(root, true, dest);
    }

    public void scan(Node<?> directory, boolean recurse, BlockingQueue<Descriptor> dest) throws IOException, InterruptedException {
        List<? extends Node> children;
        Node<?> trunkNode;
        Node<?> branchesNode;
        Node<?> tagsNode;
        List<? extends Node> grandChildren;

        if (exclude.matches(directory.getPath())) {
            return;
        }
        children = directory.list();
        if (children == null) {
            return;
        }
        log.println("scan " + directory.getPath());
        for (Node<?> child : children) {
            Descriptor.Creator m = Descriptor.match(child.getName());
            if (m != null) {
                dest.put(m.create(environment, child, this.name, child.getPath(), Long.toString(child.getLastModified()), nodeScm(child)));
                return;
            }
        }
        if (directory instanceof FileNode fileNode) {
            Descriptor descriptor = RawDescriptor.createOpt(name, fileNode);
            if (descriptor != null) {
                dest.put(descriptor);
                return;
            }
        }
        if (directory instanceof SvnNode) {
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

    public static ScmUrl nodeScm(Node descriptor) throws IOException {
        String path;
        SVNURL root;

        if (descriptor instanceof SvnNode svn) {
            // TODO: I currently use svn to crawl github ...
            // sushi path is only trunk/pom.xml - i need svnkit path instead
            root = svn.getRoot().getRepository().getLocation();
            if (root.getHost().equals("github.com")) {
                path = root.getPath();
                if (!path.startsWith("/")) {
                    throw new IllegalStateException(path);
                }
                return GitUrl.create("ssh://git@github.com" + path + ".git");
            } else {
                return new SubversionUrl(svn.getParent().getUri().toString());
            }
        } else if (descriptor instanceof FileNode fileNode) {
            FileNode directory = fileNode.getParent();
            Scm scm = Scm.probeCheckout(directory);
            if (scm == null) {
                return null;
            } else {
                return scm.getUrl(directory);
            }
        } else {
            return null;
        }
    }
}
