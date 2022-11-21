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
package net.oneandone.pommes.scm;

import net.oneandone.pommes.database.Gav;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Scm {
    private static final Scm[] SCMS = {
        new Subversion(), new Git()
    };

    public static Map<FileNode, Scm> scanCheckouts(FileNode directory, Filter excludes) throws IOException {
        Map<FileNode, Scm> result;

        result = new LinkedHashMap<>();
        scanCheckouts(directory, directory, excludes, result);
        return result;
    }

    private static void scanCheckouts(FileNode root, FileNode directory, Filter excludes, Map<FileNode, Scm> result) throws IOException {
        List<FileNode> children;
        Scm probed;

        if (excludes.matches(directory.getRelative(root))) {
            return;
        }
        probed = probeCheckout(directory);
        if (probed != null) {
            result.put(directory, probed);
        } else {
            children = directory.list();
            if (children != null) {
                Collections.sort(children, Comparator.comparing(Node::getName));
                for (FileNode child : children) {
                    if (!child.getName().startsWith(".")) {
                        scanCheckouts(root, child, excludes, result);
                    }
                }
            }
        }
    }

    public static Scm probeCheckout(FileNode checkout) throws IOException {
        for (Scm scm : SCMS) {
            if (scm.isCheckout(checkout)) {
                return scm;
            }
        }
        return null;
    }

    public static Scm probeUrl(String url) {
        for (Scm scm : SCMS) {
            if (scm.isUrl(url)) {
                return scm;
            }
        }
        return null;
    }


    //--

    public abstract boolean isUrl(String url);
    public abstract String path(String url) throws URISyntaxException;
    public abstract boolean isCheckout(FileNode directory) throws IOException;
    public abstract boolean isAlive(FileNode checkout) throws IOException;
    public abstract boolean isModified(FileNode checkout) throws IOException;
    public abstract String getUrl(FileNode checkout) throws IOException;

    public abstract Gav defaultGav(String url) throws Failure, URISyntaxException;

    public abstract Launcher checkout(FileNode dest, String url) throws Failure;

}
