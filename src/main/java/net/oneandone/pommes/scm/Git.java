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

import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class Git extends Scm<GitUrl> {
    public record UP(String username, String password) {
    }
    public Git() {
        super("git:", GitUrl::create);
    }

    public UP getCredentials(Console console, FileNode dir, String host) throws IOException {
        StringWriter out = new StringWriter();
        StringReader in = new StringReader("protocol=https\n"
          + "host=" + host + "\n"
          + "path=/\n\n");
        try {
            git(dir, "credential", "fill").exec(out, out, true, in, false);
        } catch(Failure e) {
            console.error.println(out);
            throw e;
        }
        String[] lines = out.toString().split("\n");
        Map<String, String> map = new HashMap<>();
        for (String line : lines) {
            if (!line.isBlank()) {
                var idx = line.indexOf('=');
                if (idx < 0) {
                    console.error.println(line);
                }
                var key = line.substring(0, idx).trim();
                var old = map.put(key, line.substring(idx + 1).trim());
                if (old != null) {
                    throw new IOException("duplicate key: " + key);
                }
            }
        }
        UP result = new UP(map.get("username"), map.get("password"));
        if (result.username == null) {
            throw new IOException("missing username");
        }
        if (result.password == null) {
            throw new IOException("missing password");
        }
        return result;
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
    public Launcher checkout(GitUrl url, FileNode directory) {
        return git(directory.getParent(), "clone", url.url(), directory.getName());
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
