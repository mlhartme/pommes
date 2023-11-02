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
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Separator;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/** urls are normalized by removing the tailing slash. */
public class Subversion extends Scm<SubversionUrl> {
    public Subversion() {
        super("svn:");
    }

    public boolean isCheckout(FileNode directory) {
        return directory.join(".svn").isDirectory();
    }

    @Override
    public SubversionUrl parseUrl(String url) {
        try {
            return new SubversionUrl(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(url, e);
        }
    }

    @Override
    public Gav defaultGav(String urlstr) throws URISyntaxException {
        String path;
        int idx;
        String groupId;
        String artifactId;

        path = new URI(Strings.removeLeft(urlstr, protocol())).getPath();
        path = Strings.removeRightOpt(path, "/");
        path = Strings.removeLeftOpt(path, "/svn");
        path = Strings.removeLeftOpt(path, "/");
        idx = path.indexOf("/trunk");
        if (idx == -1) {
            idx = path.indexOf("/branches");
        }
        if (idx != -1) {
            path = path.substring(0, idx);
        }
        idx = path.lastIndexOf('/');
        if (idx == -1) {
            groupId = "";
            artifactId = path;
        } else {
            groupId = path.substring(0, idx).replace('/', '.');
            artifactId = path.substring(idx + 1);
        }
        return new Gav(groupId, artifactId, "1-SNAPSHOT");
    }

    @Override
    public SubversionUrl getUrl(FileNode checkout) throws Failure {
        String url;
        int idx;
        String str;

        url = checkout.launcher("svn", "info").exec();
        idx = url.indexOf("URL: ") + 5;
        str = (Strings.removeRightOpt(url.substring(idx, url.indexOf("\n", idx)), "/"));
        try {
            return new SubversionUrl(str);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(str, e);
        }
    }

    @Override
    public Launcher checkout(FileNode directory, String fullurl) throws Failure {
        String url;

        url = Strings.removeLeft(fullurl, protocol());
        return Subversion.svn(directory.getParent(), "co", url, directory.getName());
    }

    @Override
    public boolean isAlive(FileNode checkout) {
        try {
            svn(checkout, "status", "--show-updates").exec();
            return true;
        } catch (Failure e) {
            // TODO: could also be a network error ...
            return false;
        }
    }

    @Override
    public boolean isModified(FileNode directory) throws IOException {
        return isModified(directory.exec("svn", "status"));
    }

    private static boolean isModified(String lines) {
        for (String line : Separator.on("\n").split(lines)) {
            if (line.trim().length() > 0) {
                if (line.startsWith("X") || line.startsWith("Performing status on external item")) {
                    // needed for pws workspace svn:externals  -> ok
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private static Launcher svn(FileNode dir, String... args) {
        Launcher launcher;

        launcher = new Launcher(dir);
        launcher.arg("svn");
        launcher.arg("--non-interactive");
        launcher.arg("--trust-server-cert"); // needs svn >= 1.6
        launcher.arg(args);
        return launcher;
    }
}
