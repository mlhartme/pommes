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

public class Git extends Scm<GitUrl> {
    public Git() {
        super("git:");
    }

    public GitUrl parseUrl(String url) {
        return GitUrl.create(url);
    }

    public boolean isCheckout(FileNode directory) {
        return directory.join(".git").isDirectory();
    }

    @Override
    public GitUrl getUrl(FileNode checkout) throws IOException {
        Launcher launcher;

        launcher = git(checkout, "config", "--get", "remote.origin.url");
        try {
            return GitUrl.create(launcher.exec().trim());
            // TODO return PROTOCOL + explicitSshProtocol(launcher.exec().trim());
        } catch (Failure e) {
            throw new IOException(launcher + " failed: " + e.getMessage(), e);
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

        url = Strings.removeLeft(fullurl, protocol());
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
