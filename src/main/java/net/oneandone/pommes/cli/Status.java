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
package net.oneandone.pommes.cli;

import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.database.Database;
import net.oneandone.pommes.database.Field;
import net.oneandone.pommes.database.Pom;
import net.oneandone.pommes.database.PommesQuery;
import net.oneandone.pommes.checkout.Root;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.DirectoryNotFoundException;
import net.oneandone.sushi.fs.ListException;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.util.Strings;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Status extends Base {
    private final FileNode directory;

    public Status(Environment environment, FileNode directory) {
        super(environment);
        this.directory = directory;
    }

    @Override
    public void run(Database database) throws Exception {
        Map<FileNode, Scm> checkouts;
        Root root;
        FileNode found;
        FileNode expected;
        Pom foundPom;
        Scm scm;
        String fix;
        FileNode parent;

        checkouts = Scm.scanCheckouts(directory, environment.excludes());
        if (checkouts.isEmpty()) {
            throw new ArgumentException("no checkouts in " + directory);
        }
        for (Map.Entry<FileNode, Scm> entry : checkouts.entrySet()) {
            found = entry.getKey();
            scm = entry.getValue();
            foundPom = pomByScm(database, scm.getUrl(found));
            if (foundPom == null) {
                console.info.println("? " + found + " (unknown project, fix with '" + unknownProjectFix(scm, found) + "')");
            } else {
                root = environment.home.root();
                try {
                    expected = root.directory(foundPom);
                } catch (IOException e) {
                    console.info.println("! " + found + " " + e.getMessage());
                    continue;
                }
                if (found.equals(expected)) {
                    console.info.println((scm.isCommitted(found) ? ' ' : 'M') + " " + found + " (" + foundPom + ")");
                } else {
                    parent = expected.getParent();
                    fix = parent.exists() ? "" : "mkdir -p " + parent.toString() + "; ";
                    console.info.println("C " + found + " (unexpected location, fix with '" + fix + "mv " + found + " " + expected + "'}");
                }
            }
        }
        for (FileNode u : unknown(directory, checkouts.keySet(), environment.excludes())) {
            console.info.println("? " + u + " (normal directory)");
        }
    }

    private Pom pomByScm(Database database, String url) throws IOException {
        List<Document> poms;

        // TODO: to normalize subversion urls
        url = Strings.removeRightOpt(url, "/");
        try {
            poms = database.query(PommesQuery.create("s:" + url));
        } catch (QueryNodeException e) {
            throw new IllegalStateException();
        }
        switch (poms.size()) {
            case 0:
                return null;
            case 1:
                return Field.pom(poms.get(0));
            default:
                throw new IOException("scm ambiguous: " + url);
        }
    }

    private String unknownProjectFix(Scm scm, FileNode checkout) throws Failure, URISyntaxException {
        String scmurl;
        FileNode descriptor;
        Pom pom;
        String result;

        scmurl = scm.getUrl(checkout);
        descriptor = checkout.join("external.json");
        pom = new Pom("manual", descriptor.getUri().toString(), "localfile", null, scm.defaultGav(scmurl), scmurl,null);
        result = pom.toJson().toString();
        return "pommes database-add 'inline:" + result + "'";
    }

    private static List<FileNode> unknown(FileNode root, Collection<FileNode> directories, Filter excludes) throws ListException, DirectoryNotFoundException {
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
                } else if (excludes.matches(node.getRelative(root))) {
                    // excluded
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
