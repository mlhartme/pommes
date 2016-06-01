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
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.mount.Point;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.DirectoryNotFoundException;
import net.oneandone.sushi.fs.ListException;
import net.oneandone.sushi.fs.file.FileNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ls extends Base {
    private final FileNode root;

    public Ls(Environment environment, FileNode root) {
        super(environment);
        this.root = root;
    }

    @Override
    public void run(Database notUsed) throws Exception {
        Map<FileNode, Scm> checkouts;
        Point point;
        FileNode found;
        FileNode configured;
        Pom foundPom;
        Scm scm;

        checkouts = Scm.scanCheckouts(root);
        if (checkouts.isEmpty()) {
            throw new ArgumentException("no checkouts under " + root);
        }
        for (Map.Entry<FileNode, Scm> entry : checkouts.entrySet()) {
            found = entry.getKey();
            scm = entry.getValue();
            foundPom = environment.scanPom(found);
            point = environment.mount();
            configured = point.directory(foundPom);
            if (found.equals(configured)) {
                console.info.println((scm.isCommitted(found) ? ' ' : 'M') + " " + found + " (" + foundPom + ")");
            } else {
                console.info.println("C " + found + " vs " + configured + " (" + foundPom + ")");
            }
        }
        for (FileNode directory : unknown(root, checkouts.keySet())) {
            console.info.println("? " + directory);
        }
    }

    private static List<FileNode> unknown(FileNode root, Collection<FileNode> directories) throws ListException, DirectoryNotFoundException {
        List<FileNode> lst;
        List<FileNode> result;

        result = new ArrayList<>();
        lst = new ArrayList<>();
        for (FileNode directory: directories) {
            candicates(root, directory, lst);
        }
        for (FileNode directory : lst) {
            for (FileNode node : directory.list()) {
                if (node.isFile()) {
                    // ignore
                } else if (hasAnchestor(directories, node)) {
                    // required sub-directory
                } else {
                    result.add(node);
                }
            }
        }
        return result;
    }

    private static boolean hasAnchestor(Collection<FileNode> directories, FileNode node) {
        for (FileNode directory : directories) {
            if (directory.hasAnchestor(node)) {
                return true;
            }
        }
        return false;
    }

    private static void candicates(FileNode root, FileNode directory, List<FileNode> result) {
        FileNode parent;

        if (directory.hasDifferentAnchestor(root)) {
            parent = directory.getParent();
            if (!result.contains(parent)) {
                result.add(parent);
                candicates(root, parent, result);
            }
        }
    }

}
