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
package net.oneandone.pommes.scm;

import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Scm {
    private static final Scm[] SCMS = {
        new Subversion(), new Git()
    };

    public static Map<FileNode, Scm> scanCheckouts(FileNode directory) throws IOException {
        Map<FileNode, Scm> result;

        result = new LinkedHashMap<>();
        scanCheckouts(directory, result);
        return result;
    }

    private static void scanCheckouts(FileNode directory, Map<FileNode, Scm> result) throws IOException {
        List<FileNode> children;
        Scm probed;

        probed = probeCheckout(directory);
        if (probed != null) {
            result.put(directory, probed);
        } else {
            children = directory.list();
            if (children != null) {
                for (FileNode child : children) {
                    if (!child.getName().startsWith(".")) {
                        scanCheckouts(child, result);
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

    public static Scm probeUrl(String url) throws IOException {
        for (Scm scm : SCMS) {
            if (scm.isUrl(url)) {
                return scm;
            }
        }
        return null;
    }


    //--

    public abstract boolean isUrl(String url);
    public abstract String server(String url) throws URISyntaxException;
    public abstract boolean isCheckout(FileNode directory) throws IOException;
    public abstract boolean isAlive(FileNode checkout) throws IOException;
    public abstract boolean isCommitted(FileNode checkout) throws IOException;
    public abstract String getUrl(FileNode checkout) throws Failure;

    public abstract Launcher checkout(FileNode dest, String url) throws Failure;

}
