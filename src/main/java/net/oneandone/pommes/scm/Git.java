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
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Git extends Scm {
    private static final String PROTOCOL = "git:";

    public Git() {
    }

    public boolean isCheckout(FileNode directory) {
        return directory.join(".git").isDirectory();
    }

    public boolean isUrl(String url) {
        return url.startsWith(PROTOCOL);
    }

    public String path(String url) throws URISyntaxException {
        String result;
        int idx;
        URI obj;

        url = Strings.removeLeft(url, PROTOCOL);
        idx = url.indexOf("://");
        if (idx != -1) {
            idx += 3;
        } else {
            idx = url.indexOf('@');
            if (idx != -1) {
                idx++;
            }
        }
        if (idx != -1) {
            url = url.substring(idx);
            idx = url.indexOf(':');
            if (idx != -1) {
                return url.substring(0, idx) + "/" + Strings.removeRightOpt(url.substring(idx + 1), ".git");
            }
            idx = url.indexOf('@');
            if (idx != -1) {
                url = url.substring(idx + 1);
            }
            idx = url.indexOf('/');
            if (idx == -1) {
                throw new IllegalStateException(url);
            }
            return Strings.removeRightOpt(url, ".git");
        } else {
            obj = new URI(url);
            result = obj.getHost();
            if (result == null) {
                throw new IllegalStateException(url);
            }
            result = result + obj.getPath();
            return result;
        }
    }

    @Override
    public String getUrl(FileNode checkout) throws IOException {
        Launcher launcher;

        launcher = git(checkout, "config", "--get", "remote.origin.url");
        try {
            return PROTOCOL + withProtocol(launcher.exec().trim());
        } catch (Failure e) {
            throw new IOException(launcher + " failed: " + e.getMessage(), e);
        }
    }

    private static String withProtocol(String url) {
        int idx;

        idx = url.indexOf("://");
        if (idx < 0) {
            // turn scp-like syntax into explicit ssh syntax -- see https://git-scm.com/book/en/v2/Git-on-the-Server-The-Protocols
            idx = url.indexOf(':');
            if (idx < 0) {
                throw new IllegalStateException(url);
            }
            return "ssh://" + url.substring(0, idx) + "/" + url.substring(idx + 1);
        } else {
            return url;
        }
    }

    @Override
    public Gav defaultGav(String url) {
        String artifactId;
        String groupId;
        int idx;

        idx = url.lastIndexOf('/');
        if (idx == -1) {
            throw new IllegalArgumentException(url);
        }
        groupId = url.substring(0, idx);
        artifactId = url.substring(idx + 1);
        idx = artifactId.lastIndexOf('.');
        if (idx != -1) {
            artifactId = artifactId.substring(0, idx);
        }
        idx = Math.max(groupId.lastIndexOf(':'), groupId.lastIndexOf('/'));
        if (idx != -1) {
            groupId = groupId.substring(idx + 1);
        }
        return new Gav(groupId, artifactId, "1-SNAPSHOT");
    }

    @Override
    public Launcher checkout(FileNode directory, String fullurl) {
        String url;

        url = Strings.removeLeft(fullurl, PROTOCOL);
        return git(directory.getParent(), "clone", url, directory.getName());
    }

    @Override
    public boolean isAlive(FileNode checkout) {
        try {
            git(checkout, "fetch", "--dry-run").exec();
            return true;
        } catch (Failure e) {
            // TODO: detect other failures
            return false;
        }
    }

    @Override
    public boolean isModified(FileNode checkout) {
        try {
            git(checkout, "diff", "--quiet").execNoOutput();
        } catch (Failure e) {
            return true;
        }
        try {
            git(checkout, "diff", "--cached", "--quiet").execNoOutput();
        } catch (Failure e) {
            return true;
        }

        try {
            git(checkout, "diff", "@{u}..HEAD", "--quiet").execNoOutput();
        } catch (Failure e) {
            return true;
        }
        // TODO: other branches
        // TODO: stashes
        return false;
    }

    private static Launcher git(FileNode dir, String... args) {
        Launcher launcher;

        launcher = new Launcher(dir);
        launcher.arg("git");
        launcher.arg(args);
        return launcher;
    }
}
