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

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Scm<U extends ScmUrl> {
    public static final Git GIT = new Git();
    public static final Subversion SUBVERSION = new Subversion();

    private static final Scm[] SCMS = { GIT, SUBVERSION };

    //--

    private final String protocol;
    private final UrlParser<U> parser;
    @FunctionalInterface
    public interface UrlParser<U> {
        U parse(String url) throws ScmUrlException;
    }

    protected Scm(String protocol, UrlParser<U> parser) {
        if (!protocol.endsWith(":")) {
            throw new IllegalArgumentException(protocol);
        }
        this.protocol = protocol;
        this.parser = parser;
    }

    public String protocol() {
        return protocol;
    }

    //--

    public abstract U getUrl(FileNode checkout) throws IOException;

    /** directory for checkouts */
    public abstract boolean isCheckout(FileNode directory) throws IOException;
    public abstract boolean isAlive(FileNode checkout) throws IOException;
    public abstract boolean isModified(FileNode checkout) throws IOException;
    public abstract Launcher checkout(U url, FileNode dest) throws Failure;

    //--

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

    public static ScmUrl createUrl(String url) throws ScmUrlException {
        for (Scm<?> scm : SCMS) {
            if (url.startsWith(scm.protocol)) {
                return scm.parser.parse(url.substring(scm.protocol.length()));
            }
        }
        throw new ScmUrlException(url,  "unknown scm scheme");
    }
    public static ScmUrl createValidUrl(String url) {
        try {
            return createUrl(url);
        } catch (ScmUrlException e) {
            throw new IllegalStateException(url, e);
        }
    }
}
